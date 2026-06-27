# Tài liệu Phân rã Use Case (Use Case Breakdown)

Tài liệu này mô tả chi tiết các Use Case phân rã của hệ thống Quản lý Kho hàng, dựa trên luồng nghiệp vụ hiện tại trong Codebase (Controllers, Services) và tương ứng với cấu trúc biểu đồ `.drawio` trong các thư mục `ADMIN`, `MANAGER`, `STAFF`.

## 1. Tác nhân (Actors)
1. **Quản trị viên (ADMIN):** Có toàn quyền trên hệ thống, quản lý tài khoản, cấu hình chi nhánh, sản phẩm, đối tác, nhật ký hoạt động (Audit Log), phiếu kho và kiểm kê toàn hệ thống. Mặc định thuộc kho tổng.
2. **Quản lý chi nhánh (MANAGER):** Quản lý nhân sự (STAFF) trong chi nhánh của mình, phê duyệt các phiếu kho do STAFF lập, quản lý sản phẩm, danh mục, đối tác toàn cục, và thao tác kiểm kê, nhật ký thuộc nhánh của mình.
3. **Nhân viên kho (STAFF):** Thực hiện các nghiệp vụ kho hàng ngày như lập phiếu nháp, kiểm đếm thực tế, tra cứu tồn kho, và xác nhận kiểm kê chứng từ (phiếu kho).

---

## 2. Phân hệ Nhân viên kho (STAFF)

### 01_xac_thuc
- Đăng nhập.
- Đăng xuất.

### 02_ho_so_ca_nhan
- Xem thông tin cá nhân.
- Đổi mật khẩu.

### 03_tong_quan
- Xem thống kê tổng quan (Dashboard).

### 04_phieu_kho
- Xem danh sách phiếu kho (thuộc nhánh).
- Lập phiếu kho nháp.
- Xác nhận kiểm kê phiếu (nhận hàng chuyển trạng thái PENDING_STOCKTAKE -> COMPLETED).
- In phiếu (PDF).

### 05_ton_kho
- Xem danh sách tồn kho.
- Lọc tồn kho theo hạn sử dụng.
- Cập nhật định mức tồn.

### 06_kiem_ke_kho
- Xem danh sách phiên kiểm kê (thuộc nhánh).
- Khởi tạo kiểm kê nháp.
- Nhập số lượng kiểm đếm thực tế.

### 07_doi_tac
- Xem danh sách đối tác.

---

## 3. Phân hệ Quản lý chi nhánh (MANAGER)

### 01_xac_thuc, 02_ho_so_ca_nhan, 03_tong_quan
- Tính năng tương tự STAFF.

### 04_phieu_kho
- Xem danh sách phiếu kho.
- Phê duyệt phiếu kho.
- Hủy phiếu kho.
- In phiếu (PDF).

### 05_san_pham & 06_danh_muc
- Xem, Thêm mới, Cập nhật, Xóa Danh mục và Sản phẩm (Toàn cục).

### 07_ton_kho
- Kế thừa toàn bộ quyền STAFF.

### 08_kiem_ke_kho
- Xem danh sách phiên kiểm kê.
- Khởi tạo kiểm kê.
- Nhập số lượng kiểm đếm.
- Xác nhận hoàn tất kiểm kê.
- `<<include>>` Tự động sinh phiếu điều chỉnh kho (ADJUST).

### 09_doi_tac
- Xem, Thêm, Cập nhật, Xóa đối tác.
- Vô hiệu hóa đối tác.
- `<<include>>` Chặn vô hiệu hóa / Xóa nếu khách hàng còn nợ.

### 10_nhan_vien
- Quản lý các nhân viên (STAFF) thuộc cùng chi nhánh.
- Thêm mới, Cập nhật, Xóa nhân viên.
- Khóa / Mở khóa tài khoản nhân viên.

### 11_nhat_ky
- Tra cứu Nhật ký hoạt động (Audit Log) cho các thao tác trong chi nhánh.

---

## 4. Phân hệ Quản trị viên hệ thống (ADMIN)

*Bao trùm toàn bộ quyền hạn của MANAGER, mở rộng thêm các thao tác trên toàn bộ hệ thống.*

### Các tính năng bao trùm (01 đến 10, 12)
- Xác thực, Hồ sơ, Tổng quan, Sản phẩm, Danh mục, Đối tác, Tồn kho, Phiếu kho, Kiểm kê kho, Nhật ký hoạt động: Kế thừa đầy đủ các Use Case của MANAGER nhưng ở cấp độ **toàn hệ thống** (không bị giới hạn bởi chi nhánh).

### Đặc quyền hệ thống: 11_chi_nhanh
- Xem danh sách tất cả các chi nhánh.
- Thêm mới chi nhánh.
- Cập nhật thông tin chi nhánh.
- Xóa chi nhánh.

### Quản lý nhân viên (10_nhan_vien)
- Có thể quản lý toàn bộ User bao gồm cả MANAGER.
- Thêm, sửa, xóa, khóa toàn quyền.
