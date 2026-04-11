<template>
  <div class="assistant-dock-root">
    <transition name="assistant-panel-fade">
      <div
        v-if="open"
        class="assistant-floating-panel"
        :class="[`side-${side}`, { resizing }]"
        :style="panelStyle"
      >
        <InternalAssistantPanel
          :role="role"
          :source="source"
          :context="context"
          @close="open = false"
        />

        <div
          class="resize-handle resize-handle--bottom"
          @pointerdown.stop="startResize($event, 'bottom')"
        />

        <div
          v-if="side === 'right'"
          class="resize-handle resize-handle--left"
          @pointerdown.stop="startResize($event, 'left')"
        />

        <div
          v-else
          class="resize-handle resize-handle--right"
          @pointerdown.stop="startResize($event, 'right')"
        />

        <div
          v-if="side === 'right'"
          class="resize-handle resize-handle--bottom-left"
          @pointerdown.stop="startResize($event, 'bottom-left')"
        />

        <div
          v-else
          class="resize-handle resize-handle--bottom-right"
          @pointerdown.stop="startResize($event, 'bottom-right')"
        />
      </div>
    </transition>

    <button
      type="button"
      class="assistant-floating-launcher"
      :class="{ dragging }"
      :style="launcherStyle"
      @click="handleLauncherClick"
      @pointerdown="onPointerDown"
    >
      <span class="launcher-ai">DW</span>
      <span class="launcher-text">Assistant</span>
    </button>
  </div>
</template>

<script>
import InternalAssistantPanel from "./InternalAssistantPanel.vue";

const LAUNCHER_SIZE = 64;
const EDGE_GAP = 16;
const TOP_GAP = 96;
const BOTTOM_GAP = 24;

const DEFAULT_PANEL_WIDTH = 520;
const DEFAULT_PANEL_HEIGHT = 760;

const MIN_PANEL_WIDTH = 380;
const MIN_PANEL_HEIGHT = 460;

const CLICK_DRAG_THRESHOLD = 6;
const PANEL_GAP_FROM_LAUNCHER = 12;
const VIEWPORT_PADDING = 16;

