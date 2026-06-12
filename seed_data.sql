-- ==============================================================================
-- DỮ LIỆU MẪU (SEED DATA)
-- Lưu ý: Hãy chạy toàn bộ schema.sql trước khi chạy file này.
-- ==============================================================================

-- 1. Xoá dữ liệu cũ (Tuỳ chọn: Nếu bạn muốn chạy file này nhiều lần để reset dữ liệu)
-- TRUNCATE audit_logs, stocktake_details, stocktakes, receipt_details, receipts, inventories, products, users, customers, suppliers, categories, branches RESTART IDENTITY CASCADE;

-- ==============================================================================
-- 2. DỮ LIỆU DANH MỤC
-- ==============================================================================

-- Thêm Chi nhánh (Branches)
INSERT INTO branches (name, address, low_stock_threshold) VALUES
('Chi nhánh Hà Nội', '123 Đường Cầu Giấy, Quận Cầu Giấy, Hà Nội', 10),
('Chi nhánh TP.HCM', '456 Đường Lê Lợi, Quận 1, TP.HCM', 15),
('Chi nhánh Đà Nẵng', '789 Đường Nguyễn Văn Linh, Đà Nẵng', 5);

-- Thêm Danh mục (Categories)
INSERT INTO categories (name) VALUES
('Điện thoại di động'),
('Máy tính xách tay'),
('Phụ kiện công nghệ'),
('Thực phẩm đóng gói');

-- Thêm Nhà cung cấp (Suppliers)
INSERT INTO suppliers (name, contact_info, address) VALUES
('Công ty TNHH Công Nghệ ABC', '0901234567 - lienhe@abc.com', 'Khu CNC Hoà Lạc, Hà Nội'),
('Nhà Phân Phối Thực Phẩm Vàng', '0987654321', 'KCN Tân Bình, TP.HCM');

-- Thêm Khách hàng (Customers)
INSERT INTO customers (name, contact_info, address) VALUES
('Nguyễn Văn A', '0912223334', 'Ba Đình, Hà Nội'),
('Trần Thị B', '0933445566', 'Quận 3, TP.HCM');


-- ==============================================================================
-- 3. DỮ LIỆU NGƯỜI DÙNG
-- ==============================================================================

-- Thêm Người dùng (Users)
-- Mật khẩu mặc định cho TẤT CẢ các user bên dưới là '123456' 
-- (Hash BCrypt tương ứng: $2a$10$a3Jzt1usW9MnGrWgBhGb0OLr3HtHDtvHUQHG2wHdVWVVoYIMHBnEu)
INSERT INTO users (username, password, full_name, role, branch_id, status) VALUES
('admin', '$2a$10$a3Jzt1usW9MnGrWgBhGb0OLr3HtHDtvHUQHG2wHdVWVVoYIMHBnEu', 'Quản trị viên Hệ thống', 'ADMIN', NULL, 'ACTIVE'),
('manager_hn', '$2a$10$a3Jzt1usW9MnGrWgBhGb0OLr3HtHDtvHUQHG2wHdVWVVoYIMHBnEu', 'Lê Cường (QL Hà Nội)', 'MANAGER', 1, 'ACTIVE'),
('staff_hn_1', '$2a$10$a3Jzt1usW9MnGrWgBhGb0OLr3HtHDtvHUQHG2wHdVWVVoYIMHBnEu', 'Nhân viên HN 01', 'STAFF', 1, 'ACTIVE'),
('manager_hcm', '$2a$10$a3Jzt1usW9MnGrWgBhGb0OLr3HtHDtvHUQHG2wHdVWVVoYIMHBnEu', 'Phạm My (QL HCM)', 'MANAGER', 2, 'ACTIVE'),
('staff_locked', '$2a$10$a3Jzt1usW9MnGrWgBhGb0OLr3HtHDtvHUQHG2wHdVWVVoYIMHBnEu', 'Nhân viên đã nghỉ việc', 'STAFF', 2, 'LOCKED');


-- ==============================================================================
-- 4. DỮ LIỆU SẢN PHẨM & TỒN KHO
-- ==============================================================================

-- Thêm Sản phẩm (Products)
INSERT INTO products (code, name, unit, price, category_id, has_expiry, mfg_date, exp_date) VALUES
('IP15', 'iPhone 15 Pro Max 256GB', 'Chiếc', 29900000, 1, FALSE, '1970-01-01', '1970-01-01'),
('MACM2', 'MacBook Air M2 8GB/256GB', 'Chiếc', 25500000, 2, FALSE, '1970-01-01', '1970-01-01'),
('AIRPODS', 'AirPods Pro 2', 'Hộp', 5500000, 3, FALSE, '1970-01-01', '1970-01-01'),
('MILK_OLD', 'Sữa tươi tiệt trùng 1L (Lô cũ)', 'Hộp', 35000, 4, TRUE, '2024-01-01', '2024-07-01'),
('MILK_NEW', 'Sữa tươi tiệt trùng 1L (Lô mới)', 'Hộp', 35000, 4, TRUE, '2024-05-01', '2024-11-01');

