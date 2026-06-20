# TÀI LIỆU MÔ TẢ CHI TIẾT SƠ ĐỒ ERD (CHEN NOTATION)
## HỆ THỐNG QUẢN LÝ KHO HÀNG ĐA CHI NHÁNH

Tài liệu này mô tả chi tiết các thành phần, cấu trúc thực thể, thuộc tính và ý nghĩa nghiệp vụ của tất cả các mối quan hệ (hình thoi) trong sơ đồ ERD chuẩn dạng Chen (Chen's Notation) của dự án.

---

## 1. PHÂN VÙNG PHÂN HỆ NGHIỆP VỤ (COLOR-CODED MODULES)

Sơ đồ ERD được chia làm 4 cụm nghiệp vụ tương ứng với 4 phân vùng màu sắc:

1. **Phân hệ Danh mục & Chi nhánh (Màu xanh dương - Blue):**
   * `Branches` (Chi nhánh): Quản lý thông tin chi nhánh kho vật lý. Chi nhánh có `id = 1` là **Kho Tổng**.
   * `Categories` (Danh mục): Phân loại sản phẩm.
2. **Phân hệ Nhân sự & Khách hàng (Màu xanh lá - Green):**
   * `Users` (Người dùng/Nhân viên): Tài khoản đăng nhập và phân quyền hệ thống.
   * `Customers` (Khách hàng): Đối tác mua hàng hóa đầu ra.
3. **Phân hệ Sản phẩm & Phiếu kho (Màu vàng - Yellow):**
   * `Products` (Sản phẩm): Thông tin sản phẩm, đơn giá, danh mục.
   * `Inventories` (Tồn kho): Số lượng tồn thực tế của các sản phẩm theo từng chi nhánh.
   * `Receipts` (Phiếu kho): Các giao dịch nhập, xuất, điều chuyển kho.
   * `Receipt Details` (Chi tiết phiếu kho): Danh sách mặt hàng, số lượng và giá của từng dòng giao dịch.
4. **Phân hệ Kiểm kê (Màu cam - Orange):**
   * `Stocktakes` (Kiểm kê): Đợt đối soát hàng hóa định kỳ.
   * `Stocktake Details` (Chi tiết kiểm kê): Ghi nhận chênh lệch kiểm đếm từng mặt hàng.
5. **Phân hệ Nhật ký hệ thống (Màu tím - Purple):**
   * `Audit Logs` (Nhật ký hoạt động): Ghi vết thao tác người dùng để phục vụ giám sát hệ thống.

---

## 2. CẤU TRÚC CHI TIẾT CÁC THỰC THỂ (ENTITIES) & THUỘC TÍNH (ATTRIBUTES)

Dưới đây là danh sách thuộc tính của từng thực thể được vẽ trong sơ đồ:

### 2.1. Phân hệ Danh mục & Chi nhánh (Xanh dương)
*   **`Branches` (Chi nhánh):** `id` (Khóa chính - gạch chân), `name` (Tên), `address` (Địa chỉ), `low_stock_threshold` (Ngưỡng cảnh báo tồn kho thấp).
*   **`Categories` (Danh mục):** `id` (Khóa chính - gạch chân), `name` (Tên danh mục).

### 2.2. Phân hệ Nhân sự & Khách hàng (Xanh lá)
*   **`Customers` (Khách hàng):** `id` (Khóa chính - gạch chân), `name` (Tên), `email` (Email), `phone number` (Số điện thoại), `debt` (Công nợ khách hàng phải trả), `status` (Trạng thái hoạt động: ACTIVE/INACTIVE).
*   **`Users` (Người dùng):** `id` (Khóa chính - gạch chân), `username` (Tên đăng nhập), `password` (Mật khẩu đã mã hóa), `role` (Vai trò: ADMIN/MANAGER/STAFF), `branch_id` (Chi nhánh làm việc — NULL nếu là ADMIN), `email` (Email), `status` (Trạng thái tài khoản: ACTIVE/LOCKED).

### 2.3. Phân hệ Sản phẩm & Phiếu kho (Vàng)
*   **`Products` (Sản phẩm):** `id` (Khóa chính - gạch chân), `code` (Mã sản phẩm - viết hoa), `name` (Tên), `price` (Đơn giá), `unit` (Đơn vị tính), `category_id` (Khóa ngoại danh mục), `has_expiry` (Có quản lý hạn sử dụng không), `mfg_date` (Ngày sản xuất mặc định), `exp_date` (Hạn sử dụng mặc định).
    > **Lưu ý:** `Products` **không** chứa trường `quantity`. Số lượng tồn được quản lý hoàn toàn trong bảng `Inventories` theo từng chi nhánh.
*   **`Inventories` (Tồn kho):** `id` (Khóa chính - gạch chân), `quantity` (Số lượng tồn), `mfg_date` (Ngày sản xuất của lô), `exp_date` (Hạn sử dụng của lô), `last_updated` (Thời điểm cập nhật gần nhất). *Khóa nghiệp vụ duy nhất: `(branch_id, product_id, mfg_date, exp_date)` - quản lý tồn kho theo từng lô.*
*   **`Receipts` (Phiếu kho):** `id` (Khóa chính - gạch chân), `code` (Mã phiếu), `type` (Loại phiếu: IMPORT/EXPORT/TRANSFER/ADJUST_IN/ADJUST_OUT), `status` (Trạng thái: DRAFT/COMPLETED/CANCELLED), `created_at` (Ngày lập phiếu), `payment_status` (Trạng thái thanh toán: UNPAID/PAID).
*   **`Receipt Details` (Chi tiết phiếu kho):** `id` (Khóa chính - gạch chân), `quantity` (Số lượng giao dịch), `price` (Đơn giá giao dịch), `mfg_date` (Ngày sản xuất lô hàng), `exp_date` (Hạn sử dụng lô hàng).

### 2.4. Phân hệ Kiểm kê (Cam)
*   **`Stocktakes` (Kiểm kê):** `id` (Khóa chính - gạch chân), `code` (Mã phiên kiểm kê), `status` (Trạng thái: DRAFT/COMPLETED/CANCELLED), `notes` (Ghi chú).
*   **`Stocktake Details` (Chi tiết kiểm kê):** `id` (Khóa chính - gạch chân), `actual_quantity` (Số lượng thực tế đếm được), `expected_quantity` (Số lượng sổ sách - để đối chiếu chênh lệch), `adjustment_receipt_id` (Khóa ngoại trỏ tới phiếu cân bằng kho tự động sinh), `notes` (Ghi chú).

### 2.5. Phân hệ Nhật ký hệ thống (Tím)
*   **`Audit Logs` (Nhật ký):** `id` (Khóa chính - gạch chân), `action` (Hành động), `created_at` (Thời gian ghi log), `details` (Thông tin chi tiết).

---

## 3. Ý NGHĨA CÁC MỐI QUAN HỆ LIÊN PHÂN HỆ (HÌNH THOI MÀU TRẮNG)

*   **`stores` (Chi nhánh → Tồn kho):** `Branches` (1) <---> `Inventories` (N). Chi nhánh chứa nhiều dòng tồn kho của các sản phẩm.
*   **`categorizes` (Danh mục → Sản phẩm):** `Categories` (1) <---> `Products` (N). Danh mục phân loại sản phẩm.
*   **`has staff` (Chi nhánh → Nhân viên):** `Branches` (1) <---> `Users` (N). Một chi nhánh có nhiều nhân viên (trừ ADMIN không gắn chi nhánh).
*   **`receives` (Khách hàng → Phiếu kho):** `Customers` (1) <---> `Receipts` (N). Khách hàng nhận hàng qua phiếu xuất kho.
*   **`creates` (Nhân viên → Phiếu kho):** `Users` (1) <---> `Receipts` (N). Người dùng lập phiếu kho.
*   **`records` (Nhân viên → Nhật ký):** `Users` (1) <---> `Audit Logs` (N). Hệ thống ghi vết thao tác người dùng.
*   **`creates` (Nhân viên → Kiểm kê):** `Users` (1) <---> `Stocktakes` (N). Người dùng tạo phiên kiểm kê.
*   **`performs at` (Chi nhánh → Kiểm kê):** `Branches` (1) <---> `Stocktakes` (N). Phiên kiểm kê được thực hiện tại một chi nhánh.
*   **`generates` (Chi tiết kiểm kê → Phiếu kho):** `Stocktake Details` (N) <---> `Receipts` (1). Khi kiểm kê phát hiện chênh lệch, hệ thống tự sinh phiếu `ADJUST_IN/ADJUST_OUT`.

---

## 4. Ý NGHĨA CÁC MỐI QUAN HỆ NỘI BỘ PHÂN HỆ (HÌNH THOI CÓ MÀU)

### 4.1. Phân hệ Sản phẩm & Phiếu kho (Màu Vàng)
*   **`tracks` (Sản phẩm → Tồn kho):** `Products` (1) <---> `Inventories` (N). Hệ thống theo dõi tồn kho chi tiết theo từng lô của từng sản phẩm tại từng chi nhánh.
*   **`contains` (Phiếu kho → Chi tiết phiếu):** `Receipts` (1) <---> `Receipt Details` (N). Phiếu kho chứa nhiều dòng chi tiết sản phẩm.
*   **`contains` (Tồn kho ↔ Chi tiết phiếu):** `Inventories` (1) <---> `Receipt Details` (N). Chi tiết giao dịch nhập/xuất tác động trực tiếp vào số lượng tồn kho.

### 4.2. Phân hệ Kiểm kê (Màu Cam)
*   **`contains` (Kiểm kê → Chi tiết kiểm kê):** `Stocktakes` (1) <---> `Stocktake Details` (N). Một phiên kiểm kê gồm nhiều dòng, mỗi dòng ghi nhận số lượng thực tế của một lô hàng.
*   **`adjusts` (Chi tiết phiếu ↔ Chi tiết kiểm kê):** `Receipt Details` (N) <---> `Stocktake Details` (1). Các dòng của phiếu cân bằng kho liên kết ngược lại với dòng kiểm kê để đối soát.

---

## 5. SO SÁNH GIỮA SƠ ĐỒ KHÁI NIỆM (ERD) VÀ THIẾT KẾ CSDL VẬT LÝ (SQL SCHEMA)

| Mối quan hệ trên ERD | Thực thể kết nối | Hiện thực hóa trong SQL Schema |
| :--- | :--- | :--- |
| **`stores`** | `Branches` <---> `Inventories` | Bảng `inventories` có khóa ngoại `branch_id` trỏ tới `branches`. |
| **`tracks`** | `Products` <---> `Inventories` | Bảng `inventories` có khóa ngoại `product_id` trỏ tới `products`. Khóa nghiệp vụ: `(branch_id, product_id, mfg_date, exp_date)`. |
| **`categorizes`** | `Categories` <---> `Products` | Bảng `products` có khóa ngoại `category_id` trỏ tới `categories`. |
| **`has staff`** | `Branches` <---> `Users` | Bảng `users` có khóa ngoại `branch_id` trỏ tới `branches` (NULL cho ADMIN). |
| **`receives`** | `Customers` <---> `Receipts` | Bảng `receipts` có khóa ngoại `customer_id` trỏ tới `customers`. |
| **`creates`** (Phiếu kho) | `Users` <---> `Receipts` | Bảng `receipts` có khóa ngoại `created_by` trỏ tới `users`. |
| **`records`** | `Users` <---> `Audit Logs` | Bảng `audit_logs` có khóa ngoại `user_id` trỏ tới `users`. |
| **`performs at`** | `Branches` <---> `Stocktakes` | Bảng `stocktakes` có khóa ngoại `branch_id` trỏ tới `branches`. |
| **`creates`** (Kiểm kê) | `Users` <---> `Stocktakes` | Bảng `stocktakes` có khóa ngoại `created_by` trỏ tới `users`. |
| **`contains`** | `Receipts` <---> `Receipt Details` | Bảng `receipt_details` có khóa ngoại `receipt_id` trỏ tới `receipts` (CASCADE DELETE). |
| **`adjusts`** | `Receipt Details` <---> `Stocktake Details` | Bảng `stocktake_details` có cột `adjustment_receipt_id` (FK nullable) trỏ tới `receipts`. |
| **`generates`** | `Stocktake Details` <---> `Receipts` | Cột `adjustment_receipt_id` trong `stocktake_details` lưu ID phiếu cân bằng kho tự động sinh. |
