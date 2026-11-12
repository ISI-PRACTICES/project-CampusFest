package co.edu.unillanos.campusfest.application.usecase;

import java.util.HashMap;
import java.util.Map;

import co.edu.unillanos.campusfest.application.port.RepositorioEventos;
import co.edu.unillanos.campusfest.domain.Evento;

/**
 * Doble de prueba del puerto RepositorioEventos.
 * Guarda los eventos en un mapa y cuenta cuántas veces se llamó a guardar.
 * Lo entregan los docentes: no lo modifique.
 */
class RepositorioEventosFalso implements RepositorioEventos {

    private final Map<String, Evento> eventos = new HashMap<>();
    private int vecesGuardado = 0;

    /** Deja un evento listo antes de la prueba, sin contar como guardado. */
    void agregar(Evento evento) {
        eventos.put(evento.getId(), evento);
    }

    int getVecesGuardado() {
        return vecesGuardado;
    }

    @Override
    public Evento buscar(String eventoId) {
        return eventos.get(eventoId);
    }

    @Override
    public void guardar(Evento evento) {
        eventos.put(evento.getId(), evento);
        vecesGuardado++;
    }
}
