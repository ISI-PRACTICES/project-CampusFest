package co.edu.unillanos.campusfest.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.unillanos.campusfest.domain.Evento;

/** CU1 · Consultar cupos. */
class ConsultarCuposTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 20, 18, 0);

    private RepositorioEventosFalso repositorio;
    private RelojFijo reloj;
    private Evento evento;
    private ConsultarCupos consultarCupos;

    @BeforeEach
    void preparar() {
        repositorio = new RepositorioEventosFalso();
        reloj = new RelojFijo(AHORA);
        evento = new Evento("ev-1", "Concierto de bienvenida", AHORA.plusDays(8),
                "Auditorio Barcelona", 30, 4);
        repositorio.agregar(evento);
        consultarCupos = new ConsultarCupos(repositorio, reloj);
    }

    @Test
    void devuelveLosCuposLibresDelEvento() {
        assertEquals(26, consultarCupos.consultar("ev-1"));
    }

    @Test
    void descuentaLasReservasVigentes() {
        evento.reservar("est-1", AHORA);
        evento.reservar("est-2", AHORA);

        assertEquals(24, consultarCupos.consultar("ev-1"));
    }

    @Test
    void usaLaHoraDelRelojParaLiberarLasPendientesVencidas() {
        evento.reservar("est-1", AHORA);
        reloj.avanzarMinutos(10);

        assertEquals(26, consultarCupos.consultar("ev-1"));
    }

    @Test
    void consultarNoGuardaNada() {
        consultarCupos.consultar("ev-1");

        assertEquals(0, repositorio.getVecesGuardado());
    }

    @Test
    void unEventoQueNoExisteSeRechaza() {
        assertThrows(IllegalArgumentException.class,
                () -> consultarCupos.consultar("no-existe"));
    }
}
