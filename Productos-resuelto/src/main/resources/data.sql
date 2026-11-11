-- Fabricantes (Huawei y Xiaomi no tienen productos: se podrán borrar)
INSERT INTO fabricante (nombre) VALUES ('Asus');            -- 1
INSERT INTO fabricante (nombre) VALUES ('Lenovo');          -- 2
INSERT INTO fabricante (nombre) VALUES ('Hewlett-Packard'); -- 3
INSERT INTO fabricante (nombre) VALUES ('Samsung');         -- 4
INSERT INTO fabricante (nombre) VALUES ('Seagate');         -- 5
INSERT INTO fabricante (nombre) VALUES ('Crucial');         -- 6
INSERT INTO fabricante (nombre) VALUES ('Gigabyte');        -- 7
INSERT INTO fabricante (nombre) VALUES ('Huawei');          -- 8
INSERT INTO fabricante (nombre) VALUES ('Xiaomi');          -- 9

-- Productos
INSERT INTO producto (nombre, precio, stock, codigo_fabricante) VALUES ('Disco duro SATA3 1TB', 86.99, 25, 5);
INSERT INTO producto (nombre, precio, stock, codigo_fabricante) VALUES ('Memoria RAM DDR4 8GB', 120, 40, 6);
INSERT INTO producto (nombre, precio, stock, codigo_fabricante) VALUES ('Disco SSD 1 TB', 150.99, 15, 4);
INSERT INTO producto (nombre, precio, stock, codigo_fabricante) VALUES ('GeForce GTX 1050Ti', 185, 0, 7);
INSERT INTO producto (nombre, precio, stock, codigo_fabricante) VALUES ('GeForce GTX 1080 Xtreme', 755, 3, 6);
INSERT INTO producto (nombre, precio, stock, codigo_fabricante) VALUES ('Monitor 24 LED Full HD', 202, 12, 1);
INSERT INTO producto (nombre, precio, stock, codigo_fabricante) VALUES ('Monitor 27 LED Full HD', 245.99, 8, 1);
INSERT INTO producto (nombre, precio, stock, codigo_fabricante) VALUES ('Portátil Yoga 520', 559, 5, 2);
INSERT INTO producto (nombre, precio, stock, codigo_fabricante) VALUES ('Portátil Ideapd 320', 444, 0, 2);
INSERT INTO producto (nombre, precio, stock, codigo_fabricante) VALUES ('Impresora HP Deskjet 3720', 59.99, 30, 3);
INSERT INTO producto (nombre, precio, stock, codigo_fabricante) VALUES ('Impresora HP Laserjet Pro M26nw', 180, 7, 3);