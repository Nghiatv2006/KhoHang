-- ==============================================================================
-- DỮ LIỆU MẪU (SEED DATA) - NGÀNH HÀNG: IPHONE
-- Lưu ý: Hãy chạy toàn bộ schema.sql trước khi chạy file này.
-- ==============================================================================

-- Dọn sạch toàn bộ dữ liệu cũ (bao gồm dữ liệu đã nhập tay trên hệ thống)
TRUNCATE audit_logs, stocktake_details, stocktakes, receipt_details, receipts, inventories, products, users, customers, categories, branches, password_reset_otps RESTART IDENTITY CASCADE;

-- ==============================================================================
-- 1. CHI NHÁNH
-- ==============================================================================

INSERT INTO branches (name, address, low_stock_threshold, is_head, tax_code) VALUES
('Chi nhánh Hà Nội',    '123 Đường Cầu Giấy, Quận Cầu Giấy, Hà Nội', 10, TRUE,  '0100000000'),
('Chi nhánh TP.HCM',    '456 Đường Lê Lợi, Quận 1, TP.HCM',           15, FALSE, '0200000000'),
('Chi nhánh Đà Nẵng',   '789 Đường Nguyễn Văn Linh, Đà Nẵng',         5,  FALSE, '0300000000');

-- ==============================================================================
-- 2. DANH MỤC (CATEGORIES) - IPHONE
-- ==============================================================================

INSERT INTO categories (name) VALUES
('iPhone thường'),
('iPhone Pro'),
('iPhone Pro Max')
ON CONFLICT (name) DO NOTHING;

-- ==============================================================================
-- 3. KHÁCH HÀNG (CUSTOMERS)
-- ==============================================================================

INSERT INTO customers (name, contact_info, address, debt, email, tax_code, branch_id) VALUES
('Nguyễn Văn A', '0912223334', 'Ba Đình, Hà Nội', 5000000.00, 'nguyenvana@example.com', '0101234567', 1),
('Trần Thị B', '0933445566', 'Quận 3, TP.HCM', 12500000.00, 'tranthib@example.com', '0207654321', 2),
('Công ty CP Công Nghệ Việt', '0243123456', 'Cầu Giấy, Hà Nội', 50000000.00, 'contact@congngheviet.vn', '0105678901', 1),
('Đại lý Tuấn Cường', '0988776655', 'Hải Châu, Đà Nẵng', 0.00, 'tuancuong@gmail.com', '0309876543', 3),
('Lê Hoàng Nam', '0911223344', 'Quận 1, TP.HCM', 2500000.00, 'namle@example.com', '0201122334', 2),
('Phạm Thu Hương', '0966554433', 'Hoàn Kiếm, Hà Nội', 0.00, 'huongpham@example.com', '0109988776', 1),
('Đại lý Minh Trí', '0901112223', 'Quận 10, TP.HCM', 15000000.00, 'contact@minhtri.vn', '0209998887', 2),
('Trương Văn C', '0977889900', 'Sơn Trà, Đà Nẵng', 0.00, 'truongvanc@example.com', '0301112223', 3),
('Công ty TNHH MTV Biển Đông', '0236123456', 'Thanh Khê, Đà Nẵng', 10000000.00, 'biendong@example.com', '0304445556', 3);

-- ==============================================================================
-- 4. NGƯỜI DÙNG (USERS)
-- ==============================================================================

