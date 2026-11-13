package co.edu.unillanos.campusfest.application.port;

import java.time.LocalDateTime;

/**
 * Puerto de salida: la hora actual. Los casos de uso le piden la hora al reloj en
 * lugar de leer la del sistema, y así las pruebas pueden fijarla.
 */
public interface Reloj {

    LocalDateTime ahora();
}
