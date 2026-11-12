# Guía común: orientarse en proyectos Spring MVC y JDBC

[Guía de Productos](Guia-productos.md) · [Guía de Biblioteca](Guia-biblioteca.md)

Esta guía recoge los patrones de **estos proyectos de clase**. Otros proyectos Spring pueden organizarse de otra forma. Empieza aquí y después abre la guía del ejercicio. El documento original `Guia-estudio.md` se conserva como referencia anterior.

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
- **DTO y mapper:** un DTO representa los datos que necesita la vista. El mapper convierte DTO y modelo. En el proyecto original Productos hace esa conversión en `ProductoForm`; en la solución de clase se llama `ProductoDto`. Biblioteca tiene clases mapper separadas.

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

### 1.6. Qué buscar en cada directorio y cuándo modificarlo

Abre `Productos-original` para reconocer el punto de partida y `Productos-resuelto` para ver cómo quedó la solución de clase terminada. En los pasos de Productos las rutas Java son relativas a `Productos-resuelto/src/main/java/es/daw/productojdbc/` y las de recursos a `Productos-resuelto/src/main/resources/`. El original usa `ProductoForm` y la copia de clase usa `ProductoDto`; no copies simultáneamente ambas clases ni sus métodos. El ZIP de clase incluye la mayor parte de los cambios: los bloques siguientes enseñan cómo quedan y cómo llegar a ellos, no se deben duplicar en la solución ya terminada.

- **Raíz del proyecto:** `pom.xml` declara versión de Java, dependencias y plugins. `mvnw.cmd` y `.mvn/` permiten ejecutar Maven en Windows. No pongas reglas de negocio aquí; consulta este archivo si falta una librería o falla la compilación.
- **`src/main/java/.../ProductoJdbcApplication.java`:** punto de entrada. `SpringApplication.run` inicia Spring y descubre los componentes de sus paquetes. Habitualmente no hay que cambiarlo al añadir un campo.
- **`config/`:** configura aspectos transversales de Spring MVC. Por ejemplo, `WebConfig` participa en la configuración web. No contiene los productos ni las consultas.
- **`model/`:** representa objetos del dominio, como `Producto` y `Fabricante`. Modifícalo si el objeto necesita guardar un dato nuevo. Java relaciona objetos mediante `Producto.fabricante`; en la tabla se guarda el código del fabricante.
- **`dto/`:** define datos para formularios o resultados de pantalla. `ProductoDto` recibe `codigoFabricante` y aplica validaciones; `FabricanteResumen` lleva el resultado del contador. Separa el formato de la pantalla del modelo y permite, por ejemplo, detectar que falta un entero mediante `Integer`.
- **`controller/`:** asigna URLs a métodos, recoge parámetros, valida y elige vista o redirección. `Model` es el contenedor que entrega atributos a la plantilla; no es una tabla ni la carpeta `model/`. Modifica el controlador si cambia una ruta, los datos de una vista o el tratamiento de un error.
- **`service/`:** coordina operaciones y reglas: buscar el fabricante real, recortar nombres, impedir duplicados o un borrado no permitido. Modifícalo cuando cambia una regla o se necesita coordinar repositorios. No ejecuta directamente SQL.
- **`repository/`:** las interfaces declaran lo que puede pedirse; las clases `...Jdbc` implementan las operaciones con SQL, `PreparedStatement` y `ResultSet`. `GenericRepository` reúne operaciones generales. Para una consulta nueva normalmente se toca la interfaz y su implementación.
- **`exception/`:** contiene excepciones con significado y el controlador global de errores. Una excepción permite que el servicio comunique el problema sin construir HTML. Algunas, como la de duplicado, las captura el propio controlador para asociarlas al formulario.
- **`src/main/resources/schema.sql` y `data.sql`:** el primero crea tablas, columnas y relaciones; el segundo inserta datos iniciales compatibles con ese esquema. Añadir `NOT NULL` obliga a proporcionar un valor en los INSERT existentes.
- **`application.properties`:** parámetros de conexión y arranque. En estos proyectos la base H2 es en memoria: reiniciar vuelve a cargar el esquema y los datos iniciales.
- **`templates/`:** HTML procesado en el servidor por Thymeleaf. Sus subcarpetas `productos/` y `fabricantes/` agrupan formularios y listados; `fragments/layout.html` comparte cabecera, navegación y alertas. `th:object`, `th:field`, `th:each` y `th:text` conectan HTML con datos de Spring.
- **`static/`:** recursos servidos directamente, como CSS; no contiene las consultas ni las plantillas dinámicas.
- **`src/test/`:** pruebas automatizadas. La prueba de arranque comprueba que Spring inicia, pero por sí sola no verifica todas las reglas ni pantallas. En Biblioteca, además, `mapper/` transforma DTO y modelo y `messages.properties` centraliza textos.