-- Mật khẩu mặc định cho TẤT CẢ các user bên dưới là '123456'
-- (Hash BCrypt tương ứng: $2a$10$a3Jzt1usW9MnGrWgBhGb0OLr3HtHDtvHUQHG2wHdVWVVoYIMHBnEu)
INSERT INTO users (username, password, full_name, role, branch_id, status, phone, email) VALUES
('admin',        '$2a$10$a3Jzt1usW9MnGrWgBhGb0OLr3HtHDtvHUQHG2wHdVWVVoYIMHBnEu', 'Quản trị viên Hệ thống',    'ADMIN',   1, 'ACTIVE', '0912345678', 'admin@example.com'),
('manager_hn',   '$2a$10$a3Jzt1usW9MnGrWgBhGb0OLr3HtHDtvHUQHG2wHdVWVVoYIMHBnEu', 'Lê Cường (QL Hà Nội)',      'MANAGER', 1, 'ACTIVE', '0923456789', 'manager_hn@example.com'),
('manager_dn',   '$2a$10$a3Jzt1usW9MnGrWgBhGb0OLr3HtHDtvHUQHG2wHdVWVVoYIMHBnEu', 'Lê Cường (QL Đà Nẵng)',     'MANAGER', 3, 'ACTIVE', '0923456787', 'manager_dn@example.com'),
('staff_hn_1',   '$2a$10$a3Jzt1usW9MnGrWgBhGb0OLr3HtHDtvHUQHG2wHdVWVVoYIMHBnEu', 'Nhân viên HN 01',           'STAFF',   1, 'ACTIVE', '0934567890', 'staff_hn_1@example.com'),
('manager_hcm',  '$2a$10$a3Jzt1usW9MnGrWgBhGb0OLr3HtHDtvHUQHG2wHdVWVVoYIMHBnEu', 'Phạm My (QL HCM)',          'MANAGER', 2, 'ACTIVE', '0945678901', 'manager_hcm@example.com'),
('staff_locked', '$2a$10$a3Jzt1usW9MnGrWgBhGb0OLr3HtHDtvHUQHG2wHdVWVVoYIMHBnEu', 'Nhân viên đã nghỉ việc',   'STAFF',   2, 'LOCKED', '0956789012', 'staff_locked@example.com');

-- ==============================================================================
-- 5. SẢN PHẨM (PRODUCTS) - CÁC MODEL IPHONE
-- ==============================================================================
-- Category IDs: 1 = iPhone thường, 2 = iPhone Pro, 3 = iPhone Pro Max

INSERT INTO products (code, name, unit, import_price, price, category_id, has_expiry, image_url, mfg_date, exp_date) VALUES
-- iPhone thường (category_id = 1)
('IP16_128_TITAN', 'iPhone 16 128GB Màu Titan', 'Chiếc', 18500000, 22990000, 1, FALSE, '/uploads/images/iphone16_titan.png', '1970-01-01', '1970-01-01'),
('IP16_256_DEN',   'iPhone 16 256GB Màu Đen',   'Chiếc', 20500000, 25490000, 1, FALSE, '/uploads/images/iphone16_den.png', '1970-01-01', '1970-01-01'),
('IP15_128_TRANG', 'iPhone 15 128GB Màu Trắng', 'Chiếc', 15000000, 18990000, 1, FALSE, '/uploads/images/iphone15_trang.png', '1970-01-01', '1970-01-01'),
('IP15_256_HONG',  'iPhone 15 256GB Màu Hồng',  'Chiếc', 17000000, 21490000, 1, FALSE, '/uploads/images/iphone15_hong.png', '1970-01-01', '1970-01-01'),
-- iPhone Pro (category_id = 2)
('IP16P_128_TITAN', 'iPhone 16 Pro 128GB Màu Titan', 'Chiếc', 24000000, 28990000, 2, FALSE, '/uploads/images/iphone16_titan.png', '1970-01-01', '1970-01-01'),
('IP16P_256_DEN',   'iPhone 16 Pro 256GB Màu Đen',   'Chiếc', 26500000, 31990000, 2, FALSE, '/uploads/images/iphone16_den.png', '1970-01-01', '1970-01-01'),
('IP15P_128_TRANG', 'iPhone 15 Pro 128GB Màu Trắng', 'Chiếc', 21000000, 25990000, 2, FALSE, '/uploads/images/iphone15_trang.png', '1970-01-01', '1970-01-01'),
('IP15P_256_HONG',  'iPhone 15 Pro 256GB Màu Hồng',  'Chiếc', 23000000, 27990000, 2, FALSE, '/uploads/images/iphone15_hong.png', '1970-01-01', '1970-01-01'),
-- iPhone Pro Max (category_id = 3)
('IP16PM_256_TITAN', 'iPhone 16 Pro Max 256GB Màu Titan', 'Chiếc', 29000000, 34990000, 3, FALSE, '/uploads/images/iphone16_titan.png', '1970-01-01', '1970-01-01'),
('IP16PM_512_DEN',   'iPhone 16 Pro Max 512GB Màu Đen',   'Chiếc', 33000000, 39990000, 3, FALSE, '/uploads/images/iphone16_den.png', '1970-01-01', '1970-01-01');

