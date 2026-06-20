# WareHub - Hệ thống Quản lý Kho hàng Đa Chi nhánh

Dự án này tích hợp Backend Spring Boot (Java 17) và Frontend VueJS 3 (Vite + TypeScript) kết hợp cùng PostgreSQL.

Để tiết kiệm tài nguyên hệ thống (RAM/CPU) khi phát triển và không phải mở các IDE nặng như IntelliJ IDEA, dự án cung cấp bộ script chạy nhanh bằng file batch (`.bat`) trên Windows.

---

## Hướng dẫn Khởi động nhanh bằng File Script (`.bat`)

Bộ script khởi động dự án bao gồm:
1. **`run_warehub.bat`** (ở thư mục gốc): Script chính điều khiển, mở song song Backend và Frontend trong 2 cửa sổ CMD riêng biệt.
2. **`run_backend.bat`**: Script khởi chạy Spring Boot (tự động nạp cấu hình từ `.env` thông qua PowerShell và gán JDK 17).
3. **`run_frontend.bat`**: Script chạy Vite dev server cho Frontend VueJS.

### Các bước thực hiện:
1. **Tạo Shortcut ra Desktop (Khuyên dùng)**: 
   * Click chuột phải vào file **`run_warehub.bat`** trong thư mục dự án gốc.
   * Chọn **Send to** -> **Desktop (create shortcut)**.
   * Shortcut ngoài Desktop sẽ tự động ghi nhớ đường dẫn dự án và hoạt động ở mọi máy mà không cần chỉnh sửa code.
2. **Kích hoạt**: Click đúp chuột vào Shortcut vừa tạo ngoài Desktop.
3. **Trải nghiệm**:
   * Cửa sổ CMD Backend sẽ khởi động Spring Boot trên cổng `8080`.
   * Cửa sổ CMD Frontend sẽ chạy Vite dev server trên cổng `3000` (hoặc `3001`).
   * Truy cập `http://localhost:3000` trên trình duyệt để sử dụng ứng dụng.

---

## Cơ chế tự động dò tìm JDK 17 (Auto-detect)

File script `run_backend.bat` đã được cấu hình thông minh:
* **Tự động kiểm tra**: Nếu biến môi trường `JAVA_HOME` hiện tại của bạn không trỏ tới JDK 17 hoặc bị lỗi, script sẽ tự động tìm kiếm JDK 17 hợp lệ (chứa tệp thực thi `bin\java.exe`) trong các thư mục cài đặt tiêu chuẩn:
  * `D:\jdk17`
  * `C:\Program Files\Java`
  * `C:\Program Files\Eclipse Adoptium`
  * `D:\Java`
  * `C:\Java`
* **Gán tự động**: Khi tìm thấy JDK 17 hợp lệ, hệ thống sẽ tự động gán biến `JAVA_HOME` tạm thời cho phiên làm việc để chạy dự án. Bạn **không cần phải chỉnh sửa cấu hình thủ công**.

---

## Quản lý mã nguồn trên Git cho các file `.bat`

Các file `.bat` này đã được cấu hình ignore thay đổi cục bộ sẵn trong Git. Bạn có thể thay đổi cấu hình trên máy mình thoải mái mà không sợ bị Git báo thay đổi hoặc đẩy đè cấu hình lên người khác.

Nếu bạn clone dự án về máy mới và muốn áp dụng quy tắc bỏ qua thay đổi cục bộ này cho các file `.bat`, hãy mở PowerShell tại thư mục dự án và chạy lệnh:
```bash
git update-index --skip-worktree run_backend.bat run_frontend.bat run_warehub.bat
```

*(Nếu muốn khôi phục lại trạng thái theo dõi bình thường của Git, sử dụng cờ `--no-skip-worktree`).*
