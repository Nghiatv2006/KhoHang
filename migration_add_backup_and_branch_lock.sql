-- ==============================================================================
-- MIGRATION: ADD BACKUP TABLE AND BRANCH LOCK
-- ==============================================================================

-- 1. Thêm cột is_locked vào bảng branches
ALTER TABLE branches ADD COLUMN IF NOT EXISTS is_locked BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Tạo bảng backups
CREATE TABLE IF NOT EXISTS backups (
    id SERIAL PRIMARY KEY,
    branch_id INT REFERENCES branches(id) ON DELETE CASCADE,
    filename VARCHAR(255) NOT NULL,
    filepath VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    backup_type VARCHAR(50) NOT NULL, -- 'AUTO', 'MANUAL'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by INT REFERENCES users(id) ON DELETE SET NULL
);

-- 3. Tạo index cho backups để tối ưu hóa truy vấn
CREATE INDEX IF NOT EXISTS idx_backups_branch_id ON backups (branch_id);


ALTER TYPE receipt_status ADD VALUE IF NOT EXISTS 'PENDING_ADMIN';
ALTER TYPE receipt_status ADD VALUE IF NOT EXISTS 'PENDING_STOCKTAKE';

ALTER TABLE receipts ADD COLUMN stocktake_by_id INT REFERENCES users(id);