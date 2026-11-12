# Guía de posibles retos de examen: Spring MVC y JDBC

[Guía común](Guia-comun.md) · [Guía de Productos](Guia-productos.md) · [Guía de Biblioteca](Guia-biblioteca.md)

Estos enunciados son **inventados para practicar**: no sabemos qué preguntará la profesora ni son predicciones del examen. La idea es reconocer patrones y combinarlos. Antes de copiar código, lee el enunciado y comprueba los nombres, tipos y reglas del proyecto que te entreguen.

Las referencias a código son relativas a `Productos-resuelto/src/main/java/es/daw/productojdbc/` o a `Biblioteca-resuelto/src/main/java/es/daw/bibliotecajdbc/`, salvo que se indique `src/main/resources/`. En el original de Productos el DTO se llama `ProductoForm`; en el resuelto, `ProductoDto`. Si una funcionalidad ya existe, **modifícala o reutilízala**: no dupliques métodos.

## Cómo utilizar esta guía

1. Lee solo el enunciado e intenta escribir qué capas cambiarías.
2. Consulta «Se parece a» para localizar un ejemplo real.
3. Sigue el orden propuesto y, para cada archivo, pregúntate qué dato entra y qué debe salir.
4. Comprueba «Qué debes adaptar»: el ejemplo de clase es una referencia, no una solución que siempre puedas copiar sin cambios.
5. Ejecuta los casos de prueba, incluidos los errores y resultados vacíos.

| Si el enunciado dice… | Piensa en… | Retos de esta guía |
|---|---|---|
| «Añade un campo y guárdalo» | Viaje de un dato por todas las capas | 1–4 |
| «Muestra cuántos / media / total» | Consulta agregada y DTO de lectura | 5–7 |
| «Muestra solo los que…» | Consulta o filtro y pantalla | 8–10 |
| «No permitas / comprueba antes» | Regla de servicio y posible consulta | 11–14 |
| «Al borrar…» | Regla del servicio y FK de la base | 15–16 |
| «Haz varias cosas a la vez» | Combinar patrones | 17–18 |

---

## 1. Añadir una descripción opcional a los productos

**Enunciado posible:** «Cada producto puede tener una descripción de hasta 200 caracteres. Debe poder introducirse, modificarse y verse en el listado».

**Se parece a:** reto 4 de Productos (`stock`): columna, DTO, conversiones, JDBC y vistas. Fíjate también en `@Size` de `dto/ProductoDto.java`.

**Qué hacer, en orden:**

1. `schema.sql`: añade una columna de texto de longitud adecuada. Como es opcional, decide y mantén coherencia sobre si un valor ausente se guarda como `NULL` o texto vacío.
2. `data.sql`: comprueba que los INSERT siguen funcionando; al permitir `NULL`, podrían omitir la columna. Añade ejemplos si ayudan a probar.
3. `model/Producto.java`: añade `String descripcion` y revisa todos los constructores `new Producto(...)`.
4. `dto/ProductoDto.java`: añade el campo y `@Size(max = 200)`; copia el dato tanto en `toDto` como en `toModelEntity`.
5. `ProductoRepositoryJdbc`: añade la columna a lecturas y `mapRow`, y a `INSERT` y `UPDATE` con sus parámetros correctos.
6. Formulario y listado: añade control, mensaje de error y columna. El controlador y servicio existentes pueden seguir recibiendo el objeto completo; compruébalo.

**Qué adaptar:** `stock` es entero obligatorio y descripción es texto opcional; no copies `@NotNull` ni `@Min`. Una longitud máxima no convierte por sí sola el texto vacío en `NULL`.

**Prueba:** guardar sin descripción, con texto, con 200 caracteres y con 201; editar y recargar.

---

## 2. Añadir una cantidad obligatoria que acepta cero

**Enunciado posible:** «Añade a cada libro un número de ejemplares disponibles, obligatorio y nunca negativo».

**Se parece a:** `stock` en Productos; `LibroMapper` y `LibroRepositoryJdbc` en Biblioteca.

