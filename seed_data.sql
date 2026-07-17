-- ==============================================================================
-- DỮ LIỆU MẪU (SEED DATA)
-- Lưu ý: Hãy chạy toàn bộ schema.sql trước khi chạy file này.
-- ==============================================================================

TRUNCATE audit_logs, stocktake_details, stocktakes, receipt_details, receipts, inventories, products, users, customers, categories, branches, password_reset_otps RESTART IDENTITY CASCADE;

-- ==============================================================================
-- 2. DỮ LIỆU DANH MỤC
-- ==============================================================================

-- Thêm Chi nhánh (Branches)
INSERT INTO branches (name, address, low_stock_threshold, is_head, tax_code) VALUES
('Chi nhánh Hà Nội', '123 Đường Cầu Giấy, Quận Cầu Giấy, Hà Nội', 10, TRUE, '0100000000'),
('Chi nhánh TP.HCM', '456 Đường Lê Lợi, Quận 1, TP.HCM', 15, FALSE, '0200000000'),
('Chi nhánh Đà Nẵng', '789 Đường Nguyễn Văn Linh, Đà Nẵng', 5, FALSE, '0300000000');

-- Thêm Danh mục (Categories)
INSERT INTO categories (name) VALUES
('Điện thoại di động'),
('Máy tính xách tay'),
('Phụ kiện công nghệ'),
('Thực phẩm đóng gói'),
('Màn hình máy tính'),
('Bàn phím & Chuột'),
('Thiết bị âm thanh');

INSERT INTO categories (name)
VALUES 
    ('iPhone thường'),
    ('iPhone Pro'),
    ('iPhone Pro Max')
ON CONFLICT (name) DO NOTHING;

-- Thêm Khách hàng (Customers)
INSERT INTO customers (name, contact_info, address, debt, email, tax_code, branch_id) VALUES
('Nguyễn Văn A', '0912223334', 'Ba Đình, Hà Nội', 5000000.00, 'nguyenvana@example.com', '0101234567', 1),
('Trần Thị B', '0933445566', 'Quận 3, TP.HCM', 12500000.00, 'tranthib@example.com', '0207654321', 2),
('Công ty CP Công Nghệ Việt', '0243123456', 'Cầu Giấy, Hà Nội', 50000000.00, 'contact@congngheviet.vn', '0105678901', 1),
('Đại lý Tuấn Cường', '0988776655', 'Hải Châu, Đà Nẵng', 0.00, 'tuancuong@gmail.com', '0309876543', 3),
('Lê Hoàng Nam', '0911223344', 'Quận 1, TP.HCM', 2500000.00, 'namle@example.com', '0201122334', 2),
('Phạm Thu Hương', '0966554433', 'Hoàn Kiếm, Hà Nội', 0.00, 'huongpham@example.com', '0109988776', 1);


-- ==============================================================================
-- 3. DỮ LIỆU NGƯỜI DÙNG
-- ==============================================================================

