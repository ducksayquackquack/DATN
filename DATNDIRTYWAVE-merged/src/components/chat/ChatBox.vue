<template>
  <div class="chat-container">
    <!-- Chat Header -->
    <div class="chat-header">
      <h3>Customer Support</h3>
      <span class="status-badge">ACTIVE</span>
      <button @click="closeChat" class="btn-close">×</button>
    </div>

    <!-- Messages Display -->
    <div class="chat-messages" ref="messagesContainer">
      <div v-for="message in messages" :key="message.id" :class="['message', message.senderType.toLowerCase()]">
        <div class="message-sender">{{ message.senderName }}</div>
        <div class="message-content">{{ message.content }}</div>
        <div class="message-time">{{ formatTime(message.createdAt) }}</div>
        <div v-if="message.attachmentUrl" class="message-attachment">
          <a :href="message.attachmentUrl" target="_blank">📎 Attachment</a>
        </div>
      </div>
      <div v-if="isLoading" class="message system">
        <div class="typing-indicator">
          <span></span><span></span><span></span>
        </div>
      </div>
    </div>

    <!-- Input Area -->
    <div class="chat-input-area">
      <div class="input-group">
        <input
          v-model="messageContent"
          @keydown.enter="sendMessage"
          placeholder="Type your message..."
          class="input-field"
          :disabled="sessionInfo.status === 'CLOSED'"
        />
        <button @click="sendMessage" class="btn-send" :disabled="!messageContent || sessionInfo.status === 'CLOSED'">
          Send
        </button>
        <button @click="openFileDialog" class="btn-attach" :disabled="sessionInfo.status === 'CLOSED'">
          📎
        </button>
        <input ref="fileInput" type="file" @change="handleFileUpload" style="display: none" />
      </div>
    </div>

    <!-- Session Info -->
    <div class="session-info" v-if="sessionInfo.assignedEmployeeName">
      <span>Assigned to: {{ sessionInfo.assignedEmployeeName }}</span>
    </div>
  </div>
</template>

<script>
import chatService from '@/services/chatService';

export default {
  name: 'ChatBox',
  props: {
    sessionId: {
      type: Number,
      required: false
    },
    customerEmail: {
      type: String,
      required: false
    }
  },
  data() {
    return {
      messages: [],
      messageContent: '',
      sessionInfo: {
        id: null,
        customerName: '',
        customerEmail: '',
        status: 'ACTIVE',
        chatMode: 'AUTO',
        assignedEmployeeName: null
      },
      isLoading: false,
      pollInterval: null
    };
  },
  mounted() {
    this.initializeChat();
  },
  beforeUnmount() {
    if (this.pollInterval) {
      clearInterval(this.pollInterval);
    }
  },
  methods: {
    async initializeChat() {
      try {
        if (this.sessionId) {
          // Load existing session
          await this.loadSession(this.sessionId);
        } else if (this.customerEmail) {
          // Create new session
          await this.createSession();
        }

        // Poll for new messages
        this.pollInterval = setInterval(() => {
          this.loadMessages();
        }, 3000);

        // Load messages immediately
        await this.loadMessages();
      } catch (error) {
        console.error('Failed to initialize chat:', error);
      }
    },

    async createSession() {
      try {
        const session = await chatService.createSession({
          customerEmail: this.customerEmail,
          customerName: 'Customer',
          customerPhone: ''
        });
        this.sessionInfo = session;
      } catch (error) {
        console.error('Failed to create session:', error);
      }
    },

    async loadSession(sessionId) {
      try {
        const session = await chatService.getSession(sessionId);
        this.sessionInfo = session;
      } catch (error) {
        console.error('Failed to load session:', error);
      }
    },

    async loadMessages() {
      if (!this.sessionInfo.id) return;

      try {
        const messages = await chatService.getMessages(this.sessionInfo.id);
        this.messages = messages;
        
        // Scroll to bottom
        this.$nextTick(() => {
          const container = this.$refs.messagesContainer;
          if (container) {
            container.scrollTop = container.scrollHeight;
          }
        });

        // Mark as read
        await chatService.markAllAsRead(this.sessionInfo.id);
      } catch (error) {
        console.error('Failed to load messages:', error);
      }
    },

    async sendMessage() {
      if (!this.messageContent.trim() || !this.sessionInfo.id) {
        return;
      }

      try {
        this.isLoading = true;
        await chatService.sendMessage(this.sessionInfo.id, {
          senderType: 'CUSTOMER',
          senderName: this.sessionInfo.customerName || 'Customer',
          content: this.messageContent
        });

        this.messageContent = '';
        await this.loadMessages();

        // If in AUTO mode and message sent, request human support if needed
        if (this.sessionInfo.chatMode === 'AUTO') {
          // Optionally trigger AI response here
        }
      } catch (error) {
        console.error('Failed to send message:', error);
      } finally {
        this.isLoading = false;
      }
    },

    openFileDialog() {
      this.$refs.fileInput.click();
    },

    async handleFileUpload(event) {
      const file = event.target.files[0];
      if (!file || !this.sessionInfo.id) return;

      try {
        this.isLoading = true;
        // In real app, upload file to server and get URL
        const attachmentUrl = await this.uploadFile(file);

        await chatService.sendMessageWithAttachment(this.sessionInfo.id, {
          senderType: 'CUSTOMER',
          senderName: this.sessionInfo.customerName || 'Customer',
          content: file.name,
          attachmentUrl: attachmentUrl,
          messageType: 'FILE'
        });

        this.$refs.fileInput.value = '';
        await this.loadMessages();
      } catch (error) {
        console.error('Failed to upload file:', error);
      } finally {
        this.isLoading = false;
      }
    },

    async uploadFile(file) {
      // TODO: Implement file upload to backend
      return '/files/' + file.name;
    },

    async closeChat() {
      if (this.sessionInfo.id) {
        try {
          await chatService.closeSession(this.sessionInfo.id);
          this.$emit('close');
        } catch (error) {
          console.error('Failed to close chat:', error);
        }
      }
    },

    formatTime(datetime) {
      if (!datetime) return '';
      return new Date(datetime).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
    }
  }
};
</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: 600px;
  width: 450px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  overflow: hidden;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-top-left-radius: 12px;
  border-top-right-radius: 12px;
}

