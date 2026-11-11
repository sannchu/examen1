# Cuaderno detallado de estudio: Spring, JDBC, Productos y Biblioteca

Este documento está pensado para trabajar con las carpetas **original** y **resuelto** abiertas una al lado de la otra. El original es el punto de partida real de los ZIP de clase: contiene ya bastantes clases; no es un proyecto en blanco. Los ejemplos de código proceden de la solución y se indican con su ruta. Cuando se muestra un método, se sustituye el método existente del mismo nombre; no se pega otro igual dentro de la misma clase. Cuando se muestra un campo o línea, se añade en el lugar indicado. Los imports necesarios se pueden completar con Alt+Enter en IntelliJ. Cada bloque omite lo que no se está modificando salvo que se indique lo contrario.

## 1. Antes de programar: leer y orientarse

En el examen escribe en un papel: qué dato u operación se pide, qué restricciones hay, dónde se guarda y cómo se comprueba. Si piden un nuevo campo, dibuja este recorrido:

`formulario → DTO → modelo → servicio → repositorio → tabla`  
`tabla → SELECT → ResultSet → modelo → DTO → formulario/listado`

Si alguno de los dos caminos está incompleto, el dato puede verse pero no guardarse, o guardarse y desaparecer al recargar.

### 1.1. Directorios que encontrarás en ambos proyectos

```text
proyecto/
├── pom.xml                    Dependencias y configuración Maven
├── mvnw.cmd y .mvn/           Maven Wrapper para Windows
├── src/main/java/es/daw/.../  Código Java de la aplicación
│   ├── *Application.java      Punto de entrada de Spring Boot
│   ├── config/                Configuración web
│   ├── controller/            URLs, peticiones, validación y vistas
│   ├── dto/                   Datos para formularios/listados
│   ├── exception/             Excepciones y tratamiento de errores
│   ├── mapper/                Conversión DTO ↔ modelo (Biblioteca)
│   ├── model/                 Objetos que representan el dominio
│   ├── repository/            Contratos e implementaciones JDBC
│   └── service/               Reglas de negocio y coordinación
├── src/main/resources/
│   ├── application.properties Configuración de Spring y conexión H2
│   ├── schema.sql             Creación de tablas y relaciones
│   ├── data.sql               Datos de ejemplo al arrancar
│   ├── messages.properties   Mensajes de validación (Biblioteca)
│   ├── templates/             Páginas Thymeleaf
│   └── static/                CSS y recursos estáticos
└── src/test/                  Pruebas, cuando el proyecto las incluye
```

La ruta Java concreta de Productos empieza por `src/main/java/es/daw/productojdbc/`; la de Biblioteca por `src/main/java/es/daw/bibliotecajdbc/`. Para abreviar, a partir de ahora las rutas como `repository/ProductoRepositoryJdbc.java` son relativas al paquete Java correspondiente. Las rutas `resources/...` son relativas a `src/main/`.

`pom.xml` es el punto de partida para saber con qué herramientas trabaja la aplicación: Spring MVC atiende HTTP, Thymeleaf genera HTML, Validation valida formularios, JDBC conecta con H2 y Lombok genera código repetitivo como getters, setters y constructores. Ambos ZIP configuran Java 21. `@Data` de Lombok genera accesores; `@NoArgsConstructor` crea constructor vacío; `@AllArgsConstructor` crea un constructor con todos los campos, **en el orden declarado**. Si añades un campo, cambia ese constructor y hay que revisar sus usos.

`application.properties` configura la URL, el usuario y la contraseña de H2. Spring ejecuta primero `schema.sql` y luego `data.sql`. H2 está en memoria: al detener la aplicación se pierden los cambios. Una clave primaria (PK) identifica una fila; una clave ajena (FK) apunta a la PK de otra tabla. El lado muchos almacena la FK. Crear primero la tabla padre y después la hija; al eliminar tablas, el orden es inverso.

### 1.2. Responsabilidades: quién hace cada cosa