export default {
  name: "InternalAssistantDock",
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

      resizing: false,
      resizeMode: null,

      side: "right",
      y: 180,

      panelWidth: DEFAULT_PANEL_WIDTH,
      panelHeight: DEFAULT_PANEL_HEIGHT,

      startPointerX: 0,
      startPointerY: 0,
      startY: 180,
      lastPointerX: 0,

      startWidth: DEFAULT_PANEL_WIDTH,
      startHeight: DEFAULT_PANEL_HEIGHT,
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
      const width = this.clampWidth(this.panelWidth);
      const height = this.clampHeight(this.panelHeight);
      const top = this.getPanelTop(height);

      return {
        top: `${top}px`,
        left:
          this.side === "left"
            ? `${EDGE_GAP + LAUNCHER_SIZE + PANEL_GAP_FROM_LAUNCHER}px`
            : "auto",
        right:
          this.side === "right"
            ? `${EDGE_GAP + LAUNCHER_SIZE + PANEL_GAP_FROM_LAUNCHER}px`
            : "auto",
        width: `${width}px`,
        height: `${height}px`,
      };
    },
  },
  mounted() {
    this.restoreFloatingState();
    window.addEventListener("resize", this.handleResize, { passive: true });
  },
  beforeUnmount() {
    this.removeMoveListeners();
    this.removeResizeListeners();
    window.removeEventListener("resize", this.handleResize);
  },
  methods: {
    handleLauncherClick() {
      if (this.moved || this.resizing) return;
      this.open = !this.open;
      this.persistFloatingState();
    },

    onPointerDown(event) {
      if (event.button !== undefined && event.button !== 0) return;
      if (this.resizing) return;

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

      if (
        Math.abs(deltaX) > CLICK_DRAG_THRESHOLD ||
        Math.abs(deltaY) > CLICK_DRAG_THRESHOLD
      ) {
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

      this.removeMoveListeners();
      this.persistFloatingState();

      requestAnimationFrame(() => {
        requestAnimationFrame(() => {
          this.moved = false;
        });
      });
    },

    startResize(event, mode) {
      if (event.button !== undefined && event.button !== 0) return;

      this.resizing = true;
      this.resizeMode = mode;
      this.startPointerX = event.clientX;
      this.startPointerY = event.clientY;
      this.startWidth = this.clampWidth(this.panelWidth);
      this.startHeight = this.clampHeight(this.panelHeight);

      try {
        event.currentTarget?.setPointerCapture?.(event.pointerId);
      } catch (_) {}

      window.addEventListener("pointermove", this.onResizeMove);
      window.addEventListener("pointerup", this.stopResize);
      window.addEventListener("pointercancel", this.stopResize);
    },

    onResizeMove(event) {
      if (!this.resizing) return;

      const dx = event.clientX - this.startPointerX;
      const dy = event.clientY - this.startPointerY;

      if (this.resizeMode === "right") {
        this.panelWidth = this.clampWidth(this.startWidth + dx);
      }

      if (this.resizeMode === "left") {
        this.panelWidth = this.clampWidth(this.startWidth - dx);
      }

      if (this.resizeMode === "bottom") {
        this.panelHeight = this.clampHeight(this.startHeight + dy);
      }

      if (this.resizeMode === "bottom-right") {
        this.panelWidth = this.clampWidth(this.startWidth + dx);
        this.panelHeight = this.clampHeight(this.startHeight + dy);
      }

      if (this.resizeMode === "bottom-left") {
        this.panelWidth = this.clampWidth(this.startWidth - dx);
        this.panelHeight = this.clampHeight(this.startHeight + dy);
      }
    },

    stopResize() {
      if (!this.resizing) return;

      this.resizing = false;
      this.resizeMode = null;
      this.panelWidth = this.clampWidth(this.panelWidth);
      this.panelHeight = this.clampHeight(this.panelHeight);

      this.removeResizeListeners();
      this.persistFloatingState();
    },

    removeMoveListeners() {
      window.removeEventListener("pointermove", this.onPointerMove);
      window.removeEventListener("pointerup", this.onPointerUp);
      window.removeEventListener("pointercancel", this.onPointerUp);
    },

    removeResizeListeners() {
      window.removeEventListener("pointermove", this.onResizeMove);
      window.removeEventListener("pointerup", this.stopResize);
      window.removeEventListener("pointercancel", this.stopResize);
    },

    getMaxPanelWidth() {
      return Math.max(
        MIN_PANEL_WIDTH,
        window.innerWidth -
          (EDGE_GAP + LAUNCHER_SIZE + PANEL_GAP_FROM_LAUNCHER + VIEWPORT_PADDING)
      );
    },

    getMaxPanelHeight() {
      return Math.max(MIN_PANEL_HEIGHT, window.innerHeight - 24);
    },

    clampWidth(nextWidth) {
      return Math.min(
        Math.max(nextWidth, MIN_PANEL_WIDTH),
        this.getMaxPanelWidth()
      );
    },

    clampHeight(nextHeight) {
      return Math.min(
        Math.max(nextHeight, MIN_PANEL_HEIGHT),
        this.getMaxPanelHeight()
      );
    },

    clampY(nextY) {
      const maxY = Math.max(
        TOP_GAP,
        window.innerHeight - LAUNCHER_SIZE - BOTTOM_GAP
      );
      return Math.min(Math.max(nextY, TOP_GAP), maxY);
    },

    getPanelTop(panelHeight) {
      const preferred = this.y - 16;
      const maxTop = Math.max(16, window.innerHeight - panelHeight - 16);
      return Math.min(Math.max(preferred, 16), maxTop);
    },

    handleResize() {
      this.y = this.clampY(this.y);
      this.panelWidth = this.clampWidth(this.panelWidth);
      this.panelHeight = this.clampHeight(this.panelHeight);
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
            panelWidth: this.panelWidth,
            panelHeight: this.panelHeight,
          })
        );
      } catch (_) {}
    },

    restoreFloatingState() {
      try {
        const raw = localStorage.getItem("dirtywave_ai_floating_widget");
        if (!raw) {
          this.y = this.clampY(180);
          this.panelWidth = DEFAULT_PANEL_WIDTH;
          this.panelHeight = DEFAULT_PANEL_HEIGHT;
          return;
        }

        const parsed = JSON.parse(raw);
        this.side = parsed?.side === "left" ? "left" : "right";
        this.y = this.clampY(Number(parsed?.y || 180));
        this.open = !!parsed?.open;
        this.panelWidth = this.clampWidth(
          Number(parsed?.panelWidth || DEFAULT_PANEL_WIDTH)
        );
        this.panelHeight = this.clampHeight(
          Number(parsed?.panelHeight || DEFAULT_PANEL_HEIGHT)
        );
      } catch (_) {
        this.y = this.clampY(180);
        this.panelWidth = DEFAULT_PANEL_WIDTH;
        this.panelHeight = DEFAULT_PANEL_HEIGHT;
      }
    },
  },
};
</script>

<style scoped>
.assistant-dock-root {
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
  max-width: calc(100vw - 16px);
  max-height: calc(100vh - 16px);
  overflow: hidden;
  border-radius: 24px;
  background: #fff;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.22);
  border: 1px solid rgba(226, 232, 240, 0.9);
}

.assistant-floating-panel.resizing {
  user-select: none;
}

.assistant-floating-panel :deep(.assistant-panel) {
  height: 100%;
  min-height: 100%;
  border-radius: 24px;
  overflow: hidden;
}

.resize-handle {
  position: absolute;
  z-index: 20;
  pointer-events: auto;
}

.resize-handle--left {
  top: 14px;
  left: -6px;
  width: 12px;
  height: calc(100% - 28px);
  cursor: ew-resize;
}

.resize-handle--right {
  top: 14px;
  right: -6px;
  width: 12px;
  height: calc(100% - 28px);
  cursor: ew-resize;
}

.resize-handle--bottom {
  left: 14px;
  bottom: -6px;
  width: calc(100% - 28px);
  height: 12px;
  cursor: ns-resize;
}

.resize-handle--bottom-left {
  left: -6px;
  bottom: -6px;
  width: 20px;
  height: 20px;
  cursor: nesw-resize;
}

.resize-handle--bottom-right {
  right: -6px;
  bottom: -6px;
  width: 20px;
  height: 20px;
  cursor: nwse-resize;
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
    width: min(96vw, 760px) !important;
    height: min(88vh, 860px) !important;
  }
}
</style>