.chat-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  flex: 1;
  letter-spacing: 0.3px;
}

.status-badge {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  margin-right: 12px;
  font-weight: 700;
  background: #4caf50;
  color: white;
  letter-spacing: 0.5px;
  box-shadow: 0 2px 8px rgba(76, 175, 80, 0.3);
}

.btn-close {
  background: none;
  border: none;
  color: white;
  font-size: 28px;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background 0.2s;
}

.btn-close:hover {
  background: rgba(255, 255, 255, 0.2);
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f5f5f5;
  min-height: 400px;
}

.message {
  margin-bottom: 12px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.message.customer {
  align-items: flex-end;
}

.message-sender {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.message-content {
  background: white;
  padding: 10px 12px;
  border-radius: 8px;
  border-left: 3px solid #667eea;
  word-wrap: break-word;
  max-width: 400px;
}

.message.customer .message-content {
  background: #667eea;
  color: white;
  border-left: none;
  border-right: 3px solid #667eea;
}

.message.system .message-content {
  background: #f0f0f0;
  border-left-color: #999;
  font-style: italic;
  text-align: center;
}

.message-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}

.typing-indicator {
  display: flex;
  gap: 4px;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ccc;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% {
    opacity: 0.5;
  }
  30% {
    opacity: 1;
  }
}

.chat-input-area {
  padding: 16px 20px;
  border-top: 1px solid #e0e0e0;
  background: white;
  border-bottom-left-radius: 12px;
  border-bottom-right-radius: 12px;
}

.input-group {
  display: flex;
  gap: 10px;
  align-items: center;
}

.input-field {
  flex: 1;
  padding: 14px 16px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  font-size: 15px;
  font-family: inherit;
  transition: all 0.2s;
}

.input-field:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.input-field::placeholder {
  color: #999;
}

.btn-send {
  padding: 14px 28px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  font-size: 15px;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.btn-send:hover:not(:disabled) {
  background: #5568d3;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-send:active:not(:disabled) {
  transform: translateY(0);
}

.btn-send:disabled {
  background: #ccc;
  cursor: not-allowed;
  box-shadow: none;
}

.btn-attach {
  padding: 14px 16px;
  background: #f0f0f0;
  color: #667eea;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 18px;
  transition: all 0.2s;
}

.btn-attach:hover:not(:disabled) {
  background: #e0e0e0;
  transform: scale(1.05);
}

.btn-attach:disabled {
  background: #f5f5f5;
  color: #ccc;
  cursor: not-allowed;
}

.session-info {
  padding: 8px 12px;
  background: #f0f0f0;
  font-size: 12px;
  color: #666;
  border-top: 1px solid #ddd;
}
</style>