- **Vista:** enseña HTML y envía los campos. `th:field` enlaza un campo del formulario con una propiedad Java; `th:errors` muestra su error.
- **Controlador:** recibe una URL, usa `@Valid` y `BindingResult`, pide operaciones al servicio y devuelve una vista o redirección. `Model` lleva datos a Thymeleaf.
- **Servicio:** comprueba reglas del negocio: por ejemplo, si el fabricante existe o si puede borrarse. No debería contener SQL ni construir HTML.
- **Interfaz de repositorio:** explica qué operaciones hay sin obligar al servicio a conocer JDBC. `GenericRepository<T, ID>` reúne el CRUD común.
- **Repositorio JDBC:** implementa el contrato, ejecuta SQL y convierte filas a objetos. Captura `SQLException` y la envuelve en `AccesoDatosException`.
- **Modelo:** representa los datos del dominio. Un producto tiene un objeto `Fabricante`, no solo el número de la FK.
- **DTO y mapper:** un DTO representa los datos que necesita la vista. El mapper convierte DTO y modelo. Productos hace esa conversión en `ProductoForm`; Biblioteca tiene clases mapper separadas.

`@Controller`, `@Service`, `@Repository` y `@Component` hacen que Spring detecte esas clases. La inyección por constructor proporciona sus dependencias. Si ves `@RequiredArgsConstructor`, Lombok genera un constructor para los campos `final`; Spring lo utiliza.

### 1.3. Recorrido de una petición real del proyecto original de Productos

Abre `controller/ProductoController.java` y busca `listar`. Este es el método real:



```java
public String listar(Model model) {
        model.addAttribute("productos", productoService.listar());
        return "productos/lista";
    }
```


`@GetMapping` que aparece justo encima lo asocia a GET `/productos` porque la clase tiene `@RequestMapping("/productos")`. El servicio llama a `productoRepository.findAll()`. La implementación JDBC ejecuta un SELECT con JOIN y convierte cada fila con `mapRow`. `model.addAttribute` hace disponible la lista bajo el nombre `productos`; la cadena `productos/lista` selecciona `resources/templates/productos/lista.html`. Allí `th:each="p : ${productos}"` repite una fila por producto.

Al guardar, el controlador recibe `ProductoForm` y su `BindingResult`. Si hay errores vuelve al formulario y carga otra vez la lista de fabricantes para el desplegable. Si es válido, convierte a `Producto`, llama a `crear` o `actualizar` y redirige al listado. Redirigir después de POST evita reenviar el formulario al pulsar F5.

En `repository/ProductoRepositoryJdbc.java`, `SELECT_BASE` une tablas. Los alias `f_codigo` y `f_nombre` distinguen columnas del fabricante de las del producto. La mecánica JDBC es: obtener `Connection` del `DataSource`; preparar SQL con `?`; asignar parámetros empezando en 1; usar `executeQuery` para SELECT o `executeUpdate` para modificaciones; recorrer el `ResultSet`; cerrar recursos con `try-with-resources`. Nunca concatenes valores escritos por el usuario en el SQL.

### 1.4. Archivos principales de Productos antes de los retos

- `model/Producto.java` y `Fabricante.java`: objetos del dominio. En el original, producto tiene código, nombre, precio y fabricante; todavía no tiene stock.
- `dto/ProductoForm.java`: datos y validaciones del formulario; convierte tanto hacia el modelo como desde él. El desplegable aporta `codigoFabricante`, no un objeto completo. `FabricanteResumen.java` ya existe en el ZIP, preparado para el reto del contador.
- `repository/GenericRepository.java`: contrato `save`, `findById`, `findAll`, `update`, `delete`; las interfaces específicas fijan tipos y añaden consultas propias. Las clases `...Jdbc` contienen el SQL.
- `service/ProductoService.java`: busca, crea, modifica y borra, comprueba que el fabricante exista y elimina espacios del nombre. `FabricanteService.java` impide borrar un fabricante con productos.
- `controller/ProductoController.java` y `FabricanteController.java`: coordinan páginas y formularios. `exception/GlobalExceptionHandler.java` centraliza errores; las excepciones separan errores técnicos de reglas de negocio.
- `resources/templates/productos/` y `fabricantes/`: cada una tiene listado y formulario. `fragments/layout.html` reúne partes comunes; `index.html` es la portada.

### 1.5. Archivos principales de Biblioteca antes del reto

