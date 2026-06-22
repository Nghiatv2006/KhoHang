# WareHub — Hệ thống Quản lý Kho hàng Đa Chi nhánh

WareHub là giải pháp quản lý kho hàng tập trung và phân phối hàng hóa dành cho doanh nghiệp vận hành theo mô hình **Chi nhánh Tổng (Kho trung tâm) và nhiều Chi nhánh Con (Kho vệ tinh)**. Hệ thống giúp doanh nghiệp kiểm soát chặt chẽ tồn kho theo lô sản xuất, hạn sử dụng, tự động hóa quy trình kiểm kê, quản lý công nợ khách hàng và truy vết lịch sử hoạt động thông qua nhật ký hệ thống (Audit Log).

---

## 🌟 Tính Năng Cốt Lõi (Core Features)

### 1. Mô hình Phân phối Đa Chi nhánh (Multi-Branch Architecture)
- **Chi nhánh Tổng (Head Branch):** Nhận hàng đầu vào (`IMPORT`) từ nhà cung cấp và phân phối xuống các chi nhánh con thông qua phiếu điều chuyển (`TRANSFER`).
- **Chi nhánh Con (Sub-Branch):** Nhận hàng từ kho tổng và thực hiện xuất bán trực tiếp cho khách hàng (`EXPORT`).

### 2. Quản lý Tồn kho theo Lô & Hạn sử dụng (Batch-level & Expiry Tracking)
- Quản lý tồn kho chi tiết đến từng **Mã lô (Batch Code), Ngày sản xuất (NSX) và Hạn sử dụng (HSD)**.
- Áp dụng nguyên tắc **FEFO (First Expired, First Out - Hàng cận hạn xuất trước)** để tối ưu hóa hạn sử dụng, tránh lãng phí vốn do hàng hết hạn.

### 3. Quy trình Phê duyệt Nghiêm ngặt (Manager Approval Flow)
- Nhân viên kho (`STAFF`) chỉ được phép tạo phiếu ở trạng thái nháp (`DRAFT`).
- Quản lý (`MANAGER`) kiểm tra và duyệt phiếu chuyển sang trạng thái hoàn thành (`COMPLETED`) để hệ thống tự động cộng/trừ số lượng tồn kho thực tế, hoặc hủy phiếu (`CANCELLED`).
- Mọi phiếu đã duyệt `COMPLETED` hoặc `CANCELLED` đều **không thể sửa hay xóa** để bảo vệ tính toàn vẹn dữ liệu kế toán kho.

### 4. Kiểm kê & Tự động cân bằng Kho (Stocktake & Auto Adjustment)
- Cho phép tạo phiên kiểm kê định kỳ tại từng chi nhánh.
- So sánh số lượng thực tế kiểm đếm (`actual_quantity`) với số lượng sổ sách hệ thống (`expected_quantity`).
- Khi hoàn tất kiểm kê, hệ thống tự động sinh các phiếu cân bằng tăng (`ADJUST_IN`) hoặc cân bằng giảm (`ADJUST_OUT`) tương ứng với lượng chênh lệch thực tế.

### 5. Quản lý Công nợ Khách hàng (Accounts Receivable - AR)
- Theo dõi lịch sử công nợ chi tiết của từng khách hàng.
- Khi duyệt phiếu xuất bán hàng chưa thanh toán (`UNPAID`), công nợ khách hàng tự động tăng tương ứng. Khi khách hàng thanh toán (`PAID`), công nợ tự động giảm trừ.

### 6. Nhật ký Hoạt động Bảo mật (Audit Logging)
- Tự động ghi lại mọi thao tác nghiệp vụ nhạy cảm (Đăng nhập, thêm/sửa/xóa sản phẩm, phê duyệt phiếu kho, kiểm kê, cảnh báo spam).
- Nhật ký hoạt động chỉ được đọc và không thể chỉnh sửa hay xóa bởi bất kỳ ai (kể cả ADMIN), bảo đảm tính minh bạch khi cần đối soát.

