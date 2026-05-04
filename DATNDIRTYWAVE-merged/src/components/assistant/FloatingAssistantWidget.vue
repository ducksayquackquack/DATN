<template>
  <div class="assistant-floating-root">
    <transition name="assistant-panel-fade">
      <div
        v-if="open"
        class="assistant-floating-panel"
        :class="[`side-${side}`]"
        :style="panelStyle"
      >
        <InternalAssistantPanel
          :role="role"
          :source="source"
          :context="context"
          @close="open = false"
        />
      </div>
    </transition>

    <button
      ref="launcherRef"
      type="button"
      class="assistant-floating-launcher"
      :class="[`side-${side}`, { dragging }]"
      :style="launcherStyle"
      @click="handleLauncherClick"
      @pointerdown="onPointerDown"
    >
      <span class="launcher-ai">AI</span>
      <span class="launcher-text">Demo</span>
    </button>
  </div>
</template>

<script>
import InternalAssistantPanel from "@/components/assistant/InternalAssistantPanel.vue";

const LAUNCHER_SIZE = 64;
const EDGE_GAP = 16;
const TOP_GAP = 96;
const BOTTOM_GAP = 24;
const PANEL_WIDTH = 420;
const PANEL_HEIGHT = 1330;
const CLICK_DRAG_THRESHOLD = 6;

export default {
  name: "FloatingAssistantWidget",
  components: {
    InternalAssistantPanel,
  },
  props: {
    role: {
      type: String,
      default: "EMPLOYEE",
    },
    source: {
      type: String,
      default: "EMPLOYEE_PANEL",
    },
    context: {
      type: Object,
      default: () => ({}),
    },
  },
  data() {
    return {
      open: false,
      dragging: false,
      moved: false,
      side: "right",
      y: 180,

      startPointerX: 0,
      startPointerY: 0,
      startY: 180,
      lastPointerX: 0,
      pointerId: null,
    };
  },
  computed: {
    launcherStyle() {
      return {
        top: `${this.y}px`,
        left: this.side === "left" ? `${EDGE_GAP}px` : "auto",
        right: this.side === "right" ? `${EDGE_GAP}px` : "auto",
      };
    },
    panelStyle() {
  return {
    top: "24px",
    right: this.side === "right" ? "24px" : "auto",
    left: this.side === "left" ? "24px" : "auto",

    width: "50vw",
    height: "50vh",
    minWidth: "720px",
    minHeight: "620px",
    maxWidth: "960px",
    maxHeight: "80vh",
  };
},
  },
  mounted() {
    this.restoreFloatingState();
    window.addEventListener("resize", this.handleResize, { passive: true });
  },
  beforeUnmount() {
    this.removePointerListeners();
    window.removeEventListener("resize", this.handleResize);
  },
  methods: {
    handleLauncherClick() {
      if (this.moved) return;
      this.open = !this.open;
      this.persistFloatingState();
    },

    onPointerDown(event) {
      if (event.button !== undefined && event.button !== 0) return;

      this.pointerId = event.pointerId;
      this.dragging = true;
      this.moved = false;
      this.startPointerX = event.clientX;
      this.startPointerY = event.clientY;
      this.startY = this.y;
      this.lastPointerX = event.clientX;

      try {
        event.currentTarget?.setPointerCapture?.(event.pointerId);
      } catch (_) {}

      window.addEventListener("pointermove", this.onPointerMove);
      window.addEventListener("pointerup", this.onPointerUp);
      window.addEventListener("pointercancel", this.onPointerUp);
    },

    onPointerMove(event) {
      if (!this.dragging) return;

      const deltaX = event.clientX - this.startPointerX;
      const deltaY = event.clientY - this.startPointerY;

      if (Math.abs(deltaX) > CLICK_DRAG_THRESHOLD || Math.abs(deltaY) > CLICK_DRAG_THRESHOLD) {
        this.moved = true;
      }

      this.lastPointerX = event.clientX;
      this.y = this.clampY(this.startY + deltaY);

      const viewportMid = window.innerWidth / 2;
      this.side = event.clientX < viewportMid ? "left" : "right";
    },

    onPointerUp() {
      if (!this.dragging) return;

      this.dragging = false;
      this.y = this.clampY(this.y);

      const viewportMid = window.innerWidth / 2;
      this.side = this.lastPointerX < viewportMid ? "left" : "right";

      this.removePointerListeners();
      this.persistFloatingState();

      // tránh click sau khi vừa drag
      requestAnimationFrame(() => {
        requestAnimationFrame(() => {
          this.moved = false;
        });
      });
    },

    removePointerListeners() {
      window.removeEventListener("pointermove", this.onPointerMove);
      window.removeEventListener("pointerup", this.onPointerUp);
      window.removeEventListener("pointercancel", this.onPointerUp);
    },

    clampY(nextY) {
      const maxY = Math.max(TOP_GAP, window.innerHeight - LAUNCHER_SIZE - BOTTOM_GAP);
      return Math.min(Math.max(nextY, TOP_GAP), maxY);
    },

    getPanelTop() {
      // const preferred = this.y - 16;
      // const maxTop = Math.max(16, window.innerHeight - PANEL_HEIGHT - 16);
      // return Math.min(Math.max(preferred, 16), maxTop);
      return 16;
    },

    handleResize() {
      this.y = this.clampY(this.y);
      this.persistFloatingState();
    },

    persistFloatingState() {
      try {
        localStorage.setItem(
          "dirtywave_ai_floating_widget",
          JSON.stringify({
            side: this.side,
            y: this.y,
            open: this.open,
          })
        );
      } catch (_) {}
    },

    restoreFloatingState() {
      try {
        const raw = localStorage.getItem("dirtywave_ai_floating_widget");
        if (!raw) {
          this.y = this.clampY(180);
          return;
        }

        const parsed = JSON.parse(raw);
        this.side = parsed?.side === "left" ? "left" : "right";
        this.y = this.clampY(Number(parsed?.y || 180));
        this.open = !!parsed?.open;
      } catch (_) {
        this.y = this.clampY(180);
      }
    },
  },
};
</script>