### 1.7. Conexión completa: guardar, leer y editar un producto

**Ejemplo:** crear «Monitor», precio 200, stock 3 y fabricante con código 1. Sigue el mismo valor `3` en todos los pasos; si no puedes encontrarlo en uno de ellos, el recorrido está incompleto.

1. El navegador solicita `GET /productos/nuevo`. `ProductoController.nuevo` crea un `ProductoDto` vacío y carga `fabricanteService.listar()` en el atributo `fabricantes`. Devuelve `productos/formulario`; Thymeleaf resuelve `templates/productos/formulario.html` y dibuja el desplegable.
2. El `<form>` tiene `th:object="${productoDto}"` y envía un POST a `/productos/guardar`. `th:field="*{stock}"` enlaza el input con `productoDto.stock`; el `<select>` envía `codigoFabricante=1`, no un objeto Java.
3. Spring construye y rellena el DTO con los valores enviados. En `ProductoController.guardar`, `@Valid` ejecuta sus restricciones; `BindingResult` reúne errores de validación y conversión. Si falla, se recarga `fabricantes` y se devuelve el formulario con `th:errors`: si no se recarga, el desplegable no tiene sus opciones al mostrar errores.
4. Si es válido, `productoDto.toModelEntity()` crea un `Producto` y un `Fabricante` con el código recibido. El controlador llama a `productoService.crear` o `actualizar`, según el código oculto. El servicio limpia el nombre, busca el fabricante completo y rechaza uno inexistente.
5. El servicio llama a la interfaz `ProductoRepository`; Spring proporciona `ProductoRepositoryJdbc`. En `save`, el SQL tiene `?` y `setString`, `setDouble` y `setInt` asignan los valores **en el orden de los interrogantes**, empezando en 1. `executeUpdate` inserta la fila en `producto`; `codigo_fabricante=1` es la clave ajena que apunta a `fabricante.codigo`.
6. El controlador devuelve `redirect:/productos`. El navegador realiza un GET nuevo. `ProductoController.listar` llama a `ProductoService.listar`, que llama a `findAll` del repositorio.
7. `SELECT_BASE` une `producto` y `fabricante` con `JOIN`. `executeQuery` devuelve un `ResultSet`; por cada fila, `mapRow` lee `stock` y crea un `Fabricante` y un `Producto`. El repositorio entrega la lista al servicio y este al controlador.
8. El controlador añade la lista como `productos` y devuelve `productos/lista`. `th:each` crea una fila por producto y `th:text="${p.stock}"` enseña el 3. En este listado se usan objetos `Producto` directamente: no hay conversión al DTO para mostrar la tabla.
9. Para editar, `GET /productos/editar/{codigo}` usa `findById`, el mismo `SELECT_BASE` y `mapRow`. `ProductoDto.toDto(producto)` copia los valores al formulario, incluido el stock. Al enviar, se validan y `update` ejecuta el SQL de modificación. Si olvidas `toDto`, el dato puede estar guardado y aun así no aparecer en el formulario de edición.

**Diferencia entre las dos direcciones:** guardar es `HTML → DTO → Producto → servicio → INSERT/UPDATE → tabla`; listar es `tabla → SELECT/JOIN → ResultSet → Producto → servicio → controlador → HTML`. Para editar se intercala `Producto → DTO → formulario` antes de repetir el camino de guardado. El fabricante, por su parte, viaja como código en el formulario, como objeto en Java y como clave ajena en SQL.

### 1.8. Cómo leer cada paso de los retos

