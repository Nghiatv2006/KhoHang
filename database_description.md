# Tài Liệu Mô Tả Chi Tiết Cơ Sở Dữ Liệu (Database Dictionary)

Hệ thống quản lý kho hàng nhiều chi nhánh sử dụng cơ sở dữ liệu quan hệ PostgreSQL. Dưới đây là mô tả chi tiết cấu trúc các bảng, các trường dữ liệu, khóa ngoại và quy tắc nghiệp vụ liên quan.

---

## 1. Các Kiểu Dữ Liệu Tự Định Nghĩa (Enum Types)

*   `user_role`: Phân quyền người dùng, gồm `ADMIN` (Quản trị viên hệ thống), `MANAGER` (Quản lý chi nhánh), `STAFF` (Nhân viên kho).
*   `user_status`: Trạng thái tài khoản người dùng, gồm `ACTIVE` (Đang hoạt động), `LOCKED` (Bị khóa).
*   `receipt_type`: Loại giao dịch kho, gồm `IMPORT` (Nhập từ nhà cung cấp), `EXPORT` (Xuất bán), `TRANSFER` (Điều chuyển nội bộ giữa 2 chi nhánh), `ADJUST_IN` (Cân bằng tăng), `ADJUST_OUT` (Cân bằng giảm).
*   `receipt_status`: Trạng thái phiếu kho, gồm `DRAFT` (Phiếu nháp - chưa cập nhật tồn kho), `COMPLETED` (Hoàn thành - đã thay đổi số lượng kho), `CANCELLED` (Đã hủy).
*   `stocktake_status`: Trạng thái phiên kiểm kê, gồm `DRAFT` (Đang kiểm đếm), `COMPLETED` (Đã hoàn tất), `CANCELLED` (Đã hủy).

---

## 2. Mô Tả Chi Tiết Các Bảng Dữ Liệu

### 2.1. Bảng Chi Nhánh (`branches`)
Lưu trữ thông tin các kho hàng/chi nhánh trong hệ thống.

| Tên Cột | Kiểu Dữ Liệu | Ràng Buộc | Mô Tả |
| :--- | :--- | :--- | :--- |
| `id` | SERIAL | PRIMARY KEY | ID tự tăng của chi nhánh |
| `name` | VARCHAR(255) | NOT NULL, UNIQUE | Tên chi nhánh (Ví dụ: Chi nhánh Hà Nội) |
| `address` | TEXT | NOT NULL | Địa chỉ vật lý của chi nhánh |
| `low_stock_threshold` | INT | NOT NULL, DEFAULT 5 | Ngưỡng báo động tồn kho thấp cho chi nhánh này |

### 2.2. Bảng Danh Mục Sản Phẩm (`categories`)
Phân loại nhóm sản phẩm trong hệ thống.

| Tên Cột | Kiểu Dữ Liệu | Ràng Buộc | Mô Tả |
| :--- | :--- | :--- | :--- |
| `id` | SERIAL | PRIMARY KEY | ID tự tăng của danh mục |
| `name` | VARCHAR(255) | NOT NULL, UNIQUE | Tên danh mục (Ví dụ: Điện thoại di động) |

### 2.3. Bảng Nhà Cung Cấp (`suppliers`)
Đối tác cung cấp hàng hóa cho kho.

| Tên Cột | Kiểu Dữ Liệu | Ràng Buộc | Mô Tả |
| :--- | :--- | :--- | :--- |
| `id` | SERIAL | PRIMARY KEY | ID tự tăng của nhà cung cấp |
| `name` | VARCHAR(255) | NOT NULL | Tên công ty/nhà cung cấp |
| `contact_info` | VARCHAR(255) | | Thông tin liên hệ (SĐT, Email...) |
| `address` | TEXT | | Địa chỉ nhà cung cấp |
| `debt` | NUMERIC(15,2) | NOT NULL, DEFAULT 0.00 | Công nợ hiện tại (Số tiền kho đang nợ nhà cung cấp) |
| `status` | VARCHAR(50) | NOT NULL, DEFAULT 'ACTIVE' | Trạng thái: `ACTIVE` (Hợp tác) / `INACTIVE` (Ngừng hợp tác) |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Ngày giờ tạo bản ghi |

### 2.4. Bảng Khách Hàng (`customers`)
Khách hàng mua sản phẩm từ kho.

