package co.edu.unillanos.campusfest.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.unillanos.campusfest.domain.Evento;
import co.edu.unillanos.campusfest.domain.Reserva;

/** CU3 · Editar evento. */
class EditarEventoTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 20, 18, 0);
    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 11, 28, 19, 0);

    private RepositorioEventosFalso repositorio;
    private RelojFijo reloj;
    private Evento evento;
    private EditarEvento editarEvento;

    @BeforeEach
    void preparar() {
        repositorio = new RepositorioEventosFalso();
        reloj = new RelojFijo(AHORA);
        evento = new Evento("ev-1", "Concierto de bienvenida", FECHA,
                "Auditorio Barcelona", 10, 0);
        repositorio.agregar(evento);
        editarEvento = new EditarEvento(repositorio, reloj);
    }

    @Test
    void obtenerDevuelveElEventoParaLlenarElFormulario() {
        assertSame(evento, editarEvento.obtener("ev-1"));
    }

    @Test
    void obtenerUnEventoQueNoExisteSeRechaza() {
        assertThrows(IllegalArgumentException.class,
                () -> editarEvento.obtener("no-existe"));
    }

    @Test
    void editarCambiaLosDatosYGuarda() {
        LocalDateTime nuevaFecha = FECHA.plusDays(1);

        editarEvento.editar("ev-1", "Concierto de cierre", nuevaFecha, "Plazoleta central", 15);

        Evento guardado = repositorio.buscar("ev-1");
        assertEquals("Concierto de cierre", guardado.getNombre());
        assertEquals(nuevaFecha, guardado.getFecha());
        assertEquals("Plazoleta central", guardado.getLugar());
        assertEquals(15, guardado.getAforo());
        assertEquals(1, repositorio.getVecesGuardado());
    }

    @Test
    void editarConAforoMenorQueLosOcupadosSeRechazaYNoGuarda() {
        for (int i = 0; i < 4; i++) {
            Reserva reserva = evento.reservar("est-" + i, AHORA);
            reserva.confirmar(AHORA.plusMinutes(1));
        }

        assertThrows(IllegalStateException.class,
                () -> editarEvento.editar("ev-1", "Concierto de bienvenida", FECHA,
                        "Auditorio Barcelona", 3));
        assertEquals(10, repositorio.buscar("ev-1").getAforo());
        assertEquals(0, repositorio.getVecesGuardado());
    }

    @Test
    void editarUsaElRelojParaSaberQuePendientesSiguenOcupando() {
        evento.reservar("est-1", AHORA);
        evento.reservar("est-2", AHORA);
        reloj.avanzarMinutos(10);

        editarEvento.editar("ev-1", "Concierto de bienvenida", FECHA, "Auditorio Barcelona", 0);

        assertEquals(0, repositorio.buscar("ev-1").getAforo());
    }

    @Test
    void editarUnEventoQueNoExisteSeRechaza() {
        assertThrows(IllegalArgumentException.class,
                () -> editarEvento.editar("no-existe", "X", FECHA, "Y", 10));
    }
}
