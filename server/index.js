const express = require("express");
const cors = require("cors");
const multer = require("multer");
const path = require("path");
const fs = require("fs");
const { spawn } = require("child_process");

const app = express();
const PORT = 3000;

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

// Configure Multer for temporary storage in chat/temp
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, path.join(storageDir, "chat", "temp"));
  },
  filename: (req, file, cb) => {
    const isThumb = file.originalname.startsWith("thumb_");
    const prefix = isThumb ? "" : "media-";
    const ext = path.extname(file.originalname) || ".mp4";
    const uniqueSuffix = Date.now() + "-" + Math.round(Math.random() * 1e9);
    if (isThumb) {
      // Ensure it retains thumb_ prefix correctly
      const cleanName = file.originalname.replace(/\.[^/.]+$/, ""); // strip extension
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

// 2. CDN Status Endpoint (Required by Frontend)
app.get("/cdn-status", (req, res) => {
  res.json({
    active: true,
    url: currentUrl
  });
});

// 3. Manual/External Update Endpoint
app.get("/update-url", (req, res) => {
  const url = req.query.url;
  if (url) {
    currentUrl = url.replace(/\/$/, ""); // Remove trailing slash
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

  // Classify target folder
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

  // Move file and delete from temp
  try {
    fs.copyFileSync(tempPath, finalDest);
    fs.unlinkSync(tempPath);
  } catch (err) {
    console.error("Error al mover archivo de temp a destino final:", err);
    return res.status(500).json({ error: "Error al procesar el almacenamiento final del archivo" });
  }

  // Construct dynamic URL using current active CDN domain
  const mediaUrl = `${currentUrl}/video/${filename}`;

  console.log(`📥 Archivo subido con éxito: ${filename} (guardado en ${targetFolder})`);
  console.log(`🔗 URL generada con CDN: ${mediaUrl}`);

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

// 5. Get List of Files
app.get("/files", (req, res) => {
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
              url: `${currentUrl}/video/${file}`
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

// 6. Video Stream with Full range-request support (crucial for ExoPlayer playback!)
app.get("/video/:id", (req, res) => {
  const fileId = req.params.id;
  const subdirs = ["audio", "chat/temp", "documents", "images", "stickers", "videos"];
  let filePath = null;

  for (const s of subdirs) {
    const tempPath = path.join(storageDir, s, fileId);
    if (fs.existsSync(tempPath)) {
      filePath = tempPath;
      break;
    }
  }

  if (!filePath) {
    const rootPath = path.join(storageDir, fileId);
    if (fs.existsSync(rootPath)) {
      filePath = rootPath;
    }
  }

  if (!filePath) {
    return res.status(404).send("Error: Archivo no encontrado");
  }

  // res.sendFile handles byte range requests automatically and perfectly
  res.sendFile(filePath);
});

// 7. Delete File from CDN (Required for Borrado Total)
app.delete("/delete/:id", (req, res) => {
  const fileId = req.params.id;
  const subdirs = ["audio", "chat/temp", "documents", "images", "stickers", "videos"];
  let filePath = null;

  for (const s of subdirs) {
    const tempPath = path.join(storageDir, s, fileId);
    if (fs.existsSync(tempPath)) {
      filePath = tempPath;
      break;
    }
  }

  if (!filePath) {
    const rootPath = path.join(storageDir, fileId);
    if (fs.existsSync(rootPath)) {
      filePath = rootPath;
    }
  }

  if (!filePath) {
    return res.status(404).json({ error: "Archivo no encontrado" });
  }

  try {
    fs.unlinkSync(filePath);
    console.log(`🗑️ Archivo eliminado físicamente del CDN: ${fileId}`);
    res.json({ success: true, message: "Archivo eliminado exitosamente del CDN" });
  } catch (err) {
    console.error(`Error al eliminar archivo del CDN:`, err);
    res.status(500).json({ error: "No se pudo eliminar el archivo del servidor CDN" });
  }
});

// --- CLOUDFLARED AUTO-TUNNEL INITIATION ---
function startCloudflaredTunnel() {
  console.log("⚡ Iniciando túnel de cloudflared como subproceso...");

  // Spawns cloudflared to route port 3000 to the web
  const tunnel = spawn("cloudflared", ["tunnel", "--url", `http://localhost:${PORT}`]);

  // Read stderr as cloudflared logs output there by default
  const handleOutput = (data) => {
    const text = data.toString();
    
    // Search for trycloudflare URL using regex
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
    console.log("Consejo: Asegúrate de tener 'cloudflared' instalado en tu path.");
    console.log(`El servidor Express sigue funcionando en http://localhost:${PORT}`);
    console.log("Puedes actualizar la URL manualmente consultando /update-url?url=<su_url>\n");
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

const socketEvents = require("./socket/events");
io.on("connection", (socket) => {
  console.log(`🔌 Nuevo socket conectado: ${socket.id}`);
  socketEvents(io, socket);
});

// Start Server
server.listen(PORT, () => {
  console.log(`🚀 Servidor Express y Socket.IO iniciado en el puerto ${PORT}`);
  startCloudflaredTunnel();
});
