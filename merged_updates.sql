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
