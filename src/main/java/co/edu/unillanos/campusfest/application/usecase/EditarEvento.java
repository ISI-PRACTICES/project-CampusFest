package co.edu.unillanos.campusfest.application.usecase;

import java.time.LocalDateTime;

import co.edu.unillanos.campusfest.application.port.RepositorioEventos;
import co.edu.unillanos.campusfest.application.port.Reloj;
import co.edu.unillanos.campusfest.domain.Evento;

/**
 * CU3 · Editar evento. Solo coordina: la regla del aforo mínimo la verifica
 * Evento.cambiarAforo.
 */
public class EditarEvento {

    private final RepositorioEventos repositorio;
    private final Reloj reloj;

    public EditarEvento(RepositorioEventos repositorio, Reloj reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    /** Devuelve el evento para llenar el formulario de edición. */
    public Evento obtener(String eventoId) {
        return buscarEvento(eventoId);
    }

    public void editar(String eventoId, String nombre, LocalDateTime fecha,
                       String lugar, int nuevoAforo) {
        LocalDateTime ahora = reloj.ahora();
        Evento evento = buscarEvento(eventoId);

        evento.cambiarDatos(nombre, fecha, lugar);
        evento.cambiarAforo(nuevoAforo, ahora);

        repositorio.guardar(evento);
    }

    private Evento buscarEvento(String eventoId) {
        Evento evento = repositorio.buscar(eventoId);
        if (evento == null) {
            throw new IllegalArgumentException("El evento no existe: " + eventoId);
        }
        return evento;
    }
}
