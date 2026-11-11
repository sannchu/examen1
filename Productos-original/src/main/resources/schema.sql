-- Se borran en orden inverso a las dependencias (primero la tabla "hija")
DROP TABLE IF EXISTS producto;
DROP TABLE IF EXISTS fabricante;

-- Tabla "padre": lado 1 de la relación
CREATE TABLE fabricante (
                            codigo INT          AUTO_INCREMENT PRIMARY KEY,
                            nombre VARCHAR(100) NOT NULL
);

-- Tabla "hija": lado N de la relación (cada producto tiene UN fabricante)
CREATE TABLE producto (
                          codigo            INT          AUTO_INCREMENT PRIMARY KEY,
                          nombre            VARCHAR(100) NOT NULL,
                          precio            DOUBLE       NOT NULL,
                          codigo_fabricante INT          NOT NULL,
                          CONSTRAINT fk_producto_fabricante
                              FOREIGN KEY (codigo_fabricante) REFERENCES fabricante (codigo)
);