-- ==============================================================================
-- 6. TỒN KHO BAN ĐẦU (INVENTORIES)
-- ==============================================================================

INSERT INTO inventories (branch_id, product_id, mfg_date, exp_date, batch_code, quantity, has_expiry) VALUES
-- Chi nhánh Hà Nội (kho tổng)
(1,  1, '1970-01-01', '1970-01-01', 'IP16-128-TITAN-HN-001', 80, FALSE),
(1,  2, '1970-01-01', '1970-01-01', 'IP16-256-DEN-HN-001',   60, FALSE),
(1,  3, '1970-01-01', '1970-01-01', 'IP15-128-TRANG-HN-001', 50, FALSE),
(1,  4, '1970-01-01', '1970-01-01', 'IP15-256-HONG-HN-001',  40, FALSE),
(1,  5, '1970-01-01', '1970-01-01', 'IP16P-128-TITAN-HN-001',   60, FALSE),
(1,  6, '1970-01-01', '1970-01-01', 'IP16P-256-DEN-HN-001',   50, FALSE),
(1,  7, '1970-01-01', '1970-01-01', 'IP15P-128-TRANG-HN-001',   40, FALSE),
(1,  8, '1970-01-01', '1970-01-01', 'IP15P-256-HONG-HN-001',   30, FALSE),
(1,  9, '1970-01-01', '1970-01-01', 'IP16PM-256-TITAN-HN-001',  40, FALSE),
(1, 10, '1970-01-01', '1970-01-01', 'IP16PM-512-DEN-HN-001',  25, FALSE),
-- Chi nhánh TP.HCM
(2,  1, '1970-01-01', '1970-01-01', 'IP16-128-TITAN-HCM-001', 30, FALSE),
(2,  2, '1970-01-01', '1970-01-01', 'IP16-256-DEN-HCM-001',   25, FALSE),
(2,  5, '1970-01-01', '1970-01-01', 'IP16P-128-TITAN-HCM-001',  20, FALSE),
(2,  6, '1970-01-01', '1970-01-01', 'IP16P-256-DEN-HCM-001',  15, FALSE),
(2,  9, '1970-01-01', '1970-01-01', 'IP16PM-256-TITAN-HCM-001', 12, FALSE),
(2, 10, '1970-01-01', '1970-01-01', 'IP16PM-512-DEN-HCM-001',  8, FALSE),
-- Chi nhánh Đà Nẵng
(3,  3, '1970-01-01', '1970-01-01', 'IP15-128-TRANG-DN-001', 20, FALSE),
(3,  4, '1970-01-01', '1970-01-01', 'IP15-256-HONG-DN-001',  15, FALSE),
(3,  7, '1970-01-01', '1970-01-01', 'IP15P-128-TRANG-DN-001',   18, FALSE),
(3,  8, '1970-01-01', '1970-01-01', 'IP15P-256-HONG-DN-001',   12, FALSE),
(3,  9, '1970-01-01', '1970-01-01', 'IP16PM-256-TITAN-DN-001',  10, FALSE);

-- ==============================================================================
-- 7. PHIẾU KHO MẪU (RECEIPTS)
-- ==============================================================================