Cada bloque indica si hay que **añadir un campo**, **sustituir un método completo** o **completar una parte**. Las clases y métodos existentes no se duplican. Cuando se muestra un fragmento parcial, conserva el resto del archivo y sus imports; IntelliJ puede sugerir los imports que falten. Los métodos de la profesora se muestran con sus nombres reales. Al estudiar desde `Productos-original`, ten en cuenta que el ZIP de clase también cambió nombres de DTO y plantillas; la referencia final exacta es `Productos-resuelto`.


## La interfaz del repositorio y la clase JDBC

La interfaz es el **contrato**: anuncia qué se puede pedir. La clase JDBC es la **implementación**: contiene los pasos y el SQL. Por ejemplo, en Productos:

```java
// FabricanteRepository: se declara la operación
List<FabricanteResumen> findAllWithNumProductos();

// FabricanteRepositoryJdbc: se implementa la misma operación
@Override
public List<FabricanteResumen> findAllWithNumProductos() {
    // Ejecutar SELECT, recorrer las filas y devolver la lista
}
```

`FabricanteRepositoryJdbc implements FabricanteRepository` establece la relación. El servicio declara `private final FabricanteRepository fabricanteRepository;`, así que Java solo permite llamar a las operaciones visibles en esa interfaz o heredadas. Spring le proporciona la implementación JDBC. El método aparece dos veces con fines diferentes: firma en el contrato, cuerpo en la implementación. `@Override` ayuda a detectar una firma incorrecta. Las operaciones CRUD están heredadas de `GenericRepository`, por lo que no necesitan repetirse en la interfaz específica.

## ¿Cuándo modificar cada capa?

| Necesidad | Lugares que suelen cambiar | Motivo |
|---|---|---|
| Campo persistente nuevo | tabla y datos, modelo, DTO y conversión, SQL de lectura/escritura, vistas | El valor debe viajar en ambas direcciones. |
| Validación de formulario | DTO y, si es un error especial, controlador/vista | `@Valid` ya ejecuta las anotaciones existentes. |
| Consulta nueva | interfaz y JDBC; normalmente servicio y quien la invoque | Hay que declarar la operación y definir su SQL. |
| Regla de negocio nueva | servicio; quizá excepción y controlador | El servicio decide si se puede hacer. |
| Página o ruta nueva | controlador y plantilla; capas inferiores si falta la operación | Hay que recibir la petición y mostrar el resultado. |

Son preguntas para investigar, no una lista automática: revisa el código. En el reto de stock el controlador ya recibe y valida el DTO entero y llama a su conversión; el servicio ya pasa el producto entero. Por eso pueden quedar iguales. Si se solicitara una URL para cambiar solo el stock, sí habría que crear una operación de controlador y probablemente de servicio.

## Plantillas mentales para otros ejercicios

Estas plantillas son una **guía para pensar**, no una obligación de cambiar siempre todos los archivos. El orden de trabajo te ayuda a organizarte; el recorrido en ejecución explica por dónde pasa realmente la información cuando el usuario usa la aplicación. Antes de escribir, identifica qué pide el enunciado, dónde se usa el resultado y qué restricciones existen.

### 1. Si piden un dato nuevo

**Ejemplo:** añadir `stock` a un producto. El usuario debe poder escribirlo, guardarlo, verlo y editarlo. Primero distingue si el dato debe persistir en la base o solo calcularse para una pantalla. El siguiente recorrido es para un dato que se guarda.

**Orden recomendado de trabajo:**

```text
schema.sql → data.sql → modelo → DTO y validación → conversión DTO/modelo
           → JDBC: SELECT y mapeo, INSERT y UPDATE → formulario y listado → pruebas
```

