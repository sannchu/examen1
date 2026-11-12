# Guía de estudio: Productos y Fabricantes

[Guía común](Guia-comun.md) · [Guía de Biblioteca](Guia-biblioteca.md)

Lee primero la guía común para entender las capas. Aquí están los retos de Productos en el orden de estudio: **stock (4), contador (1), productos por fabricante (2) y duplicados (3)**. Compara `Productos-original` con `Productos-resuelto`: el original usa `ProductoForm` y la solución de clase `ProductoDto`. La solución ya contiene gran parte de los cambios; no los dupliques. Las rutas Java son relativas a `Productos-resuelto/src/main/java/es/daw/productojdbc/`.

## Mapa del ejercicio

```text
FABRICANTE (1) ───── fabrica ───── (N) PRODUCTO
producto.codigo_fabricante → fabricante.codigo
```

El fabricante es padre y producto es hijo. El servicio impide borrar un fabricante con productos. `ProductoController` recibe peticiones y usa `ProductoService`; este usa las interfaces de repositorio; las clases `...Jdbc` realizan el SQL. El formulario envía el código del fabricante, mientras que el modelo `Producto` contiene un objeto `Fabricante`.

Archivos de referencia: `model/Producto.java` y `Fabricante.java`; `dto/ProductoDto.java`, `FabricanteDto.java` y `FabricanteResumen.java`; `repository/GenericRepository.java`, interfaces y clases JDBC; `service/ProductoService.java` y `FabricanteService.java`; controladores homónimos; `resources/schema.sql`, `data.sql` y `templates/`. La guía común explica el papel de cada uno.

## Antes de los retos: una petición completa

Al abrir `/productos/nuevo`, el controlador crea el DTO y carga fabricantes para el desplegable. El POST rellena y valida el DTO; si hay errores devuelve el formulario con los fabricantes. Si es válido, `toModelEntity()` construye el producto y el servicio busca el fabricante real. El repositorio inserta mediante parámetros y devuelve al controlador, que redirige al listado. Para listar, el JOIN crea productos con fabricantes, el servicio entrega la lista y Thymeleaf la pinta. Para editar, `toDto` vuelve a rellenar el formulario.

## 2. Productos: reto 4, añadir stock paso a paso

