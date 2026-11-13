# CampusFest · Contrato que deben cumplir las clases

Las pruebas de `src/test/java` las entregan los docentes. Cópielas tal cual a su
proyecto. No las modifique, no las borre ni las desactive. Su código debe hacerlas
pasar con `mvn test`.

Hay 90 pruebas en 10 clases de prueba, más 2 dobles de prueba (`RepositorioEventosFalso` y `RelojFijo`).
Ninguna prueba abre ventanas: todas corren en una máquina sin pantalla.

## Paquetes

| Anillo | Paquete |
| --- | --- |
| Entidades | `co.edu.unillanos.campusfest.domain` |
| Casos de uso | `co.edu.unillanos.campusfest.application.usecase` |
| Puertos | `co.edu.unillanos.campusfest.application.port` |
| Adaptadores | `co.edu.unillanos.campusfest.infrastructure.persistence`, `co.edu.unillanos.campusfest.infrastructure.ui` y `co.edu.unillanos.campusfest.infrastructure.reloj` |
| Frameworks y drivers | `co.edu.unillanos.campusfest` (`App`, `Launcher`), las vistas FXML, el CSS y el script SQL |

Recursos:

| Archivo | Ubicación |
| --- | --- |
| Vistas FXML y CSS | `src/main/resources/co/edu/unillanos/campusfest/vistas/` |
| Script del esquema H2 | `src/main/resources/db/schema.sql` |

## Cupos: cómo se cuentan

Estas palabras significan lo mismo en las reglas, en los mensajes y en las pruebas:

| Término | Qué es |
| --- | --- |
| Cupos ocupados por reservas | Reservas CONFIRMADAS + reservas PENDIENTES que no han vencido. Las CANCELADAS y VENCIDAS no cuentan. |
| Cupos bloqueados | Los que el organizador reservó al crear el evento (`bloqueados`). |
| Cupos disponibles | `aforo − bloqueados − cupos ocupados por reservas` |
| Aforo mínimo | `bloqueados + cupos ocupados por reservas`. El aforo nunca puede quedar por debajo de este número. |

Ejemplo: aforo 10, 2 bloqueados y 3 confirmadas → 3 cupos ocupados por reservas,
aforo mínimo 5 y 5 cupos disponibles. Bajar el aforo a 5 se acepta; bajarlo a 4 se
rechaza.

## Convención de errores

- `IllegalStateException`: la operación viola una regla del negocio (no hay
  cupos, la reserva venció, el aforo baja del mínimo, la reserva no está
  confirmada, ya ingresó) o falla la base de datos.
- `IllegalArgumentException`: el dato no existe o no es válido (evento
  inexistente, código que no es del evento, nombre o lugar vacíos).

La hora se recibe siempre como parámetro (`LocalDateTime ahora`) o se pide al
puerto `Reloj`. Ninguna clase de `domain` ni de `application` llama a `now()`.

## Entidades (`domain`)

```java
public enum EstadoReserva { PENDIENTE, CONFIRMADA, VENCIDA, CANCELADA }

public class Asistencia {
    public Asistencia(LocalDateTime hora)
    public LocalDateTime getHora()
}

public class Reserva {
    public static Reserva pendiente(String estudianteId, LocalDateTime ahora) // vence en ahora + 10 min
    public static Reserva reconstruir(String codigo, String estudianteId, LocalDateTime venceEn,
                                      EstadoReserva estado, Asistencia asistencia) // para el adaptador H2; no aplica reglas
    public boolean ocupaCupo(LocalDateTime ahora)
    public void confirmar(LocalDateTime ahora)
    public void cancelar()
    public Asistencia registrarIngreso(LocalDateTime ahora)
    public String getCodigo()          // único por reserva; es el contenido del QR
    public String getEstudianteId()
    public LocalDateTime getVenceEn()
    public EstadoReserva getEstado()
    public Asistencia getAsistencia()  // null si no ha ingresado
}

public class Evento {
    public Evento(String id, String nombre, LocalDateTime fecha, String lugar,
                  int aforo, int bloqueados)
    public Evento(String id, String nombre, LocalDateTime fecha, String lugar,
                  int aforo, int bloqueados, List<Reserva> reservas)  // para el adaptador H2
    public int cuposDisponibles(LocalDateTime ahora)                    // CU1
    public Reserva reservar(String estudianteId, LocalDateTime ahora)   // CU2
    public Reserva buscarReservaPorCodigo(String codigo)                // CU2 y CU4
    public void cambiarDatos(String nombre, LocalDateTime fecha, String lugar) // CU3
    public void cambiarAforo(int nuevoAforo, LocalDateTime ahora)       // CU3
    public String getId()
    public String getNombre()
    public LocalDateTime getFecha()
    public String getLugar()
    public int getAforo()
    public int getBloqueados()
    public List<Reserva> getReservas()  // de solo lectura
}
```

