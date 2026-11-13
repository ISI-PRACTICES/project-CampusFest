package co.edu.unillanos.campusfest.infrastructure.reloj;

import java.time.LocalDateTime;

import co.edu.unillanos.campusfest.application.port.Reloj;

/**
 * Adaptador del puerto Reloj que lee la hora real del computador.
 * Es la única clase del proyecto que llama a now(): las entidades y los casos de
 * uso reciben la hora como parámetro o se la piden al puerto.
 */
public class RelojDelSistema implements Reloj {

    @Override
    public LocalDateTime ahora() {
        return LocalDateTime.now();
    }
}
