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
*   **`Branches` (Chi nhánh):** `id` (Khóa chính - gạch chân), `code` (Mã), `name` (Tên), `address` (Địa chỉ).
*   **`Categories` (Danh mục):** `id` (Khóa chính - gạch chân), `name` (Tên), `description` (Mô tả).
*   **`Suppliers` (Nhà cung cấp):** `id` (Khóa chính - gạch chân), `name` (Tên), `contact` (Thông tin liên hệ).

### 2.2. Phân hệ Nhân sự & Khách hàng (Xanh lá)
*   **`Customers` (Khách hàng):** `id` (Khóa chính - gạch chân), `name` (Tên), `email` (Email), `phone number` (Số điện thoại).
*   **`Users` (Người dùng):** `id` (Khóa chính - gạch chân), `username` (Tên đăng nhập), `password` (Mật khẩu đã mã hóa), `role` (Vai trò).

### 2.3. Phân hệ Sản phẩm & Phiếu kho (Vàng)
*   **`Products` (Sản phẩm):** `id` (Khóa chính - gạch chân), `code` (Mã), `name` (Tên), `price` (Đơn giá), `description` (Mô tả), `category_id` (Khóa ngoại danh mục).
*   **`Inventories` (Tồn kho):** `id` (Khóa chính - gạch chân), `quantity` (Số lượng tồn), `location` (Vị trí lưu trữ).
*   **`Receipts` (Phiếu kho):** `id` (Khóa chính - gạch chân), `type` (Loại phiếu), `status` (Trạng thái), `created date` (Ngày lập phiếu).
*   **`Receipt Details` (Chi tiết phiếu kho):** `id` (Khóa chính - gạch chân), `quantity` (Số lượng giao dịch), `unit price` (Đơn giá giao dịch).

### 2.4. Phân hệ Kiểm kê & Điều chuyển nhân sự (Cam)
*   **`Stocktakes` (Kiểm kê):** `id` (Khóa chính - gạch chân), `check date` (Ngày kiểm kê), `status` (Trạng thái).
*   **`Stocktake Details` (Chi tiết kiểm kê):** `id` (Khóa chính - gạch chân), `actual quantity` (Số lượng thực tế), `note` (Ghi chú).
*   **`Branch Transfer Requests` (Yêu cầu chuyển chi nhánh):** `id` (Khóa chính - gạch chân), `code` (Mã yêu cầu), `request date` (Ngày yêu cầu), `status` (Trạng thái), `from branch` (Chi nhánh nguồn hiện tại), `from branch actual quantity` (Số lượng thực tế tại chi nhánh nguồn). *Lưu ý: Trong CSDL thực tế, thực thể này quản lý điều chuyển công tác nhân viên và được chuẩn hóa thành các trường `staff_id`, `from_branch_id`, `to_branch_id`, `created_by`, `approved_by` để đảm bảo tính toàn vẹn dữ liệu.*

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
*   **Hình thoi `transfers` (Yêu cầu điều chuyển - Chi tiết kiểm kê):**
    *   *Kết nối:* `Branch Transfer Requests` (N) <---> `Stocktake Details` (1).
    *   *Ý nghĩa:* Mối quan hệ logic nghiệp vụ: khi nhân viên được điều chuyển sang chi nhánh mới, chi nhánh làm việc của họ thay đổi, quyết định kho hàng mà nhân viên đó sẽ thực hiện kiểm kê thực tế. (Mối quan hệ này không nối trực tiếp bằng khóa ngoại ở mức database mà đi gián tiếp qua thực thể `Users`).

---

## 4. Ý NGHĨA CÁC MỐI QUAN HỆ NỘI BỘ PHÂN HỆ (HÌNH THOI CÓ MÀU)

Các hình thoi này nằm hoàn toàn bên trong các phân vùng nghiệp vụ riêng lẻ:

### 4.1. Phân hệ Sản phẩm & Phiếu kho (Màu Vàng)
*   **Hình thoi `tracks` (Sản phẩm - Tồn kho):**
    *   *Kết nối:* `Products` (1) <---> `Inventories` (N).
    *   *Ý nghĩa:* Hệ thống theo dõi thông tin tồn kho chi tiết (số lượng, vị trí) của từng sản phẩm.
