-- Tổng hợp các file migration thành 1 file duy nhất cho dễ theo dõi

-- ==============================================================================
-- Migration: Bổ sung các cột còn thiếu vào bảng audit_logs và users
-- Chạy script này trên PostgreSQL để cập nhật Database hiện có
-- ==============================================================================

-- 1. Bổ sung cột ban_until vào bảng users (lưu thời gian bị phạt SPAM)
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS ban_until TIMESTAMP;

-- 2. Bổ sung các cột còn thiếu vào bảng audit_logs đã có sẵn
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS branch_id INT;       -- Chi nhánh xảy ra hành động (phân quyền xem)
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS is_warning BOOLEAN NOT NULL DEFAULT FALSE; -- Cờ đỏ cảnh báo

-- 3. Nâng độ dài entity_id lên 100 (cũ là 50, có thể không đủ cho một số ID dài)
ALTER TABLE audit_logs ALTER COLUMN entity_id TYPE VARCHAR(100);

-- 4. Bỏ NOT NULL ở entity_name nếu có (để linh hoạt hơn khi ghi log)
ALTER TABLE audit_logs ALTER COLUMN entity_name DROP NOT NULL;

-- 5. Đánh thêm Index cho audit_logs để tăng tốc độ lọc
CREATE INDEX IF NOT EXISTS idx_audit_logs_branch_id  ON audit_logs (branch_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id    ON audit_logs (user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action     ON audit_logs (action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs (created_at DESC);


------------------------- Bản chiều 22 tháng 6 ---------------------

-- ==============================================================================
-- SCRIPT CHÈN DỮ LIỆU BÁN HÀNG & NHẬP KHO MẪU (30 NGÀY QUA)
-- Chạy script này trên PostgreSQL (database: warehouse_db)
-- ==============================================================================

-- 1. CHÈN CÁC PHIẾU XUẤT BÁN (EXPORT) TRONG 30 NGÀY QUA
INSERT INTO receipts (code, type, status, payment_status, source_branch_id, dest_branch_id, created_by, customer_id, description, created_at) VALUES
('EX20260601', 'EXPORT', 'COMPLETED', 'PAID', 1, NULL, 2, 1, 'Bán sỉ iPhone cho đại lý HN', '2026-06-01 10:00:00'),
('EX20260603', 'EXPORT', 'COMPLETED', 'PAID', 2, NULL, 5, 2, 'Bán lẻ MacBook tại HCM', '2026-06-03 14:30:00'),
('EX20260605', 'EXPORT', 'COMPLETED', 'UNPAID', 3, NULL, 3, 1, 'Bán hàng cho khách nợ Đà Nẵng', '2026-06-05 09:15:00'),
('EX20260608', 'EXPORT', 'COMPLETED', 'PAID', 1, NULL, 2, 2, 'Khách mua AirPods và Phụ kiện HN', '2026-06-08 16:45:00'),
('EX20260610', 'EXPORT', 'COMPLETED', 'UNPAID', 2, NULL, 5, 2, 'Bán sữa số lượng lớn HCM', '2026-06-10 11:20:00'),
('EX20260612', 'EXPORT', 'COMPLETED', 'PAID', 3, NULL, 3, 1, 'Khách mua lẻ iPhone ĐN', '2026-06-12 15:10:00'),
('EX20260615', 'EXPORT', 'COMPLETED', 'PAID', 1, NULL, 2, 1, 'Xuất bán MacBook HN', '2026-06-15 13:00:00'),
('EX20260618', 'EXPORT', 'COMPLETED', 'UNPAID', 2, NULL, 5, 1, 'Hợp đồng bán sỉ iPhone HCM', '2026-06-18 10:30:00'),
('EX20260620', 'EXPORT', 'COMPLETED', 'PAID', 3, NULL, 3, 2, 'Bán MacBook và AirPods ĐN', '2026-06-20 17:00:00'),
('EX20260621', 'EXPORT', 'COMPLETED', 'PAID', 1, NULL, 2, 2, 'Bán sữa hộp HN', '2026-06-21 08:30:00');

-- 2. CHI TIẾT SẢN PHẨM BÁN RA CỦA CÁC PHIẾU XUẤT TRÊN
INSERT INTO receipt_details (receipt_id, product_id, quantity, price, batch_code) VALUES
((SELECT id FROM receipts WHERE code='EX20260601'), 1, 5, 29900000, 'IP15-HN-001'), -- 5 iPhone
((SELECT id FROM receipts WHERE code='EX20260603'), 2, 3, 25500000, 'MACM2-HN-001'), -- 3 MacBook
((SELECT id FROM receipts WHERE code='EX20260605'), 1, 1, 29900000, 'IP15-HN-001'), -- 1 iPhone
((SELECT id FROM receipts WHERE code='EX20260605'), 3, 4, 5500000, 'AIR-HN-001'), -- 4 AirPods
((SELECT id FROM receipts WHERE code='EX20260608'), 3, 6, 5500000, 'AIR-HN-001'), -- 6 AirPods
((SELECT id FROM receipts WHERE code='EX20260610'), 4, 50, 35000, 'MILK-2024A'), -- 50 sữa lô cũ
((SELECT id FROM receipts WHERE code='EX20260610'), 5, 150, 35000, 'MILK-2024B'), -- 150 sữa lô mới
((SELECT id FROM receipts WHERE code='EX20260612'), 1, 2, 29900000, 'IP15-HN-001'), -- 2 iPhone
((SELECT id FROM receipts WHERE code='EX20260615'), 2, 4, 25500000, 'MACM2-HN-001'), -- 4 MacBook
((SELECT id FROM receipts WHERE code='EX20260618'), 1, 8, 29900000, 'IP15-HN-001'), -- 8 iPhone
((SELECT id FROM receipts WHERE code='EX20260620'), 2, 2, 25500000, 'MACM2-HN-001'), -- 2 MacBook
((SELECT id FROM receipts WHERE code='EX20260620'), 3, 2, 5500000, 'AIR-HN-001'), -- 2 AirPods
((SELECT id FROM receipts WHERE code='EX20260621'), 5, 80, 35000, 'MILK-2024B'); -- 80 sữa

-- 3. CHÈN CÁC PHIẾU NHẬP KHO (IMPORT) ĐỂ VẼ ĐƯỜNG XU HƯỚNG NHẬP XUẤT
INSERT INTO receipts (code, type, status, payment_status, source_branch_id, dest_branch_id, created_by, customer_id, description, created_at) VALUES
('IM20260602', 'IMPORT', 'COMPLETED', 'PAID', NULL, 1, 2, NULL, 'Nhập thêm iPhone và MacBook HN', '2026-06-02 09:00:00'),
('IM20260607', 'IMPORT', 'COMPLETED', 'PAID', NULL, 2, 5, NULL, 'Nhập thêm sữa HCM', '2026-06-07 11:00:00'),
('IM20260614', 'IMPORT', 'COMPLETED', 'PAID', NULL, 3, 3, NULL, 'Nhập AirPods Đà Nẵng', '2026-06-14 14:00:00');

-- 4. CHI TIẾT SẢN PHẨM NHẬP VÀO
INSERT INTO receipt_details (receipt_id, product_id, quantity, price, batch_code) VALUES
((SELECT id FROM receipts WHERE code='IM20260602'), 1, 10, 29000000, 'IP15-HN-001'),
((SELECT id FROM receipts WHERE code='IM20260602'), 2, 5, 24000000, 'MACM2-HN-001'),
((SELECT id FROM receipts WHERE code='IM20260607'), 5, 300, 30000, 'MILK-2024B'),
((SELECT id FROM receipts WHERE code='IM20260614'), 3, 15, 5000000, 'AIR-HN-001');


-- ==============================================================================
-- Migration: Bổ sung các cột còn thiếu vào bảng receipts và receipt_details
-- Nguyên nhân: Model Java có các trường này nhưng full_schema.sql không có
-- => Gây lỗi 500 Internal Server Error khi gọi GET /api/receipts
-- Chạy script này trên PostgreSQL (warehouse_db) để fix lỗi
-- ==============================================================================

-- 1. Bảng receipts: thêm customer_name và customer_phone
--    (dùng để lưu tên/SĐT khách ngay trong phiếu, không cần JOIN bảng customers)
ALTER TABLE receipts
    ADD COLUMN IF NOT EXISTS customer_name VARCHAR(255);

ALTER TABLE receipts
    ADD COLUMN IF NOT EXISTS customer_phone VARCHAR(50);

-- 2. Bảng receipt_details: thêm received_quantity và shortfall_reason
--    (dùng cho tính năng xác nhận nhận hàng trong phiếu TRANSFER)
ALTER TABLE receipt_details
    ADD COLUMN IF NOT EXISTS received_quantity INT;

ALTER TABLE receipt_details
    ADD COLUMN IF NOT EXISTS shortfall_reason TEXT;

-- Kiểm tra lại sau khi chạy:
-- SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'receipts' ORDER BY ordinal_position;
-- SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'receipt_details' ORDER BY ordinal_position;


-- ==============================================================================
-- SCRIPT SINH DỮ LIỆU MẪU QUY MÔ LỚN (HÀNG TỶ ĐỒNG)
-- Giúp cân bằng tỷ lệ với giao dịch 4 tỷ hiện tại của bạn
-- ==============================================================================

DO $$
DECLARE
    day_offset INT;
    curr_date TIMESTAMP;
    r_id INT;
    imp_qty INT;
    exp_qty INT;
    p_id INT;
    p_price NUMERIC(15,2);
    p_import_price NUMERIC(15,2);
    p_batch VARCHAR(100);
    target_branch INT;
    creator_id INT;
BEGIN
    -- 1. Dọn dẹp dữ liệu tự động cũ
    DELETE FROM receipt_details WHERE batch_code LIKE '%-BATCH-%';
    DELETE FROM receipts WHERE code LIKE 'IM_AUTO_%' OR code LIKE 'EX_AUTO_%';

    -- 2. Sinh dữ liệu quy mô lớn xoay vòng cho 3 chi nhánh
    FOR day_offset IN 0..29 LOOP
        curr_date := NOW() - (day_offset || ' days')::INTERVAL;
        
        -- Xoay vòng chi nhánh mục tiêu (1, 2, 3)
        target_branch := (day_offset % 3) + 1;
        
        -- Chọn ID người tạo tương ứng
        IF target_branch = 1 THEN creator_id := 3;     -- staff_hn_1
        ELSIF target_branch = 2 THEN creator_id := 4;  -- manager_hcm
        ELSE creator_id := 2;                          -- manager_dn
        END IF;

        -- Tạo phiếu NHẬP (IMPORT) mỗi 2 ngày (Giá trị khoảng 1 tỷ - 2.5 tỷ)
        IF day_offset % 2 = 0 THEN
            p_id := (day_offset % 3) + 1; -- Chọn iPhone, MacBook hoặc AirPods
            SELECT import_price, code INTO p_import_price, p_batch FROM products WHERE id = p_id;
            
            INSERT INTO receipts (code, type, status, payment_status, source_branch_id, dest_branch_id, created_by, description, created_at)
            VALUES ('IM_AUTO_' || day_offset || '_' || EXTRACT(EPOCH FROM curr_date)::BIGINT, 'IMPORT', 'COMPLETED', 'PAID', NULL, target_branch, creator_id, 'Nhập hàng mẫu quy mô lớn', curr_date)
            RETURNING id INTO r_id;
            
            -- Tăng số lượng nhập lên 35 - 80 chiếc
            imp_qty := 35 + (day_offset * 2) % 45;
            INSERT INTO receipt_details (receipt_id, product_id, quantity, price, batch_code)
            VALUES (r_id, p_id, imp_qty, p_import_price, p_batch || '-BATCH-' || day_offset);
        END IF;

        -- Tạo phiếu XUẤT BÁN (EXPORT) hằng ngày (Giá trị khoảng 400 triệu - 1.5 tỷ)
        p_id := ((day_offset + 1) % 3) + 1; 
        SELECT price, code INTO p_price, p_batch FROM products WHERE id = p_id;
        
        INSERT INTO receipts (code, type, status, payment_status, source_branch_id, dest_branch_id, created_by, customer_id, description, created_at)
        VALUES ('EX_AUTO_' || day_offset || '_' || EXTRACT(EPOCH FROM curr_date)::BIGINT, 'EXPORT', 'COMPLETED', 'PAID', target_branch, NULL, creator_id, 1, 'Xuất bán sỉ quy mô lớn', curr_date)
        RETURNING id INTO r_id;
        
        -- Tăng số lượng xuất lên 15 - 45 chiếc
        exp_qty := 15 + (day_offset * 2) % 30;
        INSERT INTO receipt_details (receipt_id, product_id, quantity, price, batch_code)
        VALUES (r_id, p_id, exp_qty, p_price, p_batch || '-BATCH-' || day_offset);
    END LOOP;
END $$;
