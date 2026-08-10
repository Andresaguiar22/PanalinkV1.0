const express = require("express");
const cors = require("cors");
const multer = require("multer");
const path = require("path");
const fs = require("fs");
const crypto = require("crypto");
const jwt = require("jsonwebtoken");
const { spawn } = require("child_process");

const app = express();
const PORT = process.env.PORT || 3000;

// Centralized single source of truth for dynamic CDN URL
let currentUrl = (process.env.APP_URL || "http://10.0.2.2:3000").replace(/\/$/, "");

// Ensure PanalinkStorage directories exist
const storageDir = path.join(__dirname, "PanalinkStorage");
const foldersList = ["audio", "chat/temp", "documents", "images", "stickers", "videos"];
foldersList.forEach(f => {
  const dir = path.join(storageDir, f);
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
  }
});

// Enable CORS and JSON parsing
app.use(cors());
app.use(express.json());

// --- MIDDLEWARE: CDN AUTHENTICATION ---
function authCdnMiddleware(req, res, next) {
  const expectedToken = process.env.CDN_API_TOKEN;
  if (!expectedToken) {
    console.error("❌ CDN_API_TOKEN no está configurado en las variables de entorno del servidor.");
    return res.status(500).json({ error: "Error de configuración de seguridad en el servidor CDN" });
  }

  let clientToken = null;
  const authHeader = req.headers.authorization || req.headers["x-api-token"] || req.headers["x-cdn-token"];

  if (authHeader && typeof authHeader === "string") {
    if (authHeader.startsWith("Bearer ")) {
      clientToken = authHeader.substring(7).trim();
    } else {
      clientToken = authHeader.trim();
    }
  } else if (req.query && (req.query.token || req.query.api_token)) {
    const qToken = req.query.token || req.query.api_token;
    if (typeof qToken === "string") {
      clientToken = qToken.trim();
    }
  }

  if (!clientToken) {
    return res.status(401).json({ error: "Acceso denegado: Credencial ausente" });
  }

  // Constant-time comparison to prevent timing attacks
  const clientBuf = Buffer.from(clientToken);
  const expectedBuf = Buffer.from(expectedToken);

  if (clientBuf.length !== expectedBuf.length || !crypto.timingSafeEqual(clientBuf, expectedBuf)) {
    return res.status(403).json({ error: "Acceso denegado: Credencial incorrecta o malformada" });
  }

  next();
}

// --- HELPER: SAFE PATH RESOLUTION (PATH TRAVERSAL PROTECTION) ---
function safeResolvePath(baseDir, fileParam, allowedSubdirs = []) {
  if (!fileParam || typeof fileParam !== "string") {
    return { error: "Parámetro inválido", path: null };
  }

  let decoded = fileParam;
  try {
    decoded = decodeURIComponent(fileParam);
  } catch (e) {
    return { error: "URI malformada", path: null };
  }

  // Detect path traversal signatures
  const hasTraversal = /(\.\.|\/|\\|%2f|%5c|%2e)/i.test(fileParam) || /(\.\.|\/|\\)/.test(decoded);
  const filename = path.basename(decoded);

  if (hasTraversal || filename !== decoded || decoded.includes("..") || decoded.includes("\0")) {
    return { error: "Intento de Path Traversal bloqueado", path: null, isTraversal: true };
  }

  const normalizedBase = path.resolve(baseDir);

  for (const sub of [...allowedSubdirs, ""]) {
    const candidate = path.resolve(normalizedBase, sub, filename);
    if (candidate.startsWith(normalizedBase + path.sep)) {
      if (fs.existsSync(candidate) && fs.statSync(candidate).isFile()) {
        return { error: null, path: candidate };
      }
    }
  }

  return { error: "Archivo no encontrado", path: null };
}

// Configure Multer for temporary storage in chat/temp
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, path.join(storageDir, "chat", "temp"));
  },
  filename: (req, file, cb) => {
    const safeOriginalName = path.basename(file.originalname).replace(/[^a-zA-Z0-9_\.-]/g, "_");
    const isThumb = safeOriginalName.startsWith("thumb_");
    const prefix = isThumb ? "" : "media-";
    const ext = path.extname(safeOriginalName) || ".mp4";
    const uniqueSuffix = Date.now() + "-" + Math.round(Math.random() * 1e9);

    if (isThumb) {
      const cleanName = safeOriginalName.replace(/\.[^/.]+$/, "");
      cb(null, `${cleanName}_${uniqueSuffix}${ext}`);
    } else {
      cb(null, `${prefix}${uniqueSuffix}${ext}`);
    }
  }
});
const upload = multer({ storage });

// --- ENDPOINTS ---

// 1. Root: Status & Navigation info
app.get("/", (req, res) => {
  res.json({
    status: "online",
    message: "Servidor de Control CDN para Panalink 🇻🇪",
    cdn: {
      active: true,
      currentUrl: currentUrl
    },
    endpoints: {
      status: "GET /cdn-status",
      updateUrl: "GET /update-url?url=<nueva_url>",
      upload: "POST /upload",
      files: "GET /files",
      video: "GET /video/:id"
    }
  });
});

