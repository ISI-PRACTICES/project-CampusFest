package co.edu.unillanos.campusfest.application.usecase;

import java.time.LocalDateTime;

import co.edu.unillanos.campusfest.application.port.RepositorioEventos;
import co.edu.unillanos.campusfest.application.port.Reloj;
import co.edu.unillanos.campusfest.domain.Asistencia;
import co.edu.unillanos.campusfest.domain.Evento;
import co.edu.unillanos.campusfest.domain.Reserva;

/**
 * CU4 · Registrar el ingreso con QR. El código del QR llega como String: el
 * lector (cámara o USB) es un adaptador y no es parte de este caso de uso.
 * Solo coordina: Reserva decide si puede ingresar y crea la Asistencia.
 */
public class RegistrarIngreso {

    private final RepositorioEventos repositorio;
    private final Reloj reloj;

    public RegistrarIngreso(RepositorioEventos repositorio, Reloj reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    public Asistencia registrar(String eventoId, String codigo) {
        LocalDateTime ahora = reloj.ahora();
        Evento evento = repositorio.buscar(eventoId);
        if (evento == null) {
            throw new IllegalArgumentException("El evento no existe: " + eventoId);
        }
        Reserva reserva = evento.buscarReservaPorCodigo(codigo);

        Asistencia asistencia = reserva.registrarIngreso(ahora);

        repositorio.guardar(evento);
        return asistencia;
    }
}
