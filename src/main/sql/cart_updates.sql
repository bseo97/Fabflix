-- Add price column to movies table
ALTER TABLE movies ADD COLUMN price DECIMAL(10,2) DEFAULT 9.99;

-- Update prices with random values between 5.99 and 19.99
UPDATE movies SET price = ROUND(RAND() * (19.99 - 5.99) + 5.99, 2);

-- Modify sales table to support multiple copies
ALTER TABLE sales ADD COLUMN quantity INT DEFAULT 1; 