// 2. CDN Status Endpoint
app.get("/cdn-status", (req, res) => {
  res.json({
    active: true,
    url: currentUrl
  });
});

// 3. Manual/External Update Endpoint (Protected by CDN_API_TOKEN)
app.get("/update-url", authCdnMiddleware, (req, res) => {
  const url = req.query.url;
  if (url) {
    currentUrl = url.replace(/\/$/, "");
    console.log("=========================================");
    console.log("🟢 CDN Actualizado Manualmente:", currentUrl);
    console.log("=========================================");
    res.send(`OK - CDN actualizado a: ${currentUrl}`);
  } else {
    res.status(400).send("ERROR: Falta el parámetro 'url'");
  }
});

// 4. File Upload (Required by Android UploadRepository)
app.post("/upload", upload.single("mediaFile"), (req, res) => {
  if (!req.file) {
    return res.status(400).json({ error: "No se recibió ningún archivo" });
  }

  const tempPath = req.file.path;
  const filename = req.file.filename;
  const mimetype = req.file.mimetype || "";
  const originalname = req.file.originalname || "";

  let targetFolder = "documents";
  const ext = path.extname(originalname).toLowerCase();

  if (filename.startsWith("thumb_") || originalname.startsWith("thumb_")) {
    targetFolder = "images";
  } else if (req.body.type === "sticker" || req.query.type === "sticker" || mimetype.includes("sticker")) {
    targetFolder = "stickers";
  } else if (mimetype.startsWith("image/")) {
    targetFolder = "images";
  } else if (mimetype.startsWith("video/")) {
    targetFolder = "videos";
  } else if (mimetype.startsWith("audio/")) {
    targetFolder = "audio";
  } else if (
    mimetype.startsWith("application/pdf") ||
    mimetype.includes("word") ||
    mimetype.includes("excel") ||
    mimetype.includes("sheet") ||
    mimetype.includes("zip") ||
    [".pdf", ".doc", ".docx", ".xls", ".xlsx", ".zip", ".rar", ".txt"].includes(ext)
  ) {
    targetFolder = "documents";
  } else {
    targetFolder = "documents";
  }

  const finalDest = path.join(storageDir, targetFolder, filename);

  try {
    fs.copyFileSync(tempPath, finalDest);
    fs.unlinkSync(tempPath);
  } catch (err) {
    console.error("Error al mover archivo de temp a destino final:", err);
    return res.status(500).json({ error: "Error al procesar el almacenamiento final del archivo" });
  }

  const mediaUrl = `${currentUrl}/video/${encodeURIComponent(filename)}`;

  console.log(`📥 Archivo subido con éxito: ${filename} (guardado en ${targetFolder})`);

  res.json({
    success: true,
    media_url: mediaUrl,
    url: mediaUrl,
    data: {
      media_url: mediaUrl,
      filename: filename,
      size: req.file.size,
      folder: targetFolder
    }
  });
});

// 5. Get List of Files (Protected by CDN_API_TOKEN)
app.get("/files", authCdnMiddleware, (req, res) => {
  const folders = ["audio", "documents", "images", "stickers", "videos"];
  const fileList = [];

  folders.forEach(f => {
    const dir = path.join(storageDir, f);
    if (fs.existsSync(dir)) {
      try {
        const files = fs.readdirSync(dir);
        files.forEach(file => {
          const filePath = path.join(dir, file);
          if (fs.statSync(filePath).isFile()) {
            fileList.push({
              filename: file,
              folder: f,
              url: `${currentUrl}/video/${encodeURIComponent(file)}`
            });
          }
        });
      } catch (e) {
        console.error(`Error al leer carpeta ${f}:`, e);
      }
    }
  });

  res.json(fileList);
});

// 6. Video Stream with Full range-request support & Path Traversal Protection
app.get("/video/:id", (req, res) => {
  const result = safeResolvePath(storageDir, req.params.id, ["audio", "chat/temp", "documents", "images", "stickers", "videos"]);

  if (result.isTraversal) {
    console.warn(`🚨 Intento de Path Traversal bloqueado en GET /video: ${req.params.id}`);
    return res.status(403).json({ error: "Acceso rechazado: Path Traversal detectado" });
  }

  if (!result.path) {
    return res.status(404).send("Error: Archivo no encontrado");
  }

  res.sendFile(result.path);
});