-- Thêm Người dùng (Users)
-- Mật khẩu mặc định cho TẤT CẢ các user bên dưới là '123456' 
-- (Hash BCrypt tương ứng: $2a$10$a3Jzt1usW9MnGrWgBhGb0OLr3HtHDtvHUQHG2wHdVWVVoYIMHBnEu)
INSERT INTO users (username, password, full_name, role, branch_id, status, phone, email) VALUES
('admin', '$2a$10$a3Jzt1usW9MnGrWgBhGb0OLr3HtHDtvHUQHG2wHdVWVVoYIMHBnEu', 'Quản trị viên Hệ thống', 'ADMIN', 1, 'ACTIVE', '0912345678', 'admin@example.com'),
('manager_hn', '$2a$10$a3Jzt1usW9MnGrWgBhGb0OLr3HtHDtvHUQHG2wHdVWVVoYIMHBnEu', 'Lê Cường (QL Hà Nội)', 'MANAGER', 1, 'ACTIVE', '0923456789', 'manager_hn@example.com'),
('manager_dn', '$2a$10$a3Jzt1usW9MnGrWgBhGb0OLr3HtHDtvHUQHG2wHdVWVVoYIMHBnEu', 'Lê Cường (QL Đà Nẵng)', 'MANAGER', 3, 'ACTIVE', '0923456787', 'manager_dn@example.com'),
('staff_hn_1', '$2a$10$a3Jzt1usW9MnGrWgBhGb0OLr3HtHDtvHUQHG2wHdVWVVoYIMHBnEu', 'Nhân viên HN 01', 'STAFF', 1, 'ACTIVE', '0934567890', 'staff_hn_1@example.com'),
('manager_hcm', '$2a$10$a3Jzt1usW9MnGrWgBhGb0OLr3HtHDtvHUQHG2wHdVWVVoYIMHBnEu', 'Phạm My (QL HCM)', 'MANAGER', 2, 'ACTIVE', '0945678901', 'manager_hcm@example.com'),
('staff_locked', '$2a$10$a3Jzt1usW9MnGrWgBhGb0OLr3HtHDtvHUQHG2wHdVWVVoYIMHBnEu', 'Nhân viên đã nghỉ việc', 'STAFF', 2, 'LOCKED', '0956789012', 'staff_locked@example.com');


-- ==============================================================================
-- 4. DỮ LIỆU SẢN PHẨM & TỒN KHO
-- ==============================================================================

-- Thêm Sản phẩm (Products)
INSERT INTO products (code, name, unit, import_price, price, category_id, has_expiry, image_url, mfg_date, exp_date) VALUES
('IP15', 'iPhone 15 Pro Max 256GB', 'Chiếc', 25000000, 29900000, 1, FALSE, '/uploads/images/iphone_15.png', '1970-01-01', '1970-01-01'),
('MACM2', 'MacBook Air M2 8GB/256GB', 'Chiếc', 20000000, 25500000, 2, FALSE, '/uploads/images/macbook_air.png', '1970-01-01', '1970-01-01'),
('AIRPODS', 'AirPods Pro 2', 'Hộp', 4000000, 5500000, 3, FALSE, '/uploads/images/airpods_pro.png', '1970-01-01', '1970-01-01'),
('MILK_OLD', 'Sữa tươi tiệt trùng 1L (Lô cũ)', 'Hộp', 20000, 35000, 4, TRUE, '/uploads/images/milk_old.png', CURRENT_DATE - INTERVAL '150 days', CURRENT_DATE - INTERVAL '30 days'),
('MILK_NEW', 'Sữa tươi tiệt trùng 1L (Lô mới)', 'Hộp', 20000, 35000, 4, TRUE, '/uploads/images/milk_new.png', CURRENT_DATE - INTERVAL '10 days', CURRENT_DATE + INTERVAL '120 days'),
('DELL_U27', 'Màn hình Dell UltraSharp U2723QE', 'Chiếc', 10000000, 12500000, 5, FALSE, '/uploads/images/dell_u27.png', '1970-01-01', '1970-01-01'),
('KEYCHRON', 'Bàn phím cơ Keychron K8 Pro', 'Chiếc', 1500000, 2100000, 6, FALSE, '/uploads/images/keychron.png', '1970-01-01', '1970-01-01'),
('LOGI_MX', 'Chuột Logitech MX Master 3S', 'Chiếc', 1800000, 2490000, 6, FALSE, '/uploads/images/logi_mx.png', '1970-01-01', '1970-01-01'),
('MARSHALL', 'Loa Bluetooth Marshall Stanmore III', 'Chiếc', 7000000, 9500000, 7, FALSE, '/uploads/images/marshall.png', '1970-01-01', '1970-01-01'),
('S24_ULTRA', 'Samsung Galaxy S24 Ultra 512GB', 'Chiếc', 28000000, 33900000, 1, FALSE, '/uploads/images/s24_ultra.png', '1970-01-01', '1970-01-01');