1. **Tabla (`schema.sql`).** Añade la columna con el tipo y las restricciones que exige el ejercicio. Ejemplo: `stock INT NOT NULL`. Decide si permite valores vacíos, si tiene valor predeterminado y qué significa cada valor. El nombre SQL puede ser diferente del nombre Java.
2. **Datos iniciales (`data.sql`).** Si los INSERT iniciales crean productos, deben ser compatibles con la nueva columna. Si es obligatoria y no tiene valor predeterminado, añade la columna y un valor a cada INSERT. Si no, la aplicación puede fallar al arrancar antes de abrir la web.
3. **Modelo (`model/`).** Añade el atributo al objeto que representa la entidad; por ejemplo, `private int stock;` en `Producto`. Comprueba constructores y sus llamadas: si Lombok genera `@AllArgsConstructor`, añadir un atributo cambia el número y orden de argumentos. Busca todos los `new Producto(...)`, también en mappers y repositorios de otras entidades.
4. **DTO y validación (`dto/`).** Si el dato se introduce o se muestra mediante DTO, añade allí la propiedad. Para stock obligatorio que admite cero, `Integer` permite distinguir un campo vacío (`null`) del 0; `@NotNull` exige valor y `@Min(0)` rechaza negativos. La validación de formulario no sustituye las restricciones de la base de datos. Si el listado usa directamente el modelo, quizá no necesita DTO.
5. **Conversión (`mapper/` o métodos del DTO).** En el camino formulario → modelo copia el campo para que llegue al servicio; en modelo → DTO cópialo para que la edición se abra rellena. Biblioteca tiene `AutorMapper`; Productos resuelto hace conversiones dentro de `ProductoDto`. Busca los dos sentidos: añadir el atributo sin copiarlo no lo transporta automáticamente entre objetos.
6. **Lectura (`...RepositoryJdbc`).** Incluye la columna en los `SELECT` que construyen ese objeto y léela de `ResultSet` en `mapRow`. Revisa consultas compartidas, JOIN y objetos anidados: el reto de año del autor también obliga a revisar cómo `LibroRepositoryJdbc` construye un autor dentro de un libro.
7. **Escritura (`...RepositoryJdbc`).** Añade la columna y su `?` al `INSERT`, y el `set...` correspondiente en el orden correcto. Haz lo mismo en `UPDATE` si se puede editar. Recuenta todos los índices: empiezan en 1 y corresponden a los `?` de izquierda a derecha.
8. **Vista (`templates/`).** Añade el campo al formulario con el mismo nombre del DTO o propiedad enlazada; muestra sus errores con `th:errors`. Si se pide verlo en una tabla, añade cabecera y celda. Revisa la vista de edición y, si procede, el `colspan` de la fila vacía.
9. **Controlador y servicio: comprobar si cambia algo.** En stock, el controlador ya valida el DTO entero y llama a la conversión; el servicio ya pasa el producto completo. Por eso no necesitan leer el stock por separado. Si hay una URL nueva, un tratamiento especial del error o una regla de negocio nueva, sí puede hacer falta modificarlos.
10. **Prueba el viaje completo.** Crea con un valor reconocible, comprueba la fila en la base, vuelve al listado, abre edición y verifica el valor precargado, cambia y guarda. Prueba vacío, límites válidos y no válidos. Reinicia solo teniendo en cuenta que la H2 en memoria se reinicia.

**Recorrido al guardar — orden de ejecución:**

```text
Navegador: usuario escribe stock
   → formulario envía POST
   → Spring rellena ProductoDto y @Valid comprueba restricciones
   → controlador: si hay errores devuelve el formulario
   → si es válido, DTO/mapper convierte a Producto
   → servicio aplica sus reglas y llama al repositorio
   → JDBC hace INSERT o UPDATE con stock
   → base de datos guarda la fila
   → controlador redirige al listado
```

**Recorrido para mostrarlo después:**

```text
GET del listado → controlador → servicio → repositorio JDBC
→ SELECT + ResultSet + mapRow → Producto con stock
→ servicio → controlador / Model → plantilla → navegador
```

Para **editar**, tras la lectura el controlador convierte `Producto → DTO` y muestra el formulario relleno. Al enviarlo se repite el camino de guardado. Si solo se cambia el formulario y no el SQL, el valor no se conserva; si solo se guarda y no se lee, no se muestra después.

### 2. Si piden una consulta nueva

**Ejemplo:** obtener fabricantes con el número de productos. Primero concreta la pregunta: ¿cuáles son las entradas?, ¿se espera una lista, un único objeto, una cuenta o un booleano?, ¿qué pasa si no hay filas?, ¿hay que incluir fabricantes con cero productos? El resultado podría ser un DTO nuevo, como `FabricanteResumen`, sin añadir una columna a la tabla.

**Orden recomendado de trabajo:**

```text
Definir resultado → interfaz del repositorio → implementación JDBC
→ servicio → controlador → plantilla → pruebas
```

