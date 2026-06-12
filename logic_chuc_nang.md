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
  4. Sai thông tin → thông báo lỗi chung (không phân biệt sai user hay sai pass).
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
| IMPORT | NULL | Có | Nhập hàng từ NCC |
| EXPORT | Có | NULL | Xuất bán hàng |
| TRANSFER | Có | Có (khác nhau) | Điều chuyển nội bộ |
| ADJUST_IN | NULL | Có | Cân bằng tăng |
| ADJUST_OUT | Có | NULL | Cân bằng giảm |

### 5.2. Quy trình lập phiếu
1. Chọn loại phiếu → hệ thống tự sinh mã phiếu unique (prefix 2 ký tự + UUID 8 ký tự, retry tối đa 5 lần nếu trùng).
2. Chọn chi nhánh xuất/nhận (tùy loại). MANAGER/STAFF bị khóa vào chi nhánh của mình.
3. Thêm từng dòng sản phẩm vào phiếu nháp (draft):
   - Chọn sản phẩm → tự điền đơn vị, đơn giá (khóa không sửa giá), mặc định SL=1.
   - NSX/HSD: nếu SP có `hasExpiry=true` → tự tích checkbox, điền sẵn, khóa (không cho sửa). Nếu không → khóa checkbox.
   - Với EXPORT/TRANSFER: hiển thị tồn kho khả dụng theo lô, tự chọn lô đầu tiên (FIFO).
   - Validate: qty > 0; nếu xuất thì không được > tồn hiện có (tính cả draft đã thêm).
   - Nếu thêm trùng SP + lô → cộng dồn số lượng.
4. Có thể xóa dòng khỏi draft.
5. Xác nhận lập phiếu:
   - Validate: code, chi nhánh, ít nhất 1 dòng.
   - Tạo Receipt (status=COMPLETED) + ReceiptDetails.
   - **Cập nhật tồn kho trong cùng 1 transaction (@Transactional):**

### 5.3. Logic cập nhật tồn kho (ReceiptService.createReceipt)
```
IMPORT / ADJUST_IN:
  → destBranch: findOrCreate Inventory(branch, product, mfgDate, expDate)
  → quantity += detail.quantity
  → lastUpdated = now()

EXPORT / ADJUST_OUT:
  → sourceBranch: find Inventory (phải tồn tại, nếu không → RuntimeException)
  → if quantity < detail.quantity → RuntimeException "Không đủ tồn kho"
  → quantity -= detail.quantity
  → lastUpdated = now()

TRANSFER:
  → sourceBranch: trừ kho (giống EXPORT)
  → destBranch: cộng kho (giống IMPORT)
```
- Nếu bất kỳ dòng nào lỗi → toàn bộ rollback (nhờ @Transactional).
- Quantity không bao giờ < 0 (check ở cả application lẫn DB constraint).

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
- User bị khóa → bị đá ra khỏi hệ thống ở thao tác tiếp theo (checkCurrentUserActive).

### 7.5. Xóa user
- Confirm dialog trước.
- Không tự xóa chính mình.
- Thất bại nếu user đã có phiếu giao dịch (FK reference → exception).

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