*   **Hình thoi `contains` (Sản phẩm - Chi tiết phiếu):**
    *   *Kết nối:* `Receipts` (1) <---> `Receipt Details` (N).
    *   *Ý nghĩa:* Phiếu kho chứa thông tin các dòng chi tiết sản phẩm cụ thể.
*   **Hình thoi `contains` có hướng (Tồn kho - Chi tiết phiếu):**
    *   *Kết nối:* `Inventories` (1) <---> `Receipt Details` (N).
    *   *Ý nghĩa:* Giao dịch xuất/nhập/điều chuyển hàng hóa chi tiết tác động trực tiếp và làm thay đổi số lượng tồn của sản phẩm tương ứng.

### 4.2. Phân hệ Kiểm kê & Điều chuyển (Màu Cam)
*   **Hình thoi `contains` (Kiểm kê - Chi tiết kiểm kê):**
    *   *Kết nối:* `Stocktakes` (1) <---> `Stocktake Details` (N).
    *   *Ý nghĩa:* Đợt kiểm kê gồm nhiều dòng sản phẩm cần đếm thực tế.
*   **Hình thoi `adjusts` (Chi tiết kiểm kê - Chi tiết phiếu kho):**
    *   *Kết nối:* `Stocktake Details` (N) <---> `Receipt Details` (1).
    *   *Ý nghĩa:* Kết quả kiểm đếm thực tế chênh lệch sẽ làm căn cứ để sinh phiếu cân bằng kho nhằm tự động điều chỉnh số lượng tồn.

---

## 5. SO SÁNH GIỮA SƠ ĐỒ KHÁI NIỆM (ERD) VÀ THIẾT KẾ CSDL VẬT LÝ (SQL SCHEMA)

Bảng đối chiếu cách sơ đồ khái niệm ERD được hiện thực hóa dưới database PostgreSQL:

| Mối quan hệ trên ERD | Thực thể kết nối trên ERD | Hiện thực hóa trong SQL Schema |
| :--- | :--- | :--- |
| **`stores`** | `Branches` <---> `Products` | Liên kết gián tiếp qua bảng `inventories` (chứa khóa ngoại `branch_id` và `product_id`). |
| **`tracks`** | `Products` <---> `Inventories` | Khóa ngoại `product_id` trong bảng `inventories` trỏ đến `products`. |
| **`supplies`** | `Suppliers` <---> `Products` | Liên kết gián tiếp qua bảng `receipts` (khóa ngoại `supplier_id`) và bảng `receipt_details` (khóa ngoại `product_id`). |
| **`adjusts`** | `Stocktake Details` <---> `Receipt Details` | Bảng `stocktake_details` chứa cột `adjustment_receipt_id` trỏ thẳng tới bảng `receipts` (đầu phiếu điều chỉnh). |
| **`has staff`** | `Branches` <---> `Users` | Bảng `users` chứa khóa ngoại `branch_id` trỏ tới bảng `branches`. |
| **`receives`** | `Customers` <---> `Receipts` | Bảng `receipts` chứa khóa ngoại `customer_id` trỏ tới bảng `customers`. |
| **`creates`** (Phiếu kho) | `Users` <---> `Receipts` | Bảng `receipts` chứa khóa ngoại `created_by` trỏ tới bảng `users`. |
| **`records`** | `Users` <---> `Audit Logs` | Bảng `audit_logs` chứa khóa ngoại `user_id` trỏ tới bảng `users`. |
| **`performs at`** | `Branches` <---> `Stocktakes` | Bảng `stocktakes` chứa khóa ngoại `branch_id` trỏ tới bảng `branches`. |
| **`creates`** (Kiểm kê) | `Users` <---> `Stocktakes` | Bảng `stocktakes` chứa khóa ngoại `created_by` trỏ tới bảng `users`. |
| **`requests`** | `Users` <---> `Branch Transfer Requests` | Bảng `branch_transfer_requests` chứa khóa ngoại `staff_id` hoặc `created_by` trỏ tới `users`. |
| **`transfers`** | `BTR` <---> `Stocktake Details` | Mối quan hệ logic nghiệp vụ. Trong CSDL thực tế, liên kết này được thực hiện gián tiếp thông qua việc cập nhật `branch_id` của nhân viên trong bảng `users` (BTR -> Users -> Stocktakes -> Stocktake Details), không có khóa ngoại nối trực tiếp giữa hai bảng này. |
