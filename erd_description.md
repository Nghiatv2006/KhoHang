# TÀI LIỆU MÔ TẢ CHI TIẾT SƠ ĐỒ ERD (CHEN NOTATION)
## HỆ THỐNG QUẢN LÝ KHO HÀNG ĐA CHI NHÁNH

Tài liệu này mô tả chi tiết các thành phần, cấu trúc thực thể, thuộc tính và ý nghĩa nghiệp vụ của tất cả các mối quan hệ (hình thoi) trong sơ đồ ERD chuẩn dạng Chen (Chen's Notation) của dự án dựa trên file thiết kế `erd-complete-er-edited.svg`.

---

## 1. PHÂN VÙNG PHÂN HỆ NGHIỆP VỤ (COLOR-CODED MODULES)

Sơ đồ ERD được chia làm 5 cụm nghiệp vụ tương ứng với 5 phân vùng màu sắc:

1. **Phân hệ Danh mục & Đối tác (Màu xanh dương - Blue):**
   * `Branches` (Chi nhánh): Quản lý thông tin chi nhánh kho vật lý.
   * `Categories` (Danh mục): Phân loại sản phẩm.
   * `Suppliers` (Nhà cung cấp): Đối tác cung ứng sản phẩm.
2. **Phân hệ Nhân sự & Khách hàng (Màu xanh lá - Green):**
   * `Users` (Người dùng/Nhân viên): Tài khoản đăng nhập và phân quyền hệ thống.
   * `Customers` (Khách hàng): Đối tác mua hàng hóa đầu ra.
3. **Phân hệ Sản phẩm & Phiếu kho (Màu vàng - Yellow):**
   * `Products` (Sản phẩm): Thông tin sản phẩm, đơn giá, danh mục.
   * `Inventories` (Tồn kho): Số lượng tồn thực tế của các sản phẩm.
   * `Receipts` (Phiếu kho): Các giao dịch nhập, xuất, điều chuyển kho.
   * `Receipt Details` (Chi tiết phiếu kho): Danh sách mặt hàng, số lượng và giá của từng dòng giao dịch.
4. **Phân hệ Kiểm kê & Điều chuyển nhân sự (Màu cam - Orange):**
   * `Stocktakes` (Kiểm kê): Đợt đối soát hàng hóa định kỳ.
   * `Stocktake Details` (Chi tiết kiểm kê): Ghi nhận chênh lệch kiểm đếm từng mặt hàng.
   * `Branch Transfer Requests` (Yêu cầu chuyển chi nhánh): Đề xuất điều chuyển công tác cho nhân viên.
5. **Phân hệ Nhật ký hệ thống (Màu tím - Purple):**
   * `Audit Logs` (Nhật ký hoạt động): Ghi vết thao tác người dùng để phục vụ giám sát hệ thống.

---

## 2. CẤU TRÚC CHI TIẾT CÁC THỰC THỂ (ENTITIES) & THUỘC TÍNH (ATTRIBUTES)

Dưới đây là danh sách thuộc tính của từng thực thể được vẽ trong sơ đồ:

### 2.1. Phân hệ Danh mục & Đối tác (Xanh dương)
*   **`Branches` (Chi nhánh):** `id` (Khóa chính - gạch chân), `name` (Tên), `address` (Địa chỉ), `low_stock_threshold` (Ngưỡng cảnh báo tồn kho thấp).
*   **`Categories` (Danh mục):** `id` (Khóa chính - gạch chân), `name` (Tên danh mục).
*   **`Suppliers` (Nhà cung cấp):** `id` (Khóa chính - gạch chân), `name` (Tên), `contact` (Thông tin liên hệ), `debt` (Công nợ phải trả), `status` (Trạng thái hoạt động: ACTIVE/INACTIVE).

### 2.2. Phân hệ Nhân sự & Khách hàng (Xanh lá)
*   **`Customers` (Khách hàng):** `id` (Khóa chính - gạch chân), `name` (Tên), `email` (Email), `phone number` (Số điện thoại), `debt` (Công nợ khách hàng phải trả), `status` (Trạng thái hoạt động: ACTIVE/INACTIVE).
*   **`Users` (Người dùng):** `id` (Khóa chính - gạch chân), `username` (Tên đăng nhập), `password` (Mật khẩu đã mã hóa), `role` (Vai trò: ADMIN/MANAGER/STAFF), `branch_id` (Chi nhánh làm việc), `email` (Email), `status` (Trạng thái tài khoản: ACTIVE/LOCKED).

### 2.3. Phân hệ Sản phẩm & Phiếu kho (Vàng)
*   **`Products` (Sản phẩm):** `id` (Khóa chính - gạch chân), `code` (Mã sản phẩm - viết hoa), `name` (Tên), `price` (Đơn giá), `unit` (Đơn vị tính), `category_id` (Khóa ngoại danh mục), `has_expiry` (Có quản lý hạn sử dụng không), `mfg_date` (Ngày sản xuất mặc định), `exp_date` (Hạn sử dụng mặc định).
*   **`Inventories` (Tồn kho):** `id` (Khóa chính - gạch chân), `quantity` (Số lượng tồn), `mfg_date` (Ngày sản xuất của lô), `exp_date` (Hạn sử dụng của lô), `last_updated` (Thời điểm cập nhật gần nhất). *Khóa nghiệp vụ duy nhất: `(branch_id, product_id, mfg_date, exp_date)` - quản lý tồn kho theo từng lô.*
*   **`Receipts` (Phiếu kho):** `id` (Khóa chính - gạch chân), `type` (Loại phiếu: IMPORT/EXPORT/TRANSFER/ADJUST_IN/ADJUST_OUT), `status` (Trạng thái: DRAFT/COMPLETED/CANCELLED), `created_at` (Ngày lập phiếu), `payment_status` (Trạng thái thanh toán: UNPAID/PAID).
*   **`Receipt Details` (Chi tiết phiếu kho):** `id` (Khóa chính - gạch chân), `quantity` (Số lượng giao dịch), `price` (Đơn giá giao dịch), `mfg_date` (Ngày sản xuất lô hàng), `exp_date` (Hạn sử dụng lô hàng).

### 2.4. Phân hệ Kiểm kê & Điều chuyển nhân sự (Cam)
*   **`Stocktakes` (Kiểm kê):** `id` (Khóa chính - gạch chân), `check date` (Ngày kiểm kê), `status` (Trạng thái: DRAFT/COMPLETED/CANCELLED).
*   **`Stocktake Details` (Chi tiết kiểm kê):** `id` (Khóa chính - gạch chân), `actual_quantity` (Số lượng thực tế đếm được), `expected_quantity` (Số lượng sổ sách - để đối chiếu chênh lệch), `adjustment_receipt_id` (Khóa ngoại trỏ tới phiếu cân bằng kho tự động sinh), `notes` (Ghi chú).
*   **`Branch Transfer Requests` (Yêu cầu chuyển chi nhánh):** `id` (Khóa chính - gạch chân), `staff_id` (Nhân viên cần điều chuyển), `from_branch_id` (Chi nhánh nguồn), `to_branch_id` (Chi nhánh đích), `created_by` (Người lập yêu cầu - Manager hoặc Staff), `approved_by` (Admin duyệt cuối), `created_at` (Ngày tạo yêu cầu), `status` (Trạng thái: PENDING/STAFF_CONFIRMED/MANAGER_APPROVED/APPROVED/REJECTED).

### 2.5. Phân hệ Nhật ký hệ thống (Tím)
*   **`Audit Logs` (Nhật ký):** `id` (Khóa chính - gạch chân), `action` (Hành động), `time` (Thời gian ghi log), `details` (Thông tin chi tiết).

---

## 3. Ý NGHĨA CÁC MỐI QUAN HỆ LIÊN PHÂN HỆ (HÌNH THOI MÀU TRẮNG)

Các hình thoi màu trắng thể hiện sự liên kết nghiệp vụ giữa các thực thể thuộc các phân hệ khác nhau. Trên sơ đồ ERD mới, tất cả mối quan hệ này đều là mối quan hệ 2 bên (Binary Relationships):

*   **Hình thoi `stores` (Chi nhánh - Sản phẩm):**
    *   *Kết nối:* `Branches` (1) <---> `Products` (N).
    *   *Ý nghĩa:* Chi nhánh lưu giữ/chứa sản phẩm. Mối quan hệ nối trực tiếp từ thực thể `Branches` sang `Products`.
*   **Hình thoi `categorizes` (Danh mục - Sản phẩm):**
    *   *Kết nối:* `Categories` (1) <---> `Products` (N).
    *   *Ý nghĩa:* Danh mục phân loại các sản phẩm khác nhau.
*   **Hình thoi `has staff` (Chi nhánh - Nhân viên):**
    *   *Kết nối:* `Branches` (1) <---> `Users` (N).
    *   *Ý nghĩa:* Một chi nhánh quản lý/sử dụng nhiều nhân viên kho làm việc.
*   **Hình thoi `supplies` (Nhà cung cấp - Sản phẩm):**
    *   *Kết nối:* `Suppliers` (1) <---> `Products` (N).
    *   *Ý nghĩa:* Nhà cung cấp cung ứng sản phẩm đầu vào cho hệ thống.
*   **Hình thoi `receives` (Khách hàng - Phiếu kho):**
    *   *Kết nối:* `Customers` (1) <---> `Receipts` (N).
    *   *Ý nghĩa:* Khách hàng nhận sản phẩm thông qua các phiếu xuất kho.
*   **Hình thoi `creates` (Nhân viên - Phiếu kho):**
    *   *Kết nối:* `Users` (1) <---> `Receipts` (N).
    *   *Ý nghĩa:* Nhân viên kho chịu trách nhiệm trực tiếp khởi tạo phiếu nhập/xuất/điều chuyển kho.
*   **Hình thoi `records` (Nhân viên - Nhật ký hệ thống):**
    *   *Kết nối:* `Users` (1) <---> `Audit Logs` (N).
    *   *Ý nghĩa:* Hệ thống ghi nhận và ghi vết các thao tác của nhân viên vào nhật ký hoạt động.
*   **Hình thoi `requests` (Nhân viên - Yêu cầu điều chuyển):**
    *   *Kết nối:* `Users` (1) <---> `Branch Transfer Requests` (N).
    *   *Ý nghĩa:* Nhân viên quản lý lập hoặc nhân viên kho gửi yêu cầu điều chuyển chi nhánh công tác.
*   **Hình thoi `creates` (Nhân viên - Kiểm kê):**
    *   *Kết nối:* `Users` (1) <---> `Stocktakes` (N).
    *   *Ý nghĩa:* Nhân viên chịu trách nhiệm tạo phiên kiểm đếm kho thực tế.
*   **Hình thoi `performs at` (Chi nhánh - Kiểm kê):**
    *   *Kết nối:* `Branches` (1) <---> `Stocktakes` (N).
    *   *Ý nghĩa:* Đợt kiểm kê được thực hiện trực tiếp tại một chi nhánh kho nhất định.
*   **Hình thoi `generates` (Chi tiết kiểm kê - Phiếu kho):**
    *   *Kết nối:* `Stocktake Details` (N) <---> `Receipts` (1).
    *   *Ý nghĩa:* Khi hoàn tất phiên kiểm kê, nếu phát hiện chênh lệch giữa số lượng thực tế và sổ sách, hệ thống tự động sinh ra một phiếu cân bằng kho (`ADJUST_IN` nếu thừa, `ADJUST_OUT` nếu thiếu). ID của phiếu cân bằng này được lưu vào cột `adjustment_receipt_id` của dòng chi tiết kiểm kê tương ứng.

---

## 4. Ý NGHĨA CÁC MỐI QUAN HỆ NỘI BỘ PHÂN HỆ (HÌNH THOI CÓ MÀU)

Các hình thoi này nằm hoàn toàn bên trong các phân vùng nghiệp vụ riêng lẻ:

### 4.1. Phân hệ Sản phẩm & Phiếu kho (Màu Vàng)
*   **Hình thoi `tracks` (Sản phẩm - Tồn kho):**
    *   *Kết nối:* `Products` (1) <---> `Inventories` (N).
    *   *Ý nghĩa:* Hệ thống theo dõi tồn kho chi tiết theo từng lô (mfg_date, exp_date) của từng sản phẩm tại từng chi nhánh.
*   **Hình thoi `contains` (Phiếu kho - Chi tiết phiếu):**
    *   *Kết nối:* `Receipts` (1) <---> `Receipt Details` (N).
    *   *Ý nghĩa:* Phiếu kho chứa nhiều dòng chi tiết sản phẩm, mỗi dòng ghi nhận một lô hàng cụ thể.
*   **Hình thoi `contains` (Tồn kho - Chi tiết phiếu):**
    *   *Kết nối:* `Inventories` (1) <---> `Receipt Details` (N).
    *   *Ý nghĩa:* Chi tiết giao dịch nhập/xuất tác động trực tiếp vào số lượng tồn của lô hàng tương ứng.

### 4.2. Phân hệ Kiểm kê & Điều chuyển (Màu Cam)
*   **Hình thoi `contains` (Kiểm kê - Chi tiết kiểm kê):**
    *   *Kết nối:* `Stocktakes` (1) <---> `Stocktake Details` (N).
    *   *Ý nghĩa:* Một phiên kiểm kê gồm nhiều dòng, mỗi dòng ghi nhận số lượng thực tế của một lô hàng.
*   **Hình thoi `adjusts` (Chi tiết phiếu - Chi tiết kiểm kê):**
    *   *Kết nối:* `Receipt Details` (N) <---> `Stocktake Details` (1).
    *   *Ý nghĩa:* Các dòng chi tiết của phiếu cân bằng kho tự động sinh liên kết ngược lại với dòng kiểm kê tương ứng để đối soát.

---

## 5. SO SÁNH GIỮA SƠ ĐỒ KHÁI NIỆM (ERD) VÀ THIẾT KẾ CSDL VẬT LÝ (SQL SCHEMA)

Bảng đối chiếu cách sơ đồ khái niệm ERD được hiện thực hóa dưới database PostgreSQL:

| Mối quan hệ trên ERD | Thực thể kết nối trên ERD | Hiện thực hóa trong SQL Schema |
| :--- | :--- | :--- |
| **`stores`** | `Branches` <---> `Products` | Liên kết gián tiếp qua bảng `inventories` (chứa khóa ngoại `branch_id` và `product_id`). |
| **`tracks`** | `Products` <---> `Inventories` | Khóa ngoại `product_id` trong bảng `inventories` trỏ đến `products`. Khóa nghiệp vụ: `(branch_id, product_id, mfg_date, exp_date)`. |
| **`supplies`** | `Suppliers` <---> `Products` | Liên kết gián tiếp qua bảng `receipts` (khóa ngoại `supplier_id`) và bảng `receipt_details` (khóa ngoại `product_id`). |
| **`adjusts`** | `Receipt Details` <---> `Stocktake Details` | Các dòng chi tiết phiếu cân bằng (`ADJUST_IN/OUT`) liên kết ngược lại với dòng chi tiết kiểm kê để đối soát. |
| **`generates`** | `Stocktake Details` <---> `Receipts` | Bảng `stocktake_details` chứa cột `adjustment_receipt_id` là khóa ngoại trỏ tới bảng `receipts` — lưu ID phiếu cân bằng kho được tự động sinh khi hoàn tất kiểm kê. |
| **`has staff`** | `Branches` <---> `Users` | Bảng `users` chứa khóa ngoại `branch_id` trỏ tới bảng `branches`. |
| **`receives`** | `Customers` <---> `Receipts` | Bảng `receipts` chứa khóa ngoại `customer_id` trỏ tới bảng `customers`. |
| **`creates`** (Phiếu kho) | `Users` <---> `Receipts` | Bảng `receipts` chứa khóa ngoại `created_by` trỏ tới bảng `users`. |
| **`records`** | `Users` <---> `Audit Logs` | Bảng `audit_logs` chứa khóa ngoại `user_id` trỏ tới bảng `users`. |
| **`performs at`** | `Branches` <---> `Stocktakes` | Bảng `stocktakes` chứa khóa ngoại `branch_id` trỏ tới bảng `branches`. |
| **`creates`** (Kiểm kê) | `Users` <---> `Stocktakes` | Bảng `stocktakes` chứa khóa ngoại `created_by` trỏ tới bảng `users`. |
| **`requests`** | `Users` <---> `Branch Transfer Requests` | Bảng `branch_transfer_requests` chứa khóa ngoại `staff_id` (nhân viên được chuyển) và `created_by` (người tạo yêu cầu) cùng trỏ tới `users`. |