Si `cambiarAforo` rechaza el nuevo aforo, el mensaje de la excepción debe decir
cuántos son los **cupos ocupados por reservas** (en el ejemplo, 3). Puede decir
además los bloqueados y el aforo mínimo. Por ejemplo:
«Hay 3 cupos ocupados por reservas y 2 bloqueados: el aforo no puede ser menor que 5.»

## Puertos (`application.port`)

```java
public interface RepositorioEventos {
    Evento buscar(String eventoId);   // null si no existe
    void guardar(Evento evento);      // inserta o actualiza el evento con sus reservas y asistencias
}

public interface Reloj {
    LocalDateTime ahora();
}
```

## Casos de uso (`application.usecase`)

Todos reciben `(RepositorioEventos repositorio, Reloj reloj)` en el constructor.

```java
public class ConsultarCupos {      // CU1
    public int consultar(String eventoId)
}

public class ReservarEntrada {     // CU2
    public Reserva solicitarReserva(String eventoId, String estudianteId)
    public Reserva confirmarReserva(String eventoId, String codigoReserva)
}

public class EditarEvento {        // CU3
    public Evento obtener(String eventoId)
    public void editar(String eventoId, String nombre, LocalDateTime fecha,
                       String lugar, int nuevoAforo)
}

public class RegistrarIngreso {    // CU4
    public Asistencia registrar(String eventoId, String codigo)
}
```

Los casos de uso que cambian algo llaman a `repositorio.guardar(evento)` una vez
cuando la operación termina bien, y no lo llaman si la operación se rechaza.
`ConsultarCupos` no guarda nada.

## Adaptadores de persistencia (`infrastructure.persistence`)

```java
public class RepositorioEventosEnMemoria implements RepositorioEventos {
    public RepositorioEventosEnMemoria()
}

public class RepositorioEventosH2 implements RepositorioEventos {
    public RepositorioEventosH2(Connection conexion)
}
```

`RepositorioEventosH2`:

- El constructor recibe una conexión abierta y crea las tablas si no existen,
  ejecutando `src/main/resources/db/schema.sql`. Crearlo dos veces sobre la misma
  base no falla (`CREATE TABLE IF NOT EXISTS`).
- Tablas `EVENTO`, `RESERVA` y `ASISTENCIA`, cada una con clave primaria.
  `RESERVA` tiene una clave foránea hacia `EVENTO` y `ASISTENCIA` una hacia `RESERVA`.
- `buscar` devuelve un objeto nuevo, reconstruido desde la base, con todas sus
  reservas (código, estudiante, estado, vencimiento) y sus asistencias.
- `guardar` dos veces el mismo evento lo actualiza, sin duplicar reservas.
- Todo `SQLException` se atrapa dentro del adaptador y sale como
  `IllegalStateException` (o una subclase). Ninguna excepción de `java.sql`
  llega a la pantalla.

## Adaptador del reloj (`infrastructure.reloj`)

```java
public class RelojDelSistema implements Reloj { }   // la única clase que llama a now()
```

## Pantallas (`infrastructure.ui` y `vistas/`)

Una pantalla por caso de uso:

