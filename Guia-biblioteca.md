# Guía de estudio: Biblioteca

[Guía común](Guia-comun.md) · [Guía de Productos](Guia-productos.md)

La profesora plantea Biblioteca como una aplicación que se construye desde cero por **hitos 0 a 8**, no como nueve retos añadidos a un proyecto inicial ya terminado. En este espacio, `Biblioteca-original` ya contiene gran parte de esa aplicación: los hitos sirven para entender cómo está construida y comprobarla. `Biblioteca-resuelto` añade el reto del año de nacimiento. No copies de nuevo métodos que ya existan.

Primero se explica el reto del año, como pedías; después se recorren los hitos de la aplicación base. El código de los hitos se consulta en `Biblioteca-original/src/main/java/es/daw/bibliotecajdbc/`; el del reto en la ruta equivalente de `Biblioteca-resuelto`. Los recursos están en `src/main/resources/`.

## Mapa rápido y archivos principales

```text
AUTOR (1) ───── escribe ───── (N) LIBRO
libro.autor_id → autor.id     ISBN identifica el libro ante el usuario
```

- `model/Autor.java` y `Libro.java`: objetos de dominio; el libro contiene un objeto `Autor` y una fecha `LocalDate`.
- `dto/AutorDto.java`, `LibroDto.java` y `FiltroLibrosDto.java`: datos de formularios y restricciones. Los DTO no son tablas.
- `mapper/AutorMapper.java` y `LibroMapper.java`: convierten DTO ↔ modelo. El autor elegido llega como id y el servicio recupera el objeto real.
- `repository/GenericRepository.java`, `AutorRepository.java`, `LibroRepository.java`: contratos. Los archivos con sufijo `Jdbc` hacen consultas y crean objetos a partir de filas.
- `service/AutorService.java` y `LibroService.java`: reglas de negocio, filtros y coordinación de repositorios.
- `controller/AutorController.java` y `LibroController.java`: URLs, validación, conversión y elección de vista.
- `exception/`: errores de acceso a datos, recursos inexistentes e ISBN duplicado y tratamiento global.
- `schema.sql`, `data.sql`, `application.properties`, `messages.properties` y `templates/`: tablas, datos, configuración, textos y vistas.

## Reto: año de nacimiento del autor

El objetivo es incorporar `anioNacimiento` obligatorio entre 1000 y **2026**, como pide este enunciado. La ruta Java de esta sección es `Biblioteca-resuelto/src/main/java/es/daw/bibliotecajdbc/`.

### Paso 1. Base de datos y datos

En `resources/schema.sql`, dentro de la tabla `autor`, añade `anio_nacimiento INT NOT NULL`; no olvides la coma después de la columna anterior.

El bloque completo de la tabla, con el reto aplicado, es:

```sql
CREATE TABLE autor (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    anio_nacimiento INT NOT NULL
);
```
 En `data.sql` añade la columna y el año en cada INSERT de autores:



```sql
INSERT INTO autor (nombre, anio_nacimiento) VALUES ('Gabriel García Márquez', 1927);
```


Consulta los nueve valores en el archivo resuelto. Como la columna es obligatoria, todos los autores iniciales deben tener año.

### Paso 2. Modelo y DTO

En `model/Autor.java` añade `private Integer anioNacimiento;`. El constructor completo de Lombok ahora recibe `(id, nombre, anioNacimiento)`. Busca todos los `new Autor(...)`: están en ambos mappers y en ambos repositorios. Hay que revisarlos todos, incluso el que construye el autor dentro de un libro.

En `dto/AutorDto.java`, añade:



```java
@jakarta.validation.constraints.NotNull(message = "{autor.anio.obligatorio}")
@jakarta.validation.constraints.Min(value = 1000, message = "{autor.anio.rango}")
@jakarta.validation.constraints.Max(value = 2026, message = "{autor.anio.rango}")
private Integer anioNacimiento;
```


Los textos referenciados por llaves se declaran en `resources/messages.properties`:



```properties
autor.anio.obligatorio=El año de nacimiento es obligatorio
autor.anio.rango=El año debe estar entre 1000 y 2026
typeMismatch.autorDto.anioNacimiento=Debe ser un número entero
```


`Integer` permite validar vacío; el mensaje `typeMismatch` cubre texto no convertible a número, que ocurre antes de aplicar `@Min` o `@Max`.

### Paso 3. Mapper: ida y vuelta

Abre `Biblioteca-resuelto/src/main/java/es/daw/bibliotecajdbc/mapper/AutorMapper.java`. En el proyecto resuelto **ya están hechos** ambos cambios. Si partes de `Biblioteca-original`, sustituye los dos métodos existentes por estos, sin duplicarlos:

```java
public AutorDto toDto(Autor autor) {
    AutorDto dto = new AutorDto();
    dto.setId(autor.getId());
    dto.setNombre(autor.getNombre());
    dto.setAnioNacimiento(autor.getAnioNacimiento()); // añadir aquí, antes del return
    return dto;
}

public Autor toModelEntity(AutorDto dto) {
    return new Autor(dto.getId(), dto.getNombre(), dto.getAnioNacimiento());
}
```

En `toDto`, `autor` contiene el dato leído de la base y `dto` lo recibe para mostrarlo. En `toModelEntity`, el constructor copia el año del formulario al modelo. Los métodos `getAnioNacimiento` y `setAnioNacimiento` los genera Lombok a partir de los campos de `Autor` y `AutorDto`; por eso no aparecen escritos a mano en esas clases.

Si olvidas la primera dirección, edición/listado no reciben el año; si olvidas la segunda, el año del formulario no llega al servicio. En `LibroMapper` cambia la construcción parcial a `new Autor(dto.getAutorId(), null, null)`; más adelante el servicio busca el autor completo por su id.

### Paso 4. JDBC de autores

En `AutorRepositoryJdbc.java`, ambos SELECT deben incluir el año:



```sql
SELECT id, nombre, anio_nacimiento FROM autor WHERE id = ?
SELECT id, nombre, anio_nacimiento FROM autor ORDER BY nombre
```


Después cambia `mapRow` a:



```java
private Autor mapRow(ResultSet rs) throws SQLException {
        return new Autor(rs.getInt("id"), rs.getString("nombre"), rs.getInt("anio_nacimiento"));
    }
```


En `save`, usa `INSERT INTO autor (nombre, anio_nacimiento) VALUES (?, ?)` y asigna nombre al 1 y año al 2. En `update`, el método completo es:



```java
public void update(Autor autor) {
        String sql = "UPDATE autor SET nombre = ?, anio_nacimiento = ? WHERE id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, autor.getNombre());
            ps.setInt(2, autor.getAnioNacimiento());
            ps.setInt(3, autor.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al actualizar el autor " + autor.getId(), e);
        }
    }
```


El id ahora es el parámetro 3 porque hay dos valores antes. `setInt(2, autor.getAnioNacimiento())` es seguro en este flujo después de validar el DTO obligatorio.

### Paso 5. JDBC de libros: revisar objetos anidados

`LibroRepositoryJdbc` también construye un `Autor` al leer el JOIN. En su `SELECT_BASE` añade `a.anio_nacimiento AS a_anio`. En el método `mapRow`, cambia la construcción del autor a:



```java
Autor autor = new Autor(rs.getInt("a_id"), rs.getString("a_nombre"), rs.getInt("a_anio"));
```


Para situar el cambio, el bloque completo de `SELECT_BASE` queda así en `repository/LibroRepositoryJdbc.java`:

```java
private static final String SELECT_BASE = """
        SELECT l.id, l.isbn, l.titulo, l.fecha_publicacion,
               a.id AS a_id, a.nombre AS a_nombre, a.anio_nacimiento AS a_anio
        FROM libro l
        JOIN autor a ON l.autor_id = a.id
        """;
```

En `mapper/LibroMapper.java`, dentro de `toModelEntity`, la construcción parcial debe quedar así:

```java
Autor autor = new Autor(dto.getAutorId(), null, null);
```

Solo se conoce el identificador del formulario; `LibroService` busca después el autor completo. El año llega como `null` en este objeto provisional, no como dato definitivo guardado.

Esto evita un autor incompleto dentro de cada libro. En un cambio de modelo, buscar todos los constructores y todos los SELECT que generan ese modelo es tan importante como modificar el repositorio principal.

### Paso 6. Formulario y lista

En `resources/templates/autores/formulario.html`, dentro del formulario, añade:



```html
<div class="mb-3">
  <label for="anioNacimiento" class="form-label">Año de nacimiento</label>
  <input type="text" th:field="*{anioNacimiento}" class="form-control"
         th:classappend="${#fields.hasErrors('anioNacimiento')} ? 'is-invalid'">
  <div class="invalid-feedback" th:errors="*{anioNacimiento}">Error</div>
</div>
```


