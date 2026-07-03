-- ==============================================================================
-- MIGRATION: Add DISPOSAL receipt type + disposal metadata columns
-- ⚠️ LƯU Ý: Phải chạy file này theo 2 bước (hoặc chạy từng phần riêng biệt)
-- vì PostgreSQL không cho phép sử dụng giá trị Enum mới trong cùng một transaction.
-- ==============================================================================

-- BƯỚC 1: Chạy duy nhất dòng dưới đây trước, sau đó nhấn COMMIT (hoặc F5 chạy riêng dòng này)
-- ------------------------------------------------------------------------------
ALTER TYPE receipt_type ADD VALUE IF NOT EXISTS 'DISPOSAL';

-- ------------------------------------------------------------------------------
-- BƯỚC 2: Sau khi chạy xong Bước 1, hãy bôi đen và chạy toàn bộ các dòng dưới đây
-- ------------------------------------------------------------------------------
-- Add new columns to receipts table
ALTER TABLE receipts ADD COLUMN IF NOT EXISTS disposal_reason VARCHAR(255);
ALTER TABLE receipts ADD COLUMN IF NOT EXISTS disposal_method VARCHAR(255);
ALTER TABLE receipts ADD COLUMN IF NOT EXISTS attachment_url VARCHAR(500);

-- Migrate existing ADJUST_OUT disposal receipts to DISPOSAL type
UPDATE receipts SET type = 'DISPOSAL' WHERE type = 'ADJUST_OUT';

-- Verify migration
SELECT type, COUNT(*) as count FROM receipts GROUP BY type ORDER BY type;
