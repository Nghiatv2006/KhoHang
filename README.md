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
1. **Sao chép ra Desktop (Tùy chọn)**: Bạn có thể copy file `run_warehub.bat` ra ngoài màn hình Desktop để kích hoạt nhanh bằng 1 click.
2. **Kích hoạt**: Click đúp chuột vào file **`run_warehub.bat`**.
3. **Trải nghiệm**:
   * Cửa sổ CMD Backend sẽ khởi động Spring Boot trên cổng `8080`.
   * Cửa sổ CMD Frontend sẽ chạy Vite dev server trên cổng `3000` (hoặc `3001`).
   * Truy cập `http://localhost:3000` trên trình duyệt để sử dụng ứng dụng.

---

## Lưu ý cấu hình riêng cho từng Máy phát triển

Vì mỗi thành viên trong đội phát triển sử dụng các đường dẫn thư mục và JDK khác nhau, dự án đã thiết lập Git để **bỏ qua các thay đổi cục bộ** của các file `.bat` này. Bạn có thể sửa đổi cấu hình trong file `.bat` trên máy mình thoải mái mà không sợ bị Git báo thay đổi hoặc đẩy đè cấu hình lên người khác.

### 1. Thay đổi đường dẫn JDK 17 (nếu cần)
Mặc định script trỏ tới thư mục cài đặt `D:\jdk17`. Nếu máy của bạn cài đặt JDK ở thư mục khác (ví dụ: `C:\Program Files\Java\jdk-17`), hãy mở file `run_backend.bat` bằng Notepad hoặc VS Code và chỉnh sửa lại dòng:
```bat
powershell -Command "... $env:JAVA_HOME='C:\Program Files\Java\jdk-17'; .\gradlew.bat bootRun"
```

### 2. Lệnh thiết lập ignore thay đổi cục bộ cho thành viên mới
Các file `.bat` này đã được cấu hình ignore sẵn. Nếu bạn clone dự án về máy mới và muốn áp dụng quy tắc bỏ qua thay đổi cục bộ cho các file `.bat`, hãy mở PowerShell tại thư mục dự án và chạy lệnh:
```bash
git update-index --skip-worktree run_backend.bat run_frontend.bat run_warehub.bat
```

*(Nếu muốn khôi phục lại trạng thái theo dõi bình thường của Git, sử dụng cờ `--no-skip-worktree`).*
