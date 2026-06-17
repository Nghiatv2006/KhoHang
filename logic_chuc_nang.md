# Mô Tả Logic Các Chức Năng Hiện Có

> Tài liệu tham chiếu khi refactor sang Spring Boot REST API + Vue.js.
> Mỗi mục mô tả: mục đích, input/output, logic xử lý, quy tắc nghiệp vụ, phân quyền.

---

## 1. Xác thực & Phiên làm việc (Authentication)

### 1.1. Đăng nhập
- **Input:** username (string), password (plaintext)
- **Logic:**
  1. Tìm user theo username trong DB.
  2. So khớp password nhập vào với BCrypt hash trong DB (`passwordEncoder.matches`).
  3. Nếu user tồn tại + đúng mật khẩu → kiểm tra `status`:
     - `LOCKED` → từ chối, thông báo "tài khoản bị khóa".
     - `ACTIVE` → đăng nhập thành công, lưu user vào session.
*   **Bước 2:** Lưu thông tin vào database.
*   **Bước 3 (Khởi tạo tồn kho):** Hệ thống tự động tạo 1 dòng tồn kho (`inventories`) cho **Kho Tổng** với `quantity = 0`. (Giả định Kho Tổng có `id` = 1 hoặc được xác định qua cơ chế riêng). Các Kho Con không được tạo tồn kho tự động.hông báo lỗi chung (không phân biệt sai user hay sai pass).
- **Output:** User object (id, username, fullName, role, branch, status)
- **Phân quyền:** Không (ai cũng gọi được)

### 1.2. Đăng xuất
- Xóa session hiện tại, quay về màn hình đăng nhập.

### 1.3. Kiểm tra tài khoản còn hoạt động (checkCurrentUserActive)
- Mỗi thao tác quan trọng đều gọi lại DB kiểm tra user hiện tại có bị LOCKED/xóa không.
- Nếu bị khóa giữa chừng → ép đăng xuất ngay lập tức.

---

## 2. Quản lý Sản phẩm (Product CRUD)

### 2.1. Danh sách & Tìm kiếm
- **GET tất cả** hoặc **tìm kiếm** theo keyword (khớp name HOẶC code, case-insensitive).
- Trả về: id, code, name, unit, price, category, hasExpiry, mfgDate, expDate.

### 2.2. Thêm sản phẩm
- **Input:** code (unique, uppercase), name, unit, price, categoryId, hasExpiry, mfgDate?, expDate?
- **Quy tắc:**
  - Code phải unique (check DB trước khi lưu).
  - Price >= 0.
  - Nếu `hasExpiry = true` → bắt buộc có mfgDate, expDate; mfgDate <= today; expDate >= mfgDate.
  - Nếu `hasExpiry = false` → mfgDate = expDate = 1970-01-01.
- **Phân quyền:** ADMIN, MANAGER (STAFF bị cấm)

### 2.3. Sửa sản phẩm
- Code **không được đổi** (field readOnly khi edit).
- **Không được đổi `hasExpiry`** nếu sản phẩm đã có tồn kho hoặc đã xuất hiện trong phiếu giao dịch (vì thay đổi cấu trúc key tồn kho).
- Các trường khác (name, unit, price, category, mfgDate, expDate) cho phép sửa.
- **Phân quyền:** ADMIN, MANAGER

### 2.4. Xóa sản phẩm
- Xóa confirm dialog trước.
- **Không được xóa** nếu sản phẩm đã có trong `receipt_details` (FK RESTRICT) → bắt exception.
- **Phân quyền:** ADMIN, MANAGER

---

## 3. Quản lý Danh mục (Category)

- Hiện chỉ dùng qua ComboBox khi tạo/sửa sản phẩm.
- CRUD cơ bản: id, name (unique).

### 3.1. Phân quyền
*   **Role hợp lệ**: `ADMIN`, `MANAGER`
*   **Ngoại lệ**: `STAFF` không có quyền truy cập.quan chi nhánh của mình (source OR dest = chi nhánh mình).

---

## 4. Quản lý Tồn kho (Inventory)

### 4.1. Xem tồn kho
- **Lọc theo chi nhánh:** ADMIN thấy tất cả hoặc chọn chi nhánh cụ thể; MANAGER/STAFF chỉ thấy chi nhánh của mình.
- **Tìm kiếm:** filter theo tên hoặc mã sản phẩm.
- **Sắp xếp mặc định:** theo `lastUpdated DESC` (sản phẩm vừa thay đổi nổi lên trên).
- **Cảnh báo tồn kho thấp:** nếu quantity <= `lowStockThreshold` → highlight đỏ.