INSERT INTO receipts (code, type, status, payment_status, source_branch_id, dest_branch_id, created_by, customer_id, description, created_at) VALUES
-- Phiếu nhập kho
('IM20260601', 'IMPORT', 'COMPLETED', 'PAID', NULL, 1, 2, NULL, 'Nhập iPhone 16 series đợt đầu cho HN',   CURRENT_TIMESTAMP - INTERVAL '28 days'),
('IM20260605', 'IMPORT', 'COMPLETED', 'PAID', NULL, 2, 5, NULL, 'Nhập iPhone 16 cho chi nhánh HCM',       CURRENT_TIMESTAMP - INTERVAL '24 days'),
('IM20260610', 'IMPORT', 'COMPLETED', 'PAID', NULL, 3, 3, NULL, 'Nhập iPhone 15 cho chi nhánh Đà Nẵng',   CURRENT_TIMESTAMP - INTERVAL '18 days'),
-- Phiếu xuất bán
('EX20260602', 'EXPORT', 'COMPLETED', 'PAID',   1, NULL, 4, 1, 'Bán iPhone 16 cho Nguyễn Văn A',          CURRENT_TIMESTAMP - INTERVAL '27 days'),
('EX20260606', 'EXPORT', 'COMPLETED', 'PAID',   2, NULL, 5, 2, 'Bán iPhone 16 Pro tại HCM',               CURRENT_TIMESTAMP - INTERVAL '22 days'),
('EX20260608', 'EXPORT', 'COMPLETED', 'UNPAID', 2, NULL, 5, 3, 'Công ty CP Công Nghệ Việt mua nợ',        CURRENT_TIMESTAMP - INTERVAL '20 days'),
('EX20260612', 'EXPORT', 'COMPLETED', 'PAID',   3, NULL, 3, 4, 'Đại lý Tuấn Cường nhập iPhone 15 ĐN',     CURRENT_TIMESTAMP - INTERVAL '16 days'),
('EX20260615', 'EXPORT', 'COMPLETED', 'PAID',   1, NULL, 4, 2, 'Bán iPhone 16 Pro Max HN',                CURRENT_TIMESTAMP - INTERVAL '13 days'),
('EX20260618', 'EXPORT', 'COMPLETED', 'UNPAID', 2, NULL, 5, 5, 'Lê Hoàng Nam mua iPhone 16 nợ',           CURRENT_TIMESTAMP - INTERVAL '10 days'),
('EX20260620', 'EXPORT', 'COMPLETED', 'PAID',   3, NULL, 3, 6, 'Phạm Thu Hương mua iPhone 15 Pro ĐN',     CURRENT_TIMESTAMP - INTERVAL '8 days');

INSERT INTO receipt_details (receipt_id, product_id, quantity, price, batch_code, mfg_date, exp_date) VALUES
-- IM20260601: Nhập HN
((SELECT id FROM receipts WHERE code='IM20260601'),  1, 20, 18500000, 'IP16-128-TITAN-HN-001', '1970-01-01', '1970-01-01'),
((SELECT id FROM receipts WHERE code='IM20260601'),  5, 15, 24000000, 'IP16P-128-TITAN-HN-001',  '1970-01-01', '1970-01-01'),
((SELECT id FROM receipts WHERE code='IM20260601'),  9, 10, 29000000, 'IP16PM-256-TITAN-HN-001', '1970-01-01', '1970-01-01'),
-- IM20260605: Nhập HCM
((SELECT id FROM receipts WHERE code='IM20260605'),  2, 15, 20500000, 'IP16-256-DEN-HCM-001', '1970-01-01', '1970-01-01'),
((SELECT id FROM receipts WHERE code='IM20260605'),  6, 10, 26500000, 'IP16P-256-DEN-HCM-001', '1970-01-01', '1970-01-01'),
-- IM20260610: Nhập Đà Nẵng
((SELECT id FROM receipts WHERE code='IM20260610'),  3, 12, 15000000, 'IP15-128-TRANG-DN-001', '1970-01-01', '1970-01-01'),
((SELECT id FROM receipts WHERE code='IM20260610'),  7, 10, 21000000, 'IP15P-128-TRANG-DN-001',  '1970-01-01', '1970-01-01'),
-- EX20260602: Xuất HN
((SELECT id FROM receipts WHERE code='EX20260602'),  1,  3, 22990000, 'IP16-128-TITAN-HN-001', '1970-01-01', '1970-01-01'),
-- EX20260606: Xuất HCM
((SELECT id FROM receipts WHERE code='EX20260606'),  6,  2, 31990000, 'IP16P-256-DEN-HCM-001', '1970-01-01', '1970-01-01'),
((SELECT id FROM receipts WHERE code='EX20260606'),  2,  3, 25490000, 'IP16-256-DEN-HCM-001', '1970-01-01', '1970-01-01'),
-- EX20260608: Xuất HCM (nợ)
((SELECT id FROM receipts WHERE code='EX20260608'),  9,  2, 34990000, 'IP16PM-256-TITAN-HCM-001','1970-01-01', '1970-01-01'),
-- EX20260612: Xuất ĐN
((SELECT id FROM receipts WHERE code='EX20260612'),  3,  5, 18990000, 'IP15-128-TRANG-DN-001', '1970-01-01', '1970-01-01'),
((SELECT id FROM receipts WHERE code='EX20260612'),  7,  3, 25990000, 'IP15P-128-TRANG-DN-001',  '1970-01-01', '1970-01-01'),
-- EX20260615: Xuất HN
((SELECT id FROM receipts WHERE code='EX20260615'),  9,  1, 34990000, 'IP16PM-256-TITAN-HN-001', '1970-01-01', '1970-01-01'),
((SELECT id FROM receipts WHERE code='EX20260615'), 10,  1, 39990000, 'IP16PM-512-DEN-HN-001', '1970-01-01', '1970-01-01'),
-- EX20260618: Xuất HCM (nợ)
((SELECT id FROM receipts WHERE code='EX20260618'),  1,  2, 22990000, 'IP16-128-TITAN-HCM-001', '1970-01-01', '1970-01-01'),
-- EX20260620: Xuất ĐN
((SELECT id FROM receipts WHERE code='EX20260620'),  8,  2, 27990000, 'IP15P-256-HONG-DN-001',  '1970-01-01', '1970-01-01');