**Qué hacer:** añade `ejemplares INT NOT NULL` y valores a todos los libros de ejemplo; añade el campo a `Libro` y `LibroDto` (para validar vacío usa `Integer`, `@NotNull` y `@Min(0)`); copia ambas direcciones en `LibroMapper`. Ajusta `SELECT_BASE`, `mapRow`, `save`, `update`, formulario y tabla. Revisa el orden de los constructores de `Libro` y los parámetros SQL.

**Qué adaptar:** en Biblioteca el mapper está en una clase independiente; en Productos el DTO resuelto hace la conversión. El servicio y el controlador solo cambian si el enunciado añade una operación o regla que no gestionan ya.

**Prueba:** 0 y 4 deben guardarse; vacío y −1 deben mostrar error; abrir edición debe recuperar el valor correcto.

---

## 3. Añadir una fecha con validación

**Enunciado posible:** «Cada fabricante tiene una fecha de fundación obligatoria que no puede ser futura».

**Se parece a:** año de nacimiento en Biblioteca para el viaje del dato; `LibroDto.fechaPublicacion` y `LibroRepositoryJdbc` para `LocalDate` y JDBC.

**Qué hacer:**

1. Añade una columna `DATE NOT NULL` y fechas válidas a los INSERT de fabricantes.
2. Añade `LocalDate fechaFundacion` al modelo y DTO; valida obligatoriedad y fecha no futura con las anotaciones apropiadas. Comprueba la conversión de fecha del formulario.
3. Ajusta la conversión entre DTO y modelo y todos los constructores de `Fabricante`.
4. En JDBC usa `setObject` al escribir y `getObject("fecha_fundacion", LocalDate.class)` al leer. **Revisa también `ProductoRepositoryJdbc`**, porque construye un fabricante anidado al leer productos.
5. Añade `input type="date"` al formulario y muestra la fecha en el listado; el formato de visualización puede ser distinto del formato enviado por el navegador.

**Qué adaptar:** un año entero y una fecha completa no son el mismo tipo. Al añadir un campo al padre revisa las consultas del hijo que construyen ese padre.

**Prueba:** fecha vacía, futura, de hoy y pasada; creación, listado y edición.

---

## 4. Campo con opciones fijas

**Enunciado posible:** «El producto debe tener estado: DISPONIBLE o DESCATALOGADO. No se admiten otros valores».

**Se parece a:** stock para el recorrido completo; validación de DTO de Productos para rechazar entradas incorrectas.

**Qué hacer:** define cómo se representa el estado (por ejemplo, enum Java y columna de texto, según el proyecto); actualiza esquema y datos de prueba; incorpora el atributo al modelo, DTO, conversiones y SQL de lectura/escritura; presenta un `<select>` y muestra el estado en la lista. Valida en servidor que el valor sea uno de los permitidos y estudia cómo tratar errores de conversión.

**Qué adaptar:** un desplegable limita lo que ofrece la interfaz, pero una petición puede enviar otros valores. No dependas únicamente del HTML. Si la base también restringe valores, mantén coherentes ambos conjuntos.

**Prueba:** cada opción, campo ausente y una petición con valor no permitido; editar y conservar la opción elegida.

---

## 5. Contar libros por autor, incluidos los que tienen cero

**Enunciado posible:** «En el listado de autores muestra cuántos libros tiene cada uno, incluso si no tiene ninguno».

**Se parece a:** reto 1 de Productos, `FabricanteRepositoryJdbc.findAllWithNumProductos()` y DTO `FabricanteResumen`. También existe `LibroRepositoryJdbc.countByAutorId`, pero cuenta un solo autor cada vez.

**Qué hacer:** crea o adapta un DTO de lectura con id, nombre y número; declara una consulta en `AutorRepository`; impleméntala en JDBC usando `LEFT JOIN` de autor a libro, `COUNT(libro.id)` y `GROUP BY` de las columnas del autor. Conecta servicio, controlador y plantilla para mostrar el contador.

**Qué adaptar:** el reto de Productos agrupa todos los padres en una consulta; `countByAutorId` devuelve un `int` para un autor concreto y se usa antes del borrado. Escoge el patrón según lo pedido. Si muestras también el año del autor, inclúyelo correctamente en consulta, agrupación y DTO.