| CU | Vista FXML | Controlador | Constructor | Debe llamar a |
| --- | --- | --- | --- | --- |
| CU1 | `pantalla-cupos.fxml` | `PantallaCuposController` | `(ConsultarCupos)` | `consultar` |
| CU2 | `pantalla-reserva.fxml` | `PantallaReservaController` | `(ReservarEntrada)` | `solicitarReserva`, `confirmarReserva` |
| CU3 | `pantalla-evento.fxml` | `PantallaEventoController` | `(EditarEvento)` | `obtener`, `editar` |
| CU4 | `pantalla-ingreso.fxml` | `PantallaIngresoController` | `(RegistrarIngreso)` | `registrar` |

Reglas de las pantallas:

- El FXML **no** declara `fx:controller`: `App` crea el controlador con su caso de
  uso y lo asigna con `FXMLLoader.setController(...)`.
- Cada `fx:id` del FXML es un campo `private` con `@FXML` en el controlador, con el
  mismo nombre.
- Cada acción del FXML (`onAction="#metodo"`) es un método con `@FXML` en el
  controlador. Cada pantalla tiene al menos una acción.
- Cada pantalla tiene `<Label fx:id="etiquetaMensaje"/>`, donde se muestra el
  resultado o el error.
- El controlador solo lee la pantalla, llama al caso de uso y muestra el resultado.
  Atrapa la excepción (`catch`) y muestra `getMessage()` en `etiquetaMensaje`.
- El controlador no contiene reglas del negocio ni llama a métodos de las
  entidades que cambian su estado o calculan reglas (`cuposDisponibles`,
  `ocupaCupo`, `getEstado()`, `reservar`, `confirmar`, `cancelar`,
  `registrarIngreso`, `cambiarAforo`, ...). Puede usar los getters para mostrar datos.
- El controlador no conoce el repositorio, el reloj ni la base de datos, y no crea
  casos de uso.
- El CSS está en `vistas/` y todas sus propiedades llevan el prefijo `-fx-`.

## Ensamblaje (`App`)

`App` es la única clase que crea las implementaciones de los puertos
(`RepositorioEventosH2`, `RepositorioEventosEnMemoria`, `RelojDelSistema`). En
`start` abre la conexión H2, crea `RepositorioEventosH2` y `RelojDelSistema`, los
cuatro casos de uso y los cuatro controladores, y carga los cuatro FXML con
`setController`.

## Dependencias en el `pom.xml`

```xml
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-controls</artifactId>
    <version>21.0.5</version>
</dependency>
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-fxml</artifactId>
    <version>21.0.5</version>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <version>2.2.224</version>
</dependency>
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.2</version>
    <scope>test</scope>
</dependency>
```

## Archivos de prueba

| Archivo | Pruebas | Qué prueba |
| --- | --- | --- |
| `domain/ReservaTest.java` | 16 | Plazo de 10 min, estados, cancelación e ingreso |
| `domain/EventoTest.java` | 14 | Cupos (datos del taller: 20), reservar, buscar por código, editar y aforo |
| `application/usecase/ConsultarCuposTest.java` | 5 | CU1 |
| `application/usecase/ReservarEntradaTest.java` | 7 | CU2 |
| `application/usecase/EditarEventoTest.java` | 6 | CU3 |
| `application/usecase/RegistrarIngresoTest.java` | 5 | CU4 |
| `application/usecase/RepositorioEventosFalso.java` | — | Doble de prueba del puerto (no es una prueba) |
| `application/usecase/RelojFijo.java` | — | Doble de prueba del reloj (no es una prueba) |
| `infrastructure/persistence/RepositorioEventosEnMemoriaTest.java` | 4 | Adaptador en memoria |
| `infrastructure/persistence/RepositorioEventosH2Test.java` | 16 | Adaptador H2: esquema, claves, guardar y leer, actualizar, errores |
| `infrastructure/ui/PantallasTest.java` | 14 | Pantallas y `App`, leyendo FXML y código (sin abrir ventanas) |
| `ReglaDeDependenciaTest.java` | 3 | Regla de dependencia y uso del reloj, leyendo el código fuente |