-- ==============================================================================
-- 8. KIỂM KÊ MẪU (STOCKTAKES)
-- ==============================================================================

INSERT INTO stocktakes (code, branch_id, created_by, status, notes) VALUES
('STK_HN_001',  1, 2, 'COMPLETED', 'Kiểm kê iPhone định kỳ tháng 7 tại Hà Nội'),
('STK_HCM_001', 2, 5, 'DRAFT',     'Đang thực hiện kiểm kê kho HCM');

INSERT INTO stocktake_details (stocktake_id, product_id, mfg_date, exp_date, batch_code, expected_quantity, actual_quantity, adjustment_receipt_id) VALUES
(1, 1, '1970-01-01', '1970-01-01', 'IP16-128-TITAN-HN-001',   77, 77, NULL),
(1, 5, '1970-01-01', '1970-01-01', 'IP16P-128-TITAN-HN-001',  60, 60, NULL);

-- ==============================================================================
-- 9. NHẬT KÝ MẪU (AUDIT LOGS)
-- ==============================================================================

INSERT INTO audit_logs (user_id, action, entity_name, entity_id, details) VALUES
(1, 'CREATE', 'users',    '2', 'Admin tạo tài khoản manager_hn'),
(2, 'CREATE', 'receipts', '1', 'Manager HN lập phiếu nhập iPhone 16 series: IM20260601'),
(5, 'CREATE', 'receipts', '2', 'Manager HCM lập phiếu nhập iPhone 16: IM20260605');

-- ==============================================================================
-- 10. SINH DỮ LIỆU QUY MÔ LỚN (TỰ ĐỘNG CHO 90 NGÀY)
-- ==============================================================================

DO $$
DECLARE
    day_offset    INT;
    curr_date     TIMESTAMP;
    r_imp_id      INT;
    r_exp_id      INT;
    imp_qty       INT;
    exp_qty       INT;
    p_id          INT;
    p_price       NUMERIC(15,2);
    p_import_price NUMERIC(15,2);
    p_batch       VARCHAR(100);
    target_branch INT;
    creator_id    INT;
    t             INT;
    exp_trend     INT;
    imp_trend     INT;
