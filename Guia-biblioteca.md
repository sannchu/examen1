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

En `resources/schema.sql`, dentro de la tabla `autor`, añade `anio_nacimiento INT NOT NULL`; no olvides la coma después de la columna anterior. En `data.sql` añade la columna y el año en cada INSERT de autores:



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

### Hito 1. Tablas y datos

`schema.sql` elimina primero la tabla hija `libro` y luego `autor`; las crea en orden inverso. `libro.autor_id` es la FK. `isbn` tiene restricción `UNIQUE` y la FK incluye `ON DELETE CASCADE`. `data.sql` inserta primero autores y después libros, incluidos ejemplos con diferentes fechas y un autor sin libros. Con el reto del año, los INSERT de autores también necesitan ese dato.

**Por qué cascada:** al ejecutar el borrado de un autor, H2 elimina sus libros asociados. En Productos la regla es diferente: el servicio impide borrar un fabricante con productos. Una misma relación 1:N puede tener reglas distintas.

### Hito 2. Modelo, DTO, mapper, interfaces y excepciones

1. `Autor` guarda id y nombre; `Libro`, id, ISBN, título, autor y fecha. En el resuelto `Autor` añade el año. `LocalDate` representa la fecha en Java.
2. `LibroDto` recibe ISBN, título, `autorId`, fecha y `edicion`; el id interno del libro no viaja en el formulario. Las anotaciones del DTO validan los campos. `FiltroLibrosDto` contiene los criterios opcionales.
3. `LibroMapper.toModelEntity` convierte el DTO a libro con id `null` y un autor construido solo con su id. El servicio completará el autor; el mapper no consulta la base. `toDto` prepara los datos para editar o listar.
4. `GenericRepository<T, ID>` declara CRUD. `LibroRepository` hereda esos métodos y declara también `Optional<Libro> findByIsbn(String isbn)` y `int countByAutorId(Integer autorId)`. `AutorRepository` hereda su CRUD.
5. `AccesoDatosException` comunica problemas de acceso; `RecursoNoEncontradoException` señala un recurso ausente; `IsbnDuplicadoException` permite mostrar un error junto al ISBN. No se necesita una excepción de autor con libros porque aquí su borrado está permitido y es en cascada.

**Contrato frente a implementación:** `LibroRepository` declara las firmas para que `LibroService` pueda invocarlas; `LibroRepositoryJdbc implements LibroRepository` contiene las consultas. Una firma en la interfaz y un cuerpo en JDBC son dos partes de la misma operación. La guía común desarrolla esta distinción.

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

### Hito 4. Servicios y reglas

`LibroService.buscar` obtiene `findAll()` y filtra en Java. Un criterio nulo deja pasar todos los libros; `>=` y `<=` incluyen los años extremos. Por ejemplo, 1960–1970 incluye ambos. El orden viene del SQL de `findAll`, que el stream conserva.

```java
.filter(l -> anioDesde == null || l.getFechaPublicacion().getYear() >= anioDesde)
```

Se lee: si no hay límite inferior, acepta el libro; si lo hay, exige que su año sea igual o mayor. Los demás filtros siguen el mismo patrón.

`crear` comprueba que el ISBN no exista, recorta el título y comprueba que el autor exista. `actualizar` y `borrar` reciben ISBN, buscan el libro y usan su id interno para la operación JDBC. El `UPDATE` no cambia el ISBN. `AutorService` recorta nombres y comprueba existencia al editar o borrar. Para borrar un autor cuenta sus libros **antes** de eliminarlo y devuelve esa cantidad; la base de datos realiza la cascada.

### Hito 5. Plantillas comunes, portada y errores

`templates/fragments/layout.html` comparte fragmentos de la página, `index.html` presenta la portada y `error.html` muestra errores. `WebConfig` configura la portada. CSS y dependencias visuales están en `static/`; `messages.properties` reúne textos de validación. Las plantillas generan HTML; no ejecutan consultas SQL.

### Hito 6. CRUD de autores

`AutorController` usa `AutorDto` para el formulario, `AutorMapper` para convertir y `AutorService` para guardar o consultar. GET de alta prepara un DTO vacío; GET de edición busca por id y convierte a DTO; POST valida y crea o actualiza. En el listado se oculta el id como dato visible, aunque viaja en las rutas de edición y borrado. El formulario de borrado usa POST y pide confirmación porque la base borrará también los libros.

Comprueba: nombre vacío, alta, edición con valores precargados, borrado con cero libros y con varios; consulta la base para verificar la cascada.

### Hito 7. CRUD de libros

El formulario usa `LibroDto`, validaciones y un desplegable de autores. ISBN es texto de 13 dígitos: tiene significado de identificador y puede empezar por cero. La fecha se envía con `input type="date"` y se valida como no futura. `edicion` distingue alta y edición; el ISBN aparece de solo lectura al editar. La lista de autores se prepara mediante `@ModelAttribute("autores")`, también cuando se vuelve por errores.

**Recorrido del alta:** formulario → `LibroDto` y `@Valid` → `LibroMapper.toModelEntity` → `LibroService.crear` comprueba ISBN y autor → `LibroRepository.save` → `LibroRepositoryJdbc` inserta → redirección al listado. Si el ISBN existe, el controlador captura `IsbnDuplicadoException`, usa `rejectValue` y vuelve al formulario con los datos conservados.

**Recorrido de edición:** URL con ISBN → servicio busca por ISBN → mapper rellena DTO → usuario modifica los campos permitidos → servicio vuelve a localizar el id interno → JDBC actualiza por id. `readonly` guía la pantalla, pero no constituye por sí solo una protección frente a peticiones manipuladas: la operación usa el ISBN enviado para localizar el libro.

### Hito 8. Filtro de libros

`GET /libros` recibe `FiltroLibrosDto` con autor, año desde y año hasta opcionales. `@Valid` comprueba los límites individuales; el controlador compara ambos y usa `rejectValue` si hasta es menor que desde. Si hay errores, no añade `libros` al modelo y la plantilla no muestra la tabla. Si son válidos, el servicio aplica filtros con streams, el controlador convierte los resultados a DTO y la plantilla muestra tabla, cantidad o mensaje vacío. El enlace Limpiar vuelve a `/libros`.

Prueba sin filtros; solo autor; solo un extremo; intervalo 1960–1970; combinación sin coincidencias; intervalo invertido; texto no numérico y Limpiar. Los extremos son inclusivos. Los inputs de año son texto para que una entrada no numérica llegue al servidor y produzca un error de conversión visible.

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