| Tên Cột | Kiểu Dữ Liệu | Ràng Buộc | Mô Tả |
| :--- | :--- | :--- | :--- |
| `id` | SERIAL | PRIMARY KEY | ID tự tăng của khách hàng |
| `name` | VARCHAR(255) | NOT NULL | Họ và tên khách hàng |
| `contact_info` | VARCHAR(255) | | Số điện thoại hoặc thông tin liên lạc |
| `address` | TEXT | | Địa chỉ khách hàng |
| `debt` | NUMERIC(15,2) | NOT NULL, DEFAULT 0.00 | Công nợ hiện tại (Số tiền khách hàng đang nợ kho) |
| `status` | VARCHAR(50) | NOT NULL, DEFAULT 'ACTIVE' | Trạng thái: `ACTIVE` (Đang mua) / `INACTIVE` (Ngừng mua) |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Ngày giờ tạo bản ghi |

### 2.5. Bảng Người Dùng (`users`)
Nhân viên vận hành hệ thống.

| Tên Cột | Kiểu Dữ Liệu | Ràng Buộc | Mô Tả |
| :--- | :--- | :--- | :--- |
| `id` | SERIAL | PRIMARY KEY | ID tự tăng của tài khoản |
| `username` | VARCHAR(100) | NOT NULL, UNIQUE | Tên đăng nhập |
| `password` | VARCHAR(255) | NOT NULL | Mật khẩu đã mã hóa BCrypt |
| `full_name` | VARCHAR(255) | NOT NULL | Họ tên đầy đủ |
| `email` | VARCHAR(255) | UNIQUE | Địa chỉ email liên hệ |
| `role` | user_role | NOT NULL | Vai trò: `ADMIN`, `MANAGER`, `STAFF` |
| `branch_id` | INT | FOREIGN KEY | Chi nhánh làm việc (NULL nếu là ADMIN toàn quyền) |
| `status` | user_status | NOT NULL, DEFAULT 'ACTIVE' | Trạng thái: `ACTIVE` (Hoạt động) / `LOCKED` (Bị khóa) |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Ngày giờ tạo bản ghi |

*   *Ràng buộc đặc biệt:* `CHECK (role = 'ADMIN' OR branch_id IS NOT NULL)` (Nhân viên MANAGER/STAFF bắt buộc phải trực thuộc một chi nhánh cụ thể).

### 2.6. Bảng Sản Phẩm (`products`)
Danh mục sản phẩm kinh doanh.

| Tên Cột | Kiểu Dữ Liệu | Ràng Buộc | Mô Tả |
| :--- | :--- | :--- | :--- |
| `id` | SERIAL | PRIMARY KEY | ID tự tăng của sản phẩm |
| `code` | VARCHAR(50) | NOT NULL, UNIQUE | Mã sản phẩm viết hoa (Ví dụ: `IP15`), check uppercase |
| `name` | VARCHAR(255) | NOT NULL | Tên sản phẩm |
| `unit` | VARCHAR(50) | NOT NULL | Đơn vị tính (Ví dụ: Chiếc, Hộp, Chai...) |
| `price` | NUMERIC(15,2) | NOT NULL, CHECK (price >= 0) | Đơn giá niêm yết của sản phẩm |
| `category_id` | INT | FOREIGN KEY | Thuộc danh mục nào |
| `has_expiry` | BOOLEAN | NOT NULL, DEFAULT FALSE | Sản phẩm có hạn sử dụng (HSD) không |
| `mfg_date` | DATE | DEFAULT '1970-01-01' | Ngày sản xuất mặc định |
| `exp_date` | DATE | DEFAULT '1970-01-01' | Hạn sử dụng mặc định |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Ngày tạo sản phẩm |

*   *Ràng buộc đặc biệt:* `CHECK (exp_date >= mfg_date)` (Hạn sử dụng luôn luôn lớn hơn hoặc bằng ngày sản xuất).

### 2.7. Bảng Tồn Kho Theo Lô (`inventories`)
Quản lý tồn kho thực tế của từng sản phẩm tại từng chi nhánh theo từng lô sản xuất.