BEGIN
    -- Dọn dữ liệu AUTO cũ nếu có
    DELETE FROM receipt_details WHERE batch_code LIKE '%-BATCH-%';
    DELETE FROM receipts WHERE code LIKE 'IM_AUTO_%' OR code LIKE 'EX_AUTO_%';

    -- Sinh dữ liệu cho 90 ngày qua, cả 3 chi nhánh
    FOR day_offset IN 0..89 LOOP
        curr_date := NOW() - (day_offset || ' days')::INTERVAL;

        -- t: 0 = quá khứ xa nhất, 89 = hôm nay
        t := 89 - day_offset;

        -- Xu hướng xuất bán: hình vòm (đỉnh ở giữa chu kỳ)
        exp_trend := 2 + (SIN((t + 10) / 105.0 * 3.14159265) * 8)::INT;

        -- Xu hướng nhập kho: tăng dần về cuối
        imp_trend := 2 + ((t * t) / 800.0)::INT;

        FOR target_branch IN 1..3 LOOP
            IF    target_branch = 1 THEN creator_id := 4;   -- staff_hn_1
            ELSIF target_branch = 2 THEN creator_id := 5;   -- manager_hcm
            ELSE                         creator_id := 3;   -- manager_dn
            END IF;

            -- Phiếu NHẬP hằng ngày
            INSERT INTO receipts (code, type, status, payment_status, source_branch_id, dest_branch_id, created_by, description, created_at)
            VALUES (
                'IM_AUTO_' || day_offset || '_' || target_branch || '_' || EXTRACT(EPOCH FROM curr_date)::BIGINT,
                'IMPORT', 'COMPLETED', 'PAID',
                CASE WHEN target_branch = 1 THEN NULL ELSE 1 END,
                target_branch,
                creator_id,
                CASE WHEN target_branch = 1 THEN 'Nhập hàng nhà cung cấp' ELSE 'Nhập từ kho tổng HN' END,
                curr_date
            )
            RETURNING id INTO r_imp_id;

            -- Phiếu XUẤT hằng ngày (chỉ chi nhánh con, HN là kho tổng)
            IF target_branch > 1 THEN
                INSERT INTO receipts (code, type, status, payment_status, source_branch_id, dest_branch_id, created_by, customer_id, description, created_at)
                VALUES (
                    'EX_AUTO_' || day_offset || '_' || target_branch || '_' || EXTRACT(EPOCH FROM curr_date)::BIGINT,
                    'EXPORT', 'COMPLETED', 'PAID',
                    target_branch, NULL,
                    creator_id,
                    CASE WHEN target_branch = 2 THEN (CASE WHEN day_offset % 3 = 0 THEN 2 WHEN day_offset % 3 = 1 THEN 5 ELSE 7 END) ELSE (CASE WHEN day_offset % 3 = 0 THEN 4 WHEN day_offset % 3 = 1 THEN 8 ELSE 9 END) END,
                    'Xuất bán iPhone hằng ngày',
                    curr_date
                )
                RETURNING id INTO r_exp_id;
            END IF;

            -- Chi tiết cho 10 sản phẩm iPhone
            FOR p_id IN 1..10 LOOP
                SELECT import_price, price, code
                INTO p_import_price, p_price, p_batch
                FROM products WHERE id = p_id;

                IF target_branch = 1 THEN
                    imp_qty := (imp_trend + (p_id % 3) + 1);
                    exp_qty := 0;
                ELSIF target_branch = 2 THEN
                    imp_qty := ((imp_trend + (p_id % 3) + 1) * 1.5)::INT;
                    exp_qty := ((exp_trend + (p_id % 4) + 1) * 1.6)::INT;
                ELSE
                    imp_qty := ((imp_trend + (p_id % 3) + 1) * 0.8)::INT;
                    exp_qty := ((exp_trend + (p_id % 4) + 1) * 0.9)::INT;
                END IF;

                -- Nhập kho
                INSERT INTO receipt_details (receipt_id, product_id, quantity, price, batch_code, mfg_date, exp_date)
                VALUES (
                    r_imp_id, p_id, imp_qty, p_import_price,
                    p_batch || '-BATCH-' || day_offset || '-' || target_branch,
                    '1970-01-01', '1970-01-01'
                );

                -- Xuất bán (chỉ chi nhánh con)
                IF target_branch > 1 THEN
                    INSERT INTO receipt_details (receipt_id, product_id, quantity, price, batch_code, mfg_date, exp_date)
                    VALUES (
                        r_exp_id, p_id, exp_qty, p_price,
                        p_batch || '-BATCH-' || day_offset || '-' || target_branch,
                        '1970-01-01', '1970-01-01'
                    );
                END IF;

            END LOOP; -- p_id
        END LOOP;     -- target_branch
    END LOOP;         -- day_offset
END $$;
