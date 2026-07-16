import { ref } from 'vue'
import { api } from '../api'

// Singleton reactive — dùng chung giữa MainLayout và StocktakesView
export const draftStocktakeCount = ref(0)

export async function refreshStocktakeBadge() {
  try {
    const res = await api.get('/api/stocktakes')
    if (res.ok) {
      const list: any[] = await res.json()
      
      const uStr = localStorage.getItem('wh_user')
      let isStaff = false
      let isManagerOrAdmin = false
      if (uStr) {
        try {
          const user = JSON.parse(uStr)
          isStaff = user.role === 'STAFF'
          isManagerOrAdmin = ['MANAGER', 'ADMIN'].includes(user.role)
        } catch {}
      }

      const newCount = list.filter(s => {
        if (isManagerOrAdmin && s.status === 'PENDING_APPROVAL') {
          return true
        }
        if (s.status === 'DRAFT') {
          if (isStaff) {
            return true // Nhân viên thấy mọi phiếu nháp để vào điền số
          }
          return true // Quản lý cũng thấy phiếu nháp chưa làm
        }
        return false
      }).length

      // Chỉ cập nhật nếu thực sự thay đổi → tránh Vue re-render sidebar mỗi 10 giây
      if (newCount !== draftStocktakeCount.value) {
        draftStocktakeCount.value = newCount
      }
    }
  } catch {
    // silent – không làm phiền user nếu lỗi mạng
  }
}
