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
INSERT INTO producto (nombre, precio, codigo_fabricante) VALUES ('Disco duro SATA3 1TB', 86.99, 5);
INSERT INTO producto (nombre, precio, codigo_fabricante) VALUES ('Memoria RAM DDR4 8GB', 120, 6);
INSERT INTO producto (nombre, precio, codigo_fabricante) VALUES ('Disco SSD 1 TB', 150.99, 4);
INSERT INTO producto (nombre, precio, codigo_fabricante) VALUES ('GeForce GTX 1050Ti', 185, 7);
INSERT INTO producto (nombre, precio, codigo_fabricante) VALUES ('GeForce GTX 1080 Xtreme', 755, 6);
INSERT INTO producto (nombre, precio, codigo_fabricante) VALUES ('Monitor 24 LED Full HD', 202, 1);
INSERT INTO producto (nombre, precio, codigo_fabricante) VALUES ('Monitor 27 LED Full HD', 245.99, 1);
INSERT INTO producto (nombre, precio, codigo_fabricante) VALUES ('Portátil Yoga 520', 559, 2);
INSERT INTO producto (nombre, precio, codigo_fabricante) VALUES ('Portátil Ideapd 320', 444, 2);
INSERT INTO producto (nombre, precio, codigo_fabricante) VALUES ('Impresora HP Deskjet 3720', 59.99, 3);
INSERT INTO producto (nombre, precio, codigo_fabricante) VALUES ('Impresora HP Laserjet Pro M26nw', 180, 3);