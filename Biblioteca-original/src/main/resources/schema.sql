-- Se borran en orden inverso a las dependencias (primero la tabla "hija")
DROP TABLE IF EXISTS libro;
DROP TABLE IF EXISTS autor;

-- Tabla "padre": lado 1 de la relación
CREATE TABLE autor (
                       id     INT          AUTO_INCREMENT PRIMARY KEY,
                       nombre VARCHAR(100) NOT NULL
);

-- Tabla "hija": lado N de la relación (cada libro tiene UN autor)
CREATE TABLE libro (
                       id                INT          AUTO_INCREMENT PRIMARY KEY,  -- clave técnica: no se publica
                       isbn              VARCHAR(13)  NOT NULL,                    -- clave de negocio: se guarda como texto
                       titulo            VARCHAR(150) NOT NULL,
                       autor_id          INT          NOT NULL,
                       fecha_publicacion DATE         NOT NULL,
                       CONSTRAINT uq_libro_isbn UNIQUE (isbn),
    -- ON DELETE CASCADE: al borrar un autor, la BD borra también sus libros.
    -- Sin esta cláusula, el DELETE de un autor con libros daría un error de integridad.
                       CONSTRAINT fk_libro_autor
                           FOREIGN KEY (autor_id) REFERENCES autor (id) ON DELETE CASCADE
);