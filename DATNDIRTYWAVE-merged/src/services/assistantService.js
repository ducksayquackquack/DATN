import axios from "axios";
import { resolveApiOrigin } from "@/utils/apiOrigin";

const API_BASE = `${resolveApiOrigin()}/api/assistant`;

const assistantHttp = axios.create({
  baseURL: API_BASE,
  withCredentials: true,
  timeout: 45000,
  headers: {
    "Content-Type": "application/json",
  },
});

export const assistantService = {
  async health() {
    const res = await assistantHttp.get(`/health`);
    return res.data;
  },

  async sendMessage(payload) {
    const finalPayload = {
      ...payload,
      context: {
        ...(payload?.context || {}),
        ...(payload?.slotValues
          ? { slotValues: payload.slotValues }
          : {}),
        ...(payload?.pendingActionToken
          ? { pendingActionToken: payload.pendingActionToken }
          : {}),
      },
    };

    const res = await assistantHttp.post(`/chat`, finalPayload, {
      withCredentials: true,
    });
    return res.data;
  },
};
