INSERT INTO categories (name)
VALUES 
    ('iPhone thường'),
    ('iPhone Pro'),
    ('iPhone Pro Max')
ON CONFLICT (name) DO NOTHING;