| Tên Cột | Kiểu Dữ Liệu | Ràng Buộc | Mô Tả |
| :--- | :--- | :--- | :--- |
| `id` | SERIAL | PRIMARY KEY | ID tự tăng |
| `branch_id` | INT | NOT NULL, FOREIGN KEY | Kho lưu trữ |
| `product_id` | INT | NOT NULL, FOREIGN KEY | Sản phẩm |
| `mfg_date` | DATE | NOT NULL, DEFAULT '1970-01-01' | Ngày sản xuất của lô hàng |
| `exp_date` | DATE | NOT NULL, DEFAULT '1970-01-01' | Hạn sử dụng của lô hàng |
| `quantity` | INT | NOT NULL, DEFAULT 0, CHECK (quantity >= 0) | Số lượng tồn kho hiện tại (Không được âm) |
| `last_updated` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Thời gian cập nhật số lượng cuối cùng |

*   *Ràng buộc đặc biệt:* `UNIQUE (branch_id, product_id, mfg_date, exp_date)` (Một sản phẩm tại một chi nhánh, tương ứng với một lô NSX/HSD chỉ có duy nhất một bản ghi tồn kho).

### 2.8. Bảng Phiếu Kho (`receipts`)
Lưu giữ thông tin chung về các giao dịch kho.

| Tên Cột | Kiểu Dữ Liệu | Ràng Buộc | Mô Tả |
| :--- | :--- | :--- | :--- |
| `id` | SERIAL | PRIMARY KEY | ID tự tăng |
| `code` | VARCHAR(50) | NOT NULL, UNIQUE | Mã phiếu duy nhất tự sinh |
| `type` | receipt_type | NOT NULL | Loại phiếu: `IMPORT`, `EXPORT`, `TRANSFER`, `ADJUST` |
| `status` | receipt_status| NOT NULL, DEFAULT 'COMPLETED' | Trạng thái: `DRAFT`, `COMPLETED`, `CANCELLED` |
| `payment_status` | VARCHAR(50) | NOT NULL, DEFAULT 'UNPAID' | Trạng thái thanh toán: `UNPAID` (Chưa trả), `PAID` (Đã thanh toán). Đối với phiếu `TRANSFER`, cột này được sử dụng để lưu trạng thái đi đường: `IN_TRANSIT` (Đang đi đường), `RECEIVED` (Đã nhận hàng và cộng kho đích) |
| `source_branch_id` | INT | FOREIGN KEY | Kho xuất hàng (NULL nếu là IMPORT, ADJUST_IN) |
| `dest_branch_id` | INT | FOREIGN KEY | Kho nhận hàng (NULL nếu là EXPORT, ADJUST_OUT) |
| `created_by` | INT | NOT NULL, FOREIGN KEY | Nhân viên lập phiếu |
| `supplier_id` | INT | FOREIGN KEY | Nhà cung cấp (Bắt buộc nếu là phiếu `IMPORT`) |
| `customer_id` | INT | FOREIGN KEY | Khách hàng (Bắt buộc nếu là phiếu `EXPORT`) |
| `description` | VARCHAR(500) | | Mô tả chi tiết hoặc lý do giao dịch |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Ngày lập phiếu |

### 2.9. Bảng Chi Tiết Phiếu Kho (`receipt_details`)
Lưu chi tiết danh sách sản phẩm và đơn giá tương ứng trong từng phiếu kho.

| Tên Cột | Kiểu Dữ Liệu | Ràng Buộc | Mô Tả |
| :--- | :--- | :--- | :--- |
| `id` | SERIAL | PRIMARY KEY | ID tự tăng |
| `receipt_id` | INT | NOT NULL, FOREIGN KEY | Liên kết với đầu phiếu (`receipts`) |
| `product_id` | INT | NOT NULL, FOREIGN KEY | Sản phẩm giao dịch |
| `mfg_date` | DATE | NOT NULL, DEFAULT '1970-01-01' | Ngày sản xuất của lô hàng giao dịch |
| `exp_date` | DATE | NOT NULL, DEFAULT '1970-01-01' | Hạn sử dụng của lô hàng giao dịch |
| `quantity` | INT | NOT NULL, CHECK (quantity > 0) | Số lượng giao dịch |
| `price` | NUMERIC(15,2) | NOT NULL, CHECK (price >= 0) | Đơn giá giao dịch thực tế tại thời điểm lập phiếu |

### 2.10. Bảng Kiểm Kê Kho (`stocktakes`)
Lưu thông tin phiên kiểm đếm kho.

