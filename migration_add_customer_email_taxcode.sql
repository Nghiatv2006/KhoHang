-- ==============================================================================
-- Migration: Bổ sung các cột còn thiếu vào bảng customers
-- Nguyên nhân: Model Java và UI có email, tax_code nhưng database table customers không có
-- => Gây lỗi 400 Bad Request khi gọi GET /api/customers
-- Chạy script này trên PostgreSQL (warehouse_db) để fix lỗi
-- ==============================================================================

-- 1. Thêm các cột email và tax_code nếu chưa tồn tại
ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS tax_code VARCHAR(50),
    ADD COLUMN IF NOT EXISTS branch_id INT;

-- 2. Thêm foreign key constraint cho branch_id nếu chưa tồn tại
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.table_constraints 
        WHERE table_name='customers' AND constraint_name='fk_customer_branch'
    ) THEN
        ALTER TABLE customers 
            ADD CONSTRAINT fk_customer_branch 
            FOREIGN KEY (branch_id) 
            REFERENCES branches(id) 
            ON DELETE RESTRICT;
    END IF;
END;
$$;
