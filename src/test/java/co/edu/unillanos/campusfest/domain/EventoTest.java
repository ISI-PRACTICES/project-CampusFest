package co.edu.unillanos.campusfest.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/**
 * Reglas del Evento: disponibilidad (CU1), reservar (CU2), editar y aforo (CU3)
 * y buscar la reserva por su código (CU2 y CU4).
 */
class EventoTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 20, 18, 0);
    private static final LocalDateTime FECHA_EVENTO = LocalDateTime.of(2026, 11, 28, 19, 0);

    private Evento nuevoEvento(int aforo, int bloqueados) {
        return new Evento("ev-1", "Concierto de bienvenida", FECHA_EVENTO,
                "Auditorio Barcelona", aforo, bloqueados);
    }

    // ---------- CU1 · Consultar cupos ----------

    @Test
    void unEventoSinReservasTieneLibresElAforoMenosLosBloqueados() {
        Evento evento = nuevoEvento(120, 5);

        assertEquals(115, evento.cuposDisponibles(AHORA));
    }

    @Test
    void losCuposDelTallerDanVeinte() {
        // Datos del taller: 120 − 5 − 83 confirmadas − 12 pendientes vigentes = 20.
        // Las 7 canceladas no ocupan cupo.
        Evento evento = nuevoEvento(120, 5);
        for (int i = 0; i < 83; i++) {
            Reserva reserva = evento.reservar("conf-" + i, AHORA);
            reserva.confirmar(AHORA.plusMinutes(1));
        }
        for (int i = 0; i < 12; i++) {
            evento.reservar("pend-" + i, AHORA.plusMinutes(5));
        }
        for (int i = 0; i < 7; i++) {
            Reserva reserva = evento.reservar("canc-" + i, AHORA);
            reserva.cancelar();
        }

        assertEquals(20, evento.cuposDisponibles(AHORA.plusMinutes(6)));
    }

    @Test
    void unaPendienteVencidaLiberaSuCupo() {
        Evento evento = nuevoEvento(10, 0);
        evento.reservar("est-1", AHORA);

        assertEquals(9, evento.cuposDisponibles(AHORA.plusMinutes(9)));
        assertEquals(10, evento.cuposDisponibles(AHORA.plusMinutes(10)));
    }

    // ---------- CU2 · Reservar ----------

    @Test
    void reservarCreaUnaPendienteYOcupaUnCupo() {
        Evento evento = nuevoEvento(10, 2);

        Reserva reserva = evento.reservar("est-1", AHORA);

        assertNotNull(reserva);
        assertEquals(EstadoReserva.PENDIENTE, reserva.getEstado());
        assertEquals("est-1", reserva.getEstudianteId());
        assertEquals(7, evento.cuposDisponibles(AHORA));
    }

    @Test
    void sePuedeReservarElUltimoCupo() {
        Evento evento = nuevoEvento(3, 1);
        evento.reservar("est-1", AHORA);

        evento.reservar("est-2", AHORA);

        assertEquals(0, evento.cuposDisponibles(AHORA));
    }

    @Test
    void sinCuposLaReservaSeRechazaYNoHaySobreventa() {
        Evento evento = nuevoEvento(3, 1);
        evento.reservar("est-1", AHORA);
        evento.reservar("est-2", AHORA);

        assertThrows(IllegalStateException.class,
                () -> evento.reservar("est-3", AHORA));
        assertEquals(0, evento.cuposDisponibles(AHORA));
    }

    @Test
    void cuandoVenceUnaPendienteSuCupoSePuedeVolverAReservar() {
        Evento evento = nuevoEvento(1, 0);
        evento.reservar("est-1", AHORA);

        Reserva nueva = evento.reservar("est-2", AHORA.plusMinutes(10));

        assertEquals(EstadoReserva.PENDIENTE, nueva.getEstado());
        assertEquals(0, evento.cuposDisponibles(AHORA.plusMinutes(10)));
    }

    @Test
    void buscarReservaPorCodigoDevuelveLaReservaDelEvento() {
        Evento evento = nuevoEvento(10, 0);
        Reserva reserva = evento.reservar("est-1", AHORA);
        evento.reservar("est-2", AHORA);

        assertSame(reserva, evento.buscarReservaPorCodigo(reserva.getCodigo()));
    }

    @Test
    void buscarUnCodigoQueNoEsDelEventoSeRechaza() {
        Evento evento = nuevoEvento(10, 0);
        Evento otroEvento = new Evento("ev-2", "Feria de emprendimiento", FECHA_EVENTO,
                "Coliseo", 50, 0);
        Reserva reservaDeOtroEvento = otroEvento.reservar("est-1", AHORA);

        assertThrows(IllegalArgumentException.class,
                () -> evento.buscarReservaPorCodigo(reservaDeOtroEvento.getCodigo()));
    }

    // ---------- CU3 · Editar evento ----------

    @Test
    void cambiarDatosActualizaNombreFechaYLugar() {
        Evento evento = nuevoEvento(10, 0);
        LocalDateTime nuevaFecha = FECHA_EVENTO.plusDays(1);

        evento.cambiarDatos("Concierto de cierre", nuevaFecha, "Plazoleta central");

        assertEquals("Concierto de cierre", evento.getNombre());
        assertEquals(nuevaFecha, evento.getFecha());
        assertEquals("Plazoleta central", evento.getLugar());
        assertEquals(10, evento.getAforo());
    }

    @Test
    void sePuedeAumentarElAforo() {
        Evento evento = nuevoEvento(10, 2);
        evento.reservar("est-1", AHORA);

        evento.cambiarAforo(20, AHORA);

        assertEquals(20, evento.getAforo());
        assertEquals(17, evento.cuposDisponibles(AHORA));
    }

    @Test
    void sePuedeBajarElAforoHastaDejarCeroCuposLibres() {
        // Aforo 10, 2 bloqueados y 3 confirmadas: el mínimo es 5.
        Evento evento = eventoConTresConfirmadas();

        evento.cambiarAforo(5, AHORA);

        assertEquals(5, evento.getAforo());
        assertEquals(0, evento.cuposDisponibles(AHORA));
    }

    @Test
    void bajarElAforoPorDebajoDeLosOcupadosSeRechazaEInformaLosOcupados() {
        Evento evento = eventoConTresConfirmadas();

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> evento.cambiarAforo(4, AHORA));

        assertEquals(10, evento.getAforo());
        assertNotNull(error.getMessage());
        assertTrue(error.getMessage().contains("3"),
                "El mensaje debe decir cuántos cupos hay ocupados (3): " + error.getMessage());
    }

    @Test
    void lasPendientesVencidasNoCuentanComoOcupadasAlBajarElAforo() {
        Evento evento = nuevoEvento(10, 0);
        evento.reservar("est-1", AHORA);
        evento.reservar("est-2", AHORA);

        evento.cambiarAforo(0, AHORA.plusMinutes(10));

        assertEquals(0, evento.getAforo());
    }

    private Evento eventoConTresConfirmadas() {
        Evento evento = nuevoEvento(10, 2);
        for (int i = 0; i < 3; i++) {
            Reserva reserva = evento.reservar("est-" + i, AHORA.minusMinutes(5));
            reserva.confirmar(AHORA.minusMinutes(4));
        }
        return evento;
    }
}
