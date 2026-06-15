-- ==============================================================================
-- BỔ SUNG CỘT CÔNG NỢ, TRẠNG THÁI VÀ THANH TOÁN (Từ update_partners.sql)
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


-- ==============================================================================
-- 1. BỔ SUNG CỘT EMAIL VÀO BẢNG USERS (Từ update_schema.sql)
-- ==============================================================================
ALTER TABLE users ADD COLUMN email VARCHAR(255) UNIQUE;


-- ==============================================================================
-- 2. TẠO BẢNG YÊU CẦU CHUYỂN CHI NHÁNH (BRANCH TRANSFER REQUESTS) (Từ update_schema.sql)
-- ==============================================================================
CREATE TABLE branch_transfer_requests (
    id SERIAL PRIMARY KEY,
    staff_id INT NOT NULL,                          -- Nhân viên cần chuyển chi nhánh
    from_branch_id INT NOT NULL,                    -- Chi nhánh hiện tại (trước khi chuyển)
    to_branch_id INT NOT NULL,                      -- Chi nhánh mới muốn chuyển tới
    created_by INT NOT NULL,                        -- MANAGER gửi yêu cầu chuyển
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',  -- Trạng thái: PENDING, APPROVED, REJECTED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- Thời gian gửi yêu cầu
    approved_by INT,                                -- ADMIN duyệt hoặc từ chối yêu cầu
    approved_at TIMESTAMP,                          -- Thời gian duyệt
    CONSTRAINT fk_request_staff FOREIGN KEY (staff_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_request_from FOREIGN KEY (from_branch_id) REFERENCES branches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_request_to FOREIGN KEY (to_branch_id) REFERENCES branches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_request_manager FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_request_admin FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL
);


-- ==============================================================================
-- 3. TẠO INDEXES HỖ TRỢ TRUY VẤN (Từ update_schema.sql)
-- ==============================================================================
CREATE INDEX idx_transfer_requests_status ON branch_transfer_requests (status);
CREATE INDEX idx_transfer_requests_staff ON branch_transfer_requests (staff_id);
CREATE INDEX idx_transfer_requests_manager ON branch_transfer_requests (created_by);