// 7. Delete File from CDN (Protected by CDN_API_TOKEN & Path Traversal Protection)
app.delete("/delete/:id", authCdnMiddleware, (req, res) => {
  const result = safeResolvePath(storageDir, req.params.id, ["audio", "chat/temp", "documents", "images", "stickers", "videos"]);

  if (result.isTraversal) {
    console.warn(`🚨 Intento de Path Traversal bloqueado en DELETE /delete: ${req.params.id}`);
    return res.status(403).json({ error: "Acceso rechazado: Path Traversal detectado" });
  }

  if (!result.path) {
    return res.status(404).json({ error: "Archivo no encontrado" });
  }

  try {
    fs.unlinkSync(result.path);
    console.log(`🗑️ Archivo eliminado físicamente del CDN: ${req.params.id}`);
    res.json({ success: true, message: "Archivo eliminado exitosamente del CDN" });
  } catch (err) {
    console.error(`Error al eliminar archivo del CDN:`, err);
    res.status(500).json({ error: "No se pudo eliminar el archivo del servidor CDN" });
  }
});

// --- CLOUDFLARED AUTO-TUNNEL INITIATION ---
function startCloudflaredTunnel() {
  console.log("⚡ Iniciando túnel de cloudflared como subproceso...");
  const tunnel = spawn("cloudflared", ["tunnel", "--url", `http://localhost:${PORT}`]);

  const handleOutput = (data) => {
    const text = data.toString();
    const match = text.match(/https:\/\/[a-z0-9-]+\.trycloudflare\.com/);
    if (match) {
      const detectedUrl = match[0];
      currentUrl = detectedUrl;
      console.log("\n=======================================================");
      console.log("🌐 ¡TÚNEL DE CLOUDFLARED INICIADO Y DETECTADO CON ÉXITO!");
      console.log("🔗 URL Pública Activa:", currentUrl);
      console.log(`👉 Consulta el estado en: http://localhost:${PORT}/cdn-status`);
      console.log("=======================================================\n");
    }
  };

  let spawnFailed = false;
  tunnel.stdout.on("data", handleOutput);
  tunnel.stderr.on("data", handleOutput);

  tunnel.on("close", (code) => {
    console.log(`⚠️ Proceso de cloudflared terminado con código: ${code}`);
    if (spawnFailed) {
      console.log("No se reintentará iniciar cloudflared porque el binario no existe en este entorno.");
      return;
    }
    console.log("Se intentará reiniciar en 10 segundos...");
    setTimeout(startCloudflaredTunnel, 10000);
  });

  tunnel.on("error", (err) => {
    if (err.code === "ENOENT") {
      spawnFailed = true;
    }
    console.warn("❌ No se pudo ejecutar cloudflared automáticamente:", err.message);
  });
}

// Integrate Socket.IO with existing Express server on the same port
const http = require("http");
const { Server } = require("socket.io");
const server = http.createServer(app);
const io = new Server(server, {
  cors: {
    origin: "*",
    methods: ["GET", "POST"]
  }
});

// --- SOCKET.IO JWT AUTHENTICATION MIDDLEWARE ---
io.use((socket, next) => {
  const secret = process.env.SOCKET_JWT_SECRET;
  if (!secret) {
    console.error("❌ SOCKET_JWT_SECRET no configurado en variables de entorno.");
    return next(new Error("Authentication error: Server secret not configured"));
  }

  const auth = socket.handshake.auth || {};
  let token = auth.token || auth.jwt || socket.handshake.headers?.authorization;

  if (typeof token === "string" && token.startsWith("Bearer ")) {
    token = token.substring(7).trim();
  }

  if (!token || typeof token !== "string") {
    console.warn(`❌ Intento de conexión Socket.IO rechazado: Token ausente (${socket.id})`);
    return next(new Error("Authentication error: Token missing"));
  }

  try {
    const decoded = jwt.verify(token, secret, {
      algorithms: ["HS256", "HS384", "HS512"]
    });

    const userId = decoded.sub || decoded.userId || decoded.id || decoded.user_id;

    if (!userId || typeof userId !== "string" || userId.trim() === "") {
      console.warn(`❌ Intento de conexión Socket.IO rechazado: Claim 'sub' o 'userId' inválido (${socket.id})`);
      return next(new Error("Authentication error: Invalid token claims (sub missing)"));
    }

    socket.data.userId = userId.trim();
    socket.data.userClaim = decoded;
    console.log(`🔑 Socket.IO autenticado con éxito: socket=${socket.id}, userId=${socket.data.userId}`);
    next();
  } catch (err) {
    console.warn(`❌ Intento de conexión Socket.IO rechazado: ${err.message} (${socket.id})`);
    if (err.name === "TokenExpiredError") {
      return next(new Error("Authentication error: Token expired"));
    }
    return next(new Error("Authentication error: Invalid token signature or format"));
  }
});

const socketEvents = require("./socket/events");
io.on("connection", (socket) => {
  console.log(`🔌 Nuevo socket conectado: ${socket.id} (usuario ${socket.data.userId})`);
  socketEvents(io, socket);
});

// Start Server
server.listen(PORT, () => {
  console.log(`🚀 Servidor Express y Socket.IO iniciado en el puerto ${PORT}`);
  startCloudflaredTunnel();
});

