package co.edu.unillanos.campusfest.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Evento estudiantil con aforo limitado. Es el experto en los cupos porque
 * tiene el aforo, los bloqueados y la lista de sus reservas, y es quien crea
 * las reservas: así nadie puede vender más cupos de los que hay.
 */
public class Evento {

    private final String id;
    private String nombre;
    private LocalDateTime fecha;
    private String lugar;
    private int aforo;
    private final int bloqueados;
    private final List<Reserva> reservas = new ArrayList<>();

    public Evento(String id, String nombre, LocalDateTime fecha, String lugar,
                  int aforo, int bloqueados) {
        validarTexto(nombre, "El nombre");
        validarTexto(lugar, "El lugar");
        this.id = id;
        this.nombre = nombre;
        this.fecha = fecha;
        this.lugar = lugar;
        this.aforo = aforo;
        this.bloqueados = bloqueados;
    }

    /** Vuelve a armar un evento ya guardado con sus reservas (la usa el adaptador de persistencia). */
    public Evento(String id, String nombre, LocalDateTime fecha, String lugar,
                  int aforo, int bloqueados, List<Reserva> reservas) {
        this(id, nombre, fecha, lugar, aforo, bloqueados);
        for (Reserva reserva : reservas) {
            this.reservas.add(reserva);
        }
    }

    /** CU1: aforo menos bloqueados y menos las reservas que ocupan cupo en ahora. */
    public int cuposDisponibles(LocalDateTime ahora) {
        return aforo - bloqueados - reservasQueOcupanCupo(ahora);
    }

    /** CU2: crea una reserva pendiente si queda al menos un cupo. */
    public Reserva reservar(String estudianteId, LocalDateTime ahora) {
        if (cuposDisponibles(ahora) <= 0) {
            throw new IllegalStateException("No hay cupos disponibles para este evento");
        }
        Reserva reserva = Reserva.pendiente(estudianteId, ahora);
        reservas.add(reserva);
        return reserva;
    }

    /** CU2 y CU4: la reserva de este evento con ese código. */
    public Reserva buscarReservaPorCodigo(String codigo) {
        for (Reserva reserva : reservas) {
            if (reserva.getCodigo().equals(codigo)) {
                return reserva;
            }
        }
        throw new IllegalArgumentException("El código no pertenece a este evento");
    }

    /** CU3: cambia los datos que no afectan los cupos. */
    public void cambiarDatos(String nombre, LocalDateTime fecha, String lugar) {
        validarTexto(nombre, "El nombre");
        validarTexto(lugar, "El lugar");
        this.nombre = nombre;
        this.fecha = fecha;
        this.lugar = lugar;
    }

    /** CU3: el aforo no puede bajar del aforo mínimo (bloqueados + cupos ocupados por reservas). */
    public void cambiarAforo(int nuevoAforo, LocalDateTime ahora) {
        int ocupadosPorReservas = reservasQueOcupanCupo(ahora);
        int aforoMinimo = bloqueados + ocupadosPorReservas;
        if (nuevoAforo < aforoMinimo) {
            throw new IllegalStateException(
                    "Hay " + ocupadosPorReservas + " cupos ocupados por reservas y "
                    + bloqueados + " bloqueados: el aforo no puede ser menor que " + aforoMinimo);
        }
        this.aforo = nuevoAforo;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getLugar() {
        return lugar;
    }

    public int getAforo() {
        return aforo;
    }

    public int getBloqueados() {
        return bloqueados;
    }

    /** Lista de solo lectura: las reservas solo se agregan con reservar. */
    public List<Reserva> getReservas() {
        return Collections.unmodifiableList(reservas);
    }

    private static void validarTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " no puede estar vacío");
        }
    }

    /** La única regla de ocupación vive en Reserva.ocupaCupo. */
    private int reservasQueOcupanCupo(LocalDateTime ahora) {
        int cantidad = 0;
        for (Reserva reserva : reservas) {
            if (reserva.ocupaCupo(ahora)) {
                cantidad++;
            }
        }
        return cantidad;
    }
}