- `model/Autor.java` y `Libro.java`: `Libro` contiene ISBN, título, fecha `LocalDate` y objeto `Autor`. El `id` del libro es técnico; ISBN identifica el libro ante el usuario.
- `dto/AutorDto.java`, `LibroDto.java` y `FiltroLibrosDto.java`: datos de la vista y sus validaciones. `mapper/AutorMapper.java` y `LibroMapper.java` los convierten al modelo y viceversa.
- `repository/AutorRepositoryJdbc.java` y `LibroRepositoryJdbc.java`: CRUD y consultas manuales. `LibroRepositoryJdbc` hace JOIN con autor y convierte `DATE` mediante `getObject(..., LocalDate.class)` y `setObject`.
- `service/LibroService.java`: comprueba ISBN duplicado, busca el autor y aplica filtros opcionales sobre `findAll()`. `AutorService.java` cuenta libros antes de borrar el autor.
- `controller/AutorController.java` y `LibroController.java`: reciben peticiones y coordinan DTO, mappers y servicios. El ISBN viaja en la URL para libros; el id del autor viaja cuando hace falta, pero no aparece como dato en la tabla.
- `resources/schema.sql`: `libro.autor_id` referencia a `autor.id` con `ON DELETE CASCADE`; cuando se borra un autor, la base borra sus libros. Esto contrasta con Productos, donde se prohíbe borrar un fabricante con productos.
- `resources/messages.properties`: textos de validación. `resources/templates/autores/` y `libros/`: formularios y listas; en libros también está el formulario de filtros.

## 2. Productos: reto 4, añadir stock

El enunciado indica empezar por este reto. Se necesita un entero obligatorio, que admita cero. Se modifica tanto el camino de guardar como el de leer. Las rutas siguientes son dentro de `Productos-resuelto`.

### Paso 1. Tabla y datos iniciales

En `src/main/resources/schema.sql`, dentro de `CREATE TABLE producto`, añade esta columna junto a `precio`:



```sql
stock INT NOT NULL,
```


`NOT NULL` obliga a que toda fila tenga valor. Por eso hay que cambiar cada INSERT de productos en `data.sql`, indicando columna y valor. Un ejemplo real:



```sql
INSERT INTO producto (nombre, precio, stock, codigo_fabricante) VALUES ('Disco duro SATA3 1TB', 86.99, 25, 5);
```


Las cuatro columnas y los cuatro valores se corresponden en orden. Consulta el archivo completo para ver los once INSERT. Si falta el valor de stock en uno, falla la inicialización.

### Paso 2. Modelo y DTO

En `model/Producto.java`, añade `private int stock;` después de `precio`. Comprueba el orden de campos: el constructor completo generado por Lombok pasa a recibir `(codigo, nombre, precio, stock, fabricante)`.

En `dto/ProductoForm.java` añade, antes de `codigoFabricante`:



```java
@NotNull(message = "El stock es obligatorio")
@jakarta.validation.constraints.PositiveOrZero(message = "El stock no puede ser negativo")
private Integer stock;
```


Usamos `Integer` en el formulario: `null` permite detectar un campo sin rellenar; `0` es una cantidad válida. `@NotNull` y `@PositiveOrZero` tienen responsabilidades distintas. Dentro de `toDto`, añade `form.setStock(p.getStock());`; en `toModelEntity` usa:



```java
return new Producto(codigo, nombre, precio, stock, fabricante);
```


Esto permite que al editar se rellene el formulario y que al guardar el valor llegue al modelo. La conversión solo se ejecuta tras superar la validación del controlador.

### Paso 3. Lectura JDBC

En `repository/ProductoRepositoryJdbc.java`, el SELECT común queda así:



```java
private static final String SELECT_BASE = """
            SELECT p.codigo, p.nombre, p.precio, p.stock,
                   f.codigo AS f_codigo, f.nombre AS f_nombre
            FROM producto p
            JOIN fabricante f ON p.codigo_fabricante = f.codigo
            """;
```


Todos los métodos que reutilizan `SELECT_BASE` reciben `p.stock`. En el método `mapRow`, añade la lectura antes del fabricante, respetando el orden del constructor:



```java
private Producto mapRow(ResultSet rs) throws SQLException {
        Fabricante fabricante = new Fabricante(
                rs.getInt("f_codigo"),
                rs.getString("f_nombre"));

        return new Producto(
                rs.getInt("codigo"),
                rs.getString("nombre"),
                rs.getDouble("precio"),
                rs.getInt("stock"),
                fabricante);
    }
```


`rs.getInt("stock")` lee por nombre de columna. Sin esa línea el valor nunca llegaría a Java aunque estuviera en la base.

