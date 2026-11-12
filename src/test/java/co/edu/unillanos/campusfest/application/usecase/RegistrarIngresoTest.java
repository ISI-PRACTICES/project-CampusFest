package co.edu.unillanos.campusfest.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.unillanos.campusfest.domain.Asistencia;
import co.edu.unillanos.campusfest.domain.Evento;
import co.edu.unillanos.campusfest.domain.Reserva;

/** CU4 · Registrar el ingreso con QR. El código QR llega como String. */
class RegistrarIngresoTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 28, 18, 30);

    private RepositorioEventosFalso repositorio;
    private RelojFijo reloj;
    private Evento evento;
    private RegistrarIngreso registrarIngreso;

    @BeforeEach
    void preparar() {
        repositorio = new RepositorioEventosFalso();
        reloj = new RelojFijo(AHORA);
        evento = new Evento("ev-1", "Concierto de bienvenida", AHORA.plusMinutes(30),
                "Auditorio Barcelona", 10, 0);
        repositorio.agregar(evento);
        registrarIngreso = new RegistrarIngreso(repositorio, reloj);
    }

    private Reserva reservaConfirmada(String estudianteId) {
        Reserva reserva = evento.reservar(estudianteId, AHORA.minusDays(2));
        reserva.confirmar(AHORA.minusDays(2).plusMinutes(3));
        return reserva;
    }

    @Test
    void unaReservaConfirmadaIngresaConLaHoraDelRelojYSeGuarda() {
        Reserva reserva = reservaConfirmada("est-1");

        Asistencia asistencia = registrarIngreso.registrar("ev-1", reserva.getCodigo());

        assertNotNull(asistencia);
        assertEquals(AHORA, asistencia.getHora());
        assertEquals(1, repositorio.getVecesGuardado());
        assertNotNull(repositorio.buscar("ev-1")
                .buscarReservaPorCodigo(reserva.getCodigo()).getAsistencia());
    }

    @Test
    void elSegundoIngresoSeRechazaYConservaElPrimero() {
        Reserva reserva = reservaConfirmada("est-1");
        registrarIngreso.registrar("ev-1", reserva.getCodigo());
        reloj.avanzarMinutos(20);

        assertThrows(IllegalStateException.class,
                () -> registrarIngreso.registrar("ev-1", reserva.getCodigo()));
        assertEquals(AHORA, reserva.getAsistencia().getHora());
        assertEquals(1, repositorio.getVecesGuardado());
    }

    @Test
    void unaReservaNoConfirmadaSeRechaza() {
        Reserva pendiente = evento.reservar("est-1", AHORA.minusMinutes(2));

        assertThrows(IllegalStateException.class,
                () -> registrarIngreso.registrar("ev-1", pendiente.getCodigo()));
        assertNull(pendiente.getAsistencia());
        assertEquals(0, repositorio.getVecesGuardado());
    }

    @Test
    void unCodigoDeOtroEventoSeRechaza() {
        Evento otroEvento = new Evento("ev-2", "Feria de emprendimiento", AHORA.plusHours(1),
                "Coliseo", 50, 0);
        repositorio.agregar(otroEvento);
        Reserva reservaDeOtroEvento = otroEvento.reservar("est-1", AHORA.minusDays(1));
        reservaDeOtroEvento.confirmar(AHORA.minusDays(1).plusMinutes(1));

        assertThrows(IllegalArgumentException.class,
                () -> registrarIngreso.registrar("ev-1", reservaDeOtroEvento.getCodigo()));
        assertNull(reservaDeOtroEvento.getAsistencia());
    }

    @Test
    void unEventoQueNoExisteSeRechaza() {
        assertThrows(IllegalArgumentException.class,
                () -> registrarIngreso.registrar("no-existe", "cualquier-codigo"));
    }
}