### 4.2. Ngưỡng cảnh báo (lowStockThreshold)
- Mỗi chi nhánh có `low_stock_threshold` riêng (lưu DB, bảng `branches`).
- ADMIN xem "Tất cả": dùng giá trị toàn cục (lưu Preferences, persistent giữa phiên).
- Khi thay đổi ngưỡng:
  - Nếu đang chọn 1 chi nhánh → lưu vào DB (`branches.low_stock_threshold`).
  - Nếu ADMIN xem "Tất cả" → lưu vào Preferences local.

### 4.3. Cân bằng kho (Inventory Audit — thủ công)
- Từ Dashboard, chọn 1 dòng tồn kho → nhập số lượng thực tế.
- Nếu thực tế > sổ sách → hệ thống tự tạo phiếu `ADJUST_IN` (bổ sung chênh lệch).
- Nếu thực tế < sổ sách → hệ thống tự tạo phiếu `ADJUST_OUT` (trừ chênh lệch).
- Phiếu cân bằng được gán cho user hiện tại.

### 4.4. Cấu trúc key tồn kho
- Composite key: `(branch_id, product_id, mfg_date, exp_date)`
- Cho phép cùng 1 sản phẩm có nhiều lô hàng khác nhau tại 1 kho.
- Sản phẩm không có HSD: mfg_date = exp_date = 1970-01-01.

---

## 5. Lập Phiếu Kho (Receipt / Transaction)

### 5.1. Các loại phiếu

| Loại | source_branch | dest_branch | Ý nghĩa |
|------|:---:|:---:|---|
| IMPORT | NULL | Có | Nhập hàng vào chi nhánh Tổng (không gắn nhà cung cấp) |
| EXPORT | Có | NULL | Xuất bán hàng cho khách hàng |
| TRANSFER | Có | Có (khác nhau) | Điều chuyển nội bộ từ chi nhánh Tổng xuống chi nhánh Con |
| ADJUST_IN | NULL | Có | Cân bằng tăng (do kiểm kê phát hiện thừa) |
| ADJUST_OUT | Có | NULL | Cân bằng giảm (do kiểm kê phát hiện thiếu) |

### 5.2. Quy trình lập phiếu
1. Chọn loại phiếu → hệ thống tự sinh mã phiếu unique (prefix 2 ký tự + UUID 8 ký tự, retry tối đa 5 lần nếu trùng).
2. Lập chi tiết phiếu:
   * **Quyền:** `STAFF` tại Kho Con lập phiếu. `ADMIN` trực tiếp lập phiếu tại Kho Tổng.
   * Ràng buộc: `quantity > 0` và `price >= 0`.
   * Đối với `EXPORT`, `TRANSFER`, `ADJUST_OUT`: Hệ thống kiểm tra số lượng xuất không được vượt quá `quantity` hiện có trong bảng `inventories` của lô hàng tương ứng tại `source_branch_id`.
3. Lưu nháp (Trạng thái `DRAFT`).

### 5.3. Quy trình duyệt phiếu
*   **Quyền:** `MANAGER` duyệt phiếu tại Kho Con. `ADMIN` trực tiếp duyệt phiếu tại Kho Tổng.
*   **Hành động:** Chuyển trạng thái từ `DRAFT` → `COMPLETED` (hoặc `CANCELLED` nếu từ chối).
*   **Khi duyệt (COMPLETED):**
    * Đối với `IMPORT`, `ADJUST_IN`: Cộng số lượng vào bảng `inventories` của `dest_branch_id` (nếu chưa có lô hàng thì `INSERT`, nếu có thì `UPDATE`).
    * Đối với `EXPORT`, `ADJUST_OUT`: Trừ số lượng khỏi bảng `inventories` của `source_branch_id`.
    * Đối với `TRANSFER`: Trừ số lượng ở `source_branch_id` và cộng số lượng ở `dest_branch_id`.
*   **Lưu ý:** Phiếu đã `COMPLETED` hoặc `CANCELLED` sẽ bị khóa vĩnh viễn (không được UPDATE/DELETE).

### 5.4. Ghi chú phiếu
- Field `description` (tối đa 500 ký tự), tùy chọn.

---

## 6. Lịch sử Giao dịch (Transaction History)

### 6.1. Xem & Lọc
- Lọc theo: loại phiếu, kho xuất, kho nhận, người lập, khoảng thời gian (từ–đến).
- Mặc định lọc ngày = hôm nay.
- Click vào phiếu → hiển thị chi tiết (danh sách sản phẩm + giá giao dịch + NSX/HSD).