### Paso 4. Escritura JDBC

En `save`, sustituye el SQL y la asignación de parámetros por estas líneas, conservando el `try` y la recuperación de claves generadas del original:



```java
String sql = "INSERT INTO producto (nombre, precio, stock, codigo_fabricante) VALUES (?, ?, ?, ?)";
// Dentro del try, después de crear ps:
ps.setString(1, producto.getNombre());
ps.setDouble(2, producto.getPrecio());
ps.setInt(3, producto.getStock());
ps.setInt(4, producto.getFabricante().getCodigo());
ps.executeUpdate();
```


Para edición, el método completo de la solución es:



```java
public void update(Producto producto) {
        String sql = "UPDATE producto SET nombre = ?, precio = ?, stock = ?, codigo_fabricante = ? WHERE codigo = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, producto.getNombre());
            ps.setDouble(2, producto.getPrecio());
            ps.setInt(3, producto.getStock());
            ps.setInt(4, producto.getFabricante().getCodigo());
            ps.setInt(5, producto.getCodigo());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al actualizar el producto " + producto.getCodigo(), e);
        }
    }
```


Lee los `?` de izquierda a derecha: 1 nombre, 2 precio, 3 stock, 4 FK, 5 código del producto. El 5 identifica qué fila modificar; olvidarlo o desplazarlo puede producir errores o escribir valores en columnas equivocadas. El DELETE no necesita stock.

### Paso 5. Formulario y listado

En `resources/templates/productos/formulario.html`, dentro del `<form>` y antes del desplegable de fabricante, añade:



```html
<div class="mb-3">
  <label for="stock" class="form-label">Stock</label>
  <input type="number" min="0" th:field="*{stock}" class="form-control"
         th:classappend="${#fields.hasErrors('stock')} ? 'is-invalid'">
  <div class="invalid-feedback" th:errors="*{stock}">Error</div>
</div>
```


`th:field` enlaza con `ProductoForm.stock`; `th:errors` muestra el mensaje que produce la validación en servidor. `min="0"` ayuda en navegador, pero no reemplaza la validación Java. En `productos/lista.html`, agrega `<th>Stock</th>` a la cabecera y `<td th:text="${p.stock}">0</td>` a cada fila en la misma posición; aumenta `colspan` de 6 a 7.

Comprueba alta con 0 y con 7, vacío y -1, y edición: al volver a abrir debe mantener el valor. Si se guarda pero no aparece, revisa SELECT y `mapRow`; si se ve al editar pero no cambia en BD, revisa UPDATE e índices.

## 3. Productos: reto 1, contar productos por fabricante

### Paso 1. Entender el resultado

Hay que mostrar fabricantes con y sin productos. El ZIP ya trae `dto/FabricanteResumen.java` con `codigo`, `nombre`, `numProductos`: representa una fila combinada, no modifica la tabla fabricante. `COUNT(p.codigo)` cuenta solo valores no nulos. Un `LEFT JOIN` conserva la fila del fabricante aunque no haya productos; con `COUNT(*)` ese fabricante contaría incorrectamente como 1.

### Paso 2. Interfaz

En `repository/FabricanteRepository.java`, dentro de la interfaz, añade:



```java
java.util.List<es.daw.productojdbc.dto.FabricanteResumen> findAllConNumeroProductos();
```


La interfaz declara **qué** operación existe. Con imports, el mismo tipo puede escribirse simplemente como `List<FabricanteResumen>`.

### Paso 3. Implementación JDBC

En `repository/FabricanteRepositoryJdbc.java`, dentro de la clase, añade:



```java
public java.util.List<es.daw.productojdbc.dto.FabricanteResumen> findAllConNumeroProductos() {
        String sql = "SELECT f.codigo, f.nombre, COUNT(p.codigo) AS total FROM fabricante f LEFT JOIN producto p ON p.codigo_fabricante = f.codigo GROUP BY f.codigo, f.nombre ORDER BY f.nombre";
        java.util.List<es.daw.productojdbc.dto.FabricanteResumen> lista = new java.util.ArrayList<>();
        try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(new es.daw.productojdbc.dto.FabricanteResumen(rs.getInt("codigo"), rs.getString("nombre"), rs.getInt("total")));
            return lista;
        } catch (SQLException e) { throw new AccesoDatosException("Error al contar productos", e); }
    }
```