**Prueba:** autor con cero, uno y varios libros. Con `COUNT(*)`, una fila del `LEFT JOIN` sin libro podría contarse como 1; `COUNT(libro.id)` ignora ese id nulo y da 0.

---

## 6. Contar un subconjunto

**Enunciado posible:** «Muestra para cada fabricante cuántos productos tienen precio superior a 100, incluyendo fabricantes con cero productos que cumplan».

**Se parece a:** contador del reto 1 de Productos; combina `LEFT JOIN`, agregación y condición SQL.

**Qué hacer:** define un DTO de resumen y declara e implementa la consulta; conecta servicio, controlador y tabla. Una opción es colocar la condición del producto en el `ON` del `LEFT JOIN`, por ejemplo `... ON p.codigo_fabricante = f.codigo AND p.precio > ?`, y contar `p.codigo`. Asigna el límite mediante `PreparedStatement` si es parámetro.

**Qué adaptar:** colocar `p.precio > 100` en un `WHERE` elimina las filas donde el producto es nulo y puede hacer desaparecer fabricantes con cero coincidencias. Decide si 100 está incluido: «superior» significa `>`, no `>=`.

**Prueba:** fabricante sin productos; con productos todos de 100 o menos; con uno de 101; con varios. Los dos primeros deben mostrar 0.

---

## 7. Calcular una media o un total sin añadir una columna

**Enunciado posible:** «En el listado de fabricantes muestra el precio medio de sus productos. Para un fabricante sin productos, muestra un guion».

**Se parece a:** reto 1 de Productos: consulta agrupada y DTO de resumen, sustituyendo `COUNT` por `AVG`.

**Qué hacer:** declara y ejecuta una consulta agrupada con `LEFT JOIN` y `AVG(p.precio)`; guarda el resultado en un DTO de lectura y llévalo a la vista. Usa un tipo capaz de representar ausencia, como `Double`, y comprueba el valor SQL `NULL` correctamente: `getDouble` devuelve 0 para un NULL y hay que consultar `wasNull()`, o usar una lectura compatible que conserve nulidad.

**Qué adaptar:** una media sin productos es `NULL`, no necesariamente 0. No añadas el valor calculado al `schema.sql` salvo que el ejercicio pida guardarlo. Para dinero real, estudia los tipos decimales exigidos; aquí respeta los tipos del proyecto.

**Prueba:** sin productos → guion; con un precio → ese precio; con dos → media correcta; un precio 0 real no debe confundirse con ausencia.

---

## 8. Ver los libros de un autor concreto

**Enunciado posible:** «Desde cada autor, abre una página con sus libros. Si el autor no existe, muestra el error correspondiente».

**Se parece a:** reto 2 de Productos: `ProductoRepository.findByFabricanteCodigo`, `ProductoService.listarPorFabricante`, controlador y plantilla de productos del fabricante.

**Qué hacer:** declara `List<Libro> findByAutorId(Integer autorId)` en `LibroRepository`; implementa el SELECT con `WHERE ... = ?`, usando el mapeo existente. El servicio comprueba que el autor exista y llama al repositorio. El controlador recibe el id en una ruta, solicita datos y entrega autor y libros a la plantilla; añade el enlace en la lista de autores.

**Qué adaptar:** un autor existente sin libros devuelve lista vacía y un mensaje adecuado; un id inexistente provoca el tratamiento de recurso no encontrado. Comprueba también qué campos muestra la pantalla y si necesita convertir libros a DTO.

**Prueba:** autor con varios libros, autor sin libros e id inexistente.

---

## 9. Filtrar por rango de precio

**Enunciado posible:** «El listado de productos permite introducir precio mínimo y máximo, opcionales, y mantiene los filtros tras buscar».

**Se parece a:** hito 8 de Biblioteca: `FiltroLibrosDto`, `LibroController.listar` y `LibroService.buscar`.

**Qué hacer:** crea un DTO de filtro con campos opcionales y restricciones; el controlador los recibe por GET y rechaza un máximo menor que el mínimo. Si son válidos, el servicio aplica los filtros sobre `findAll()` si ese es el enfoque pedido; la plantilla conserva valores, ofrece Limpiar y muestra errores o resultados.

