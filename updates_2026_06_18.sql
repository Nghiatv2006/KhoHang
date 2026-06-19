-- ==============================================================================
-- BỔ SUNG CÁC CỘT MỚI VÀO DATABASE (Cập nhật ngày 18/06/2026)
-- ==============================================================================

-- 1. Bổ sung trường branch_id cho bảng users để quản lý Chi nhánh của nhân viên/quản lý
ALTER TABLE users ADD COLUMN IF NOT EXISTS branch_id INT;
ALTER TABLE users DROP CONSTRAINT IF EXISTS fk_user_branch;
ALTER TABLE users ADD CONSTRAINT fk_user_branch FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE SET NULL;

-- 2. Bổ sung trường unit (Đơn vị tính) cho bảng products
ALTER TABLE products ADD COLUMN IF NOT EXISTS unit VARCHAR(50);

-- 3. Bổ sung trường image_url (Đường dẫn ảnh) cho bảng products
ALTER TABLE products ADD COLUMN IF NOT EXISTS image_url VARCHAR(500);