`GROUP BY` agrupa por fabricante; `AS total` permite leer el contador con `rs.getInt("total")`; `while` convierte todas las filas a resúmenes. `try-with-resources` libera conexión, sentencia y resultado. Si falla el SQL se conserva la causa original al envolver la excepción.

### Paso 4. Servicio, controlador y vista

En `service/FabricanteService.java` añade:



```java
public java.util.List<es.daw.productojdbc.dto.FabricanteResumen> listarResumen() {
    return fabricanteRepository.findAllConNumeroProductos();
}
```


En `controller/FabricanteController.java`, en el método `listar`, cambia la llamada por `fabricanteService.listarResumen()`. Mantén el método `listar()` original del servicio: el desplegable de productos sigue necesitando fabricantes normales. En `templates/fabricantes/lista.html` añade una cabecera y una celda por fila:



```html
<th>Productos</th>
<!-- En la fila de cada fabricante: -->
<td><a th:href="@{/fabricantes/{codigo}/productos(codigo=${f.codigo})}"
       th:text="${f.numProductos}">0</a></td>
```


El enlace también sirve para el reto 2. Comprueba un fabricante con productos y otro sin ellos: el segundo debe salir con 0 y seguir apareciendo.

## 4. Productos: reto 2, ver los productos de un fabricante

### Paso 1. Contrato y SQL

Añade a `repository/ProductoRepository.java`:



```java
java.util.List<es.daw.productojdbc.model.Producto> findByFabricanteCodigo(Integer codigoFabricante);
```


Y en `ProductoRepositoryJdbc.java` implementa:



```java
public List<Producto> findByFabricanteCodigo(Integer codigoFabricante) {
        String sql = SELECT_BASE + " WHERE p.codigo_fabricante = ? ORDER BY p.codigo";
        List<Producto> lista = new ArrayList<>();
        try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, codigoFabricante);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) lista.add(mapRow(rs)); }
            return lista;
        } catch (SQLException e) { throw new AccesoDatosException("Error al buscar productos por fabricante", e); }
    }
```


Se reutilizan el JOIN y `mapRow`; el parámetro 1 filtra la FK. Una lista vacía significa que el fabricante no tiene productos, siempre que exista.

### Paso 2. Servicio y controlador

En `service/ProductoService.java`:



```java
public List<Producto> listarPorFabricante(Integer codigo) {
        fabricanteRepository.findById(codigo).orElseThrow(() -> new RecursoNoEncontradoException("No existe el fabricante con código " + codigo));
        return productoRepository.findByFabricanteCodigo(codigo);
    }
```


La comprobación de existencia distingue código inexistente de fabricante válido sin productos. En `FabricanteController` se inyecta también `ProductoService` en el constructor; después se añade:



```java
public String productos(@PathVariable Integer codigo, Model model) {
        model.addAttribute("fabricante", fabricanteService.buscarPorCodigo(codigo));
        model.addAttribute("productos", productoService.listarPorFabricante(codigo));
        return "fabricantes/productos";
    }
```


Coloca `@GetMapping("/{codigo}/productos")` inmediatamente encima. El método pone fabricante y lista en el modelo y devuelve el nombre de la plantilla. En `templates/fabricantes/productos.html` la solución muestra `fabricante.nombre`, recorre `productos`, enseña nombre, precio y stock y contempla lista vacía. Comprueba `/fabricantes/1/productos`, uno sin productos y un código inexistente.

## 5. Productos: reto 3, evitar fabricantes duplicados

### Paso 1. Planteamiento

En alta hay que buscar cualquier nombre igual sin distinguir mayúsculas. En edición hay que ignorar la propia fila: si Asus guarda Asus, eso no es duplicado. Se recortan espacios al principio y final antes de comparar. El repositorio consulta; el servicio decide; el controlador muestra el error.

En `repository/FabricanteRepository.java`, declara:



```java
boolean existsByNombreIgnoreCase(String nombre);
boolean existsByNombreIgnoreCaseAndCodigoNot(String nombre, Integer codigo);
```


### Paso 2. Consultas y excepción

La solución comparte la consulta con un método privado en `FabricanteRepositoryJdbc.java`:



```java
public boolean existsByNombreIgnoreCase(String nombre) { return existeNombre(nombre, null); }
```



