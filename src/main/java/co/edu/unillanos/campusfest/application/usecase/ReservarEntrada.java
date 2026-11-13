package co.edu.unillanos.campusfest.application.usecase;

import java.time.LocalDateTime;

import co.edu.unillanos.campusfest.application.port.RepositorioEventos;
import co.edu.unillanos.campusfest.application.port.Reloj;
import co.edu.unillanos.campusfest.domain.Evento;
import co.edu.unillanos.campusfest.domain.Reserva;

/**
 * CU2 · Reservar y confirmar una entrada. Solo coordina: las reglas (cupos y
 * plazo de 10 minutos) las aplican Evento y Reserva.
 */
public class ReservarEntrada {

    private final RepositorioEventos repositorio;
    private final Reloj reloj;

    public ReservarEntrada(RepositorioEventos repositorio, Reloj reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    public Reserva solicitarReserva(String eventoId, String estudianteId) {
        LocalDateTime ahora = reloj.ahora();
        Evento evento = buscarEvento(eventoId);

        Reserva reserva = evento.reservar(estudianteId, ahora);

        repositorio.guardar(evento);
        return reserva;
    }

    public Reserva confirmarReserva(String eventoId, String codigoReserva) {
        LocalDateTime ahora = reloj.ahora();
        Evento evento = buscarEvento(eventoId);
        Reserva reserva = evento.buscarReservaPorCodigo(codigoReserva);

        reserva.confirmar(ahora);

        repositorio.guardar(evento);
        return reserva;
    }

    private Evento buscarEvento(String eventoId) {
        Evento evento = repositorio.buscar(eventoId);
        if (evento == null) {
            throw new IllegalArgumentException("El evento no existe: " + eventoId);
        }
        return evento;
    }
}