**Qué adaptar:** Biblioteca filtra en Java con streams; si el enunciado exige filtrar en SQL, declara e implementa una consulta con parámetros y maneja allí los filtros opcionales. No apliques sin pensar la validación de años a los precios. Decide si los extremos son inclusivos.

**Prueba:** ningún filtro, cada extremo por separado, ambos iguales, rango invertido, texto no numérico y ningún resultado.

---

## 10. Buscar por fragmento de nombre

**Enunciado posible:** «Busca productos cuyo nombre contenga el texto escrito, sin distinguir mayúsculas y minúsculas».

**Se parece a:** filtrado de Biblioteca para el formulario GET; `existsByNombreIgnoreCase` de Productos para `LOWER` y parámetros JDBC.

**Qué hacer:** define si texto vacío muestra todos los productos; crea un método de búsqueda en interfaz y JDBC si se exige SQL, con comparación insensible a mayúsculas y `LIKE`; conecta servicio, controlador y formulario. Usa parámetros en `PreparedStatement`.

**Qué adaptar:** `LIKE` trata `%` y `_` como comodines. Si el enunciado exige búsqueda literal de esos caracteres, debes escaparlos con una estrategia compatible con la base. Un `LOWER(nombre) = LOWER(?)` busca igualdad, no fragmentos.

**Prueba:** fragmento central, mayúsculas distintas, sin resultados, vacío y caracteres especiales si se exige búsqueda literal.

---

## 11. Impedir títulos repetidos dentro del mismo autor

**Enunciado posible:** «Un autor no puede tener dos libros con el mismo título, ignorando mayúsculas; distintos autores sí pueden compartir título».

**Se parece a:** reto 3 de Productos (`FabricanteService` y consultas `existsByNombreIgnoreCase...`); combina con `LibroService.crear/actualizar`.

**Qué hacer:** especifica normalización (espacios y mayúsculas). Declara e implementa una consulta que compruebe título **y** autor, y otra o una variante para edición que excluya el libro actual por id. En `LibroService` valida antes de guardar; comunica el error al controlador mediante una excepción apropiada; usa `rejectValue` para mostrarlo junto al título.

**Qué adaptar:** la regla de fabricante comprueba un nombre global; aquí la unicidad depende de dos valores. Al editar, localizar primero el libro por ISBN permite recuperar su id y excluirse a sí mismo. Considera una restricción de base coherente con la regla si se exige integridad también ante concurrencia.

**Prueba:** mismo autor y título con distinto uso de mayúsculas → rechazo; otro autor y mismo título → permitido; editar sin cambiar título → permitido; cambiar al de otro libro del mismo autor → rechazo.

---

## 12. Impedir que una cantidad quede negativa

**Enunciado posible:** «Permite retirar unidades de stock; la cantidad solicitada debe ser positiva y no puede superar el stock disponible».

**Se parece a:** stock del reto 4, comprobaciones de servicio en `ProductoService` y manejo de errores de duplicado en `FabricanteController`.

**Qué hacer:** añade una ruta y formulario para la operación si no existen; recibe y valida cantidad; el servicio busca el producto, comprueba que existe y que hay unidades suficientes, calcula el nuevo stock y lo guarda. Si incumple la regla, devuelve un error claro y conserva la entrada. El repositorio necesitará una operación adecuada de actualización si no se reutiliza una existente sin perder datos.

**Qué adaptar:** `@Min(0)` en el DTO del producto evita un stock negativo introducido en ese formulario, pero la retirada exige comparar dos valores en el servicio. Para varias operaciones simultáneas, una lectura seguida de un update separado puede perder cambios: se necesita una estrategia atómica o de concurrencia adecuada, por ejemplo una actualización condicionada y comprobar filas afectadas.

**Prueba:** retirada 1, retirada igual al stock, superior al stock, cero, negativa y producto inexistente. Verifica que un rechazo no cambie el stock.

---

## 13. Comprobar una fecha en relación con otra

**Enunciado posible:** «No se puede guardar un libro con fecha de publicación anterior al año de nacimiento del autor».