<style scoped>
.assistant-floating-root {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 9999;
}

.assistant-floating-launcher {
  position: fixed;
  width: 64px;
  height: 64px;
  border: none;
  border-radius: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  pointer-events: auto;
  cursor: grab;
  user-select: none;
  touch-action: none;
  color: #fff;
  background: linear-gradient(135deg, #1d4ed8, #2563eb);
  box-shadow: 0 16px 36px rgba(37, 99, 235, 0.34);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.assistant-floating-launcher:hover {
  transform: translateY(-2px);
  box-shadow: 0 20px 40px rgba(37, 99, 235, 0.4);
}

.assistant-floating-launcher.dragging {
  cursor: grabbing;
  transform: scale(1.04);
  box-shadow: 0 22px 44px rgba(15, 23, 42, 0.28);
}

.launcher-ai {
  font-size: 20px;
  font-weight: 800;
  line-height: 1;
}

.launcher-text {
  font-size: 11px;
  font-weight: 700;
  line-height: 1;
  opacity: 0.95;
}

.assistant-floating-panel {
  position: fixed;
  pointer-events: auto;
  overflow: hidden;
  border-radius: 24px;
  background: #fff;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.22);
  border: 1px solid rgba(226, 232, 240, 0.9);
}

.assistant-floating-panel :deep(.assistant-panel) {
  height: 100%;
  min-height: 100%;
  border-radius: 24px;
  overflow: hidden;
}

.assistant-panel-fade-enter-active,
.assistant-panel-fade-leave-active {
  transition: all 0.2s ease;
}

.assistant-panel-fade-enter-from,
.assistant-panel-fade-leave-to {
  opacity: 0;
  transform: scale(0.96);
}

@media (max-width: 768px) {
  .assistant-floating-panel {
    width: min(92vw, 420px) !important;
    height: min(78vh, 680px) !important;
  }
}
</style>