[Fuente: ZIP de la profesora del 10 de noviembre](https://github.com/profeMelola/DWES-02-2026-27/blob/main/ejercicios/logistica-src-clase10Nov-todosLosRetos.zip). El ZIP solo trae `src`; el proyecto conserva el `pom.xml` y el wrapper del original. La profesora ya había hecho gran parte del reto. Aquí se muestra el resultado completo y se señalan los ajustes posteriores.

**Análisis:** se pide un entero obligatorio que puede ser 0. Hay que almacenarlo al crear y editar, recuperarlo al listar y editar y enseñarlo en pantalla. El orden de trabajo sigue el viaje del dato. En el código del ZIP ya estaba casi todo salvo la columna del listado y una validación coherente con cero y campo vacío.

### Paso 1. Añadir la columna y los datos iniciales

En `schema.sql`, dentro de `CREATE TABLE producto`, comprueba que aparece la columna entre precio y fabricante:

```sql
precio            DOUBLE       NOT NULL,
stock             INT          NOT NULL,
codigo_fabricante INT          NOT NULL,
```

En `data.sql`, **cada INSERT de producto** debe indicar `stock` y su valor en el mismo orden; por ejemplo:

```sql
INSERT INTO producto (nombre, precio, stock, codigo_fabricante)
VALUES ('Disco duro SATA3 1TB', 86.99, 25, 5);
```

Sin este dato los INSERT antiguos fallan porque la columna no admite NULL. No basta con añadirlo al primer producto.

### Paso 2. Añadir el dato al modelo

En `model/Producto.java`, junto a los demás atributos, añade `private int stock;` **antes de** `fabricante`. Con `@AllArgsConstructor`, el orden resultante es `codigo, nombre, precio, stock, fabricante`. Revisa todas las llamadas a `new Producto(...)`, incluido el mapeo JDBC y el DTO: el constructor ahora requiere ese argumento.

### Paso 3. DTO: validación y conversiones

En `dto/ProductoDto.java`, añade o sustituye el campo stock y conserva el resto de atributos:

```java
@NotNull(message = "El stock es obligatorio")
@Min(value = 0, message = "El stock no puede ser negativo")
private Integer stock;
```

`Integer` puede ser `null`, por lo que `@NotNull` detecta la ausencia. Con `int`, el valor inicial es 0 y no se distingue de un cero escrito. `@Min(0)` acepta 0 y rechaza negativos. La profesora había utilizado `int`, `@Positive`, `@Min(1)` y `@Max(100)`; se cambian porque contradicen el cero de los datos de ejemplo o añaden un límite que no pide el enunciado. La conversión a `int` del modelo ocurre después de validar.

En el mismo archivo, **sustituye** los métodos de conversión por estos si aún no incluyen stock:

```java
public static ProductoDto toDto(Producto p) {
        ProductoDto form = new ProductoDto();
        form.setCodigo(p.getCodigo());
        form.setNombre(p.getNombre());
        form.setPrecio(p.getPrecio());
        form.setStock(p.getStock());
        form.setCodigoFabricante(p.getFabricante().getCodigo());
        return form;
    }
```

```java
public Producto toModelEntity() {
        Fabricante fabricante = new Fabricante(codigoFabricante, null);
        return new Producto(codigo, nombre, precio, stock, fabricante);
    }
```

`toDto` se usa al abrir la edición; `toModelEntity` al guardar. El orden en el constructor debe coincidir con el modelo. Si solo cambias uno, el dato se pierde en una de las dos direcciones.

### Paso 4. Consultas de lectura y mapeo JDBC

En `repository/ProductoRepositoryJdbc.java`, sustituye `SELECT_BASE` por la versión con `p.stock`:

```java
private static final String SELECT_BASE = """
        SELECT p.codigo, p.nombre, p.precio, p.stock,
               f.codigo AS f_codigo, f.nombre AS f_nombre
        FROM producto p
        JOIN fabricante f ON p.codigo_fabricante = f.codigo
        """;
```

La usan `findAll`, `findById` y `findByFabricanteCodigo`; así todas traen el dato. Sustituye también `mapRow`:

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

Los alias identifican las columnas del fabricante. `rs.getInt("stock")` recupera la columna para construir el producto. La columna es `NOT NULL`; por eso no se pierde información de nulidad al usar `getInt`.

### Paso 5. Consultas de escritura JDBC

En el mismo repositorio, sustituye `save` y `update` por estos métodos. Mantén sus anotaciones `@Override` en el archivo:

```java
public void save(Producto producto) {
        String sql = "INSERT INTO producto (nombre, precio, stock, codigo_fabricante) VALUES (?, ?, ?, ?)";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, producto.getNombre());
            ps.setDouble(2, producto.getPrecio());
            ps.setInt(3, producto.getStock());
            ps.setInt(4, producto.getFabricante().getCodigo());

            ps.executeUpdate();

            // Recuperamos el código que ha generado AUTO_INCREMENT y se lo asignamos al objeto
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    producto.setCodigo(claves.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al guardar el producto", e);
        }
    }
```

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

`stock` ocupa la posición 3 en ambos SQL y se enlaza con `setInt(3, ...)`. En la actualización el fabricante pasa a la posición 4 y el código del producto a la 5. Si cambias SQL y no cambias índices, se guardan datos incorrectos o falla la consulta. `delete` solo necesita el código y no cambia por este reto.

### Paso 6. Formulario y listado

En `templates/productos/formulario.html`, añade el bloque junto al precio:

```html
<div class="mb-3">
    <label for="stock" class="form-label">Stock</label>
    <input type="number" step="1" min="0" th:field="*{stock}" class="form-control"
           th:classappend="${#fields.hasErrors('stock')} ? 'is-invalid'">
    <div class="invalid-feedback" th:errors="*{stock}">Error</div>
</div>
```

`th:field` enlaza el campo; `th:errors` presenta el error del DTO; `step="1"` expresa unidades enteras. La validación del servidor sigue siendo necesaria. En `templates/productos/lista.html`, añade la cabecera después de precio y la celda correspondiente dentro del `th:each`:

```html
<th>Stock</th>
<!-- En la fila de cada producto: -->
<td th:text="${p.stock}">0</td>
```

Como la tabla tiene una columna más, cambia el `colspan` de la fila «No hay productos» de 6 a 7. **Comprobación:** crea con 0 y con 101, edita y recarga; prueba vacío, negativo y decimal. Los valores válidos deben persistir mientras la aplicación esté arrancada; los inválidos deben volver al formulario con error.

## 3. Productos: reto 1, contar productos por fabricante

**Análisis:** queremos una fila por fabricante, incluidos los que tienen cero productos. La cantidad es un dato calculado para el listado. Recorrido: SQL → `FabricanteResumen` → servicio → controlador → plantilla.

### Paso 1. Resultado de la consulta

En `dto/FabricanteResumen.java`, el ZIP de clase utiliza esta estructura (ya existe en el original, así que comprueba sus campos antes de modificarla):

```java
@Data
@AllArgsConstructor
public class FabricanteResumen {
    private Integer codigo;
    private String nombre;
    private int numProductos;
}
```

Lombok genera getters y el constructor usado por JDBC. Es un DTO de lectura; no modifica la tabla `fabricante`.

### Paso 2. Contrato del repositorio

En `repository/FabricanteRepository.java`, dentro de la interfaz, añade:

```java
List<FabricanteResumen> findAllWithNumProductos();
```

Importa `List` y `FabricanteResumen`. La interfaz permite que el servicio solicite la operación sin depender de JDBC.

### Paso 3. SQL y conversión del resultado

En `repository/FabricanteRepositoryJdbc.java`, añade el método con `@Override` e imports correspondientes:

```java
public List<FabricanteResumen> findAllWithNumProductos() {
        String sql = """
                        SELECT f.codigo, f.nombre, COUNT(p.codigo) AS num_productos
                        FROM fabricante f
                        LEFT JOIN producto p ON p.codigo_fabricante = f.codigo
                        GROUP BY f.codigo, f.nombre
                        ORDER BY f.nombre                        
                        """;

        List<FabricanteResumen> fabricantes = new ArrayList<>();
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                fabricantes.add(new FabricanteResumen(
                        rs.getInt("codigo"),
                        rs.getString("nombre"),
                        rs.getInt("num_productos")
                ));
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al listar los fabricantes con su nº de productos", e);
        }
        return fabricantes;
    }
```

`LEFT JOIN` conserva fabricantes sin producto. `COUNT(p.codigo)` ignora el código nulo de esa fila y da 0; `COUNT(*)` contaría la fila y daría 1. `GROUP BY` reúne las filas de cada fabricante; el alias `num_productos` permite leer la cuenta desde `ResultSet`.

### Paso 4. Servicio y controlador

En `service/FabricanteService.java`, añade:

```java
public List<FabricanteResumen> findAllWithNumProductos() {
        return fabricanteRepository.findAllWithNumProductos();
    }
```

En `controller/FabricanteController.java`, sustituye `listar` conservando `@GetMapping`:

```java
public String listar(Model model) {

        //model.addAttribute("fabricantes", fabricanteService.listar());

        // ------------ RETO 1 --------------
        model.addAttribute("fabricantes",fabricanteService.findAllWithNumProductos());

        return "fabricantes/lista";

    }
```

El nombre `fabricantes` debe coincidir con la expresión usada por la plantilla. El servicio mantiene el controlador separado del repositorio.

### Paso 5. Pintar el contador

En `templates/fabricantes/lista.html`, añade `<th>Num productos</th>` en la cabecera y esta celda dentro de `th:each="f : ${fabricantes}"`:

```html
<td th:text="${f.numProductos}"
    th:classappend="${f.numProductos > 0}? 'text-bg-primary':'text-bg-secondary'">x</td>
```

Ajusta a 5 el `colspan` de la fila vacía. En la solución el botón de borrado también usa `th:disabled="${f.numProductos > 0}"`; la regla real sigue comprobándose en el servicio. **Comprueba:** un fabricante con varios productos y otro recién creado sin productos deben aparecer con sus cantidades correctas.

## 4. Productos: reto 2, ver los productos de un fabricante

**Análisis:** desde un fabricante hay que filtrar sus productos y enseñar una vista de consulta. Un fabricante sin productos debe mostrar una lista vacía; un código inexistente debe producir 404. Recorrido: enlace → controlador → servicios → repositorio → SQL → lista → vista.

### Paso 1. Declarar la consulta

En `repository/ProductoRepository.java`, añade dentro de la interfaz:

```java
List<Producto> findByFabricanteCodigo(Integer codigoFabricante);
```

### Paso 2. Implementarla con JDBC

En `repository/ProductoRepositoryJdbc.java`, añade el método conservando `@Override`:

```java
public List<Producto> findByFabricanteCodigo(Integer codigoFabricante) {
        String sql = SELECT_BASE + "WHERE p.codigo_fabricante = ? ORDER BY p.nombre"; // por defecto es ASC

        List<Producto> productos = new ArrayList<>();

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, codigoFabricante);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    productos.add(mapRow(rs));
                }

            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al listar los productos del fabricante " + codigoFabricante, e);
        }

        return productos;
    }
```

Se reutilizan el JOIN y `mapRow` del reto 4. `?` recibe el código con `setInt`; no se concatena la entrada. `ORDER BY p.nombre` ordena los resultados. La lista vacía se devuelve si no hay coincidencias.

### Paso 3. Comprobar el fabricante en el servicio

En `service/ProductoService.java`, añade:

```java
public List<Producto> listarPorFabricante(Integer codigoFabricante) {

        // Regla de negocio: comprobar que el codigoFabricante existe...
        fabricanteRepository.findById(codigoFabricante).orElseThrow(
                () -> new RecursoNoEncontradoException("No existe el fabricante con código "+codigoFabricante));

        return productoRepository.findByFabricanteCodigo(codigoFabricante);
    }
```

La comprobación separa «fabricante existente sin productos» de «fabricante inexistente». Si no existe, `orElseThrow` produce la excepción tratada como 404.

### Paso 4. Crear la ruta

En `controller/FabricanteController.java`, inyecta `private final ProductoService productoService;` junto a `fabricanteService`. `@RequiredArgsConstructor` de la solución genera el constructor para ambos. Añade el método y su anotación:

```java
@GetMapping("/{codigo}/productos")
public String productosDelFabricante(@PathVariable("codigo") Integer codigo, Model model) {

        model.addAttribute("fabricante",fabricanteService.buscarPorCodigo(codigo));
        model.addAttribute("productos",productoService.listarPorFabricante(codigo));

        return "fabricantes/productosDelFabricante";

    }
```

El fabricante se carga aparte para mostrar su nombre incluso cuando la lista está vacía. El nombre retornado apunta al archivo `templates/fabricantes/productosDelFabricante.html`.

### Paso 5. Enlace y plantilla

En `templates/fabricantes/lista.html`, sustituye la celda del nombre por:

```html
<td>
    <a th:href="@{/fabricantes/{codigo}/productos(codigo=${f.codigo})}"
       th:text="${f.nombre}">Nombre</a>
</td>
```

`{codigo}` se reemplaza por el valor de cada fabricante. Crea `templates/fabricantes/productosDelFabricante.html`: este es el archivo completo de la profesora:

```html
<!DOCTYPE html>
<html lang="es" xmlns:th="http://www.thymeleaf.org">
<head th:replace="~{fragments/layout :: head('Productos del fabricante')}"></head>
<body>
<nav th:replace="~{fragments/layout :: navbar}"></nav>

<!-- RETO 2: productos de un fabricante -->
<div class="container" style="max-width: 800px;">

    <div class="d-flex justify-content-between align-items-center mb-3">
        <h2 class="text-success">
            <!-- El fabricante llega aparte en el modelo: si no tiene productos, también hay título -->
            🏭 <span th:text="${fabricante.nombre}"></span>
            <!-- |...| es sustitución literal: mezcla texto y expresiones sin concatenar con + -->
            <small class="text-muted fs-5" th:text="|${#lists.size(productos)} productos|">(0 productos)</small>
        </h2>
        <a th:href="@{/fabricantes}" class="btn btn-outline-secondary">
            <i class="bi bi-arrow-left"></i> Volver a fabricantes
        </a>
    </div>

    <!-- Sin columna Fabricante (ya sale en el título) ni botones: es una página de consulta -->
    <table class="table table-bordered table-hover bg-white align-middle">
        <thead class="table-dark">
        <tr>
            <th>Código</th>
            <th>Nombre</th>
            <th>Precio</th>
        </tr>
        </thead>
        <tbody>
        <!-- colspan="3": la fila ocupa las tres columnas de la tabla -->
        <tr th:if="${#lists.isEmpty(productos)}">
            <td colspan="3" class="text-center text-muted">Este fabricante no tiene productos</td>
        </tr>
        <tr th:each="p : ${productos}">
            <td th:text="${p.codigo}">1</td>
            <td th:text="${p.nombre}">Nombre</td>
            <!-- 1 cifra entera, dos decimales y el punto (POINT, COMMA, DEFAULT (locale del navegador) -->
            <td th:text="${#numbers.formatDecimal(p.precio, 1, 2, 'POINT')}">0.00</td>
        </tr>
        </tbody>
    </table>
</div>

<th:block th:replace="~{fragments/layout :: scripts}"></th:block>
</body>
</html>
```

`#lists.size` calcula la cantidad, `th:if` muestra el mensaje si está vacía y `th:each` recorre los productos. **Comprueba:** un fabricante con productos, otro sin ellos y `/fabricantes/99999/productos`.

## 5. Productos: reto 3, impedir fabricantes duplicados

**Análisis:** al crear, ningún otro fabricante puede tener el mismo nombre sin distinguir mayúsculas. Al editar, se excluye el propio código para permitir conservar su nombre. El servicio aplica la regla; el controlador comunica el error al formulario.

### Paso 1. Declarar las consultas

En `repository/FabricanteRepository.java`, dentro de la interfaz, añade:

```java
boolean existsByNombreIgnoreCase(String nombre);
boolean existsByNombreIgnoreCaseAndCodigoNot(String nombre, Integer codigo);
```

### Paso 2. Implementar las consultas

En `repository/FabricanteRepositoryJdbc.java`, añade ambos métodos con sus `@Override`:

```java
public boolean existsByNombreIgnoreCase(String nombre) {
        // En bd tengo ASUS y llega asus -> true
        // En bd tengo ASUS y llega Asus -> true
        // En bd tengo ASUS y llega ASUS -> true
        // En bd tengo asus y llega ASUS  -> true
        // .......
        String sql = "SELECT 1 FROM fabricante WHERE LOWER(nombre) = LOWER(?)";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // TRUE si encuentra el fabricante con ese nombre...
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al comprobar el nombre del fabricante: " + nombre, e);
        }

    }
```

```java
public boolean existsByNombreIgnoreCaseAndCodigoNot(String nombre, Integer codigo) {
        String sql = "SELECT 1 FROM fabricante WHERE LOWER(nombre) = LOWER(?) AND codigo <> ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setInt(2, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // TRUE si encuentra el fabricante con ese nombre...
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al comprobar el nombre del fabricante: " + nombre, e);
        }

    }
```

`LOWER` compara sin distinguir mayúsculas. `SELECT 1` solo necesita comprobar existencia y `rs.next()` indica si hay una fila. En edición, `codigo <> ?` excluye al propio fabricante. Los parámetros se asignan en orden y no se concatenan en el SQL.

### Paso 3. Crear una excepción propia

Crea `exception/FabricanteDuplicadoException.java`:

```java
package es.daw.productojdbc.exception;

public class FabricanteDuplicadoException extends RuntimeException {
    public FabricanteDuplicadoException(String nombre) {
        //super("Ya existe un fabricante con el nombre: " + nombre);
        super("Ya existe un fabricante llamado \""+nombre+"\".");
    }
}
```

La excepción comunica el problema desde el servicio sin depender de la vista. El mensaje incluye el nombre solicitado.

### Paso 4. Aplicar la regla en el servicio

En `service/FabricanteService.java`, sustituye estos dos métodos:

```java
public Fabricante crear(Fabricante fabricante) {

        // eliminar espacios a izq y derecha
        fabricante.setNombre(fabricante.getNombre().strip());

        // -------------- RETO 3 ------------
        // NUEVA REGLA DE NEGOCIO: nombre único
        // Antes de crear el fabricante, tengo que verificar que no
        // existe ninguno con el mismo nombre
        if (fabricanteRepository.existsByNombreIgnoreCase(fabricante.getNombre())){
            // lanzar excepción propia
            throw new FabricanteDuplicadoException(fabricante.getNombre());
        }

        //----------------------------------------------
        fabricanteRepository.save(fabricante);
        return fabricante;
    }
```

```java
public Fabricante actualizar(Fabricante fabricante) {
        // REGLA DE NEGOCIO PARA BUSCAR EL CÓDIGO DE FABRICANTE. SI NO EXISTE LANZA EXCEPCIÓN...
        buscarPorCodigo(fabricante.getCodigo());
        // REGLA DE NEGOCIO PARA ELIMINAR ESPACIOS EN EL NOMBRE DEL FABRICANTE..
        fabricante.setNombre(fabricante.getNombre().strip());

        // -------------- RETO 3 ------------
        // NUEVA REGLA DE NEGOCIO: nombre único
        // Antes de crear el fabricante, tengo que verificar que no
        // existe ninguno con el mismo nombre
        if (fabricanteRepository.existsByNombreIgnoreCaseAndCodigoNot(fabricante.getNombre(),fabricante.getCodigo())){
            // lanzar excepción propia
            throw new FabricanteDuplicadoException(fabricante.getNombre());
        }

        //----------------------------------------------

        fabricanteRepository.update(fabricante);
        return fabricante;
    }
```

`strip()` quita espacios exteriores antes de comparar y guardar. En edición se verifica primero que el código existe. Cada caso usa su consulta para no rechazar el nombre del propio registro.

### Paso 5. Mostrar el error junto al nombre

En `controller/FabricanteController.java`, sustituye `guardar`, manteniendo `@PostMapping("/guardar")`:

```java
public String guardar(@Valid @ModelAttribute FabricanteDto fabricanteDto,
                          BindingResult result,
                          RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "fabricantes/formulario";
        }

        try {
            if (fabricanteDto.getCodigo() == null) {
                Fabricante fabricante = fabricanteService.crear(FabricanteDto.toModelEntity(fabricanteDto));
                redirectAttributes.addFlashAttribute("mensaje",
                        "Fabricante creado con código " + fabricante.getCodigo());
            } else {
                Fabricante fabricante = fabricanteService.actualizar(FabricanteDto.toModelEntity(fabricanteDto));
                redirectAttributes.addFlashAttribute("mensaje",
                        "Fabricante " + fabricante.getCodigo() + " actualizado");
            }
        }
        catch (FabricanteDuplicadoException e) {
            result.rejectValue("nombre", "", e.getMessage());
            return "fabricantes/formulario";
        }
        return "redirect:/fabricantes";
    }
```

`rejectValue` vincula el mensaje a `nombre`; al volver a `fabricantes/formulario`, `th:errors="*{nombre}"` lo muestra sin perder el valor escrito. El formulario ya contiene ese bloque:

```html
<input id="nombre" type="text" th:field="*{nombre}" class="form-control"
       th:classappend="${#fields.hasErrors('nombre')} ? 'is-invalid'">
<div class="invalid-feedback" th:errors="*{nombre}">Error</div>
```

**Comprueba:** crear `asus` cuando existe `Asus`; editar un fabricante sin cambiar su nombre; intentar ponerle el nombre de otro; probar espacios exteriores. La comprobación en servicio no garantiza unicidad ante peticiones simultáneas; para ello se necesitaría también una restricción adecuada en la base.


## ¿Por qué el stock no obliga a cambiar controller ni service?

`guardar` ya recibe `@Valid @ModelAttribute ProductoDto` y comprueba `BindingResult`. Spring rellena el nuevo `stock` y valida las anotaciones añadidas al DTO. El controlador ya llama a `toModelEntity()`: al actualizar esa conversión, pasa el stock dentro del `Producto` sin pedirlo campo por campo. Al editar ya llama a `ProductoDto.toDto(producto)`, que ahora lo copia. El servicio recibe y pasa el producto completo; sus reglas existentes no cambian. En cambio JDBC enumera las columnas y debe leer y escribir explícitamente `stock`.

En una consulta nueva, como la del reto 1, se declara el método en `FabricanteRepository` y se implementa en `FabricanteRepositoryJdbc`; la guía común explica por qué. En este caso además se necesita conectar servicio, controlador y vista porque aparece información nueva en el listado.

## Pruebas de repaso

Crea y edita productos con stock 0 y positivo; prueba vacío y negativo; recarga y comprueba el valor. Verifica el contador de un fabricante sin productos y otro con varios, abre su listado, y prueba nombres duplicados con distintas mayúsculas tanto en alta como en edición. Comprueba además que editar un fabricante sin cambiar el nombre sigue siendo posible.

## Ejecución y comprobaciones

Abre una carpeta de proyecto en IntelliJ como Maven y ejecuta `ProductoJdbcApplication` o `BibliotecaJdbcApplication`. En PowerShell, dentro de la carpeta correspondiente: `./mvnw.cmd spring-boot:run`. Abre `http://localhost:8080/`. Ambas usan el puerto 8080: ejecútalas una cada vez. La primera descarga de Maven y dependencias puede necesitar conexión; después quedan en la caché local. La consola H2 está en `/h2-console`; usa la URL de `application.properties`, usuario `sa` y contraseña vacía. Al detener la aplicación, H2 en memoria pierde los cambios.

Utiliza las comprobaciones de cada reto para practicar y verificar también los casos vacíos y de error.

Fuentes de clase: [Productos](https://github.com/profeMelola/DWES-02-2026-27/blob/main/ejercicios/ejercicio_productos_spring_jdbc.md) y [Biblioteca](https://github.com/profeMelola/DWES-02-2026-27/blob/main/ejercicios/ejercicio_biblioteca_spring_jdbc.md).