-- Thêm Tồn kho (Inventories)
INSERT INTO inventories (branch_id, product_id, mfg_date, exp_date, batch_code, quantity, has_expiry) VALUES
-- Tồn tại Chi nhánh Hà Nội
(1, 1, '1970-01-01', '1970-01-01', 'IP15-HN-001', 50, FALSE),
(1, 2, '1970-01-01', '1970-01-01', 'MACM2-HN-001', 20, FALSE),
(1, 3, '1970-01-01', '1970-01-01', 'AIR-HN-001', 1, FALSE),
(1, 4, CURRENT_DATE - INTERVAL '150 days', CURRENT_DATE - INTERVAL '30 days', 'MILK-2024A', 100, TRUE),
(1, 5, CURRENT_DATE - INTERVAL '10 days', CURRENT_DATE + INTERVAL '120 days', 'MILK-2024B', 200, TRUE),
(1, 6, '1970-01-01', '1970-01-01', 'DELL-HN-001', 15, FALSE),
(1, 7, '1970-01-01', '1970-01-01', 'KEY-HN-001', 40, FALSE),
(1, 8, '1970-01-01', '1970-01-01', 'MX-HN-001', 35, FALSE),
-- Tồn tại Chi nhánh HCM
(2, 1, '1970-01-01', '1970-01-01', 'IP15-HCM-001', 12, FALSE),
(2, 2, '1970-01-01', '1970-01-01', 'MACM2-HCM-001', 18, FALSE),
(2, 6, '1970-01-01', '1970-01-01', 'DELL-HCM-001', 22, FALSE),
(2, 9, '1970-01-01', '1970-01-01', 'MAR-HCM-001', 10, FALSE),
(2, 4, CURRENT_DATE - INTERVAL '120 days', CURRENT_DATE - INTERVAL '10 days', 'MILK-HCM-OLD', 50, TRUE),
(2, 5, CURRENT_DATE - INTERVAL '5 days', CURRENT_DATE + INTERVAL '90 days', 'MILK-HCM-NEW', 150, TRUE),
-- Tồn tại Chi nhánh Đà Nẵng
(3, 10, '1970-01-01', '1970-01-01', 'S24-DN-001', 30, FALSE),
(3, 3, '1970-01-01', '1970-01-01', 'AIR-DN-001', 50, FALSE),
(3, 4, CURRENT_DATE - INTERVAL '110 days', CURRENT_DATE - INTERVAL '5 days', 'MILK-DN-OLD', 40, TRUE),
(3, 5, CURRENT_DATE - INTERVAL '2 days', CURRENT_DATE + INTERVAL '80 days', 'MILK-DN-NEW', 120, TRUE);


-- ==============================================================================
-- 5. DỮ LIỆU PHIẾU KHO & KIỂM KÊ
-- ==============================================================================

-- Thêm Phiếu kho (Receipts)
INSERT INTO receipts (code, type, status, source_branch_id, dest_branch_id, created_by, customer_id, description) VALUES
('IM12345678', 'IMPORT', 'COMPLETED', NULL, 1, 2, NULL, 'Nhập hàng đầu tháng'),
('EX87654321', 'EXPORT', 'COMPLETED', 1, NULL, 3, 1, 'Xuất bán hàng cho anh A'),
('TR11223344', 'TRANSFER', 'DRAFT', 1, 2, 2, NULL, 'Điều chuyển gấp vào HCM do thiếu tồn kho (Chưa duyệt)'),
('AD99999999', 'ADJUST_IN', 'COMPLETED', NULL, 1, 2, NULL, 'Cân bằng kho sau khi kiểm đếm thực tế đợt 1');