1. **Resultado y datos.** Decide el tipo devuelto (`List`, `Optional`, `int`, `boolean`...) y las propiedades necesarias. Si necesitas nombre y una cuenta calculada, un DTO de lectura puede reunir ambos. No conviertas un resultado calculado en columna persistente si el enunciado no lo pide.
2. **Contrato (`...Repository.java`).** Declara la firma con nombre, parámetros y tipo de retorno; por ejemplo `List<FabricanteResumen> findAllWithNumProductos();`. Esto permite al servicio conocer y solicitar la operación. Si se trata de un CRUD ya heredado de `GenericRepository`, no hace falta volver a declararlo.
3. **Implementación (`...RepositoryJdbc.java`).** Añade el mismo método con `@Override` y escribe el SQL. Si recibe datos, utiliza `?` y los `set...` apropiados; evita concatenar entradas. Para un contador que debe incluir fabricantes sin productos, revisa `LEFT JOIN` y `COUNT(p.codigo)`. Cierra recursos y transforma `SQLException` según el patrón del proyecto.
4. **Resultado SQL a Java.** Ejecuta `executeQuery`, recorre el `ResultSet` y crea los objetos o lee la cuenta. Una lista sin filas normalmente queda vacía; una consulta de un solo elemento puede devolver `Optional.empty()`. No confundas ausencia de resultados con error técnico.
5. **Servicio (`service/`).** Expón o coordina la consulta y aplica las comprobaciones de negocio necesarias. Por ejemplo, antes de listar productos de un fabricante por código, el servicio puede comprobar que el fabricante existe. La lista vacía para un fabricante existente y el fabricante inexistente son situaciones distintas.
6. **Controlador (`controller/`).** Si la consulta responde a una petición web, recibe los parámetros, llama al servicio y añade el resultado a `Model` con un nombre concreto. Crea una ruta solo si se necesita una nueva; una pantalla existente puede reutilizar su ruta.
7. **Vista (`templates/`).** Usa el mismo nombre de atributo que el controlador. Recorre la lista con `th:each`, pinta sus propiedades y decide qué mostrar cuando esté vacía. Una consulta usada solo para una regla de negocio no necesita necesariamente controlador ni vista nuevos.
8. **Pruebas.** Comprueba valores normales, cero resultados y límites; si hay identificadores, prueba uno inexistente. Para contadores, compara un padre con varios hijos y otro sin ninguno.

**Recorrido de la petición en ejecución:**

```text
GET del navegador → controlador recibe parámetros
→ servicio comprueba y solicita datos
→ llamada a la interfaz del repositorio
→ implementación JDBC ejecuta SQL con parámetros
→ ResultSet se convierte en lista / objeto / cuenta
→ resultado vuelve al servicio y al controlador
→ Model → plantilla → navegador
```

La interfaz **declara** lo que se puede pedir; `...Jdbc implements ...Repository` **implementa** cómo obtenerlo. Spring proporciona la implementación al servicio. Si el método solo está en JDBC, no se puede llamar mediante una variable del tipo interfaz; si solo se declara y la clase concreta no lo implementa, no cumple el contrato.

### 3. Si piden una regla nueva

**Ejemplo:** impedir crear dos fabricantes con el mismo nombre ignorando mayúsculas. Una regla decide si una operación está permitida o qué debe ocurrir; no es simplemente mostrar un campo. También puede cambiar cómo se edita, se borra o se trata un error.

**Orden recomendado de trabajo:**

```text
Definir regla y casos → identificar datos necesarios → consulta si hace falta
→ servicio decide → excepción o resultado → controlador comunica → vista muestra → pruebas
```

