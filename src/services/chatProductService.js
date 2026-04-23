import axios from "axios"
import { resolveApiOrigin } from "../utils/apiOrigin"

const API_BASE = `${resolveApiOrigin()}`

export const searchChatProducts = async (params) => {
  try {
    const response = await axios.get(`${API_BASE}/api/chatbot/products/search`, {
      params: {
        q: params?.q || "",
        color: params?.color || "",
        maxPrice: params?.maxPrice || undefined
      }
    })

    const mapped = Array.isArray(response.data)
      ? response.data.map((p) => ({
          id: p.id,
          name: p.name || p.tenSanPham || "",
          summary: p.summary || p.moTa || "",
          price: Number(p.price || p.giaBan || p.giaTu || 0),
          stock: Number(p.stock || p.soLuong || p.tongTon || 0),
          image: p.image || p.anh || null,
          sizes: Array.isArray(p.sizes) ? p.sizes : Array.isArray(p.kichThuoc) ? p.kichThuoc : [],
          colors: Array.isArray(p.colors) ? p.colors : Array.isArray(p.mauSac) ? p.mauSac : [],
          category: p.category || p.loai || p.danhMuc || "",
          defaultVariantId: p.defaultVariantId ?? null,
          defaultSize: p.defaultSize || "",
          defaultColor: p.defaultColor || ""
        }))
      : []

    return { data: mapped }
  } catch (error) {
    console.error("searchChatProducts error:", error)
    return { data: [] }
  }
}