### 6.2. Phân quyền
- ADMIN: thấy tất cả.
- MANAGER/STAFF: thấy phiếu liên quan chi nhánh của mình (source OR dest = chi nhánh mình).

---

## 7. Quản lý Người dùng (User Management)

### 7.1. Phân quyền truy cập
- **ADMIN:** thấy tất cả user, CRUD toàn quyền.
- **MANAGER:** chỉ thấy STAFF cùng chi nhánh, có thể thêm/sửa/xóa/khóa STAFF. Không thấy ADMIN/MANAGER khác.
- **STAFF:** không có quyền vào module này.

### 7.2. Thêm user
- Input: username (unique), password (sẽ hash BCrypt), fullName, role, branchId, status.
- MANAGER tạo → role bắt buộc = STAFF, branch bắt buộc = chi nhánh của mình.
- Validate: username không trùng, password bắt buộc khi tạo mới, role != ADMIN thì phải chọn branch.

### 7.3. Sửa user
- Không tự hạ quyền ADMIN của chính mình.
- Không tự khóa chính mình.
- Password: chỉ nhập khi muốn đổi; bỏ trống = giữ mật khẩu cũ.
- Nếu sửa chính mình → cập nhật lại session hiển thị.

### 7.4. Khóa / Mở khóa
- Toggle status ACTIVE ↔ LOCKED.
- Không tự khóa chính mình.
- User bị khóa → bị đá ra khỏi hệ thống ở thao tác tiếp theo hoặc ngay lập tức (checkCurrentUserActive).

### 7.5. Xóa user
- Confirm dialog trước.
- Không tự xóa chính mình.
- Thất bại nếu user đã có phiếu giao dịch (FK reference → exception). Khuyến nghị khoá tài khoản hơn

### 7.6. Bộ lọc
- Tìm kiếm: theo username hoặc fullName.
- Lọc: theo role, branch, status.

---

## 8. Quản lý Chi nhánh (Branch Management)

### 8.1. Phân quyền
- Chỉ ADMIN mới thấy và thao tác được.

### 8.2. CRUD
- **Thêm:** name (unique), address (bắt buộc), lowStockThreshold (mặc định 5).
- **Sửa:** đổi tên (check unique trừ chính nó), address, threshold.
- **Xóa:** thất bại nếu còn user, tồn kho, hoặc phiếu liên quan (FK RESTRICT/CASCADE).
- Sau mỗi thao tác: refresh tất cả ComboBox chi nhánh trong app (dashboard filter, receipt form, history filter, user form).

### 8.3. Bộ lọc
- Tìm kiếm theo tên hoặc địa chỉ.

---

## 9. Phân quyền tổng hợp (RBAC)

| Chức năng | ADMIN | MANAGER | STAFF |
|-----------|:-----:|:-------:|:-----:|
| Xem tồn kho | Tất cả CN | CN mình | CN mình |
| Cài ngưỡng cảnh báo | Toàn cục + theo CN | CN mình | CN mình |
| CRUD sản phẩm | ✅ | ✅ | ❌ |
| Lập phiếu kho | ✅ (chọn bất kỳ CN) | ✅ (khóa CN mình) | ✅ (khóa CN mình) |
| Xem lịch sử | Tất cả | CN mình | CN mình |
| Quản lý user | Tất cả | STAFF cùng CN | ❌ |
| Quản lý chi nhánh | ✅ | ❌ | ❌ |

---

## 10. Quy tắc kỹ thuật quan trọng

- **Mật khẩu:** luôn hash BCrypt trước khi lưu. Nếu string không bắt đầu bằng `$2a$` hoặc `$2b$` → tự encode.
- **Ngày mặc định:** 1970-01-01 = "không áp dụng" (sản phẩm không có HSD). Hiển thị "-" trên UI.
- **Transaction:** `createReceipt` chạy trong @Transactional(rollbackFor = Exception.class). Một lỗi = rollback toàn bộ.
- **Mã phiếu unique:** prefix + UUID 8 char, retry 5 lần. Fallback: timestamp.
- **Hiển thị tiền:** dùng Locale GERMANY (dấu chấm phân cách nghìn), pattern `#,##0`.
- **Hiển thị ngày:** dd/MM/yyyy.
- **Sắp xếp tồn kho:** lastUpdated DESC.
- **Kiểm tra "sản phẩm đã dùng":** query `inventories` + `receipt_details` bằng productId (DB query, không load all).
