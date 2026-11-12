package co.edu.unillanos.campusfest.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.unillanos.campusfest.domain.EstadoReserva;
import co.edu.unillanos.campusfest.domain.Evento;
import co.edu.unillanos.campusfest.domain.Reserva;

/** CU2 · Reservar y confirmar una entrada. */
class ReservarEntradaTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 20, 18, 0);

    private RepositorioEventosFalso repositorio;
    private RelojFijo reloj;
    private ReservarEntrada reservarEntrada;

    @BeforeEach
    void preparar() {
        repositorio = new RepositorioEventosFalso();
        reloj = new RelojFijo(AHORA);
        repositorio.agregar(new Evento("ev-1", "Concierto de bienvenida", AHORA.plusDays(8),
                "Auditorio Barcelona", 2, 0));
        reservarEntrada = new ReservarEntrada(repositorio, reloj);
    }

    // ---------- Solicitar ----------

    @Test
    void solicitarCreaUnaPendienteQueVenceEnDiezMinutosSegunElReloj() {
        Reserva reserva = reservarEntrada.solicitarReserva("ev-1", "est-1");

        assertEquals(EstadoReserva.PENDIENTE, reserva.getEstado());
        assertEquals("est-1", reserva.getEstudianteId());
        assertEquals(AHORA.plusMinutes(10), reserva.getVenceEn());
    }

    @Test
    void solicitarGuardaElEventoYOcupaUnCupo() {
        reservarEntrada.solicitarReserva("ev-1", "est-1");

        assertEquals(1, repositorio.getVecesGuardado());
        assertEquals(1, repositorio.buscar("ev-1").cuposDisponibles(AHORA));
    }

    @Test
    void sinCuposLaSolicitudSeRechaza() {
        reservarEntrada.solicitarReserva("ev-1", "est-1");
        reservarEntrada.solicitarReserva("ev-1", "est-2");

        assertThrows(IllegalStateException.class,
                () -> reservarEntrada.solicitarReserva("ev-1", "est-3"));
    }

    @Test
    void solicitarEnUnEventoQueNoExisteSeRechaza() {
        assertThrows(IllegalArgumentException.class,
                () -> reservarEntrada.solicitarReserva("no-existe", "est-1"));
    }

    // ---------- Confirmar ----------

    @Test
    void confirmarAntesDelPlazoLaDejaConfirmadaYGuarda() {
        Reserva reserva = reservarEntrada.solicitarReserva("ev-1", "est-1");
        reloj.avanzarMinutos(9);

        Reserva confirmada = reservarEntrada.confirmarReserva("ev-1", reserva.getCodigo());

        assertEquals(EstadoReserva.CONFIRMADA, confirmada.getEstado());
        assertEquals(2, repositorio.getVecesGuardado());
        Reserva guardada = repositorio.buscar("ev-1").buscarReservaPorCodigo(reserva.getCodigo());
        assertEquals(EstadoReserva.CONFIRMADA, guardada.getEstado());
    }

    @Test
    void confirmarALosDiezMinutosSeRechazaYElCupoQuedaLibre() {
        Reserva reserva = reservarEntrada.solicitarReserva("ev-1", "est-1");
        reloj.avanzarMinutos(10);

        assertThrows(IllegalStateException.class,
                () -> reservarEntrada.confirmarReserva("ev-1", reserva.getCodigo()));
        assertEquals(2, repositorio.buscar("ev-1").cuposDisponibles(reloj.ahora()));
    }

    @Test
    void confirmarUnCodigoQueNoEsDelEventoSeRechaza() {
        assertThrows(IllegalArgumentException.class,
                () -> reservarEntrada.confirmarReserva("ev-1", "codigo-inventado"));
    }
}