1. **Escribe la regla con ejemplos.** ¿`Asus` y `ASUS` son iguales? ¿Qué pasa si edito Asus sin cambiar su nombre? En edición, excluir el código actual evita considerar al propio registro un duplicado. Decide qué ocurre con espacios y valores vacíos según el enunciado.
2. **Identifica dónde están los datos.** Si necesitas saber si ya hay otro registro, busca si el repositorio dispone de esa consulta. Si no, declara la firma en la interfaz y escribe la implementación JDBC. Para duplicados, el proyecto tiene consultas diferenciadas para alta y edición.
3. **Decide en el servicio.** El servicio recibe el objeto, aplica las comprobaciones y decide si continúa o detiene la operación. Por ejemplo, ante duplicado lanza `FabricanteDuplicadoException`; si no lo hay, continúa con `save` o `update`. La consulta informa; el servicio interpreta qué significa para el negocio.
4. **Comunica el problema.** Una excepción propia da un significado claro. El controlador puede capturarla y usar `BindingResult.rejectValue` para asociar el error al campo adecuado, manteniendo los datos del formulario. Para otros problemas puede intervenir el manejador global. Sigue el patrón que utilice el proyecto.
5. **Muestra el mensaje.** La plantilla debe poder enseñar el error junto al campo; si hay desplegables, deben estar disponibles también al volver con errores. `messages.properties` puede centralizar textos cuando el proyecto lo usa.
6. **Considera las restricciones de base.** Una restricción `UNIQUE` puede respaldar la integridad cuando corresponde. La comprobación previa en el servicio no garantiza por sí sola la unicidad ante peticiones concurrentes; además, su comparación de mayúsculas debe coincidir con la regla que se quiera imponer en la base.
7. **Prueba todos los caminos.** Prueba alta válida, duplicado con mayúsculas diferentes, edición sin cambiar el nombre, edición cambiando a un nombre ocupado y tratamiento del mensaje. Comprueba también que la operación rechazada no modifica datos.

**Recorrido en ejecución:**

```text
Formulario → controlador valida formato → servicio recibe la operación
→ servicio consulta los datos necesarios al repositorio
→ repositorio devuelve resultado → servicio decide
   ├─ regla cumplida → guardar / modificar → redirección o resultado
   └─ regla incumplida → excepción → controlador añade error → formulario
```

No todas las reglas requieren una consulta: una validación individual de formulario puede expresarse con anotaciones en el DTO; una regla que compara información o coordina operaciones suele necesitar lógica de servicio. En Productos se impide borrar un fabricante con productos; en Biblioteca se permite borrar un autor y la base elimina sus libros en cascada. Lo determina el enunciado.

### 4. Si necesitas depurar rápidamente

Depurar es localizar **el primer punto donde el resultado deja de ser el esperado**. No cambies todas las capas a la vez: reproduce el problema con un dato reconocible, sigue su recorrido y comprueba una hipótesis cada vez.

**Orden recomendado:**

```text
Reproducir → leer mensaje completo → identificar fase → seguir el dato
→ localizar primera discrepancia → corregir → repetir la prueba y casos cercanos
```

1. **Reproduce y anota.** ¿Qué URL, acción y valores has usado? ¿Esperabas ver 3 y aparece vacío? ¿Falla al arrancar, al guardar, al listar o solo al editar? Si no puedes reproducirlo, no sabes todavía si la corrección ha funcionado.
2. **Lee el primer mensaje útil.** En la consola busca la causa concreta, clase y línea, no solo el último mensaje genérico. Un error SQL de arranque apunta a `schema.sql`/`data.sql`; un error de plantilla puede señalar un nombre de atributo; un error de compilación impide ejecutar la aplicación.
3. **Ubica la fase y sigue el dato en el sentido adecuado.** Si no se guarda, empieza en formulario → DTO → conversión → servicio → INSERT/UPDATE. Si se guarda pero no aparece, empieza en la fila de la base → SELECT → `mapRow` → controlador → plantilla. Si se lista bien pero no se precarga al editar, revisa modelo → DTO → `th:field`.
4. **Comprueba los puntos concretos.** Usa el depurador de IntelliJ, inspección de variables, mensajes de registro o la consola H2. Ejemplo: verifica que el DTO contiene 3, después que el modelo contiene 3 y luego el parámetro SQL. El primer salto donde cambia o desaparece reduce la búsqueda.
5. **Corrige una causa y repite.** Guarda los archivos, vuelve a ejecutar si es necesario y prueba el mismo caso. Después prueba un caso válido, uno de error y la operación relacionada (por ejemplo, alta y edición). Recuerda que reiniciar H2 en memoria puede borrar el dato que estabas siguiendo.