El campo es texto para que el servidor pueda recibir y comunicar también un error como `abc`. En `autores/lista.html` añade `<th>Nacimiento</th>` en la cabecera y `<td th:text="${a.anioNacimiento}">1927</td>` en cada fila; cambia el `colspan` de 3 a 4. El controlador ya pasa por `AutorMapper`, por eso no necesita un parámetro separado.

Prueba crear y editar un autor, volver a editarlo y ver el año guardado; prueba vacío, 999, 2027 y texto. Los errores deben aparecer junto al campo y conservar el nombre. Revisa también `/libros` para comprobar que el JOIN sigue funcionando.

## Fragmentos completos para situar los cambios

Estos ejemplos completan los pasos anteriores. Sustituye cada método existente por el mostrado; no añadas dos métodos con la misma firma. Las anotaciones `@GetMapping`, `@PostMapping` y `@Override` se colocan justo encima del método correspondiente.

### Biblioteca: escritura completa del autor

En `repository/AutorRepositoryJdbc.java`, este método muestra tanto el INSERT como la recuperación del identificador autogenerado:

```java
public void save(Autor autor) {
        String sql = "INSERT INTO autor (nombre, anio_nacimiento) VALUES (?, ?)";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, autor.getNombre());
            ps.setInt(2, autor.getAnioNacimiento());
            ps.executeUpdate();

            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    autor.setId(claves.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al guardar el autor", e);
        }
    }
```
`RETURN_GENERATED_KEYS` solicita a JDBC las claves generadas; si aparece una fila, se copia el id al objeto. `getGeneratedKeys()` no sustituye a `executeUpdate()`: primero se ejecuta el INSERT y después se consulta la clave.

### Biblioteca: conversión completa del autor

En `mapper/AutorMapper.java`, el contenido de los dos métodos queda así:

```java
public AutorDto toDto(Autor autor) {
        AutorDto dto = new AutorDto();
        dto.setId(autor.getId());
        dto.setNombre(autor.getNombre());
        dto.setAnioNacimiento(autor.getAnioNacimiento());
        return dto;
    }
```

```java
public Autor toModelEntity(AutorDto dto) {
        return new Autor(dto.getId(), dto.getNombre(), dto.getAnioNacimiento());
    }
```
Al seguir el recorrido desde el formulario, busca `autorMapper.toModelEntity` en el controlador. En sentido inverso, al editar busca `autorMapper.toDto`: así puedes comprobar exactamente dónde se utiliza cada conversión.


## Los hitos de Biblioteca: aplicación base

