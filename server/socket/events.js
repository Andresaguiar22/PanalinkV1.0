const users = require("./users");

module.exports = (io, socket) => {
  // Register connected user mapping
  socket.on("register-user", (userId) => {
    if (userId) {
      users.registerUser(userId, socket.id);
      console.log(`👤 Usuario registrado: ${userId} -> socket: ${socket.id}`);
    }
  });

  // 1. call_offer
  socket.on("call_offer", (data) => {
    const receiverId = data.receiverId || data.targetUserId;
    console.log(`📞 [call_offer] de ${data.senderId || data.callerId} para ${receiverId}`);
    const target = users.getUserSocket(receiverId);
    if (target) {
      io.to(target).emit("incoming_call", data);
    }
  });

  // 2. call_answer
  socket.on("call_answer", (data) => {
    const receiverId = data.receiverId || data.targetUserId;
    console.log(`📞 [call_answer] de ${data.senderId || data.targetUserId} para ${receiverId}`);
    const target = users.getUserSocket(receiverId);
    if (target) {
      io.to(target).emit("call_answer", data);
    }
  });

  // 3. ice_candidate
  socket.on("ice_candidate", (data) => {
    const receiverId = data.receiverId || data.targetUserId;
    console.log(`🧊 [ice_candidate] de ${data.senderId || data.targetUserId} para ${receiverId}`);
    const target = users.getUserSocket(receiverId);
    if (target) {
      io.to(target).emit("ice_candidate", data);
    }
  });

  // 4. call_reject
  socket.on("call_reject", (data) => {
    const receiverId = data.receiverId || data.targetUserId;
    console.log(`❌ [call_reject] de ${data.senderId || data.targetUserId} para ${receiverId}`);
    const target = users.getUserSocket(receiverId);
    if (target) {
      io.to(target).emit("call_reject", data);
    }
  });

  // 5. call_end
  socket.on("call_end", (data) => {
    const receiverId = data.receiverId || data.targetUserId;
    console.log(`🔌 [call_end] de ${data.senderId || data.targetUserId} para ${receiverId}`);
    const target = users.getUserSocket(receiverId);
    if (target) {
      io.to(target).emit("call_end", data);
    }
  });

  // 6. call_busy
  socket.on("call_busy", (data) => {
    const receiverId = data.receiverId || data.targetUserId;
    console.log(`🔇 [call_busy] de ${data.senderId || data.targetUserId} para ${receiverId}`);
    const target = users.getUserSocket(receiverId);
    if (target) {
      io.to(target).emit("call_busy", data);
    }
  });

  // Existing placeholder event handlers
  socket.on("user_online", (data) => {
    console.log(`user_online: ${JSON.stringify(data)}`);
  });

  socket.on("typing", (data) => {
    console.log(`typing: ${JSON.stringify(data)}`);
  });

  socket.on("message_seen", (data) => {
    console.log(`message_seen: ${JSON.stringify(data)}`);
  });

  socket.on("ping_server", (data, callback) => {
    if (typeof callback === "function") {
      callback({ status: "pong" });
    }
  });

  socket.on("disconnect", () => {
    users.removeUser(socket.id);
    console.log(`🔌 Socket desconectado: ${socket.id}`);
  });
};
