-- Script cập nhật database: Thêm trường import_price vào bảng products
-- Sử dụng: Chạy lệnh này trên database hiện tại để thêm cột Giá nhập cho các sản phẩm cũ
-- Nếu bảng đã có dữ liệu, thêm cột với DEFAULT 0.00 sẽ tự động gán giá trị 0.00 cho các dữ liệu cũ.

ALTER TABLE products ADD COLUMN import_price NUMERIC(15, 2) DEFAULT 0.00;