Fuente: [enunciado de Biblioteca de la profesora](https://github.com/profeMelola/DWES-02-2026-27/blob/main/ejercicios/ejercicio_biblioteca_spring_jdbc.md). La carpeta original ya implementa los elementos principales que se describen a continuación. Son pasos de construcción y aprendizaje; no los trates como modificaciones pendientes solo porque se llamen hitos. Comprueba cada requisito en el código y en ejecución.

### Hito 0. Proyecto y configuración

**Objetivo:** arrancar Spring Boot con Maven, MVC, Thymeleaf, validación, JDBC y H2. `pom.xml` reúne dependencias; `BibliotecaJdbcApplication` arranca Spring; `application.properties` configura `jdbc:h2:mem:bibliotecadb` y la consola H2. Comprueba que inicia y que `/h2-console` conecta con la URL configurada.

#### Código de referencia del hito 0

Los siguientes bloques son **archivos completos del proyecto resuelto**: si reconstruyes desde cero, crea el archivo indicado; si ya existe, compara o sustituye su contenido, sin pegar una clase o método duplicado. La versión incorpora el reto del año. Los nombres de ruta `java/...` y `resources/...` son relativos a `Biblioteca-resuelto/src/main/`.

**Archivo: `resources/application.properties`**

Configura la conexión y la consola H2. Copia o compara el archivo completo; revisa que la URL sea la misma que introduces en la consola.

```properties
spring.application.name=biblioteca-jdbc

# --- Base de datos H2 en memoria ---
spring.datasource.url=jdbc:h2:mem:bibliotecadb;DB_CLOSE_DELAY=-1
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# Ejecutar schema.sql y data.sql al arrancar
spring.sql.init.mode=always
spring.sql.init.encoding=UTF-8

# --- Consola H2 ---
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

**Archivo: `java/es/daw/bibliotecajdbc/BibliotecaJdbcApplication.java`**

Punto de entrada: `SpringApplication.run` inicia la aplicación y descubre los componentes.

```java
package es.daw.bibliotecajdbc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BibliotecaJdbcApplication {

    public static void main(String[] args) {
        SpringApplication.run(BibliotecaJdbcApplication.class, args);
    }

}
```

### Hito 1. Tablas y datos

`schema.sql` elimina primero la tabla hija `libro` y luego `autor`; las crea en orden inverso. `libro.autor_id` es la FK. `isbn` tiene restricción `UNIQUE` y la FK incluye `ON DELETE CASCADE`. `data.sql` inserta primero autores y después libros, incluidos ejemplos con diferentes fechas y un autor sin libros. Con el reto del año, los INSERT de autores también necesitan ese dato.

**Por qué cascada:** al ejecutar el borrado de un autor, H2 elimina sus libros asociados. En Productos la regla es diferente: el servicio impide borrar un fabricante con productos. Una misma relación 1:N puede tener reglas distintas.

#### Código de referencia del hito 1

Los siguientes bloques son **archivos completos del proyecto resuelto**: si reconstruyes desde cero, crea el archivo indicado; si ya existe, compara o sustituye su contenido, sin pegar una clase o método duplicado. La versión incorpora el reto del año. Los nombres de ruta `java/...` y `resources/...` son relativos a `Biblioteca-resuelto/src/main/`.

**Archivo: `resources/schema.sql`**

Define las tablas; el orden respeta la relación y la FK en cascada. Esta versión incluye el reto del año.

```sql
-- Se borran en orden inverso a las dependencias (primero la tabla "hija")
DROP TABLE IF EXISTS libro;
DROP TABLE IF EXISTS autor;

-- Tabla "padre": lado 1 de la relación
CREATE TABLE autor (
                       id     INT          AUTO_INCREMENT PRIMARY KEY,
                       nombre VARCHAR(100) NOT NULL,
                       anio_nacimiento INT NOT NULL
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
```

**Archivo: `resources/data.sql`**

Inserta autores antes que libros. Incluye el año porque el esquema resuelto lo exige.

```sql
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
```

### Hito 2. Modelo, DTO, mapper, interfaces y excepciones

1. `Autor` guarda id y nombre; `Libro`, id, ISBN, título, autor y fecha. En el resuelto `Autor` añade el año. `LocalDate` representa la fecha en Java.
2. `LibroDto` recibe ISBN, título, `autorId`, fecha y `edicion`; el id interno del libro no viaja en el formulario. Las anotaciones del DTO validan los campos. `FiltroLibrosDto` contiene los criterios opcionales.
3. `LibroMapper.toModelEntity` convierte el DTO a libro con id `null` y un autor construido solo con su id. El servicio completará el autor; el mapper no consulta la base. `toDto` prepara los datos para editar o listar.
4. `GenericRepository<T, ID>` declara CRUD. `LibroRepository` hereda esos métodos y declara también `Optional<Libro> findByIsbn(String isbn)` y `int countByAutorId(Integer autorId)`. `AutorRepository` hereda su CRUD.
5. `AccesoDatosException` comunica problemas de acceso; `RecursoNoEncontradoException` señala un recurso ausente; `IsbnDuplicadoException` permite mostrar un error junto al ISBN. No se necesita una excepción de autor con libros porque aquí su borrado está permitido y es en cascada.

**Contrato frente a implementación:** `LibroRepository` declara las firmas para que `LibroService` pueda invocarlas; `LibroRepositoryJdbc implements LibroRepository` contiene las consultas. Una firma en la interfaz y un cuerpo en JDBC son dos partes de la misma operación. La guía común desarrolla esta distinción.

#### Código de referencia del hito 2

Los siguientes bloques son **archivos completos del proyecto resuelto**: si reconstruyes desde cero, crea el archivo indicado; si ya existe, compara o sustituye su contenido, sin pegar una clase o método duplicado. La versión incorpora el reto del año. Los nombres de ruta `java/...` y `resources/...` son relativos a `Biblioteca-resuelto/src/main/`.

**Archivo: `java/es/daw/bibliotecajdbc/model/Autor.java`**

Modelo de autor; Lombok genera el constructor con los campos en el orden declarado.

```java
package es.daw.bibliotecajdbc.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Fila de la tabla AUTOR.
 * POJO sin anotaciones: ni de persistencia ni de validación.
 * Las validaciones del formulario están en AutorDto.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Autor {

    private Integer id;
    private String nombre;
    private Integer anioNacimiento;
}
```

**Archivo: `java/es/daw/bibliotecajdbc/model/Libro.java`**

Modelo de libro: guarda el autor completo y una fecha `LocalDate`.

```java
package es.daw.bibliotecajdbc.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Fila de la tabla LIBRO.
 *
 * - id: clave técnica. Se usa internamente y no se publica.
 * - isbn: clave de negocio. Es lo que ve el usuario y lo que viaja en las URL.
 * - autor: objeto completo, no el INT de la columna autor_id.
 * - fechaPublicacion: LocalDate, una fecha sin hora ni zona horaria,
 *   que es el equivalente en Java al tipo DATE de la base de datos.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Libro {

    private Integer id;
    private String isbn;
    private String titulo;
    private Autor autor;
    private LocalDate fechaPublicacion;
}
```

**Archivo: `java/es/daw/bibliotecajdbc/dto/AutorDto.java`**

DTO del formulario de autor con sus validaciones, incluido el año.

```java
package es.daw.bibliotecajdbc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Datos del autor entre la vista y el controlador.
 * El id viaja porque el autor no tiene clave de negocio: lo necesitan las URL
 * de edición y borrado y el value del select. Nunca se muestra como dato.
 */
@Data
@NoArgsConstructor
public class AutorDto {

    private Integer id;      // null = alta, con valor = edición

    @NotBlank(message = "{autor.nombre.obligatorio}")
    @Size(max = 100, message = "{autor.nombre.tamanio}")
    private String nombre;

    @jakarta.validation.constraints.NotNull(message = "{autor.anio.obligatorio}")
    @jakarta.validation.constraints.Min(value = 1000, message = "{autor.anio.rango}")
    @jakarta.validation.constraints.Max(value = 2026, message = "{autor.anio.rango}")
    private Integer anioNacimiento;
}
```

**Archivo: `java/es/daw/bibliotecajdbc/dto/LibroDto.java`**

DTO del formulario de libro: el ISBN identifica el libro y `autorId` representa la selección.

```java
package es.daw.bibliotecajdbc.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Datos del libro entre la vista y el controlador.
 *
 * - No lleva id: el id del libro no se publica nunca.
 * - edicion: false = alta, true = edición. Viaja oculto en el formulario.
 * - autorId: lo que envía el select.
 * - autorNombre: solo para mostrar en el listado.
 */
@Data
@NoArgsConstructor
public class LibroDto {

    // Con @NotNull + @Pattern, un campo vacío da un único error.
    // Con @NotBlank + @Pattern saldrían dos errores a la vez.
    @NotNull(message = "{libro.isbn.formato}")
    @Pattern(regexp = "\\d{13}", message = "{libro.isbn.formato}")
    private String isbn;

    private boolean edicion;

    @NotBlank(message = "{libro.titulo.obligatorio}")
    @Size(max = 150, message = "{libro.titulo.tamanio}")
    private String titulo;

    @NotNull(message = "{libro.autor.obligatorio}")
    private Integer autorId;

    private String autorNombre;

    // El navegador envía la fecha como 2026-11-03.
    // @DateTimeFormat le indica a Spring ese formato, tanto al recibir el dato
    // como al escribirlo en el <input type="date"> al editar.
    @NotNull(message = "{libro.fecha.obligatoria}")
    @PastOrPresent(message = "{libro.fecha.futura}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaPublicacion;
}
```

**Archivo: `java/es/daw/bibliotecajdbc/dto/FiltroLibrosDto.java`**

DTO de los parámetros opcionales del filtro; el hito 8 los utilizará.

```java
package es.daw.bibliotecajdbc.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Filtros del listado de libros. Todos son opcionales. */
@Data
@NoArgsConstructor
public class FiltroLibrosDto {

    private Integer autorId;

    @Min(value = 1000, message = "{filtro.anio.rango}")
    @Max(value = 2100, message = "{filtro.anio.rango}")
    private Integer anioDesde;

    @Min(value = 1000, message = "{filtro.anio.rango}")
    @Max(value = 2100, message = "{filtro.anio.rango}")
    private Integer anioHasta;
}
```

**Archivo: `java/es/daw/bibliotecajdbc/mapper/AutorMapper.java`**

Convierte el autor y su DTO en ambas direcciones.

```java
package es.daw.bibliotecajdbc.mapper;

import es.daw.bibliotecajdbc.dto.AutorDto;
import es.daw.bibliotecajdbc.model.Autor;
import org.springframework.stereotype.Component;

/** Convierte entre AutorDto y Autor. Solo convierte: no usa servicios ni repositorios. */
@Component
public class AutorMapper {

    public AutorDto toDto(Autor autor) {
        AutorDto dto = new AutorDto();
        dto.setId(autor.getId());
        dto.setNombre(autor.getNombre());
        dto.setAnioNacimiento(autor.getAnioNacimiento());
        return dto;
    }

    public Autor toModelEntity(AutorDto dto) {
        return new Autor(dto.getId(), dto.getNombre(), dto.getAnioNacimiento());
    }
}
```

**Archivo: `java/es/daw/bibliotecajdbc/mapper/LibroMapper.java`**

Convierte el libro; al llegar del formulario, construye un autor parcial con su id.

```java
package es.daw.bibliotecajdbc.mapper;

import es.daw.bibliotecajdbc.dto.LibroDto;
import es.daw.bibliotecajdbc.model.Autor;
import es.daw.bibliotecajdbc.model.Libro;
import org.springframework.stereotype.Component;

/** Convierte entre LibroDto y Libro. Solo convierte: no usa servicios ni repositorios. */
@Component
public class LibroMapper {

    /** Un libro que viene de la BD ya existe: si se abre su formulario, es una edición. */
    public LibroDto toDto(Libro libro) {
        LibroDto dto = new LibroDto();
        dto.setIsbn(libro.getIsbn());
        dto.setEdicion(true);
        dto.setTitulo(libro.getTitulo());
        dto.setAutorId(libro.getAutor().getId());
        dto.setAutorNombre(libro.getAutor().getNombre());
        dto.setFechaPublicacion(libro.getFechaPublicacion());
        return dto;
    }

    /**
     * El Libro resultante no está completo:
     * - id es null, porque el DTO no lo lleva.
     * - El autor solo lleva su id.
     * LibroService se encarga de completarlo.
     */
    public Libro toModelEntity(LibroDto dto) {
        Autor autor = new Autor(dto.getAutorId(), null, null);
        return new Libro(null, dto.getIsbn(), dto.getTitulo(), autor, dto.getFechaPublicacion());
    }
}
```

**Archivo: `java/es/daw/bibliotecajdbc/repository/GenericRepository.java`**

Declara las operaciones CRUD comunes sin implementar SQL.

```java
package es.daw.bibliotecajdbc.repository;

import java.util.List;
import java.util.Optional;

/**
 * Operaciones CRUD comunes a cualquier entidad.
 *
 * @param <T>  tipo de la entidad (Libro, Autor...)
 * @param <ID> tipo de su clave primaria
 */
public interface GenericRepository<T, ID> {

    void save(T entidad);

    Optional<T> findById(ID id);

    List<T> findAll();

    void update(T entidad);

    void delete(ID id);
}
```

**Archivo: `java/es/daw/bibliotecajdbc/repository/AutorRepository.java`**

Especializa el contrato genérico para autores.

```java
package es.daw.bibliotecajdbc.repository;

import es.daw.bibliotecajdbc.model.Autor;

public interface AutorRepository extends GenericRepository<Autor, Integer> {
}
```

**Archivo: `java/es/daw/bibliotecajdbc/repository/LibroRepository.java`**

Añade las firmas de búsqueda por ISBN y cuenta por autor al contrato CRUD.

```java
package es.daw.bibliotecajdbc.repository;

import es.daw.bibliotecajdbc.model.Libro;

import java.util.List;
import java.util.Optional;

public interface LibroRepository extends GenericRepository<Libro, Integer> {

    /** El ISBN es la clave de negocio: el usuario identifica los libros por él. */
    Optional<Libro> findByIsbn(String isbn);

    /** Cuántos libros tiene un autor. Lo usa AutorService antes de borrar. */
    int countByAutorId(Integer autorId);

}
```

**Archivo: `java/es/daw/bibliotecajdbc/exception/AccesoDatosException.java`**

Envuelve los errores técnicos de acceso a la base.

```java
package es.daw.bibliotecajdbc.exception;

/** Error técnico al acceder a la BD. Envuelve la SQLException original. */
public class AccesoDatosException extends RuntimeException {

  public AccesoDatosException(String mensaje, Throwable causa) {
    super(mensaje, causa);
  }
}
```

**Archivo: `java/es/daw/bibliotecajdbc/exception/RecursoNoEncontradoException.java`**

Representa la ausencia de un recurso solicitado.

```java
package es.daw.bibliotecajdbc.exception;

/** Se pide un libro o un autor que no existe. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
```

**Archivo: `java/es/daw/bibliotecajdbc/exception/IsbnDuplicadoException.java`**

Permite comunicar al controlador que el ISBN ya existe.

```java
package es.daw.bibliotecajdbc.exception;

/** Regla de negocio: no puede haber dos libros con el mismo ISBN. */
public class IsbnDuplicadoException extends RuntimeException {

    public IsbnDuplicadoException(String isbn) {
        super("Ya existe un libro con el ISBN " + isbn);
    }
}
```

**Archivo: `java/es/daw/bibliotecajdbc/exception/GlobalExceptionHandler.java`**

Traduce excepciones tratadas globalmente a una respuesta y una página de error.

```java
package es.daw.bibliotecajdbc.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Manejo centralizado de errores para todos los controladores. */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String noEncontrado(RecursoNoEncontradoException ex, Model model) {
        model.addAttribute("titulo", "No encontrado");
        model.addAttribute("mensaje", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(AccesoDatosException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String errorAccesoDatos(AccesoDatosException ex, Model model) {
        // La causa (la SQLException original) va al log, no al usuario
        log.error(ex.getMessage(), ex);
        model.addAttribute("titulo", "Error de base de datos");
        model.addAttribute("mensaje", "No se ha podido completar la operación");
        return "error";
    }
}
```

### Hito 3. Repositorios JDBC

En `AutorRepositoryJdbc` y `LibroRepositoryJdbc` sigue este patrón: obtener una conexión del `DataSource`, preparar SQL con `?`, asignar parámetros desde 1, ejecutar y cerrar recursos con `try-with-resources`. `executeQuery` lee; `executeUpdate` inserta, actualiza o borra. Se transforma `SQLException` en `AccesoDatosException`.

En libros, `SELECT_BASE` hace JOIN con autor y usa alias `a_id`, `a_nombre` para distinguir columnas. `mapRow` construye primero el autor y después el libro. `findAll` ordena por fecha descendente; `findByIsbn` devuelve `Optional`; `countByAutorId` devuelve la cuenta. `save` recupera el id generado con `RETURN_GENERATED_KEYS`.

```java
// Al escribir un LocalDate en la columna DATE
ps.setObject(4, libro.getFechaPublicacion());
// Al leerla del ResultSet
rs.getObject("fecha_publicacion", LocalDate.class);
```

El repositorio modifica y borra por id interno; para buscar desde la pantalla existe la consulta por ISBN. Con el reto del año, la lectura del autor anidado en el JOIN también necesita ese campo: no basta con cambiar el repositorio de autores.

#### Código de referencia del hito 3

Los siguientes bloques son **archivos completos del proyecto resuelto**: si reconstruyes desde cero, crea el archivo indicado; si ya existe, compara o sustituye su contenido, sin pegar una clase o método duplicado. La versión incorpora el reto del año. Los nombres de ruta `java/...` y `resources/...` son relativos a `Biblioteca-resuelto/src/main/`.

**Archivo: `java/es/daw/bibliotecajdbc/repository/AutorRepositoryJdbc.java`**

Implementa el contrato de autores: lee y escribe el año, mapea filas y obtiene el id generado.

```java
package es.daw.bibliotecajdbc.repository;

import es.daw.bibliotecajdbc.exception.AccesoDatosException;
import es.daw.bibliotecajdbc.model.Autor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AutorRepositoryJdbc implements AutorRepository {

    private final DataSource dataSource;


    @Override
    public Optional<Autor> findById(Integer id) {
        String sql = "SELECT id, nombre, anio_nacimiento FROM autor WHERE id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al buscar el autor " + id, e);
        }
    }

    @Override
    public List<Autor> findAll() {
        String sql = "SELECT id, nombre, anio_nacimiento FROM autor ORDER BY nombre";
        List<Autor> autores = new ArrayList<>();
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                autores.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al listar los autores", e);
        }
        return autores;
    }

    @Override
    public void save(Autor autor) {
        String sql = "INSERT INTO autor (nombre, anio_nacimiento) VALUES (?, ?)";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, autor.getNombre());
            ps.setInt(2, autor.getAnioNacimiento());
            ps.executeUpdate();

            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    autor.setId(claves.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al guardar el autor", e);
        }
    }

    @Override
    public void update(Autor autor) {
        String sql = "UPDATE autor SET nombre = ?, anio_nacimiento = ? WHERE id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, autor.getNombre());
            ps.setInt(2, autor.getAnioNacimiento());
            ps.setInt(3, autor.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al actualizar el autor " + autor.getId(), e);
        }
    }

    /**
     * Solo borra de la tabla autor.
     * Sus libros los borra la BD por el ON DELETE CASCADE de schema.sql.
     */
    @Override
    public void delete(Integer id) {
        String sql = "DELETE FROM autor WHERE id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al borrar el autor " + id, e);
        }
    }

    private Autor mapRow(ResultSet rs) throws SQLException {
        return new Autor(rs.getInt("id"), rs.getString("nombre"), rs.getInt("anio_nacimiento"));
    }
}
```

**Archivo: `java/es/daw/bibliotecajdbc/repository/LibroRepositoryJdbc.java`**

Implementa CRUD, búsqueda y conteo. El JOIN incluye el año del autor; `mapRow` reconstruye el libro y su autor.

```java
package es.daw.bibliotecajdbc.repository;

