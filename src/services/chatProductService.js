import axios from "axios"

const API_BASE =
  (import.meta.env.VITE_API_ORIGIN || "http://localhost:8080").replace(/\/$/, "")

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
          price: Number(p.price || p.giaBan || 0),
          stock: Number(p.stock || 0),
          image: p.image || p.anh || null,
          sizes: Array.isArray(p.sizes) ? p.sizes : [],
          colors: Array.isArray(p.colors) ? p.colors : [],
          category: p.category || p.loai || "",
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