-- Thêm Chi tiết phiếu kho (Receipt Details)
-- Chi tiết cho phiếu Nhập (IM12345678)
INSERT INTO receipt_details (receipt_id, product_id, mfg_date, exp_date, batch_code, quantity, price) VALUES
(1, 1, '1970-01-01', '1970-01-01', 'IP15-HN-001', 50, 29000000), -- Nhập iPhone giá vốn
(1, 2, '1970-01-01', '1970-01-01', 'MACM2-HN-001', 20, 24000000);
-- Chi tiết cho phiếu Xuất (EX87654321)
INSERT INTO receipt_details (receipt_id, product_id, mfg_date, exp_date, batch_code, quantity, price) VALUES
(2, 1, '1970-01-01', '1970-01-01', 'IP15-HN-001', 2, 29900000);
-- Chi tiết cho phiếu Điều chuyển (TR11223344)
INSERT INTO receipt_details (receipt_id, product_id, mfg_date, exp_date, batch_code, quantity, price) VALUES
(3, 1, '1970-01-01', '1970-01-01', 'IP15-HN-001', 10, 29000000);
-- Chi tiết phiếu Điều chỉnh tăng từ việc dư kho đợt kiểm kê
INSERT INTO receipt_details (receipt_id, product_id, mfg_date, exp_date, batch_code, quantity, price) VALUES
(4, 3, '1970-01-01', '1970-01-01', 'AIR-HN-001', 1, 5500000);

-- Thêm Phiên kiểm kê (Stocktakes)
INSERT INTO stocktakes (code, branch_id, created_by, status, notes) VALUES
('STK_HN_001', 1, 2, 'COMPLETED', 'Kiểm kê định kỳ tháng 5 tại Hà Nội'),
('STK_HCM_001', 2, 4, 'DRAFT', 'Đang thực hiện đếm kho tại HCM');

-- Thêm Chi tiết kiểm kê (Stocktake Details)
-- Trong đợt kiểm kê HN, sổ sách báo 0 Airpods, thực tế phát hiện 1 (lệnh ADJUST_IN số 4 đã xử lý)
INSERT INTO stocktake_details (stocktake_id, product_id, mfg_date, exp_date, batch_code, expected_quantity, actual_quantity, adjustment_receipt_id) VALUES
(1, 1, '1970-01-01', '1970-01-01', 'IP15-HN-001', 50, 50, NULL),
(1, 3, '1970-01-01', '1970-01-01', 'AIR-HN-001', 0, 1, 4);

-- ==============================================================================
-- 6. DỮ LIỆU NHẬT KÝ (AUDIT)
-- ==============================================================================

-- Thêm Audit Logs
INSERT INTO audit_logs (user_id, action, entity_name, entity_id, details) VALUES
(1, 'CREATE', 'users', '2', 'Admin tạo tài khoản manager_hn'),
(2, 'CREATE', 'receipts', '1', 'Manager HN lập phiếu nhập kho IM12345678'),
(2, 'COMPLETE', 'stocktakes', '1', 'Hoàn tất phiên kiểm kê STK_HN_001, phát hiện dư 1 Airpods');


-- ==============================================================================
-- 7. DỮ LIỆU MẪU PHIẾU XUẤT BÁN & NHẬP KHO (30 NGÀY QUA)
-- ==============================================================================

-- Phiếu xuất bán (EXPORT)
INSERT INTO receipts (code, type, status, payment_status, source_branch_id, dest_branch_id, created_by, customer_id, description, created_at) VALUES
('EX20260601', 'EXPORT', 'COMPLETED', 'PAID', 1, NULL, 2, 1, 'Bán sỉ iPhone cho đại lý HN', CURRENT_TIMESTAMP - INTERVAL '28 days'),
('EX20260603', 'EXPORT', 'COMPLETED', 'PAID', 2, NULL, 5, 2, 'Bán lẻ MacBook tại HCM', CURRENT_TIMESTAMP - INTERVAL '25 days'),
('EX20260605', 'EXPORT', 'COMPLETED', 'UNPAID', 3, NULL, 3, 1, 'Bán hàng cho khách nợ Đà Nẵng', CURRENT_TIMESTAMP - INTERVAL '22 days'),
('EX20260608', 'EXPORT', 'COMPLETED', 'PAID', 1, NULL, 2, 2, 'Khách mua AirPods và Phụ kiện HN', CURRENT_TIMESTAMP - INTERVAL '19 days'),
('EX20260610', 'EXPORT', 'COMPLETED', 'UNPAID', 2, NULL, 5, 2, 'Bán sữa số lượng lớn HCM', CURRENT_TIMESTAMP - INTERVAL '16 days'),
('EX20260612', 'EXPORT', 'COMPLETED', 'PAID', 3, NULL, 3, 1, 'Khách mua lẻ iPhone ĐN', CURRENT_TIMESTAMP - INTERVAL '14 days'),
('EX20260615', 'EXPORT', 'COMPLETED', 'PAID', 1, NULL, 2, 1, 'Xuất bán MacBook HN', CURRENT_TIMESTAMP - INTERVAL '11 days'),
('EX20260618', 'EXPORT', 'COMPLETED', 'UNPAID', 2, NULL, 5, 1, 'Hợp đồng bán sỉ iPhone HCM', CURRENT_TIMESTAMP - INTERVAL '8 days'),
('EX20260620', 'EXPORT', 'COMPLETED', 'PAID', 3, NULL, 3, 2, 'Bán MacBook và AirPods ĐN', CURRENT_TIMESTAMP - INTERVAL '5 days'),
('EX20260621', 'EXPORT', 'COMPLETED', 'PAID', 1, NULL, 2, 2, 'Bán sữa hộp HN', CURRENT_TIMESTAMP - INTERVAL '3 days');