### 7. Dashboard Phân tích Kinh doanh Hiện đại (Modern ECharts Analytics)
- **Xu hướng Nhập - Xuất kho (30 ngày):** Biểu đồ đường (Line Chart) so sánh lượng tiền nhập hàng và xuất bán mỗi ngày.
- **Doanh thu xuất bán theo Chi nhánh (30 ngày):** Biểu đồ cột đứng so sánh doanh số thực tế giữa các kho con.
- **Tỷ trọng doanh thu theo Danh mục (30 ngày):** Biểu đồ Donut phân tích cơ cấu đóng góp doanh thu của từng nhóm sản phẩm.
- **Top 10 sản phẩm bán chạy nhất:** Biểu đồ cột ngang hiển thị danh sách mặt hàng đắt khách nhất.
- **Dự báo số ngày bán hàng còn lại (Inventory Runway):** Biểu đồ cột ngang thông minh tự động đổi màu cột dựa theo mức độ khẩn cấp (Đỏ `< 7` ngày, Vàng `7-15` ngày, Xanh `> 15` ngày) để cảnh báo thủ kho chủ động nhập thêm hàng.

---

## 🛠️ Công Nghệ Sử Dụng (Tech Stack)

### Backend (Spring Boot API)
- **Framework:** Spring Boot (Java 17)
- **Database:** PostgreSQL (Cơ sở dữ liệu quan hệ)
- **ORM:** Spring Data JPA
- **Security:** Spring Security (Xác thực phân quyền RBAC: ADMIN, MANAGER, STAFF)

### Frontend (VueJS Web App)
- **Framework:** Vue 3 Composition API
- **Build Tool:** Vite + TypeScript
- **Styling:** Tailwind CSS (Vanilla CSS & Glassmorphism design)
- **Charts:** Apache ECharts (Trực quan hóa dữ liệu hiệu năng cao)

---

## 💾 Cấu trúc Cơ sở Dữ liệu chính

```
                                      [categories]
                                           │ (1)
                                           ▼ (N)
 [branches] ──(1)──► (N) [inventories] ◄──(N)── [products]
     │                       ▲                      ▲
     │ (1)                   │ (1)                  │ (1)
     ▼ (N)                   │                      │
 [receipts] ◄──(1)──────(N) [receipt_details] ──────┘
     │
     ▼ (N)
 [customers]
```

---

## 🔌 Hướng dẫn Khởi động nhanh bằng File Script (`.bat`)

Để tiết kiệm tài nguyên hệ thống (RAM/CPU) khi phát triển và không phải mở các IDE nặng như IntelliJ IDEA, dự án cung cấp bộ script chạy nhanh bằng file batch (`.bat`) trên Windows.

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

## ☕ Cơ chế tự động dò tìm JDK 17 (Auto-detect)

File script `run_backend.bat` đã được cấu hình thông minh:
* **Tự động kiểm tra**: Nếu biến môi trường `JAVA_HOME` hiện tại của bạn không trỏ tới JDK 17 hoặc bị lỗi, script sẽ tự động tìm kiếm JDK 17 hợp lệ (chứa tệp thực thi `bin\java.exe`) trong các thư mục cài đặt tiêu chuẩn:
  * `D:\jdk17`
  * `C:\Program Files\Java`
  * `C:\Program Files\Eclipse Adoptium`
  * `D:\Java`
  * `C:\Java`
* **Gán tự động**: Khi tìm thấy JDK 17 hợp lệ, hệ thống sẽ tự động gán biến `JAVA_HOME` tạm thời cho phiên làm việc để chạy dự án. Bạn **không cần phải chỉnh sửa cấu hình thủ công**.

---

## 🐙 Quản lý mã nguồn trên Git cho các file `.bat`

Các file `.bat` này đã được cấu hình ignore thay đổi cục bộ sẵn trong Git. Bạn có thể thay đổi cấu hình trên máy mình thoải mái mà không sợ bị Git báo thay đổi hoặc đẩy đè cấu hình lên người khác.

Nếu bạn clone dự án về máy mới và muốn áp dụng quy tắc bỏ qua thay đổi cục bộ này cho các file `.bat`, hãy mở PowerShell tại thư mục dự án và chạy lệnh:
```bash
git update-index --skip-worktree run_backend.bat run_frontend.bat run_warehub.bat
```

*(Nếu muốn khôi phục lại trạng thái theo dõi bình thường của Git, sử dụng cờ `--no-skip-worktree`).*
