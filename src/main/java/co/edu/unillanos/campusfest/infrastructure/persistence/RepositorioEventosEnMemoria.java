package co.edu.unillanos.campusfest.infrastructure.persistence;

import java.util.HashMap;
import java.util.Map;

import co.edu.unillanos.campusfest.application.port.RepositorioEventos;
import co.edu.unillanos.campusfest.domain.Evento;

/**
 * Adaptador del puerto RepositorioEventos que guarda los eventos en un mapa.
 * Sirve para pruebas y para ejecutar la aplicación sin base de datos.
 */
public class RepositorioEventosEnMemoria implements RepositorioEventos {

    private final Map<String, Evento> eventos = new HashMap<>();

    public RepositorioEventosEnMemoria() {
    }

    @Override
    public Evento buscar(String eventoId) {
        return eventos.get(eventoId);
    }

    @Override
    public void guardar(Evento evento) {
        eventos.put(evento.getId(), evento);
    }
}