INSERT INTO receipt_details (receipt_id, product_id, quantity, price, batch_code, mfg_date, exp_date) VALUES
((SELECT id FROM receipts WHERE code='EX20260601'), 1, 5, 29900000, 'IP15-HN-001', '1970-01-01', '1970-01-01'),
((SELECT id FROM receipts WHERE code='EX20260603'), 2, 3, 25500000, 'MACM2-HN-001', '1970-01-01', '1970-01-01'),
((SELECT id FROM receipts WHERE code='EX20260605'), 1, 1, 29900000, 'IP15-HN-001', '1970-01-01', '1970-01-01'),
((SELECT id FROM receipts WHERE code='EX20260605'), 3, 4, 5500000, 'AIR-HN-001', '1970-01-01', '1970-01-01'),
((SELECT id FROM receipts WHERE code='EX20260608'), 3, 6, 5500000, 'AIR-HN-001', '1970-01-01', '1970-01-01'),
((SELECT id FROM receipts WHERE code='EX20260610'), 4, 50, 35000, 'MILK-2024A', CURRENT_DATE - INTERVAL '150 days', CURRENT_DATE - INTERVAL '30 days'),
((SELECT id FROM receipts WHERE code='EX20260610'), 5, 150, 35000, 'MILK-2024B', CURRENT_DATE - INTERVAL '10 days', CURRENT_DATE + INTERVAL '120 days'),
((SELECT id FROM receipts WHERE code='EX20260612'), 1, 2, 29900000, 'IP15-HN-001', '1970-01-01', '1970-01-01'),
((SELECT id FROM receipts WHERE code='EX20260615'), 2, 4, 25500000, 'MACM2-HN-001', '1970-01-01', '1970-01-01'),
((SELECT id FROM receipts WHERE code='EX20260618'), 1, 8, 29900000, 'IP15-HN-001', '1970-01-01', '1970-01-01'),
((SELECT id FROM receipts WHERE code='EX20260620'), 2, 2, 25500000, 'MACM2-HN-001', '1970-01-01', '1970-01-01'),
((SELECT id FROM receipts WHERE code='EX20260620'), 3, 2, 5500000, 'AIR-HN-001', '1970-01-01', '1970-01-01'),
((SELECT id FROM receipts WHERE code='EX20260621'), 5, 80, 35000, 'MILK-2024B', CURRENT_DATE - INTERVAL '10 days', CURRENT_DATE + INTERVAL '120 days');

-- Phiếu nhập kho (IMPORT)
INSERT INTO receipts (code, type, status, payment_status, source_branch_id, dest_branch_id, created_by, customer_id, description, created_at) VALUES
('IM20260602', 'IMPORT', 'COMPLETED', 'PAID', NULL, 1, 2, NULL, 'Nhập thêm iPhone và MacBook HN', CURRENT_TIMESTAMP - INTERVAL '27 days'),
('IM20260607', 'IMPORT', 'COMPLETED', 'PAID', NULL, 2, 5, NULL, 'Nhập thêm sữa HCM', CURRENT_TIMESTAMP - INTERVAL '20 days'),
('IM20260614', 'IMPORT', 'COMPLETED', 'PAID', NULL, 3, 3, NULL, 'Nhập AirPods Đà Nẵng', CURRENT_TIMESTAMP - INTERVAL '12 days');

