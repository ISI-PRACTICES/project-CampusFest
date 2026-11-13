package co.edu.unillanos.campusfest.application.port;

import co.edu.unillanos.campusfest.domain.Evento;

/**
 * Puerto de salida: lo que los casos de uso necesitan de la persistencia.
 * La interfaz vive en el anillo de casos de uso; su implementación (memoria o H2)
 * vive afuera, en los adaptadores.
 */
public interface RepositorioEventos {

    /** Devuelve el evento, o null si no existe. */
    Evento buscar(String eventoId);

    /** Inserta o actualiza el evento con sus reservas y asistencias. */
    void guardar(Evento evento);
}