import es.daw.bibliotecajdbc.exception.AccesoDatosException;
import es.daw.bibliotecajdbc.model.Autor;
import es.daw.bibliotecajdbc.model.Libro;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LibroRepositoryJdbc implements LibroRepository {

    /*
     * SELECT común a todas las consultas.
     * Los alias a_id y a_nombre evitan la ambigüedad con el id del libro.
     */
    private static final String SELECT_BASE = """
            SELECT l.id, l.isbn, l.titulo, l.fecha_publicacion,
                   a.id AS a_id, a.nombre AS a_nombre, a.anio_nacimiento AS a_anio
            FROM libro l
            JOIN autor a ON l.autor_id = a.id
            """;

    private final DataSource dataSource;


    @Override
    public Optional<Libro> findById(Integer id) {
        String sql = SELECT_BASE + "WHERE l.id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al buscar el libro " + id, e);
        }
    }

    @Override
    public Optional<Libro> findByIsbn(String isbn) {
        String sql = SELECT_BASE + "WHERE l.isbn = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, isbn);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al buscar el libro con ISBN " + isbn, e);
        }
    }

    @Override
    public List<Libro> findAll() {
        String sql = SELECT_BASE + "ORDER BY l.fecha_publicacion DESC, l.titulo";
        List<Libro> libros = new ArrayList<>();
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                libros.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al listar los libros", e);
        }
        return libros;
    }


    @Override
    public void save(Libro libro) {
        String sql = """
                INSERT INTO libro (isbn, titulo, autor_id, fecha_publicacion)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, libro.getIsbn());
            ps.setString(2, libro.getTitulo());
            ps.setInt(3, libro.getAutor().getId());
            ps.setObject(4, libro.getFechaPublicacion());   // LocalDate → DATE
            ps.executeUpdate();

            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    libro.setId(claves.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al guardar el libro", e);
        }
    }

    /** El ISBN no se modifica: no aparece en el SET. */
    @Override
    public void update(Libro libro) {
        String sql = """
                UPDATE libro
                SET titulo = ?, autor_id = ?, fecha_publicacion = ?
                WHERE id = ?
                """;
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, libro.getTitulo());
            ps.setInt(2, libro.getAutor().getId());
            ps.setObject(3, libro.getFechaPublicacion());
            ps.setInt(4, libro.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al actualizar el libro " + libro.getIsbn(), e);
        }
    }

    @Override
    public void delete(Integer id) {
        String sql = "DELETE FROM libro WHERE id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al borrar el libro " + id, e);
        }
    }

    @Override
    public int countByAutorId(Integer autorId) {
        String sql = "SELECT COUNT(*) FROM libro WHERE autor_id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, autorId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();   // COUNT siempre devuelve una fila
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al contar los libros del autor " + autorId, e);
        }
    }

    private Libro mapRow(ResultSet rs) throws SQLException {
        Autor autor = new Autor(rs.getInt("a_id"), rs.getString("a_nombre"), rs.getInt("a_anio"));

        return new Libro(
                rs.getInt("id"),
                rs.getString("isbn"),
                rs.getString("titulo"),
                autor,
                // getObject con el tipo: convierte DATE directamente a LocalDate
                rs.getObject("fecha_publicacion", LocalDate.class));
    }
}
```

### Hito 4. Servicios y reglas

`LibroService.buscar` obtiene `findAll()` y filtra en Java. Un criterio nulo deja pasar todos los libros; `>=` y `<=` incluyen los años extremos. Por ejemplo, 1960–1970 incluye ambos. El orden viene del SQL de `findAll`, que el stream conserva.

```java
.filter(l -> anioDesde == null || l.getFechaPublicacion().getYear() >= anioDesde)
```

Se lee: si no hay límite inferior, acepta el libro; si lo hay, exige que su año sea igual o mayor. Los demás filtros siguen el mismo patrón.

`crear` comprueba que el ISBN no exista, recorta el título y comprueba que el autor exista. `actualizar` y `borrar` reciben ISBN, buscan el libro y usan su id interno para la operación JDBC. El `UPDATE` no cambia el ISBN. `AutorService` recorta nombres y comprueba existencia al editar o borrar. Para borrar un autor cuenta sus libros **antes** de eliminarlo y devuelve esa cantidad; la base de datos realiza la cascada.

#### Código de referencia del hito 4

Los siguientes bloques son **archivos completos del proyecto resuelto**: si reconstruyes desde cero, crea el archivo indicado; si ya existe, compara o sustituye su contenido, sin pegar una clase o método duplicado. La versión incorpora el reto del año. Los nombres de ruta `java/...` y `resources/...` son relativos a `Biblioteca-resuelto/src/main/`.

**Archivo: `java/es/daw/bibliotecajdbc/service/AutorService.java`**

Comprueba la existencia, limpia el nombre y cuenta los libros antes del borrado en cascada.

```java
package es.daw.bibliotecajdbc.service;

import es.daw.bibliotecajdbc.exception.RecursoNoEncontradoException;
import es.daw.bibliotecajdbc.model.Autor;
import es.daw.bibliotecajdbc.repository.AutorRepository;
import es.daw.bibliotecajdbc.repository.LibroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AutorService {

    private final AutorRepository autorRepository;
    private final LibroRepository libroRepository;


    public List<Autor> listar() {
        return autorRepository.findAll();
    }

    public Autor buscarPorId(Integer id) {
        return autorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el autor indicado"));
    }

    public Autor crear(Autor autor) {
        autor.setNombre(autor.getNombre().trim());
        autorRepository.save(autor);
        return autor;
    }

    public Autor actualizar(Autor autor) {
        buscarPorId(autor.getId());
        autor.setNombre(autor.getNombre().trim());
        autorRepository.update(autor);
        return autor;
    }

    /**
     * Borra el autor y devuelve cuántos libros tenía.
     * Los libros NO se borran aquí: lo hace la BD por el ON DELETE CASCADE.
     * Por eso hay que contarlos antes: después ya no existen.
     */
    public int borrar(Integer id) {
        buscarPorId(id);
        int numLibros = libroRepository.countByAutorId(id);
        autorRepository.delete(id);
        return numLibros;
    }
}
```

**Archivo: `java/es/daw/bibliotecajdbc/service/LibroService.java`**

Comprueba ISBN y autor, filtra por criterios opcionales y recupera el id interno al editar o borrar.

```java
package es.daw.bibliotecajdbc.service;

import es.daw.bibliotecajdbc.exception.IsbnDuplicadoException;
import es.daw.bibliotecajdbc.exception.RecursoNoEncontradoException;
import es.daw.bibliotecajdbc.model.Autor;
import es.daw.bibliotecajdbc.model.Libro;
import es.daw.bibliotecajdbc.repository.AutorRepository;
import es.daw.bibliotecajdbc.repository.LibroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LibroService {

    private final LibroRepository libroRepository;
    private final AutorRepository autorRepository;



    /** Todos los filtros son opcionales. */
    /** Todos los filtros son opcionales: si llega null, ese filtro no se aplica. */
    public List<Libro> buscar(Integer autorId, Integer anioDesde, Integer anioHasta) {
        return libroRepository.findAll().stream()
                .filter(l -> autorId == null || l.getAutor().getId().equals(autorId))
                .filter(l -> anioDesde == null || l.getFechaPublicacion().getYear() >= anioDesde)
                .filter(l -> anioHasta == null || l.getFechaPublicacion().getYear() <= anioHasta)
                .toList();
    }

    public Libro buscarPorIsbn(String isbn) {
        return libroRepository.findByIsbn(isbn)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el libro con ISBN " + isbn));
    }

    public Libro crear(Libro libro) {
        // Regla de negocio: el ISBN no se puede repetir
        if (libroRepository.findByIsbn(libro.getIsbn()).isPresent()) {
            throw new IsbnDuplicadoException(libro.getIsbn());
        }
        prepararParaGuardar(libro);
        libroRepository.save(libro);
        return libro;
    }

    /**
     * El libro llega del mapper sin id, porque el DTO no lo lleva.
     * Lo localizamos por ISBN para recuperar su id, que es lo que usa el repositorio.
     */
    public Libro actualizar(Libro libro) {
        Libro existente = buscarPorIsbn(libro.getIsbn());
        libro.setId(existente.getId());
        prepararParaGuardar(libro);
        libroRepository.update(libro);
        return libro;
    }

    public void borrar(String isbn) {
        Libro libro = buscarPorIsbn(isbn);
        libroRepository.delete(libro.getId());
    }

    // Reglas comunes a crear y actualizar
    private void prepararParaGuardar(Libro libro) {
        libro.setTitulo(libro.getTitulo().trim());

        // Regla de negocio: el autor tiene que existir
        Integer autorId = libro.getAutor().getId();
        Autor autor = autorRepository.findById(autorId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el autor indicado"));
        libro.setAutor(autor);   // ahora lleva también el nombre
    }
}
```

### Hito 5. Plantillas comunes, portada y errores

`templates/fragments/layout.html` comparte fragmentos de la página, `index.html` presenta la portada y `error.html` muestra errores. `WebConfig` configura la portada. CSS y dependencias visuales están en `static/`; `messages.properties` reúne textos de validación. Las plantillas generan HTML; no ejecutan consultas SQL.

#### Código de referencia del hito 5

Los siguientes bloques son **archivos completos del proyecto resuelto**: si reconstruyes desde cero, crea el archivo indicado; si ya existe, compara o sustituye su contenido, sin pegar una clase o método duplicado. La versión incorpora el reto del año. Los nombres de ruta `java/...` y `resources/...` son relativos a `Biblioteca-resuelto/src/main/`.

**Archivo: `java/es/daw/bibliotecajdbc/config/WebConfig.java`**

Asocia la portada a su vista según la configuración de este proyecto.

```java
package es.daw.bibliotecajdbc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Vistas sin lógica: la portada no necesita controlador. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("index");
    }
}
```

**Archivo: `resources/templates/fragments/layout.html`**

Contiene fragmentos compartidos de estructura y navegación que las páginas reutilizan.

```html
<!DOCTYPE html>
<html lang="es" xmlns:th="http://www.thymeleaf.org">

<!-- Fragmento 1: <head> común. Recibe el título de la página -->
<head th:fragment="head(titulo)">
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title th:text="${titulo} + ' · Biblioteca'">Biblioteca</title>
  <!-- Ficheros locales: la aplicación funciona sin internet -->
  <link rel="stylesheet" th:href="@{/vendor/bootstrap/bootstrap.min.css}">
  <link rel="stylesheet" th:href="@{/vendor/bootstrap-icons/bootstrap-icons.min.css}">
  <link rel="stylesheet" th:href="@{/css/estilos.css}">
</head>

<body>

<!-- Fragmento 2: barra de navegación -->
<nav th:fragment="navbar" class="navbar navbar-expand navbar-dark bg-dark mb-4">
  <div class="container">
    <a class="navbar-brand fw-bold" th:href="@{/}"><i class="bi bi-book"></i> Biblioteca</a>
    <div class="navbar-nav">
      <a class="nav-link" th:href="@{/libros}">Libros</a>
      <a class="nav-link" th:href="@{/autores}">Autores</a>
    </div>
  </div>
</nav>

<!-- Fragmento 3: mensajes flash (éxito / error) tras un redirect -->
<div th:fragment="alertas">
  <div th:if="${mensaje}" class="alert alert-success alert-dismissible fade show">
    <i class="bi bi-check-circle"></i> <span th:text="${mensaje}">Mensaje</span>
    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
  </div>
  <div th:if="${error}" class="alert alert-danger alert-dismissible fade show">
    <i class="bi bi-exclamation-triangle"></i> <span th:text="${error}">Error</span>
    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
  </div>
</div>

<!-- Fragmento 4: scripts al final del body -->
<th:block th:fragment="scripts">
  <script th:src="@{/vendor/bootstrap/bootstrap.bundle.min.js}"></script>
</th:block>

</body>
</html>
```

**Archivo: `resources/templates/index.html`**

Es la portada que presenta las opciones.

```html
<!DOCTYPE html>
<html lang="es" xmlns:th="http://www.thymeleaf.org">
<head th:replace="~{fragments/layout :: head('Inicio')}"></head>
<body>
<nav th:replace="~{fragments/layout :: navbar}"></nav>

<div class="container hero text-center py-4">
    <h1 class="mt-3">Bienvenido a la Gestión de Biblioteca</h1>
    <p class="lead mb-4">Administra tus libros y autores fácilmente</p>

    <div class="row justify-content-center g-4">
        <div class="col-md-4">
            <div class="tarjeta bg-white p-4">
                <i class="bi bi-book-fill fs-1"></i>
                <h4 class="mt-2">Libros</h4>
                <a th:href="@{/libros}" class="btn btn-primary mt-2">Gestionar libros</a>
            </div>
        </div>
        <div class="col-md-4">
            <div class="tarjeta bg-white p-4">
                <i class="bi bi-person-fill fs-1"></i>
                <h4 class="mt-2">Autores</h4>
                <a th:href="@{/autores}" class="btn btn-primary mt-2">Gestionar autores</a>
            </div>
        </div>
    </div>
</div>

<th:block th:replace="~{fragments/layout :: scripts}"></th:block>
</body>
</html>
```

**Archivo: `resources/templates/error.html`**

Presenta al usuario los errores preparados por el manejador global.

```html
<!DOCTYPE html>
<html lang="es" xmlns:th="http://www.thymeleaf.org">
<head th:replace="~{fragments/layout :: head('Error')}"></head>
<body>
<nav th:replace="~{fragments/layout :: navbar}"></nav>

<div class="container text-center py-4">
  <i class="bi bi-exclamation-triangle-fill fs-1 text-danger"></i>
  <h2 class="mt-2" th:text="${titulo} ?: 'Error'">Error</h2>
  <p class="lead" th:text="${mensaje} ?: 'Se ha producido un error inesperado'">Mensaje</p>
  <a th:href="@{/}" class="btn btn-primary">Volver al inicio</a>
</div>

<th:block th:replace="~{fragments/layout :: scripts}"></th:block>
</body>
</html>
```

**Archivo: `resources/messages.properties`**

Centraliza los mensajes de validación y conversión; las claves deben coincidir con las anotaciones.

```properties
autor.nombre.obligatorio=El nombre es obligatorio
autor.nombre.tamanio=Máximo 100 caracteres

libro.isbn.formato=El ISBN debe tener 13 dígitos
libro.isbn.duplicado=Ya existe un libro con ese ISBN
libro.titulo.obligatorio=El título es obligatorio
libro.titulo.tamanio=Máximo 150 caracteres
libro.autor.obligatorio=Selecciona un autor
libro.fecha.obligatoria=La fecha es obligatoria
libro.fecha.futura=La fecha no puede ser futura

filtro.anio.rango=Debe estar entre 1000 y 2100
filtro.rango.invalido=No puede ser menor que Desde

typeMismatch.filtro.autorId=Autor no válido
typeMismatch.filtro.anioDesde=Debe ser un número
typeMismatch.filtro.anioHasta=Debe ser un número
typeMismatch.libroDto.fechaPublicacion=La fecha no es válida
autor.anio.obligatorio=El año de nacimiento es obligatorio
autor.anio.rango=El año debe estar entre 1000 y 2026
typeMismatch.autorDto.anioNacimiento=Debe ser un número entero
```

### Hito 6. CRUD de autores

`AutorController` usa `AutorDto` para el formulario, `AutorMapper` para convertir y `AutorService` para guardar o consultar. GET de alta prepara un DTO vacío; GET de edición busca por id y convierte a DTO; POST valida y crea o actualiza. En el listado se oculta el id como dato visible, aunque viaja en las rutas de edición y borrado. El formulario de borrado usa POST y pide confirmación porque la base borrará también los libros.

Comprueba: nombre vacío, alta, edición con valores precargados, borrado con cero libros y con varios; consulta la base para verificar la cascada.

#### Código de referencia del hito 6

Los siguientes bloques son **archivos completos del proyecto resuelto**: si reconstruyes desde cero, crea el archivo indicado; si ya existe, compara o sustituye su contenido, sin pegar una clase o método duplicado. La versión incorpora el reto del año. Los nombres de ruta `java/...` y `resources/...` son relativos a `Biblioteca-resuelto/src/main/`.

**Archivo: `java/es/daw/bibliotecajdbc/controller/AutorController.java`**

Gestiona el CRUD de autores; valida el DTO, usa el mapper y llama al servicio.

```java
package es.daw.bibliotecajdbc.controller;

import es.daw.bibliotecajdbc.dto.AutorDto;
import es.daw.bibliotecajdbc.mapper.AutorMapper;
import es.daw.bibliotecajdbc.model.Autor;
import es.daw.bibliotecajdbc.service.AutorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/autores")
@RequiredArgsConstructor
public class AutorController {

    private final AutorService autorService;
    private final AutorMapper autorMapper;

    @GetMapping
    public String listar(Model model) {
        List<AutorDto> autores = autorService.listar().stream()
                .map(autorMapper::toDto)
                .toList();
        model.addAttribute("autores", autores);
        return "autores/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("autorDto", new AutorDto());
        return "autores/formulario";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model) {
        Autor autor = autorService.buscarPorId(id);   // 404 si no existe
        model.addAttribute("autorDto", autorMapper.toDto(autor));
        return "autores/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute AutorDto autorDto,
                          BindingResult result,
                          RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "autores/formulario";
        }

        Autor autor = autorMapper.toModelEntity(autorDto);

        if (autorDto.getId() == null) {
            Autor creado = autorService.crear(autor);
            redirectAttributes.addFlashAttribute("mensaje",
                    "Autor " + creado.getNombre() + " creado");
        } else {
            Autor actualizado = autorService.actualizar(autor);
            redirectAttributes.addFlashAttribute("mensaje",
                    "Autor " + actualizado.getNombre() + " actualizado");
        }

        return "redirect:/autores";
    }

    @PostMapping("/borrar/{id}")
    public String borrar(@PathVariable Integer id,
                         RedirectAttributes redirectAttributes) {
        String nombre = autorService.buscarPorId(id).getNombre();
        int numLibros = autorService.borrar(id);

        String libros = numLibros == 1 ? "1 libro" : numLibros + " libros";
        redirectAttributes.addFlashAttribute("mensaje",
                "Autor " + nombre + " borrado junto con " + libros);

        return "redirect:/autores";
    }
}
```

**Archivo: `resources/templates/autores/formulario.html`**

Enlaza el DTO, muestra errores y permite crear o editar, incluyendo el año.

```html
<!DOCTYPE html>
<html lang="es" xmlns:th="http://www.thymeleaf.org">
<head th:replace="~{fragments/layout :: head('Autor')}"></head>
<body>
<nav th:replace="~{fragments/layout :: navbar}"></nav>

<div class="container" style="max-width: 640px;">

  <h2 class="titulo-seccion mb-4"
      th:text="${autorDto.id == null} ? 'Nuevo autor' : 'Editar autor'">
    Autor
  </h2>

  <form th:action="@{/autores/guardar}" th:object="${autorDto}" method="post"
        class="card card-body shadow-sm" novalidate>

    <!-- El id viaja oculto: vacío en alta, relleno en edición -->
    <input type="hidden" th:field="*{id}">

    <div class="mb-4">
      <label for="nombre" class="form-label">Nombre</label>
      <input type="text" th:field="*{nombre}" class="form-control"
             th:classappend="${#fields.hasErrors('nombre')} ? 'is-invalid'">
      <div class="invalid-feedback" th:errors="*{nombre}">Error</div>
    </div>

    <div class="mb-3">
      <label for="anioNacimiento" class="form-label">Año de nacimiento</label>
      <input type="text" th:field="*{anioNacimiento}" class="form-control" th:classappend="${#fields.hasErrors('anioNacimiento')} ? 'is-invalid'">
      <div class="invalid-feedback" th:errors="*{anioNacimiento}">Error</div>
    </div>
    <div class="d-flex gap-2">
      <button type="submit" class="btn btn-primary"><i class="bi bi-save"></i> Guardar</button>
      <a th:href="@{/autores}" class="btn btn-outline-secondary">Cancelar</a>
    </div>
  </form>
</div>

<th:block th:replace="~{fragments/layout :: scripts}"></th:block>
</body>
</html>
```

**Archivo: `resources/templates/autores/lista.html`**

Muestra autores y año; las acciones de edición y borrado usan el id necesario internamente.

```html
<!DOCTYPE html>
<html lang="es" xmlns:th="http://www.thymeleaf.org">
<head th:replace="~{fragments/layout :: head('Autores')}"></head>
<body>
<nav th:replace="~{fragments/layout :: navbar}"></nav>

<div class="container">

  <div class="d-flex justify-content-between align-items-center mb-3">
    <h2 class="titulo-seccion">✍️ Autores</h2>
    <a th:href="@{/autores/nuevo}" class="btn btn-success">
      <i class="bi bi-plus-lg"></i> Añadir autor
    </a>
  </div>

  <div th:replace="~{fragments/layout :: alertas}"></div>

  <table class="table table-bordered table-hover bg-white align-middle">
    <thead class="table-dark">
    <tr>
      <th>Nombre</th>
      <th>Nacimiento</th>
      <th class="col-accion">Editar</th>
      <th class="col-accion">Borrar</th>
    </tr>
    </thead>
    <tbody>
    <tr th:if="${#lists.isEmpty(autores)}">
      <td colspan="4" class="text-center text-muted">No hay autores</td>
    </tr>
    <tr th:each="a : ${autores}">
      <td th:text="${a.nombre}">Nombre</td>
      <td th:text="${a.anioNacimiento}">1927</td>
      <td>
        <a th:href="@{/autores/editar/{id}(id=${a.id})}"
           class="btn btn-warning btn-sm" title="Editar">
          <i class="bi bi-pencil"></i>
        </a>
      </td>
      <td>
        <form th:action="@{/autores/borrar/{id}(id=${a.id})}" method="post"
              onsubmit="return confirm('Se borrarán también todos sus libros. ¿Continuar?');">
          <button type="submit" class="btn btn-danger btn-sm" title="Borrar">
            <i class="bi bi-trash"></i>
          </button>
        </form>
      </td>
    </tr>
    </tbody>
  </table>
</div>

<th:block th:replace="~{fragments/layout :: scripts}"></th:block>
</body>
</html>
```

### Hito 7. CRUD de libros

El formulario usa `LibroDto`, validaciones y un desplegable de autores. ISBN es texto de 13 dígitos: tiene significado de identificador y puede empezar por cero. La fecha se envía con `input type="date"` y se valida como no futura. `edicion` distingue alta y edición; el ISBN aparece de solo lectura al editar. La lista de autores se prepara mediante `@ModelAttribute("autores")`, también cuando se vuelve por errores.

**Recorrido del alta:** formulario → `LibroDto` y `@Valid` → `LibroMapper.toModelEntity` → `LibroService.crear` comprueba ISBN y autor → `LibroRepository.save` → `LibroRepositoryJdbc` inserta → redirección al listado. Si el ISBN existe, el controlador captura `IsbnDuplicadoException`, usa `rejectValue` y vuelve al formulario con los datos conservados.

**Recorrido de edición:** URL con ISBN → servicio busca por ISBN → mapper rellena DTO → usuario modifica los campos permitidos → servicio vuelve a localizar el id interno → JDBC actualiza por id. `readonly` guía la pantalla, pero no constituye por sí solo una protección frente a peticiones manipuladas: la operación usa el ISBN enviado para localizar el libro.

#### Código de referencia del hito 7

Los siguientes bloques son **archivos completos del proyecto resuelto**: si reconstruyes desde cero, crea el archivo indicado; si ya existe, compara o sustituye su contenido, sin pegar una clase o método duplicado. La versión incorpora el reto del año. Los nombres de ruta `java/...` y `resources/...` son relativos a `Biblioteca-resuelto/src/main/`.

**Archivo: `java/es/daw/bibliotecajdbc/controller/LibroController.java`**

Controlador completo: incluye el CRUD de este hito y también el listado y filtro del hito 8; no lo pegues dos veces.

```java
package es.daw.bibliotecajdbc.controller;

import es.daw.bibliotecajdbc.dto.AutorDto;
import es.daw.bibliotecajdbc.dto.FiltroLibrosDto;
import es.daw.bibliotecajdbc.dto.LibroDto;
import es.daw.bibliotecajdbc.exception.IsbnDuplicadoException;
import es.daw.bibliotecajdbc.mapper.AutorMapper;
import es.daw.bibliotecajdbc.mapper.LibroMapper;
import es.daw.bibliotecajdbc.model.Libro;
import es.daw.bibliotecajdbc.service.AutorService;
import es.daw.bibliotecajdbc.service.LibroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/libros")
@RequiredArgsConstructor
public class LibroController {

    private final LibroService libroService;
    private final AutorService autorService;
    private final LibroMapper libroMapper;
    private final AutorMapper autorMapper;

    /** Disponible en todas las vistas de este controlador, también al volver con errores. */
    @ModelAttribute("autores")
    public List<AutorDto> autores() {
        return autorService.listar().stream()
                .map(autorMapper::toDto)
                .toList();
    }

    // READ: listado con filtros opcionales
    @GetMapping
    public String listar(@Valid @ModelAttribute("filtro") FiltroLibrosDto filtro,
                         BindingResult result,
                         Model model) {

        Integer desde = filtro.getAnioDesde();
        Integer hasta = filtro.getAnioHasta();
        if (desde != null && hasta != null && hasta < desde) {
            result.rejectValue("anioHasta", "filtro.rango.invalido");
        }

        // Con errores no se añade "libros": la vista no muestra la tabla
        if (!result.hasErrors()) {
            List<LibroDto> libros = libroService.buscar(filtro.getAutorId(), desde, hasta).stream()
                    .map(libroMapper::toDto)
                    .toList();
            model.addAttribute("libros", libros);
        }

        return "libros/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("libroDto", new LibroDto());
        return "libros/formulario";
    }

    @GetMapping("/editar/{isbn}")
    public String editar(@PathVariable String isbn, Model model) {
        Libro libro = libroService.buscarPorIsbn(isbn);   // 404 si no existe
        model.addAttribute("libroDto", libroMapper.toDto(libro));
        return "libros/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute LibroDto libroDto,
                          BindingResult result,
                          RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "libros/formulario";
        }

        Libro libro = libroMapper.toModelEntity(libroDto);

        if (libroDto.isEdicion()) {
            Libro actualizado = libroService.actualizar(libro);
            redirectAttributes.addFlashAttribute("mensaje",
                    "Libro " + actualizado.getTitulo() + " actualizado");
        } else {
            try {
                Libro creado = libroService.crear(libro);
                redirectAttributes.addFlashAttribute("mensaje",
                        "Libro " + creado.getTitulo() + " creado");
            } catch (IsbnDuplicadoException e) {
                // Regla de negocio: se muestra junto al campo, sin perder los datos
                result.rejectValue("isbn", "libro.isbn.duplicado");
                return "libros/formulario";
            }
        }

        return "redirect:/libros";
    }

    @PostMapping("/borrar/{isbn}")
    public String borrar(@PathVariable String isbn,
                         RedirectAttributes redirectAttributes) {
        libroService.borrar(isbn);
        redirectAttributes.addFlashAttribute("mensaje", "Libro con ISBN " + isbn + " borrado");
        return "redirect:/libros";
    }
}
```

**Archivo: `resources/templates/libros/formulario.html`**

Formulario compartido de alta y edición; enlaza ISBN, título, autor y fecha, y muestra los errores.

```html
<!DOCTYPE html>
<html lang="es" xmlns:th="http://www.thymeleaf.org">
<head th:replace="~{fragments/layout :: head('Libro')}"></head>
<body>
<nav th:replace="~{fragments/layout :: navbar}"></nav>

<div class="container" style="max-width: 640px;">

  <h2 class="titulo-seccion mb-4"
      th:text="${libroDto.edicion} ? 'Editar libro' : 'Nuevo libro'">
    Libro
  </h2>

  <form th:action="@{/libros/guardar}" th:object="${libroDto}" method="post"
        class="card card-body shadow-sm" novalidate>

    <!-- false = alta, true = edición -->
    <input type="hidden" th:field="*{edicion}">

    <div class="mb-3">
      <label for="isbn" class="form-label">ISBN</label>
      <!-- readonly y no disabled: un campo disabled no se envía -->
      <input type="text" th:field="*{isbn}" th:readonly="*{edicion}" class="form-control"
             th:classappend="${#fields.hasErrors('isbn')} ? 'is-invalid'">
      <div class="invalid-feedback" th:errors="*{isbn}">Error</div>
    </div>

    <div class="mb-3">
      <label for="titulo" class="form-label">Título</label>
      <input type="text" th:field="*{titulo}" class="form-control"
             th:classappend="${#fields.hasErrors('titulo')} ? 'is-invalid'">
      <div class="invalid-feedback" th:errors="*{titulo}">Error</div>
    </div>

    <div class="mb-3">
      <label for="autorId" class="form-label">Autor</label>
      <!-- Se envía el id del autor; se muestra su nombre -->
      <select th:field="*{autorId}" class="form-select"
              th:classappend="${#fields.hasErrors('autorId')} ? 'is-invalid'">
        <option value="">-- Selecciona un autor --</option>
        <option th:each="a : ${autores}"
                th:value="${a.id}"
                th:text="${a.nombre}">Autor</option>
      </select>
      <div class="invalid-feedback" th:errors="*{autorId}">Error</div>
    </div>

    <div class="mb-4">
      <label for="fechaPublicacion" class="form-label">Fecha de publicación</label>
      <input type="date" th:field="*{fechaPublicacion}" class="form-control"
             th:classappend="${#fields.hasErrors('fechaPublicacion')} ? 'is-invalid'">
      <div class="invalid-feedback" th:errors="*{fechaPublicacion}">Error</div>
    </div>

    <div class="d-flex gap-2">
      <button type="submit" class="btn btn-primary"><i class="bi bi-save"></i> Guardar</button>
      <a th:href="@{/libros}" class="btn btn-outline-secondary">Cancelar</a>
    </div>
  </form>
</div>

<th:block th:replace="~{fragments/layout :: scripts}"></th:block>
</body>
</html>
```

### Hito 8. Filtro de libros

`GET /libros` recibe `FiltroLibrosDto` con autor, año desde y año hasta opcionales. `@Valid` comprueba los límites individuales; el controlador compara ambos y usa `rejectValue` si hasta es menor que desde. Si hay errores, no añade `libros` al modelo y la plantilla no muestra la tabla. Si son válidos, el servicio aplica filtros con streams, el controlador convierte los resultados a DTO y la plantilla muestra tabla, cantidad o mensaje vacío. El enlace Limpiar vuelve a `/libros`.

Prueba sin filtros; solo autor; solo un extremo; intervalo 1960–1970; combinación sin coincidencias; intervalo invertido; texto no numérico y Limpiar. Los extremos son inclusivos. Los inputs de año son texto para que una entrada no numérica llegue al servidor y produzca un error de conversión visible.

#### Código de referencia del hito 8

Los siguientes bloques son **archivos completos del proyecto resuelto**: si reconstruyes desde cero, crea el archivo indicado; si ya existe, compara o sustituye su contenido, sin pegar una clase o método duplicado. La versión incorpora el reto del año. Los nombres de ruta `java/...` y `resources/...` son relativos a `Biblioteca-resuelto/src/main/`.

**Archivo: `resources/templates/libros/lista.html`**

Contiene filtro GET y resultados. El controlador y servicio completos ya se mostraron en los hitos 7 y 4; reutilízalos.

```html
<!DOCTYPE html>
<html lang="es" xmlns:th="http://www.thymeleaf.org">
<head th:replace="~{fragments/layout :: head('Libros')}"></head>
<body>
<nav th:replace="~{fragments/layout :: navbar}"></nav>

<div class="container">

    <div class="d-flex justify-content-between align-items-center mb-3">
        <h2 class="titulo-seccion">📚 Libros</h2>
        <a th:href="@{/libros/nuevo}" class="btn btn-success">
            <i class="bi bi-plus-lg"></i> Añadir libro
        </a>
    </div>

    <div th:replace="~{fragments/layout :: alertas}"></div>

    <!-- Por GET: los filtros quedan en la URL y se conservan tras buscar -->
    <form th:action="@{/libros}" th:object="${filtro}" method="get"
          class="card card-body shadow-sm mb-3" novalidate>
        <div class="row g-2 align-items-start">
            <div class="col-md-4">
                <label for="autorId" class="form-label">Autor</label>
                <select th:field="*{autorId}" class="form-select"
                        th:classappend="${#fields.hasErrors('autorId')} ? 'is-invalid'">
                    <option value="">Todos los autores</option>
                    <option th:each="a : ${autores}"
                            th:value="${a.id}"
                            th:text="${a.nombre}">Autor</option>
                </select>
                <div class="invalid-feedback" th:errors="*{autorId}">Error</div>
            </div>
            <div class="col-md-2">
                <label for="anioDesde" class="form-label">Desde</label>
                <input type="text" inputmode="numeric" th:field="*{anioDesde}" class="form-control"
                       th:classappend="${#fields.hasErrors('anioDesde')} ? 'is-invalid'">
                <div class="invalid-feedback" th:errors="*{anioDesde}">Error</div>
            </div>
            <div class="col-md-2">
                <label for="anioHasta" class="form-label">Hasta</label>
                <input type="text" inputmode="numeric" th:field="*{anioHasta}" class="form-control"
                       th:classappend="${#fields.hasErrors('anioHasta')} ? 'is-invalid'">
                <div class="invalid-feedback" th:errors="*{anioHasta}">Error</div>
            </div>
            <div class="col-md-4 d-flex gap-2 text-nowrap" style="padding-top: 2rem;">
                <button type="submit" class="btn btn-primary">
                    <i class="bi bi-funnel"></i> Filtrar
                </button>
                <a th:href="@{/libros}" class="btn btn-outline-secondary">Limpiar</a>
            </div>
        </div>
    </form>

    <!-- Si hay errores en el filtro, "libros" no está en el modelo -->
    <th:block th:if="${libros != null}">

        <p th:if="${#lists.isEmpty(libros)}" class="alert alert-info">
            No hay libros con esos filtros
        </p>

        <th:block th:unless="${#lists.isEmpty(libros)}">
            <p class="text-muted" th:text="|${#lists.size(libros)} libros encontrados|">0 libros</p>

            <table class="table table-bordered table-hover bg-white align-middle">
                <thead class="table-dark">
                <tr>
                    <th>ISBN</th>
                    <th>Título</th>
                    <th>Autor</th>
                    <th>Publicación</th>
                    <th class="col-accion">Editar</th>
                    <th class="col-accion">Borrar</th>
                </tr>
                </thead>
                <tbody>
                <tr th:each="l : ${libros}">
                    <td th:text="${l.isbn}">ISBN</td>
                    <td th:text="${l.titulo}">Título</td>
                    <td th:text="${l.autorNombre}">Autor</td>
                    <td th:text="${#temporals.format(l.fechaPublicacion, 'dd/MM/yyyy')}">01/01/2000</td>
                    <td>
                        <a th:href="@{/libros/editar/{isbn}(isbn=${l.isbn})}"
                           class="btn btn-warning btn-sm" title="Editar">
                            <i class="bi bi-pencil"></i>
                        </a>
                    </td>
                    <td>
                        <form th:action="@{/libros/borrar/{isbn}(isbn=${l.isbn})}" method="post"
                              onsubmit="return confirm('¿Seguro que quieres borrar este libro?');">
                            <button type="submit" class="btn btn-danger btn-sm" title="Borrar">
                                <i class="bi bi-trash"></i>
                            </button>
                        </form>
                    </td>
                </tr>
                </tbody>
            </table>
        </th:block>
    </th:block>
</div>

<th:block th:replace="~{fragments/layout :: scripts}"></th:block>
</body>
</html>
```

## Resumen: qué existe y qué añade el reto

| Parte | Biblioteca-original | Biblioteca-resuelto |
|---|---|---|
| Hitos 0–8 | Aplicación base ya construida en gran medida; estudiar y comprobar | También presentes |
| Año de nacimiento | Autor sin el nuevo campo | Campo incorporado en esquema, DTO, modelo, mapper, repositorios y vistas |

Para verificar un hito, abre el archivo indicado y ejecuta la prueba descrita. No se deduce que falte código únicamente porque el enunciado proponga construirlo desde cero.

## Ejecución y comprobaciones

Abre una carpeta de proyecto en IntelliJ como Maven y ejecuta `ProductoJdbcApplication` o `BibliotecaJdbcApplication`. En PowerShell, dentro de la carpeta correspondiente: `./mvnw.cmd spring-boot:run`. Abre `http://localhost:8080/`. Ambas usan el puerto 8080: ejecútalas una cada vez. La primera descarga de Maven y dependencias puede necesitar conexión; después quedan en la caché local. La consola H2 está en `/h2-console`; usa la URL de `application.properties`, usuario `sa` y contraseña vacía. Al detener la aplicación, H2 en memoria pierde los cambios.

Utiliza las comprobaciones de cada reto para practicar y verificar también los casos vacíos y de error.

Fuentes de clase: [Productos](https://github.com/profeMelola/DWES-02-2026-27/blob/main/ejercicios/ejercicio_productos_spring_jdbc.md) y [Biblioteca](https://github.com/profeMelola/DWES-02-2026-27/blob/main/ejercicios/ejercicio_biblioteca_spring_jdbc.md).