INSERT INTO receipt_details (receipt_id, product_id, quantity, price, batch_code, mfg_date, exp_date) VALUES
((SELECT id FROM receipts WHERE code='IM20260602'), 1, 10, 29000000, 'IP15-HN-001', '1970-01-01', '1970-01-01'),
((SELECT id FROM receipts WHERE code='IM20260602'), 2, 5, 24000000, 'MACM2-HN-001', '1970-01-01', '1970-01-01'),
((SELECT id FROM receipts WHERE code='IM20260607'), 5, 300, 30000, 'MILK-2024B', CURRENT_DATE - INTERVAL '10 days', CURRENT_DATE + INTERVAL '120 days'),
((SELECT id FROM receipts WHERE code='IM20260614'), 3, 15, 5000000, 'AIR-HN-001', '1970-01-01', '1970-01-01');


-- ==============================================================================
-- 8. DỮ LIỆU MẪU QUY MÔ LỚN (TỰ ĐỘNG SINH CHO 90 NGÀY)
-- ==============================================================================

DO $$
DECLARE
    day_offset INT;
    curr_date TIMESTAMP;
    r_imp_id INT;
    r_exp_id INT;
    imp_qty INT;
    exp_qty INT;
    p_id INT;
    p_price NUMERIC(15,2);
    p_import_price NUMERIC(15,2);
    p_batch VARCHAR(100);
    target_branch INT;
    creator_id INT;
    t INT;
    exp_trend INT;
    imp_trend INT;
