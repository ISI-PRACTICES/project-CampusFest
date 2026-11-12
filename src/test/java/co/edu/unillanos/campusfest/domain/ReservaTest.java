package co.edu.unillanos.campusfest.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/**
 * Reglas de la Reserva: plazo de 10 minutos, estados e ingreso.
 * Usadas por CU1 (ocupaCupo), CU2 (pendiente, confirmar) y CU4 (registrarIngreso).
 */
class ReservaTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 20, 18, 0);

    // ---------- Creación (CU2) ----------

    @Test
    void unaReservaNuevaQuedaPendienteYVenceEnDiezMinutos() {
        Reserva reserva = Reserva.pendiente("est-1", AHORA);

        assertEquals(EstadoReserva.PENDIENTE, reserva.getEstado());
        assertEquals("est-1", reserva.getEstudianteId());
        assertEquals(AHORA.plusMinutes(10), reserva.getVenceEn());
        assertNotNull(reserva.getCodigo());
        assertNull(reserva.getAsistencia());
    }

    @Test
    void cadaReservaTieneUnCodigoDistinto() {
        Reserva primera = Reserva.pendiente("est-1", AHORA);
        Reserva segunda = Reserva.pendiente("est-1", AHORA);

        assertNotEquals(primera.getCodigo(), segunda.getCodigo());
    }

    // ---------- Ocupación de cupo (CU1) ----------

    @Test
    void unaPendienteOcupaCupoAntesDeVencer() {
        Reserva reserva = Reserva.pendiente("est-1", AHORA);

        assertTrue(reserva.ocupaCupo(AHORA.plusMinutes(9).plusSeconds(59)));
    }

    @Test
    void unaPendienteDejaDeOcuparCupoALosDiezMinutosExactos() {
        Reserva reserva = Reserva.pendiente("est-1", AHORA);

        assertFalse(reserva.ocupaCupo(AHORA.plusMinutes(10)));
    }

    @Test
    void unaConfirmadaOcupaCupoAunqueHayaPasadoElPlazo() {
        Reserva reserva = Reserva.pendiente("est-1", AHORA);
        reserva.confirmar(AHORA.plusMinutes(5));

        assertTrue(reserva.ocupaCupo(AHORA.plusHours(3)));
    }

    @Test
    void unaCanceladaNoOcupaCupo() {
        Reserva reserva = Reserva.pendiente("est-1", AHORA);
        reserva.cancelar();

        assertFalse(reserva.ocupaCupo(AHORA.plusMinutes(1)));
    }

    // ---------- Confirmación (CU2) ----------

    @Test
    void confirmarAntesDelPlazoLaDejaConfirmada() {
        Reserva reserva = Reserva.pendiente("est-1", AHORA);

        reserva.confirmar(AHORA.plusMinutes(9).plusSeconds(59));

        assertEquals(EstadoReserva.CONFIRMADA, reserva.getEstado());
    }

    @Test
    void confirmarALosDiezMinutosExactosSeRechaza() {
        Reserva reserva = Reserva.pendiente("est-1", AHORA);

        assertThrows(IllegalStateException.class,
                () -> reserva.confirmar(AHORA.plusMinutes(10)));
        assertNotEquals(EstadoReserva.CONFIRMADA, reserva.getEstado());
    }

    @Test
    void noSePuedeConfirmarDosVeces() {
        Reserva reserva = Reserva.pendiente("est-1", AHORA);
        reserva.confirmar(AHORA.plusMinutes(1));

        assertThrows(IllegalStateException.class,
                () -> reserva.confirmar(AHORA.plusMinutes(2)));
    }

    @Test
    void noSePuedeConfirmarUnaCancelada() {
        Reserva reserva = Reserva.pendiente("est-1", AHORA);
        reserva.cancelar();

        assertThrows(IllegalStateException.class,
                () -> reserva.confirmar(AHORA.plusMinutes(1)));
    }

    // ---------- Cancelación ----------

    @Test
    void sePuedeCancelarUnaConfirmada() {
        Reserva reserva = Reserva.pendiente("est-1", AHORA);
        reserva.confirmar(AHORA.plusMinutes(1));

        reserva.cancelar();

        assertEquals(EstadoReserva.CANCELADA, reserva.getEstado());
    }

    @Test
    void noSePuedeCancelarDosVeces() {
        Reserva reserva = Reserva.pendiente("est-1", AHORA);
        reserva.cancelar();

        assertThrows(IllegalStateException.class, () -> reserva.cancelar());
    }

    // ---------- Ingreso (CU4) ----------

    @Test
    void unaConfirmadaRegistraElIngresoConLaHoraRecibida() {
        Reserva reserva = Reserva.pendiente("est-1", AHORA);
        reserva.confirmar(AHORA.plusMinutes(2));
        LocalDateTime horaIngreso = AHORA.plusHours(2);

        Asistencia asistencia = reserva.registrarIngreso(horaIngreso);

        assertNotNull(asistencia);
        assertEquals(horaIngreso, asistencia.getHora());
        assertEquals(asistencia, reserva.getAsistencia());
    }

    @Test
    void unaPendienteNoPuedeIngresar() {
        Reserva reserva = Reserva.pendiente("est-1", AHORA);

        assertThrows(IllegalStateException.class,
                () -> reserva.registrarIngreso(AHORA.plusMinutes(1)));
        assertNull(reserva.getAsistencia());
    }

    @Test
    void unaCanceladaNoPuedeIngresar() {
        Reserva reserva = Reserva.pendiente("est-1", AHORA);
        reserva.confirmar(AHORA.plusMinutes(1));
        reserva.cancelar();

        assertThrows(IllegalStateException.class,
                () -> reserva.registrarIngreso(AHORA.plusHours(2)));
    }

    @Test
    void elSegundoIngresoSeRechazaYConservaLaHoraDelPrimero() {
        Reserva reserva = Reserva.pendiente("est-1", AHORA);
        reserva.confirmar(AHORA.plusMinutes(1));
        LocalDateTime primerIngreso = AHORA.plusHours(2);
        reserva.registrarIngreso(primerIngreso);

        assertThrows(IllegalStateException.class,
                () -> reserva.registrarIngreso(AHORA.plusHours(3)));
        assertEquals(primerIngreso, reserva.getAsistencia().getHora());
    }
}
