const userSockets = new Map(); // userId -> socketId

module.exports = {
  registerUser: (userId, socketId) => {
    userSockets.set(userId, socketId);
  },
  removeUser: (socketId) => {
    for (const [userId, id] of userSockets.entries()) {
      if (id === socketId) {
        userSockets.delete(userId);
        break;
      }
    }
  },
  getUserSocket: (userId) => {
    return userSockets.get(userId);
  }
};