```java
public boolean existsByNombreIgnoreCaseAndCodigoNot(String nombre, Integer codigo) { return existeNombre(nombre, codigo); }
```



```java
private boolean existeNombre(String nombre, Integer codigo) {
        String sql = "SELECT 1 FROM fabricante WHERE LOWER(nombre) = LOWER(?)" + (codigo == null ? "" : " AND codigo <> ?");
        try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre); if (codigo != null) ps.setInt(2, codigo);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { throw new AccesoDatosException("Error al comprobar el nombre", e); }
    }
```


Añade `@Override` sobre los dos métodos públicos. `LOWER` compara ambos textos en minúsculas; `codigo <> ?` excluye el código editado. `rs.next()` devuelve verdadero cuando se encuentra al menos una fila. Aunque el SQL se construye condicionalmente, los valores siguen pasando mediante `?`.

Crea `exception/FabricanteDuplicadoException.java`:



```java
package es.daw.productojdbc.exception;
public class FabricanteDuplicadoException extends RuntimeException {
    public FabricanteDuplicadoException() { super("Ya existe un fabricante con ese nombre"); }
}
```


### Paso 3. Regla en el servicio y error en el formulario

En `FabricanteService.crear`, justo después del `trim()` y antes de `save`:



```java
if (fabricanteRepository.existsByNombreIgnoreCase(fabricante.getNombre()))
    throw new es.daw.productojdbc.exception.FabricanteDuplicadoException();
```


En `actualizar`, después de comprobar que existe y recortar el nombre, antes de `update`:



```java
if (fabricanteRepository.existsByNombreIgnoreCaseAndCodigoNot(
        fabricante.getNombre(), fabricante.getCodigo()))
    throw new es.daw.productojdbc.exception.FabricanteDuplicadoException();
```


En el controlador, rodea la selección crear/actualizar con `try` y captura la excepción:



```java
catch (es.daw.productojdbc.exception.FabricanteDuplicadoException e) {
    result.rejectValue("nombre", "duplicado", e.getMessage());
    return "fabricantes/formulario";
}
```


`rejectValue` asocia el error a `nombre`; la vista ya tiene `th:errors` para ese campo. Observa el método completo `guardar` en la solución para situar correctamente las llaves y el redirect. Prueba crear `asus`, editar Asus sin cambiarlo y editar otro fabricante para llamarlo `ASUS`. La consulta previa es didáctica: una restricción única apropiada en BD también sería necesaria para garantizar la regla con peticiones simultáneas.

## 6. Biblioteca: reto año de nacimiento del autor

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

En `mapper/AutorMapper.java` añade `dto.setAnioNacimiento(autor.getAnioNacimiento());` en `toDto`. Sustituye el retorno de `toModelEntity` por:



```java
return new Autor(dto.getId(), dto.getNombre(), dto.getAnioNacimiento());
```


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

## 7. Fragmentos completos para situar los cambios

Estos ejemplos completan los pasos anteriores. Sustituye cada método existente por el mostrado; no añadas dos métodos con la misma firma. Las anotaciones `@GetMapping`, `@PostMapping` y `@Override` se colocan justo encima del método correspondiente.

### Productos: constructor del controlador del reto 2

En `controller/FabricanteController.java`, añade el campo y cambia el constructor para que Spring inyecte ambos servicios:

```java
private final FabricanteService fabricanteService;
private final es.daw.productojdbc.service.ProductoService productoService;

public FabricanteController(FabricanteService fabricanteService,
        es.daw.productojdbc.service.ProductoService productoService) {
    this.fabricanteService = fabricanteService;
    this.productoService = productoService;
}
```
Spring proporciona las dependencias al crear el controlador. El controlador llama a servicios, que a su vez usan repositorios.

### Productos: vista completa del reto 2

Este es el contenido de `resources/templates/fabricantes/productos.html`. `th:if` muestra el mensaje cuando la lista está vacía; `th:unless` muestra la tabla en caso contrario:

