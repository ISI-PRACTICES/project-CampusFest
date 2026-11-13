package co.edu.unillanos.campusfest.domain;

/**
 * Estados de una reserva.
 *
 * PENDIENTE -> CONFIRMADA o VENCIDA
 * PENDIENTE o CONFIRMADA -> CANCELADA
 */
public enum EstadoReserva {
    PENDIENTE,
    CONFIRMADA,
    VENCIDA,
    CANCELADA
}
