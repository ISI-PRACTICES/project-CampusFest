package co.edu.unillanos.campusfest.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import co.edu.unillanos.campusfest.application.port.RepositorioEventos;
import co.edu.unillanos.campusfest.domain.Evento;

/** Adaptador en memoria del puerto RepositorioEventos. */
class RepositorioEventosEnMemoriaTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 11, 28, 19, 0);

    @Test
    void implementaElPuerto() {
        assertTrue(new RepositorioEventosEnMemoria() instanceof RepositorioEventos);
    }

    @Test
    void buscaLoQueSeGuardo() {
        RepositorioEventos repositorio = new RepositorioEventosEnMemoria();
        repositorio.guardar(new Evento("ev-1", "Concierto", FECHA, "Auditorio", 10, 0));

        Evento encontrado = repositorio.buscar("ev-1");

        assertEquals("ev-1", encontrado.getId());
        assertEquals("Concierto", encontrado.getNombre());
    }

    @Test
    void guardarDosVecesElMismoEventoLoActualiza() {
        RepositorioEventos repositorio = new RepositorioEventosEnMemoria();
        Evento evento = new Evento("ev-1", "Concierto", FECHA, "Auditorio", 10, 0);
        repositorio.guardar(evento);

        evento.cambiarDatos("Concierto de cierre", FECHA, "Plazoleta");
        repositorio.guardar(evento);

        assertEquals("Concierto de cierre", repositorio.buscar("ev-1").getNombre());
    }

    @Test
    void buscarUnEventoQueNoExisteDevuelveNull() {
        RepositorioEventos repositorio = new RepositorioEventosEnMemoria();

        assertNull(repositorio.buscar("no-existe"));
    }
}