```html
<!DOCTYPE html><html lang="es" xmlns:th="http://www.thymeleaf.org"><head th:replace="~{fragments/layout :: head('Productos del fabricante')}"></head><body><nav th:replace="~{fragments/layout :: navbar}"></nav><main class="container"><h2 th:text="'Productos de ' + ${fabricante.nombre}"></h2><p th:if="${#lists.isEmpty(productos)}">Este fabricante no tiene productos.</p><table th:unless="${#lists.isEmpty(productos)}" class="table"><thead><tr><th>Nombre</th><th>Precio</th><th>Stock</th></tr></thead><tbody><tr th:each="p : ${productos}"><td th:text="${p.nombre}"></td><td th:text="${p.precio}"></td><td th:text="${p.stock}"></td></tr></tbody></table><a th:href="@{/fabricantes}">Volver a fabricantes</a></main></body></html>
```

### Productos: método completo que muestra el error de duplicado

En `controller/FabricanteController.java`, conserva `@PostMapping("/guardar")` sobre este método:

```java
public String guardar(@Valid @ModelAttribute Fabricante fabricante,
                          BindingResult result,
                          RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "fabricantes/formulario";
        }

        try {
        if (fabricante.getCodigo() == null) {
            fabricanteService.crear(fabricante);
            redirectAttributes.addFlashAttribute("mensaje",
                    "Fabricante creado con código " + fabricante.getCodigo());
        } else {
            fabricanteService.actualizar(fabricante);
            redirectAttributes.addFlashAttribute("mensaje",
                    "Fabricante " + fabricante.getCodigo() + " actualizado");
        }
        } catch (es.daw.productojdbc.exception.FabricanteDuplicadoException e) {
            result.rejectValue("nombre", "duplicado", e.getMessage());
            return "fabricantes/formulario";
        }
        return "redirect:/fabricantes";
    }
```
Primero `@Valid` aplica las anotaciones del modelo y `result.hasErrors()` detiene el guardado si fallan. Después el servicio comprueba el duplicado; si existe, `rejectValue` añade el error al mismo objeto `BindingResult` y vuelve a mostrar la vista. Solo después de un guardado correcto se ejecuta el `redirect`.

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

## 8. Plantillas mentales para otros ejercicios

**Nuevo dato:** tabla → datos iniciales → modelo → DTO y validación → mapper en ambas direcciones → todos los SELECT y `mapRow` que construyen el objeto → INSERT y UPDATE → formulario → lista → pruebas. Si cambia un constructor Lombok, busca todos sus usos.

**Nueva consulta:** interfaz → SQL con `?` → parámetros → `ResultSet`/mapeo → servicio → controlador → vista. Primero decide qué debe pasar con cero filas y con un identificador inexistente.

**Nueva regla:** identifica los datos necesarios → añade consulta si hace falta → servicio decide y lanza excepción → controlador convierte esa excepción en mensaje útil. Una validación del navegador no reemplaza la del servidor.

**Depuración rápida:** columna SQL inexistente: revisa schema y SELECT. Índice de parámetro: cuenta los `?` de izquierda a derecha. Error de constructor: revisa orden y número de campos Lombok. Dato que desaparece: revisa ambos sentidos del mapper y `mapRow`. Desplegable vacío tras error: vuelve a cargar sus opciones antes de devolver el formulario. Error al reiniciar: revisa todos los INSERT si añadiste una columna NOT NULL.

## 9. Ejecución y comprobaciones

Abre una carpeta de proyecto en IntelliJ como Maven y ejecuta `ProductoJdbcApplication` o `BibliotecaJdbcApplication`. En PowerShell, dentro de la carpeta correspondiente: `./mvnw.cmd spring-boot:run`. Abre `http://localhost:8080/`. Ambas usan el puerto 8080: ejecútalas una cada vez. La primera descarga de Maven y dependencias puede necesitar conexión; después quedan en la caché local. La consola H2 está en `/h2-console`; usa la URL de `application.properties`, usuario `sa` y contraseña vacía. Al detener la aplicación, H2 en memoria pierde los cambios.

Las copias resueltas se compilaron y se verificaron las pantallas principales, alta de stock, rechazo de stock negativo y fabricante duplicado, y alta y validación de año. No se probaron automáticamente todas las combinaciones posibles: utiliza las comprobaciones de cada reto para practicar.

Fuentes de clase: [Productos](https://github.com/profeMelola/DWES-02-2026-27/blob/main/ejercicios/ejercicio_productos_spring_jdbc.md) y [Biblioteca](https://github.com/profeMelola/DWES-02-2026-27/blob/main/ejercicios/ejercicio_biblioteca_spring_jdbc.md).