BEGIN
    -- Dọn dẹp dữ liệu tự động cũ nếu có
    DELETE FROM receipt_details WHERE batch_code LIKE '%-BATCH-%';
    DELETE FROM receipts WHERE code LIKE 'IM_AUTO_%' OR code LIKE 'EX_AUTO_%';

    -- Sinh dữ liệu quy mô lớn cho TẤT CẢ 3 chi nhánh mỗi ngày
    FOR day_offset IN 0..89 LOOP
        curr_date := NOW() - (day_offset || ' days')::INTERVAL;
        
        -- t là trục thời gian đi từ 0 (quá khứ, cách đây 89 ngày) đến 89 (hôm nay)
        t := 89 - day_offset;
        
        -- Đường màu xanh dương (Xuất bán): Sóng cong vòm đỉnh ở giữa chu kỳ
        exp_trend := 3 + (SIN((t + 10) / 105.0 * 3.14159265) * 15)::INT;
        
        -- Đường màu xanh lá (Nhập kho): Đường thoai thoải, ngóc đầu lên ở cuối chu kỳ và cắt đường xanh dương
        imp_trend := 2 + ((t * t) / 630.0)::INT;

        FOR target_branch IN 1..3 LOOP
            IF target_branch = 1 THEN creator_id := 3;     -- staff_hn_1
            ELSIF target_branch = 2 THEN creator_id := 4;  -- manager_hcm
            ELSE creator_id := 2;                          -- manager_dn
            END IF;

            -- Tạo 1 Phiếu NHẬP hằng ngày cho chi nhánh này
            INSERT INTO receipts (code, type, status, payment_status, source_branch_id, dest_branch_id, created_by, description, created_at)
            VALUES ('IM_AUTO_' || day_offset || '_' || target_branch || '_' || EXTRACT(EPOCH FROM curr_date)::BIGINT, 'IMPORT', 'COMPLETED', 'PAID', CASE WHEN target_branch = 1 THEN NULL ELSE 1 END, target_branch, creator_id, CASE WHEN target_branch = 1 THEN 'Nhập hàng định kỳ' ELSE 'Nhập từ kho tổng' END, curr_date)
            RETURNING id INTO r_imp_id;

            -- Chỉ Tạo Phiếu XUẤT hằng ngày cho chi nhánh nhánh (target_branch > 1), vì Hà Nội (1) là kho tổng không bán lẻ
            IF target_branch > 1 THEN
                INSERT INTO receipts (code, type, status, payment_status, source_branch_id, dest_branch_id, created_by, customer_id, description, created_at)
                VALUES ('EX_AUTO_' || day_offset || '_' || target_branch || '_' || EXTRACT(EPOCH FROM curr_date)::BIGINT, 'EXPORT', 'COMPLETED', 'PAID', target_branch, NULL, creator_id, 1, 'Xuất bán sỉ hằng ngày', curr_date)
                RETURNING id INTO r_exp_id;
            END IF;

            -- Thêm chi tiết cho cả 10 sản phẩm
            FOR p_id IN 1..10 LOOP
                SELECT import_price, price, code INTO p_import_price, p_price, p_batch FROM products WHERE id = p_id;
                
                -- Phân bổ trend cho các sản phẩm (cộng thêm 1 chút để ko bị 0)
                -- NHÂN VỚI HỆ SỐ CỦA CHI NHÁNH ĐỂ CÁC CHI NHÁNH KHÔNG BẰNG NHAU
                IF target_branch = 1 THEN
                    imp_qty := (imp_trend + (p_id % 2)) * 1.0; 
                    exp_qty := 0; -- Kho tổng không xuất bán
                ELSIF target_branch = 2 THEN
                    imp_qty := (imp_trend + (p_id % 2)) * 1.5; 
                    exp_qty := (exp_trend + (p_id % 3)) * 1.6; 
                ELSE
                    imp_qty := (imp_trend + (p_id % 2)) * 0.7; 
                    exp_qty := (exp_trend + (p_id % 3)) * 0.8; 
                END IF;

                IF p_id = 4 OR p_id = 5 THEN
                    -- Milk (has expiry):
                    -- Lô cũ (p_id = 4) là hàng hết hạn (exp_date trước curr_date)
                    -- Lô mới (p_id = 5) là hàng còn hạn (exp_date sau curr_date)
                    INSERT INTO receipt_details (receipt_id, product_id, quantity, price, batch_code, mfg_date, exp_date)
                    VALUES (r_imp_id, p_id, imp_qty, p_import_price, p_batch || '-BATCH-' || day_offset || '-' || target_branch,
                            (curr_date - INTERVAL '150 days')::DATE,
                            (curr_date + CASE WHEN p_id = 4 THEN -INTERVAL '15 days' ELSE INTERVAL '120 days' END)::DATE);

                    IF target_branch > 1 THEN
                        INSERT INTO receipt_details (receipt_id, product_id, quantity, price, batch_code, mfg_date, exp_date)
                        VALUES (r_exp_id, p_id, exp_qty, p_price, p_batch || '-BATCH-' || day_offset || '-' || target_branch,
                                (curr_date - INTERVAL '150 days')::DATE,
                                (curr_date + CASE WHEN p_id = 4 THEN -INTERVAL '15 days' ELSE INTERVAL '120 days' END)::DATE);
                    END IF;
                ELSE
                    -- Các sản phẩm không có hạn sử dụng (has_expiry = false)
                    INSERT INTO receipt_details (receipt_id, product_id, quantity, price, batch_code, mfg_date, exp_date)
                    VALUES (r_imp_id, p_id, imp_qty, p_import_price, p_batch || '-BATCH-' || day_offset || '-' || target_branch, '1970-01-01', '1970-01-01');

                    IF target_branch > 1 THEN
                        INSERT INTO receipt_details (receipt_id, product_id, quantity, price, batch_code, mfg_date, exp_date)
                        VALUES (r_exp_id, p_id, exp_qty, p_price, p_batch || '-BATCH-' || day_offset || '-' || target_branch, '1970-01-01', '1970-01-01');
                    END IF;
                END IF;
            END LOOP;
        END LOOP;
    END LOOP;
END $$;