**Se parece a:** reto del año en Biblioteca y `LibroService.prepararParaGuardar`, que busca el autor completo. Usa además las validaciones de fecha de `LibroDto`.

**Qué hacer:** una vez validado el formulario, el servicio busca el autor por id y compara `libro.getFechaPublicacion().getYear()` con `autor.getAnioNacimiento()`. Si incumple, comunica una excepción; el controlador presenta el error en el campo de fecha. El autor recuperado debe llevar el año, por lo que hay que comprobar su consulta JDBC.

**Qué adaptar:** `@PastOrPresent` solo comprueba que la fecha no sea futura; no compara con otro objeto. Un año de nacimiento no permite saber el día exacto de nacimiento: una publicación del mismo año cumple la regla tal como está redactada, pero no demuestra que fuera posterior al nacimiento.

**Prueba:** anterior → rechazo; mismo año y posterior → permitidos; autor inexistente y fecha vacía → errores adecuados.

---

## 14. Limitar el número de libros de un autor

**Enunciado posible:** «No se puede dar de alta un libro si el autor ya tiene 10 libros».

**Se parece a:** `LibroRepository.countByAutorId` y `AutorService.borrar` de Biblioteca para contar; reglas de `LibroService.crear` para decidir.

**Qué hacer:** reutiliza `countByAutorId` desde `LibroService` después de comprobar que el autor existe. Si la cuenta es 10 o más, rechaza la creación y muestra un mensaje; si es menor, guarda. Valora también la edición: si permite cambiar el autor, comprueba el límite del autor de destino; si no cambia, no cuentes el libro existente como una incorporación nueva.

**Qué adaptar:** contar para un mensaje de borrado no es lo mismo que usar la cuenta como regla. Si puede haber altas concurrentes, contar y después insertar por separado no garantiza el límite: hace falta una estrategia transaccional y de concurrencia apropiada.

**Prueba:** autor con 9, con 10 y con 0; editar sin cambiar autor y cambiar a un autor que ya llegó al límite.

---

## 15. Prohibir borrar un padre con hijos

**Enunciado posible:** «No se puede borrar un autor mientras tenga libros».

**Se parece a:** `FabricanteService.borrar` y `FabricanteConProductosException` en Productos. Usa `LibroRepository.countByAutorId` en Biblioteca para conocer si hay libros.

**Qué hacer:** comprueba que el autor exista; consulta si tiene libros; si hay, lanza una excepción o devuelve un resultado que el controlador pueda traducir a un mensaje. Si no hay, borra. Revisa también el esquema: Biblioteca tiene `ON DELETE CASCADE`, que permite el borrado de hijos; si la nueva regla exige protección en base de datos, hay que modificar la FK según el ejercicio y comprobar que el rechazo también se mantiene ante accesos directos.

**Qué adaptar:** no dejes simplemente activada la cascada confiando en que la base impedirá el borrado. La restricción FK sin cascada protege la integridad, pero el servicio permite dar un mensaje comprensible. El cambio de esquema puede requerir recrear la FK según el entorno.

**Prueba:** autor sin libros se borra; autor con libros se conserva y sus libros también; id inexistente da el error correspondiente.

---

## 16. Borrar un padre y comunicar cuántos hijos se eliminan

**Enunciado posible:** «Al borrar un fabricante, borra sus productos y muestra cuántos se han borrado».

**Se parece a:** `AutorService.borrar`, `LibroRepository.countByAutorId` y FK `ON DELETE CASCADE` de Biblioteca. Es una regla distinta de la de Productos actual.

**Qué hacer:** adapta la FK de producto para que tenga cascada si el enunciado la exige; añade consulta de cuenta en interfaz y JDBC; cambia el servicio para comprobar existencia, contar productos antes de borrar y devolver la cantidad; adapta el controlador y la confirmación de la plantilla.

**Qué adaptar:** elimina o cambia la regla actual que impide borrar fabricantes con productos; no dejes dos reglas contradictorias. La cascada la ejecuta la base, no hace falta borrar cada producto en un bucle. El conteo y el borrado deben coordinarse adecuadamente si pueden ocurrir cambios simultáneos.

