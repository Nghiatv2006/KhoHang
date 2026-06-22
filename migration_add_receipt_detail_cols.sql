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