-- Thêm Tồn kho (Inventories)
INSERT INTO inventories (branch_id, product_id, mfg_date, exp_date, quantity) VALUES
-- Tồn tại Chi nhánh Hà Nội
(1, 1, '1970-01-01', '1970-01-01', 50), -- 50 iPhone
(1, 2, '1970-01-01', '1970-01-01', 20), -- 20 Macbook
(1, 4, '2024-01-01', '2024-07-01', 100), -- 100 hộp sữa lô cũ
(1, 5, '2024-05-01', '2024-11-01', 200), -- 200 hộp sữa lô mới
-- Tồn tại Chi nhánh HCM (iPhone sắp hết, dưới ngưỡng 15)
(2, 1, '1970-01-01', '1970-01-01', 12); 


-- ==============================================================================
-- 5. DỮ LIỆU PHIẾU KHO & KIỂM KÊ
-- ==============================================================================

-- Thêm Phiếu kho (Receipts)
INSERT INTO receipts (code, type, status, source_branch_id, dest_branch_id, created_by, supplier_id, customer_id, description) VALUES
('IM12345678', 'IMPORT', 'COMPLETED', NULL, 1, 2, 1, NULL, 'Nhập hàng đầu tháng từ Công ty ABC'),
('EX87654321', 'EXPORT', 'COMPLETED', 1, NULL, 3, NULL, 1, 'Xuất bán hàng cho anh A'),
('TR11223344', 'TRANSFER', 'DRAFT', 1, 2, 2, NULL, NULL, 'Điều chuyển gấp vào HCM do thiếu tồn kho (Chưa duyệt)'),
('AD99999999', 'ADJUST_IN', 'COMPLETED', NULL, 1, 2, NULL, NULL, 'Cân bằng kho sau khi kiểm đếm thực tế đợt 1');

-- Thêm Chi tiết phiếu kho (Receipt Details)
-- Chi tiết cho phiếu Nhập (IM12345678)
INSERT INTO receipt_details (receipt_id, product_id, mfg_date, exp_date, quantity, price) VALUES
(1, 1, '1970-01-01', '1970-01-01', 50, 29000000), -- Nhập iPhone giá vốn
(1, 2, '1970-01-01', '1970-01-01', 20, 24000000);
-- Chi tiết cho phiếu Xuất (EX87654321)
INSERT INTO receipt_details (receipt_id, product_id, mfg_date, exp_date, quantity, price) VALUES
(2, 1, '1970-01-01', '1970-01-01', 2, 29900000);
-- Chi tiết cho phiếu Điều chuyển (TR11223344)
INSERT INTO receipt_details (receipt_id, product_id, mfg_date, exp_date, quantity, price) VALUES
(3, 1, '1970-01-01', '1970-01-01', 10, 29000000);
-- Chi tiết phiếu Điều chỉnh tăng từ việc dư kho đợt kiểm kê
INSERT INTO receipt_details (receipt_id, product_id, mfg_date, exp_date, quantity, price) VALUES
(4, 3, '1970-01-01', '1970-01-01', 1, 5500000);

-- Thêm Phiên kiểm kê (Stocktakes)
INSERT INTO stocktakes (code, branch_id, created_by, status, notes) VALUES
('STK_HN_001', 1, 2, 'COMPLETED', 'Kiểm kê định kỳ tháng 5 tại Hà Nội'),
('STK_HCM_001', 2, 4, 'DRAFT', 'Đang thực hiện đếm kho tại HCM');

-- Thêm Chi tiết kiểm kê (Stocktake Details)
-- Trong đợt kiểm kê HN, sổ sách báo 0 Airpods, thực tế phát hiện 1 (lệnh ADJUST_IN số 4 đã xử lý)
INSERT INTO stocktake_details (stocktake_id, product_id, mfg_date, exp_date, expected_quantity, actual_quantity, adjustment_receipt_id) VALUES
(1, 1, '1970-01-01', '1970-01-01', 50, 50, NULL),
(1, 3, '1970-01-01', '1970-01-01', 0, 1, 4);

-- ==============================================================================
-- 6. DỮ LIỆU NHẬT KÝ (AUDIT)
-- ==============================================================================

-- Thêm Audit Logs
INSERT INTO audit_logs (user_id, action, entity_name, entity_id, details) VALUES
(1, 'CREATE', 'users', '2', 'Admin tạo tài khoản manager_hn'),
(2, 'CREATE', 'receipts', '1', 'Manager HN lập phiếu nhập kho IM12345678'),
(2, 'COMPLETE', 'stocktakes', '1', 'Hoàn tất phiên kiểm kê STK_HN_001, phát hiện dư 1 Airpods');
