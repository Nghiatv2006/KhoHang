-- ==============================================================================
-- BỔ SUNG CỘT CÔNG NỢ, TRẠNG THÁI VÀ THANH TOÁN
-- ==============================================================================

-- 1. Bổ sung cho bảng Nhà cung cấp (suppliers)
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS debt NUMERIC(15, 2) NOT NULL DEFAULT 0.00;
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE';

-- 2. Bổ sung cho bảng Khách hàng (customers)
ALTER TABLE customers ADD COLUMN IF NOT EXISTS debt NUMERIC(15, 2) NOT NULL DEFAULT 0.00;
ALTER TABLE customers ADD COLUMN IF NOT EXISTS status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE';

-- 3. Bổ sung cho bảng Phiếu kho (receipts) để quản lý luồng tiền thanh toán
ALTER TABLE receipts ADD COLUMN IF NOT EXISTS payment_status VARCHAR(50) NOT NULL DEFAULT 'UNPAID';

-- 4. Tạo chỉ mục (Index) hỗ trợ truy vấn lọc nhanh
CREATE INDEX IF NOT EXISTS idx_suppliers_status ON suppliers (status);
CREATE INDEX IF NOT EXISTS idx_customers_status ON customers (status);
CREATE INDEX IF NOT EXISTS idx_receipts_payment_status ON receipts (payment_status);
