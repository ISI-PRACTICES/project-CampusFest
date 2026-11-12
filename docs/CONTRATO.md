# CampusFest · Contrato que deben cumplir las clases

Las pruebas de `src/test/java` las entregan los docentes. Cópielas tal cual a su
proyecto. No las modifique, no las borre ni las desactive. Su código debe hacerlas
pasar con `mvn test`.

Hay 60 pruebas en 8 clases de prueba, más 2 dobles de prueba (`RepositorioEventosFalso` y `RelojFijo`).

## Paquetes

| Anillo | Paquete |
| --- | --- |
| Entidades | `co.edu.unillanos.campusfest.domain` |
| Casos de uso | `co.edu.unillanos.campusfest.application.usecase` |
| Puertos | `co.edu.unillanos.campusfest.application.port` |
| Adaptadores | `co.edu.unillanos.campusfest.infrastructure.persistence` y `co.edu.unillanos.campusfest.infrastructure.ui` |
| Frameworks y drivers | `co.edu.unillanos.campusfest` (`App`, `Launcher`) |

## Convención de errores

- `IllegalStateException`: la operación viola una regla del negocio (no hay
  cupos, la reserva venció, el aforo baja de los ocupados, la reserva no está
  confirmada, ya ingresó).
- `IllegalArgumentException`: el dato no existe (evento inexistente, código que
  no es del evento).

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
}
```

El mensaje de la excepción de `cambiarAforo` debe incluir cuántos cupos hay
ocupados.

## Puertos (`application.port`)

```java
public interface RepositorioEventos {
    Evento buscar(String eventoId);   // null si no existe
    void guardar(Evento evento);
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

## Adaptador en memoria (`infrastructure.persistence`)

```java
public class RepositorioEventosEnMemoria implements RepositorioEventos {
    public RepositorioEventosEnMemoria()
}
```

## Dependencia en el `pom.xml`

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.2</version>
    <scope>test</scope>
</dependency>
```

## Archivos de prueba

| Archivo | Qué prueba |
| --- | --- |
| `domain/ReservaTest.java` | Plazo de 10 min, estados, cancelación e ingreso |
| `domain/EventoTest.java` | Cupos (datos del taller: 20), reservar, buscar por código, editar y aforo |
| `application/usecase/ConsultarCuposTest.java` | CU1 |
| `application/usecase/ReservarEntradaTest.java` | CU2 |
| `application/usecase/EditarEventoTest.java` | CU3 |
| `application/usecase/RegistrarIngresoTest.java` | CU4 |
| `application/usecase/RepositorioEventosFalso.java` | Doble de prueba del puerto (no es una prueba) |
| `application/usecase/RelojFijo.java` | Doble de prueba del reloj (no es una prueba) |
| `infrastructure/persistence/RepositorioEventosEnMemoriaTest.java` | Adaptador en memoria |
| `ReglaDeDependenciaTest.java` | Regla de dependencia y uso del reloj, leyendo el código fuente |