**Prueba:** fabricante con 0, con 2 y código inexistente; comprueba en la base que los productos asociados ya no están y que el mensaje muestra el número esperado.

---

## 17. Reto combinado: stock bajo por fabricante

**Enunciado posible:** «Muestra por fabricante cuántos productos tienen stock menor que 5 y permite abrir la lista de esos productos. Incluye fabricantes sin coincidencias».

**Se parece a:** reto 4 de Productos para stock; reto 1 para la cuenta con `LEFT JOIN`; reto 2 para abrir un listado filtrado por fabricante.

**Qué hacer:** primero comprueba que stock ya se guarda y se recupera. Añade un DTO de resumen y una consulta agrupada con condición de stock en el `ON` para conservar fabricantes con cero coincidencias. Después declara otra consulta que reciba código de fabricante y devuelva solo productos con `stock < 5`. El servicio comprueba existencia del fabricante; el controlador prepara la página y la vista enlaza desde el resumen.

**Qué adaptar:** son dos resultados distintos: una cuenta por fabricante y una lista detallada. No hace falta guardar el contador en la tabla. «Menor que 5» no incluye 5.

**Prueba:** sin productos, stock 0, 4 y 5, varios fabricantes e id inexistente; verifica que cuenta y detalle coinciden.

---

## 18. Reto combinado: autor con año y estadísticas

**Enunciado posible:** «Añade año de nacimiento obligatorio al autor y muestra en su ficha la cantidad de libros publicados desde un año indicado».

**Se parece a:** reto de año de Biblioteca para el campo; `countByAutorId` para una cuenta concreta; filtro por año de `LibroService.buscar` para el criterio; reto 2 de Productos para la ruta por padre.

**Qué hacer:** primero completa año en esquema, datos, modelo, DTO, mapper, JDBC de autores y de libros, y vistas, siguiendo la guía de Biblioteca. Después decide si la cuenta filtrada se hará en SQL o en Java según el enunciado. Declara e implementa la operación necesaria, valida el año de filtro y comprueba existencia del autor. Expón una ruta y muestra los datos y el resultado.

**Qué adaptar:** el año de nacimiento es un **dato persistente**; la cantidad de libros desde una fecha es un **resultado calculado**. No se resuelven igual. Si se usa SQL sobre la fecha, decide claramente si el año límite es inclusivo y utiliza parámetros.

**Prueba:** autor sin libros, libros anteriores y posteriores al límite, libro justo en el límite, filtro vacío o inválido según la regla, y autor inexistente.

---

## Último repaso: qué copiar mentalmente de cada ejercicio

| Referencia | Patrón que enseña | No olvides adaptar |
|---|---|---|
| Productos, reto 4: stock | Dato persistente completo y validación | Tipo, nulidad, restricciones y constructores. |
| Biblioteca, año de nacimiento | Campo en un padre usado también dentro de otros objetos | Mappers y JOIN que construyen el autor. |
| Productos, reto 1: contador | `LEFT JOIN`, `COUNT`, agrupación y DTO de lectura | Qué se cuenta, ceros y campos agrupados. |
| Productos, reto 2: productos por fabricante | Consulta con parámetro, ruta y listado de hijos | Existencia del padre frente a lista vacía. |
| Productos, reto 3: duplicados | Consulta de existencia, regla, excepción y mensaje | Criterio de unicidad y exclusión del registro editado. |
| Biblioteca, hito 8: filtros | DTO de filtro, validación, GET y streams | Criterios opcionales, límites y si se pide SQL. |
| Biblioteca, CRUD de libros | ISBN público, id técnico, fecha y autor | Identificador usado por cada capa y conversión de fecha. |
| Biblioteca, borrado de autor | Contar antes y borrar en cascada | Si el enunciado permite o prohíbe borrar hijos. |

Para desarrollar un reto en examen, combina solo las piezas necesarias. El **orden para programar** suele ir desde datos/contratos hacia la vista; el **orden cuando se ejecuta** comienza en el navegador, pasa por controlador y servicio hasta JDBC, y vuelve con el resultado.