| Síntoma | Dónde mirar primero | Qué comprobar |
|---|---|---|
| Falla el arranque después de añadir columna | `schema.sql` y `data.sql` | La columna existe y todos los INSERT obligatorios aportan valor. |
| Error de constructor | Modelo y cada `new ...` | Número, tipo y orden de parámetros; Lombok cambia el constructor generado al añadir campos. |
| Índice de parámetro JDBC incorrecto | SQL y `set...` | Cuenta los `?` de izquierda a derecha desde 1 y compara con cada índice. |
| Se guarda, pero no sale en el listado | Base, SELECT, `mapRow`, `Model` y plantilla | Que cada paso conserve el valor y la plantilla use el nombre correcto. |
| Se ve en lista, pero falta al editar | `toDto`, atributo del modelo y formulario | Que la conversión copie el valor y `th:field` apunte a la propiedad correcta. |
| Desplegable vacío al volver con errores | Controlador y método `@ModelAttribute` | Que las opciones estén en el modelo también en la rama de error. |
| Error de validación no aparece | DTO, `@Valid`, `BindingResult`, plantilla | Que se valide el objeto correcto y `th:errors` señale el campo correcto. |
| Consulta nueva no se puede invocar | Interfaz y servicio | Que la firma esté declarada en el tipo de la variable y coincida con la implementación. |

### Esquema de repaso para el examen

| Tipo de cambio | Pregunta clave | Orden para trabajar |
|---|---|---|
| Dato nuevo persistente | ¿Cómo viaja al guardar, leer y editar? | Tabla → datos → modelo → DTO → conversión → JDBC → vistas → pruebas. |
| Consulta nueva | ¿Qué se pregunta y qué se devuelve si no hay resultados? | Resultado → interfaz → JDBC → servicio → controlador/vista si hacen falta → pruebas. |
| Regla nueva | ¿Cuándo se permite o rechaza y cómo se informa? | Casos → datos/consulta → servicio → error → controlador/vista → pruebas. |
| Depuración | ¿En qué primer punto deja de ser correcto? | Reproducir → leer error → seguir dato → localizar → corregir → volver a probar. |

El orden puede variar por dependencias y no equivale al orden de ejecución. Las flechas anteriores son un mapa para no olvidar piezas; confirma siempre qué hace ya el proyecto antes de modificar una capa.

## Errores habituales y comprobación

- Campo nuevo solo en la vista: puede verse en el formulario, pero no guardarse.
- Campo en INSERT pero no UPDATE: funciona al crear y falla al editar.
- Campo guardado pero ausente de SELECT o de `mapRow`: se pierde al leer.
- Conversión de ida sin vuelta: al editar el formulario no se rellena bien.
- `NOT NULL` sin actualizar los `INSERT` iniciales: falla el arranque.
- `@Valid` sin `BindingResult` o sin recargar las opciones del desplegable: los errores no se muestran correctamente.
- Método solo en JDBC: no se puede invocar desde una variable cuyo tipo es la interfaz si esta no lo declara.

Comprueba alta, listado, edición, borrado y errores relevantes, no solo que la aplicación arranque. En los bloques de las guías, sustituye los métodos existentes cuando se indique; no dupliques clases ni métodos.


## Ejecución y comprobaciones

Abre una carpeta de proyecto en IntelliJ como Maven y ejecuta `ProductoJdbcApplication` o `BibliotecaJdbcApplication`. En PowerShell, dentro de la carpeta correspondiente: `./mvnw.cmd spring-boot:run`. Abre `http://localhost:8080/`. Ambas usan el puerto 8080: ejecútalas una cada vez. La primera descarga de Maven y dependencias puede necesitar conexión; después quedan en la caché local. La consola H2 está en `/h2-console`; usa la URL de `application.properties`, usuario `sa` y contraseña vacía. Al detener la aplicación, H2 en memoria pierde los cambios.

Utiliza las comprobaciones de cada reto para practicar y verificar también los casos vacíos y de error.

Fuentes de clase: [Productos](https://github.com/profeMelola/DWES-02-2026-27/blob/main/ejercicios/ejercicio_productos_spring_jdbc.md) y [Biblioteca](https://github.com/profeMelola/DWES-02-2026-27/blob/main/ejercicios/ejercicio_biblioteca_spring_jdbc.md).

