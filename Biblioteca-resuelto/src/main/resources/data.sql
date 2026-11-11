-- Autores (Elena Poniatowska no tiene libros: es el caso 0..N)
INSERT INTO autor (nombre, anio_nacimiento) VALUES ('Gabriel García Márquez', 1927);  -- 1
INSERT INTO autor (nombre, anio_nacimiento) VALUES ('Julio Cortázar', 1914);          -- 2
INSERT INTO autor (nombre, anio_nacimiento) VALUES ('Mario Vargas Llosa', 1936);      -- 3
INSERT INTO autor (nombre, anio_nacimiento) VALUES ('Isabel Allende', 1942);          -- 4
INSERT INTO autor (nombre, anio_nacimiento) VALUES ('Carmen Laforet', 1921);          -- 5
INSERT INTO autor (nombre, anio_nacimiento) VALUES ('Ana María Matute', 1925);        -- 6
INSERT INTO autor (nombre, anio_nacimiento) VALUES ('Miguel Delibes', 1920);          -- 7
INSERT INTO autor (nombre, anio_nacimiento) VALUES ('Almudena Grandes', 1960);        -- 8
INSERT INTO autor (nombre, anio_nacimiento) VALUES ('Elena Poniatowska', 1932);       -- 9

-- Libros. Los títulos, autores y años son reales.
-- Los ISBN son de ejemplo, con dígito de control correcto; el día y el mes son orientativos.
INSERT INTO libro (isbn, titulo, autor_id, fecha_publicacion) VALUES ('9788490000076', 'Bestiario', 2, '1951-07-20');
INSERT INTO libro (isbn, titulo, autor_id, fecha_publicacion) VALUES ('9788490000144', 'La isla y los demonios', 5, '1952-03-14');
INSERT INTO libro (isbn, titulo, autor_id, fecha_publicacion) VALUES ('9788490000212', 'Nada', 5, '1945-05-10');
INSERT INTO libro (isbn, titulo, autor_id, fecha_publicacion) VALUES ('9788490000281', 'La sombra del ciprés es alargada', 7, '1948-02-16');
INSERT INTO libro (isbn, titulo, autor_id, fecha_publicacion) VALUES ('9788490000359', 'Los hijos muertos', 6, '1958-11-03');
INSERT INTO libro (isbn, titulo, autor_id, fecha_publicacion) VALUES ('9788490000427', 'Primera memoria', 6, '1960-01-22');
INSERT INTO libro (isbn, titulo, autor_id, fecha_publicacion) VALUES ('9788490000496', 'Rayuela', 2, '1963-06-28');
INSERT INTO libro (isbn, titulo, autor_id, fecha_publicacion) VALUES ('9788490000564', 'La ciudad y los perros', 3, '1963-10-10');
INSERT INTO libro (isbn, titulo, autor_id, fecha_publicacion) VALUES ('9788490000632', 'Todos los fuegos el fuego', 2, '1966-04-18');
INSERT INTO libro (isbn, titulo, autor_id, fecha_publicacion) VALUES ('9788490000700', 'La casa verde', 3, '1966-03-07');
INSERT INTO libro (isbn, titulo, autor_id, fecha_publicacion) VALUES ('9788490000779', 'Cinco horas con Mario', 7, '1966-11-21');
INSERT INTO libro (isbn, titulo, autor_id, fecha_publicacion) VALUES ('9788490000847', 'Cien años de soledad', 1, '1967-05-30');
INSERT INTO libro (isbn, titulo, autor_id, fecha_publicacion) VALUES ('9788490000915', 'Conversación en La Catedral', 3, '1969-03-15');
INSERT INTO libro (isbn, titulo, autor_id, fecha_publicacion) VALUES ('9788490000984', 'Relato de un náufrago', 1, '1970-12-09');
INSERT INTO libro (isbn, titulo, autor_id, fecha_publicacion) VALUES ('9788490001059', 'Crónica de una muerte anunciada', 1, '1981-04-27');
INSERT INTO libro (isbn, titulo, autor_id, fecha_publicacion) VALUES ('9788490001127', 'Los santos inocentes', 7, '1981-09-14');
INSERT INTO libro (isbn, titulo, autor_id, fecha_publicacion) VALUES ('9788490001196', 'La casa de los espíritus', 4, '1982-10-05');
INSERT INTO libro (isbn, titulo, autor_id, fecha_publicacion) VALUES ('9788490001264', 'El amor en los tiempos del cólera', 1, '1985-12-02');
INSERT INTO libro (isbn, titulo, autor_id, fecha_publicacion) VALUES ('9788490001332', 'Eva Luna', 4, '1987-06-08');
INSERT INTO libro (isbn, titulo, autor_id, fecha_publicacion) VALUES ('9788490001400', 'Las edades de Lulú', 8, '1989-02-20');
INSERT INTO libro (isbn, titulo, autor_id, fecha_publicacion) VALUES ('9788490001479', 'Malena es un nombre de tango', 8, '1994-09-19');