| Tên Cột | Kiểu Dữ Liệu | Ràng Buộc | Mô Tả |
| :--- | :--- | :--- | :--- |
| `id` | SERIAL | PRIMARY KEY | ID tự tăng |
| `code` | VARCHAR(50) | NOT NULL, UNIQUE | Mã phiên kiểm kê |
| `branch_id` | INT | NOT NULL, FOREIGN KEY | Chi nhánh được kiểm kê |
| `created_by` | INT | NOT NULL, FOREIGN KEY | Người khởi tạo phiên |
| `status` | stocktake_status| NOT NULL, DEFAULT 'DRAFT' | Trạng thái kiểm kê |
| `notes` | TEXT | | Ghi chú, kết luận kiểm kê |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Thời gian lập |

### 2.11. Bảng Chi Tiết Kiểm Kê (`stocktake_details`)
Chi tiết kết quả kiểm đếm từng lô hàng sản phẩm trong phiên kiểm kê.

| Tên Cột | Kiểu Dữ Liệu | Ràng Buộc | Mô Tả |
| :--- | :--- | :--- | :--- |
| `id` | SERIAL | PRIMARY KEY | ID tự tăng |
| `stocktake_id` | INT | NOT NULL, FOREIGN KEY | Thuộc phiên kiểm kê nào |
| `product_id` | INT | NOT NULL, FOREIGN KEY | Sản phẩm kiểm kê |
| `mfg_date` | DATE | NOT NULL, DEFAULT '1970-01-01' | Lô sản xuất |
| `exp_date` | DATE | NOT NULL, DEFAULT '1970-01-01' | Hạn sử dụng |
| `expected_quantity` | INT | NOT NULL | Số lượng trên sổ sách hệ thống |
| `actual_quantity` | INT | NOT NULL | Số lượng kiểm đếm thực tế |
| `adjustment_receipt_id`| INT | FOREIGN KEY | Phiếu điều chỉnh tự sinh nếu có chênh lệch |

### 2.12. Bảng Yêu Cầu Chuyển Chi Nhánh (`branch_transfer_requests`)
Theo dõi việc xin chuyển nhân viên sang chi nhánh khác.

| Tên Cột | Kiểu Dữ Liệu | Ràng Buộc | Mô Tả |
| :--- | :--- | :--- | :--- |
| `id` | SERIAL | PRIMARY KEY | ID tự tăng |
| `staff_id` | INT | NOT NULL, FOREIGN KEY | Nhân viên cần chuyển |
| `from_branch_id` | INT | NOT NULL, FOREIGN KEY | Chi nhánh gốc |
| `to_branch_id` | INT | NOT NULL, FOREIGN KEY | Chi nhánh mới đề xuất |
| `created_by` | INT | NOT NULL, FOREIGN KEY | Manager gửi đề xuất |
| `status` | VARCHAR(50) | NOT NULL, DEFAULT 'PENDING' | Trạng thái quy trình 3 bước: `PENDING` (Chờ nhân viên xác nhận), `STAFF_CONFIRMED` (Chờ quản lý thông qua), `MANAGER_APPROVED` (Chờ Admin duyệt), `APPROVED` (Đã duyệt thành công), `REJECTED` (Đơn bị từ chối) |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Thời điểm đề xuất |
| `approved_by` | INT | FOREIGN KEY | Admin phê duyệt/từ chối |
| `approved_at` | TIMESTAMP | | Thời điểm phê duyệt |

### 2.13. Bảng Nhật Ký Hoạt Động (`audit_logs`)
Lưu lịch sử tác động dữ liệu.

| Tên Cột | Kiểu Dữ Liệu | Ràng Buộc | Mô Tả |
| :--- | :--- | :--- | :--- |
| `id` | SERIAL | PRIMARY KEY | ID tự tăng |
| `user_id` | INT | FOREIGN KEY | Ai là người thực hiện (Có thể NULL nếu user bị xóa) |
| `action` | VARCHAR(50) | NOT NULL | Hành động: `CREATE`, `UPDATE`, `DELETE`, `LOGIN`, `LOCK`... |
| `entity_name` | VARCHAR(100) | NOT NULL | Tên bảng bị tác động (Ví dụ: `users`, `products`) |
| `entity_id` | VARCHAR(50) | | ID của bản ghi bị tác động |
| `details` | TEXT | | Nội dung chi tiết thay đổi (JSON hoặc mô tả văn bản) |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Thời điểm phát sinh hành động |
