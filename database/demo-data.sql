-- Run against stationery_shop after the application's first start creates product.
-- Re-running this script keeps existing demo stock and prices.
INSERT INTO product (name, category, description, price, stock, status)
SELECT 'A5 方格笔记本', '纸品', '轻便方格内页，适合课堂笔记和错题整理。', 8.90, 36, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = 'A5 方格笔记本');

INSERT INTO product (name, category, description, price, stock, status)
SELECT 'B5 活页笔记本', '纸品', '可替换内页，方便整理不同科目的笔记。', 16.90, 24, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = 'B5 活页笔记本');

INSERT INTO product (name, category, description, price, stock, status)
SELECT '0.5 mm 黑色中性笔', '书写工具', '速干黑色墨水，适合日常书写。', 3.50, 80, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = '0.5 mm 黑色中性笔');

INSERT INTO product (name, category, description, price, stock, status)
SELECT '多色荧光笔套装', '书写工具', '六色搭配，用于标记课本和复习资料。', 12.80, 28, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = '多色荧光笔套装');

INSERT INTO product (name, category, description, price, stock, status)
SELECT '索引便利贴', '办公用品', '可移除的彩色页签，方便定位重点内容。', 5.90, 40, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = '索引便利贴');
