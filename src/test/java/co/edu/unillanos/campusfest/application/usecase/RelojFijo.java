package co.edu.unillanos.campusfest.application.usecase;

import java.time.LocalDateTime;

import co.edu.unillanos.campusfest.application.port.Reloj;

/**
 * Doble de prueba del puerto Reloj: devuelve la hora que la prueba le indique.
 * Permite probar el plazo de 10 minutos sin esperar 10 minutos.
 * Lo entregan los docentes: no lo modifique.
 */
class RelojFijo implements Reloj {

    private LocalDateTime hora;

    RelojFijo(LocalDateTime hora) {
        this.hora = hora;
    }

    void avanzarMinutos(int minutos) {
        hora = hora.plusMinutes(minutos);
    }

    @Override
    public LocalDateTime ahora() {
        return hora;
    }
}
