-- Các câu lệnh cập nhật DB phát sinh tiếp theo sẽ được ghi nhận vào đây.

-- Cập nhật lô sản xuất (batch_code) cho phân hệ Quản lý Tồn kho
ALTER TABLE inventories ADD COLUMN IF NOT EXISTS batch_code VARCHAR(100) NOT NULL DEFAULT 'DEFAULT_BATCH';
ALTER TABLE inventories DROP CONSTRAINT IF EXISTS unique_inventory_batch;
ALTER TABLE inventories ADD CONSTRAINT unique_inventory_batch UNIQUE (branch_id, product_id, batch_code);

ALTER TABLE receipt_details ADD COLUMN IF NOT EXISTS batch_code VARCHAR(100) NOT NULL DEFAULT 'DEFAULT_BATCH';

ALTER TABLE stocktake_details ADD COLUMN IF NOT EXISTS batch_code VARCHAR(100) NOT NULL DEFAULT 'DEFAULT_BATCH';

-- Cập nhật dữ liệu batch_code cho các bản ghi inventories hiện có để tránh dùng DEFAULT_BATCH
UPDATE inventories SET batch_code = 'IP15-HN-001' WHERE branch_id = 1 AND product_id = 1;
UPDATE inventories SET batch_code = 'MACM2-HN-001' WHERE branch_id = 1 AND product_id = 2;
UPDATE inventories SET batch_code = 'AIR-HN-001' WHERE branch_id = 1 AND product_id = 3;
UPDATE inventories SET batch_code = 'MILK-2024A' WHERE branch_id = 1 AND product_id = 4 AND mfg_date = '2024-01-01';
UPDATE inventories SET batch_code = 'MILK-2024B' WHERE branch_id = 1 AND product_id = 5 AND mfg_date = '2024-05-01';
UPDATE inventories SET batch_code = 'IP15-HCM-001' WHERE branch_id = 2 AND product_id = 1;

-- Cập nhật dữ liệu batch_code cho receipt_details
UPDATE receipt_details SET batch_code = 'IP15-HN-001' WHERE receipt_id = 1 AND product_id = 1;
UPDATE receipt_details SET batch_code = 'MACM2-HN-001' WHERE receipt_id = 1 AND product_id = 2;
UPDATE receipt_details SET batch_code = 'IP15-HN-001' WHERE receipt_id = 2 AND product_id = 1;
UPDATE receipt_details SET batch_code = 'IP15-HN-001' WHERE receipt_id = 3 AND product_id = 1;
UPDATE receipt_details SET batch_code = 'AIR-HN-001' WHERE receipt_id = 4 AND product_id = 3;

-- Cập nhật dữ liệu batch_code cho stocktake_details
UPDATE stocktake_details SET batch_code = 'IP15-HN-001' WHERE stocktake_id = 1 AND product_id = 1;
UPDATE stocktake_details SET batch_code = 'AIR-HN-001' WHERE stocktake_id = 1 AND product_id = 3;

-- Bổ sung trường has_expiry và số ngày cảnh báo hạn dùng (expiry_warning_days) cho bảng inventories
ALTER TABLE inventories ADD COLUMN IF NOT EXISTS has_expiry BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE inventories ADD COLUMN IF NOT EXISTS expiry_warning_days INT NOT NULL DEFAULT 30;

