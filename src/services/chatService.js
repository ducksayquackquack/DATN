import axios from 'axios';

// Use the Vite proxy to route to backend
// Vite config proxies /api to http://127.0.0.1:8080
const API_BASE = '/api/chat';

const chatService = {
  /**
   * Create a new chat session
   */
  async createSession(data) {
    const response = await axios.post(`${API_BASE}/sessions`, data);
    return response.data;
  },

  /**
   * Get a specific chat session
   */
  async getSession(sessionId) {
    const response = await axios.get(`${API_BASE}/sessions/${sessionId}`);
    return response.data;
  },

  /**
   * Get sessions for a customer
   */
  async getCustomerSessions(email) {
    const response = await axios.get(`${API_BASE}/sessions/customer/${email}`);
    return response.data;
  },

  /**
   * Get all active sessions (admin)
   */
  async getActiveSessions() {
    const response = await axios.get(`${API_BASE}/sessions/active`);
    return response.data;
  },

  /**
   * Get sessions assigned to an employee
   */
  async getEmployeeSessions(employeeId) {
    const response = await axios.get(`${API_BASE}/sessions/employee/${employeeId}`);
    return response.data;
  },

  /**
   * Assign session to an employee
   */
  async assignSession(sessionId, employeeId, employeeName) {
    const response = await axios.post(`${API_BASE}/sessions/${sessionId}/assign`, {
      employeeId,
      employeeName
    });
    return response.data;
  },

  /**
   * Close a chat session
   */
  async closeSession(sessionId) {
    const response = await axios.post(`${API_BASE}/sessions/${sessionId}/close`);
    return response.data;
  },

  /**
   * Send a message
   */
  async sendMessage(sessionId, messageData) {
    const response = await axios.post(`${API_BASE}/sessions/${sessionId}/messages`, messageData);
    return response.data;
  },

  /**
   * Send a message with attachment
   */
  async sendMessageWithAttachment(sessionId, messageData) {
    const response = await axios.post(`${API_BASE}/sessions/${sessionId}/messages/upload`, messageData);
    return response.data;
  },

  /**
   * Get all messages in a session
   */
  async getMessages(sessionId) {
    const response = await axios.get(`${API_BASE}/sessions/${sessionId}/messages`);
    return response.data;
  },

  /**
   * Get recent messages
   */
  async getRecentMessages(sessionId, limit = 20) {
    const response = await axios.get(`${API_BASE}/sessions/${sessionId}/messages/recent`, {
      params: { limit }
    });
    return response.data;
  },

  /**
   * Get unread messages
   */
  async getUnreadMessages(sessionId) {
    const response = await axios.get(`${API_BASE}/sessions/${sessionId}/messages/unread`);
    return response.data;
  },

  /**
   * Mark a message as read
   */
  async markAsRead(messageId) {
    const response = await axios.post(`${API_BASE}/messages/${messageId}/read`);
    return response.data;
  },

  /**
   * Mark all messages as read
   */
  async markAllAsRead(sessionId) {
    const response = await axios.post(`${API_BASE}/sessions/${sessionId}/messages/read-all`);
    return response.data;
  },

  /**
   * Get unread count
   */
  async getUnreadCount(sessionId) {
    const response = await axios.get(`${API_BASE}/sessions/${sessionId}/unread-count`);
    return response.data.unreadCount;
  },

  /**
   * Search messages
   */
  async searchMessages(sessionId, keyword) {
    const response = await axios.get(`${API_BASE}/sessions/${sessionId}/search`, {
      params: { keyword }
    });
    return response.data;
  },

  /**
   * Health check
   */
  async healthCheck() {
    const response = await axios.get(`${API_BASE}/health`);
    return response.data;
  }
};

export default chatService;
