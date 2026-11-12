package co.edu.unillanos.campusfest.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Reserva de una entrada. Nace PENDIENTE y tiene 10 minutos para confirmarse;
 * pasado el plazo deja de ocupar cupo. El vencimiento se deduce de la hora que
 * recibe cada método, por eso ningún método de esta clase lee el reloj.
 */
public class Reserva {

    private static final int PLAZO_MINUTOS = 10;

    private final String codigo;
    private final String estudianteId;
    private final LocalDateTime venceEn;
    private EstadoReserva estado;
    private Asistencia asistencia;

    private Reserva(String estudianteId, LocalDateTime venceEn) {
        this.codigo = UUID.randomUUID().toString();
        this.estudianteId = estudianteId;
        this.venceEn = venceEn;
        this.estado = EstadoReserva.PENDIENTE;
    }

    /** Crea una reserva pendiente que vence 10 minutos después de ahora. */
    public static Reserva pendiente(String estudianteId, LocalDateTime ahora) {
        return new Reserva(estudianteId, ahora.plusMinutes(PLAZO_MINUTOS));
    }

    /** Una confirmada ocupa cupo; una pendiente, solo mientras no venza. */
    public boolean ocupaCupo(LocalDateTime ahora) {
        if (estado == EstadoReserva.CONFIRMADA) {
            return true;
        }
        return estado == EstadoReserva.PENDIENTE && !haVencido(ahora);
    }

    /** Estado visible en el momento ahora: una pendiente fuera de plazo se ve VENCIDA. */
    public EstadoReserva estadoEn(LocalDateTime ahora) {
        if (estado == EstadoReserva.PENDIENTE && haVencido(ahora)) {
            return EstadoReserva.VENCIDA;
        }
        return estado;
    }

    public void confirmar(LocalDateTime ahora) {
        if (estado != EstadoReserva.PENDIENTE) {
            throw new IllegalStateException(
                    "Solo se puede confirmar una reserva pendiente; esta está " + estado);
        }
        if (haVencido(ahora)) {
            throw new IllegalStateException(
                    "La reserva venció: el plazo de " + PLAZO_MINUTOS + " minutos terminó");
        }
        estado = EstadoReserva.CONFIRMADA;
    }

    public void cancelar() {
        if (estado != EstadoReserva.PENDIENTE && estado != EstadoReserva.CONFIRMADA) {
            throw new IllegalStateException(
                    "Solo se puede cancelar una reserva pendiente o confirmada; esta está " + estado);
        }
        estado = EstadoReserva.CANCELADA;
    }

    public Asistencia registrarIngreso(LocalDateTime ahora) {
        if (estado != EstadoReserva.CONFIRMADA) {
            throw new IllegalStateException(
                    "Solo una reserva confirmada puede ingresar; esta está " + estado);
        }
        if (asistencia != null) {
            throw new IllegalStateException(
                    "Esta reserva ya registró su ingreso a las " + asistencia.getHora());
        }
        asistencia = new Asistencia(ahora);
        return asistencia;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getEstudianteId() {
        return estudianteId;
    }

    public LocalDateTime getVenceEn() {
        return venceEn;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public Asistencia getAsistencia() {
        return asistencia;
    }

    /** A los 10:00 exactos ya está vencida. */
    private boolean haVencido(LocalDateTime ahora) {
        return !ahora.isBefore(venceEn);
    }
}
