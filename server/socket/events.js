const users = require("./users");

module.exports = (io, socket) => {
  const authenticatedUserId = socket.data.userId;

  // Auto-register connected user mapping using authenticated JWT identity
  if (authenticatedUserId) {
    users.registerUser(authenticatedUserId, socket.id);
    console.log(`👤 Usuario registrado automáticamente por JWT: ${authenticatedUserId} -> socket: ${socket.id}`);
  }

  // Register connected user mapping (ignoring client-supplied spoofed userId)
  socket.on("register-user", (clientProvidedId) => {
    const targetUserId = authenticatedUserId || clientProvidedId;
    if (targetUserId) {
      users.registerUser(targetUserId, socket.id);
      console.log(`👤 Evento register-user procesado con ID autenticado: ${targetUserId} -> socket: ${socket.id}`);
    }
  });

  // Helper function to safely process and forward signaling events
  const forwardSignaling = (eventName, data, targetEventName = eventName) => {
    if (!data || typeof data !== "object") return;

    const receiverId = data.receiverId || data.targetUserId;
    // Overwrite sender identity with authenticated user ID
    data.senderId = authenticatedUserId;
    if (data.callerId) {
      data.callerId = authenticatedUserId;
    }

    console.log(`📞 [${eventName}] de ${authenticatedUserId} para ${receiverId}`);
    if (receiverId) {
      const targetSocketId = users.getUserSocket(receiverId);
      if (targetSocketId) {
        io.to(targetSocketId).emit(targetEventName, data);
      } else {
        console.log(`⚠️ Receptor ${receiverId} no conectado para el evento ${eventName}`);
      }
    }
  };

  // 1. call_offer
  socket.on("call_offer", (data) => {
    forwardSignaling("call_offer", data, "incoming_call");
  });

  // 2. call_answer
  socket.on("call_answer", (data) => {
    forwardSignaling("call_answer", data, "call_answer");
  });

  // 3. ice_candidate
  socket.on("ice_candidate", (data) => {
    forwardSignaling("ice_candidate", data, "ice_candidate");
  });

  // 4. call_reject
  socket.on("call_reject", (data) => {
    forwardSignaling("call_reject", data, "call_reject");
  });

  // 5. call_end
  socket.on("call_end", (data) => {
    forwardSignaling("call_end", data, "call_end");
  });

  // 6. call_busy
  socket.on("call_busy", (data) => {
    forwardSignaling("call_busy", data, "call_busy");
  });

  // Placeholder event handlers
  socket.on("user_online", (data) => {
    console.log(`user_online for user ${authenticatedUserId}`);
  });

  socket.on("typing", (data) => {
    console.log(`typing from user ${authenticatedUserId}`);
  });

  socket.on("message_seen", (data) => {
    console.log(`message_seen from user ${authenticatedUserId}`);
  });

  socket.on("ping_server", (data, callback) => {
    if (typeof callback === "function") {
      callback({ status: "pong", userId: authenticatedUserId });
    }
  });

  socket.on("disconnect", () => {
    users.removeUser(socket.id);
    console.log(`🔌 Socket desconectado: ${socket.id} (usuario: ${authenticatedUserId})`);
  });
};

