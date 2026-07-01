import { ref } from 'vue'
import { api } from '../api'

// Singleton reactive — dùng chung giữa MainLayout và StocktakesView
export const draftStocktakeCount = ref(0)

export async function refreshStocktakeBadge() {
  try {
    const res = await api.get('/api/stocktakes')
    if (res.ok) {
      const list: any[] = await res.json()
      const newCount = list.filter(s => s.status === 'DRAFT').length
      // Chỉ cập nhật nếu thực sự thay đổi → tránh Vue re-render sidebar mỗi 10 giây
      if (newCount !== draftStocktakeCount.value) {
        draftStocktakeCount.value = newCount
      }
    }
  } catch {
    // silent – không làm phiền user nếu lỗi mạng
  }
}
