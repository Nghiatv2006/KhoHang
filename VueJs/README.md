# Vue.js Frontend (Vite + TypeScript)

Thư mục này chứa dự án Frontend viết bằng Vue.js 3, Vite và TypeScript để giao tiếp với Backend Spring Boot.

## 🛠️ Cấu hình phát triển (Development Setup)

Dự án đã được thiết lập sẵn một Dev Proxy trong `vite.config.ts`:
*   **Cổng chạy mặc định:** `http://localhost:3000`
*   **Dev Proxy:** Tất cả các request bắt đầu bằng `/api` (ví dụ: `/api/auth/login`) sẽ tự động được chuyển tiếp tới Backend Spring Boot đang chạy ở cổng `http://localhost:8080`. Điều này giúp tránh lỗi CORS khi chạy thử nghiệm trên máy local.

---

## 🚀 Các câu lệnh chạy dự án (NPM Scripts)

Bạn cần mở terminal trong thư mục `VueJs` và sử dụng các câu lệnh sau:

### 1. Chạy môi trường phát triển (Local Development)
Khởi chạy máy chủ phát triển cục bộ với tính năng Hot Module Replacement (HMR):
```bash
npm run dev
```

### 2. Biên dịch dự án cho Production (Build)
Biên dịch kiểm tra lỗi TypeScript và đóng gói mã nguồn tối ưu vào thư mục `dist`:
```bash
npm run build
```

### 3. Chạy thử bản build (Preview)
Xem trước sản phẩm sau khi đã biên dịch đóng gói:
```bash
npm run preview
```

---

## 📦 Các thư viện đã được cài đặt sẵn (Installed Dependencies)
*   **`vue-router`** (v4): Thư viện định tuyến chính thức của Vue để quản lý chuyển trang.
*   **`axios`**: Thư viện HTTP client để gửi các request lên Backend.
