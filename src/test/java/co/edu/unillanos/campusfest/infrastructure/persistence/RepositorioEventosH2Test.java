package co.edu.unillanos.campusfest.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.unillanos.campusfest.application.port.RepositorioEventos;
import co.edu.unillanos.campusfest.domain.EstadoReserva;
import co.edu.unillanos.campusfest.domain.Evento;
import co.edu.unillanos.campusfest.domain.Reserva;

/**
 * Adaptador H2 del puerto RepositorioEventos.
 * Cada prueba usa una base H2 en memoria nueva y vacía.
 * Lo que se guarda se debe poder volver a leer igual, con sus reservas y asistencias.
 */
class RepositorioEventosH2Test {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 20, 18, 0);
    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 11, 28, 19, 0);

    private Connection conexion;
    private RepositorioEventosH2 repositorio;

    @BeforeEach
    void preparar() throws SQLException {
        String url = "jdbc:h2:mem:prueba-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        conexion = DriverManager.getConnection(url, "sa", "");
        repositorio = new RepositorioEventosH2(conexion);
    }

    @AfterEach
    void cerrar() throws SQLException {
        conexion.close();
    }

    private Evento nuevoEvento() {
        return new Evento("ev-1", "Concierto de bienvenida", FECHA, "Auditorio Barcelona", 10, 2);
    }

    // ---------- Esquema ----------

    @Test
    void implementaElPuerto() {
        assertTrue(repositorio instanceof RepositorioEventos);
    }

    @Test
    void elConstructorCreaLasTablasEventoReservaYAsistencia() throws SQLException {
        assertTrue(existeTabla("EVENTO"), "Falta la tabla EVENTO");
        assertTrue(existeTabla("RESERVA"), "Falta la tabla RESERVA");
        assertTrue(existeTabla("ASISTENCIA"), "Falta la tabla ASISTENCIA");
    }

    @Test
    void lasTablasTienenClavePrimaria() throws SQLException {
        assertTrue(tieneClavePrimaria("EVENTO"), "EVENTO no tiene clave primaria");
        assertTrue(tieneClavePrimaria("RESERVA"), "RESERVA no tiene clave primaria");
        assertTrue(tieneClavePrimaria("ASISTENCIA"), "ASISTENCIA no tiene clave primaria");
    }

    @Test
    void reservaApuntaAEventoYAsistenciaApuntaAReserva() throws SQLException {
        assertTrue(tieneClaveForaneaHacia("RESERVA", "EVENTO"),
                "RESERVA debe tener una clave foránea hacia EVENTO");
        assertTrue(tieneClaveForaneaHacia("ASISTENCIA", "RESERVA"),
                "ASISTENCIA debe tener una clave foránea hacia RESERVA");
    }

    @Test
    void elEsquemaEstaEnElScriptSqlDeRecursos() throws IOException {
        File script = new File("src/main/resources/db/schema.sql");

        assertTrue(script.isFile(), "Falta src/main/resources/db/schema.sql");
        String sql = new String(Files.readAllBytes(script.toPath())).toUpperCase();
        assertTrue(sql.contains("CREATE TABLE"), "schema.sql debe crear las tablas");
        assertTrue(sql.contains("FOREIGN KEY") || sql.contains("REFERENCES"),
                "schema.sql debe declarar las claves foráneas");
    }

    @Test
    void crearDosRepositoriosSobreLaMismaBaseNoFalla() {
        RepositorioEventosH2 otro = new RepositorioEventosH2(conexion);

        assertNotNull(otro);
    }

    // ---------- Guardar y buscar ----------

    @Test
    void buscarUnEventoQueNoExisteDevuelveNull() {
        assertNull(repositorio.buscar("no-existe"));
    }

    @Test
    void guardaYLeeLosDatosDelEvento() {
        repositorio.guardar(nuevoEvento());

        Evento leido = repositorio.buscar("ev-1");

        assertNotNull(leido);
        assertEquals("ev-1", leido.getId());
        assertEquals("Concierto de bienvenida", leido.getNombre());
        assertEquals(FECHA, leido.getFecha());
        assertEquals("Auditorio Barcelona", leido.getLugar());
        assertEquals(10, leido.getAforo());
        assertEquals(2, leido.getBloqueados());
    }

    @Test
    void leerDevuelveUnObjetoNuevoNoElMismoQueSeGuardo() {
        Evento evento = nuevoEvento();
        repositorio.guardar(evento);

        assertNotSame(evento, repositorio.buscar("ev-1"));
    }

    @Test
    void guardaYLeeLasReservasConCodigoEstudianteEstadoYVencimiento() {
        Evento evento = nuevoEvento();
        Reserva pendiente = evento.reservar("est-1", AHORA);
        Reserva confirmada = evento.reservar("est-2", AHORA);
        confirmada.confirmar(AHORA.plusMinutes(1));
        Reserva cancelada = evento.reservar("est-3", AHORA);
        cancelada.cancelar();
        repositorio.guardar(evento);

        Evento leido = repositorio.buscar("ev-1");

        assertEquals(3, leido.getReservas().size());
        Reserva pendienteLeida = leido.buscarReservaPorCodigo(pendiente.getCodigo());
        assertEquals("est-1", pendienteLeida.getEstudianteId());
        assertEquals(EstadoReserva.PENDIENTE, pendienteLeida.getEstado());
        assertEquals(AHORA.plusMinutes(10), pendienteLeida.getVenceEn());
        assertEquals(EstadoReserva.CONFIRMADA,
                leido.buscarReservaPorCodigo(confirmada.getCodigo()).getEstado());
        assertEquals(EstadoReserva.CANCELADA,
                leido.buscarReservaPorCodigo(cancelada.getCodigo()).getEstado());
    }

    @Test
    void guardaYLeeLaAsistencia() {
        Evento evento = nuevoEvento();
        Reserva reserva = evento.reservar("est-1", AHORA);
        reserva.confirmar(AHORA.plusMinutes(1));
        reserva.registrarIngreso(AHORA.plusHours(2));
        Reserva sinIngreso = evento.reservar("est-2", AHORA);
        sinIngreso.confirmar(AHORA.plusMinutes(1));
        repositorio.guardar(evento);

        Evento leido = repositorio.buscar("ev-1");

        assertNotNull(leido.buscarReservaPorCodigo(reserva.getCodigo()).getAsistencia());
        assertEquals(AHORA.plusHours(2),
                leido.buscarReservaPorCodigo(reserva.getCodigo()).getAsistencia().getHora());
        assertNull(leido.buscarReservaPorCodigo(sinIngreso.getCodigo()).getAsistencia());
    }

    @Test
    void lasReglasFuncionanIgualConElEventoLeido() {
        Evento evento = nuevoEvento();
        Reserva confirmada = evento.reservar("est-1", AHORA);
        confirmada.confirmar(AHORA.plusMinutes(1));
        confirmada.registrarIngreso(AHORA.plusHours(2));
        evento.reservar("est-2", AHORA);
        repositorio.guardar(evento);

        Evento leido = repositorio.buscar("ev-1");

        // 10 - 2 bloqueados - 1 confirmada - 1 pendiente vigente
        assertEquals(6, leido.cuposDisponibles(AHORA.plusMinutes(5)));
        // a los 10 minutos la pendiente vence
        assertEquals(7, leido.cuposDisponibles(AHORA.plusMinutes(10)));
        // un segundo ingreso se sigue rechazando
        Reserva confirmadaLeida = leido.buscarReservaPorCodigo(confirmada.getCodigo());
        assertThrows(IllegalStateException.class,
                () -> confirmadaLeida.registrarIngreso(AHORA.plusHours(3)));
    }

    @Test
    void guardarDosVecesActualizaSinDuplicar() {
        Evento evento = nuevoEvento();
        Reserva reserva = evento.reservar("est-1", AHORA);
        repositorio.guardar(evento);

        Evento leido = repositorio.buscar("ev-1");
        leido.buscarReservaPorCodigo(reserva.getCodigo()).confirmar(AHORA.plusMinutes(2));
        leido.cambiarDatos("Concierto de cierre", FECHA.plusDays(1), "Plazoleta central");
        leido.cambiarAforo(20, AHORA.plusMinutes(2));
        repositorio.guardar(leido);

        Evento otraVez = repositorio.buscar("ev-1");
        assertEquals("Concierto de cierre", otraVez.getNombre());
        assertEquals(FECHA.plusDays(1), otraVez.getFecha());
        assertEquals(20, otraVez.getAforo());
        assertEquals(1, otraVez.getReservas().size());
        assertEquals(EstadoReserva.CONFIRMADA,
                otraVez.buscarReservaPorCodigo(reserva.getCodigo()).getEstado());
    }

    @Test
    void guardaVariosEventosPorSeparado() {
        Evento uno = nuevoEvento();
        uno.reservar("est-1", AHORA);
        Evento dos = new Evento("ev-2", "Feria de emprendimiento", FECHA, "Coliseo", 50, 0);
        repositorio.guardar(uno);
        repositorio.guardar(dos);

        assertEquals(1, repositorio.buscar("ev-1").getReservas().size());
        assertEquals(0, repositorio.buscar("ev-2").getReservas().size());
        assertEquals("Coliseo", repositorio.buscar("ev-2").getLugar());
    }

    // ---------- Errores ----------

    @Test
    void unErrorDeLaBaseSaleComoIllegalStateExceptionAlBuscar() throws SQLException {
        conexion.close();

        assertThrows(IllegalStateException.class, () -> repositorio.buscar("ev-1"));
    }

    @Test
    void unErrorDeLaBaseSaleComoIllegalStateExceptionAlGuardar() throws SQLException {
        Evento evento = nuevoEvento();
        conexion.close();

        assertThrows(IllegalStateException.class, () -> repositorio.guardar(evento));
    }

    // ---------- Utilidades ----------

    private boolean existeTabla(String tabla) throws SQLException {
        DatabaseMetaData datos = conexion.getMetaData();
        try (ResultSet rs = datos.getTables(null, null, tabla, new String[] {"TABLE", "BASE TABLE"})) {
            return rs.next();
        }
    }

    private boolean tieneClavePrimaria(String tabla) throws SQLException {
        try (ResultSet rs = conexion.getMetaData().getPrimaryKeys(null, null, tabla)) {
            return rs.next();
        }
    }

    private boolean tieneClaveForaneaHacia(String tabla, String tablaReferida) throws SQLException {
        try (ResultSet rs = conexion.getMetaData().getImportedKeys(null, null, tabla)) {
            while (rs.next()) {
                if (tablaReferida.equalsIgnoreCase(rs.getString("PKTABLE_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
