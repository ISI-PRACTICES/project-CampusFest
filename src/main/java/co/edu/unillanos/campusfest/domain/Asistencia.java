package co.edu.unillanos.campusfest.domain;

import java.time.LocalDateTime;

/** Registro de que un estudiante ingresó al evento con su reserva. */
public class Asistencia {

    private final LocalDateTime hora;

    public Asistencia(LocalDateTime hora) {
        this.hora = hora;
    }

    public LocalDateTime getHora() {
        return hora;
    }
}
