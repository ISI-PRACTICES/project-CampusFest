package co.edu.unillanos.campusfest.infrastructure.persistence;

import co.edu.unillanos.campusfest.application.port.RepositorioEventos;
import co.edu.unillanos.campusfest.domain.Asistencia;
import co.edu.unillanos.campusfest.domain.EstadoReserva;
import co.edu.unillanos.campusfest.domain.Evento;
import co.edu.unillanos.campusfest.domain.Reserva;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador de persistencia que almacena eventos, reservas
 * y asistencias en una base de datos H2.
 */
public class RepositorioEventosH2 implements RepositorioEventos {

    private final Connection conexion;

    public RepositorioEventosH2(Connection conexion) {
        this.conexion = conexion;
        inicializarEsquema();
    }

    /**
     * Ejecuta el esquema SQL al crear el repositorio.
     */
    private void inicializarEsquema() {
        try (InputStream recurso = getClass().getResourceAsStream("/db/schema.sql")) {

            if (recurso == null) {
                throw new IllegalStateException(
                        "No se encontró el archivo /db/schema.sql");
            }

            try (BufferedReader lector = new BufferedReader(
                    new InputStreamReader(recurso, StandardCharsets.UTF_8));
                 Statement sentencia = conexion.createStatement()) {

                StringBuilder contenido = new StringBuilder();
                String linea;

                while ((linea = lector.readLine()) != null) {
                    contenido.append(linea).append("\n");
                }

                String[] instrucciones = contenido.toString().split(";");

                for (String sql : instrucciones) {
                    if (!sql.isBlank()) {
                        sentencia.execute(sql);
                    }
                }
            }

        } catch (IOException | SQLException e) {
            throw new IllegalStateException(
                    "No fue posible inicializar el esquema de la base de datos", e);
        }
    }

    /**
     * Busca un evento y reconstruye sus reservas y asistencias.
     *
     * @return el evento reconstruido o null si no existe.
     */
    @Override
    public Evento buscar(String eventoId) {
        String sqlEvento = """
                SELECT ID, NOMBRE, FECHA, LUGAR, AFORO, BLOQUEADOS
                FROM EVENTO
                WHERE ID = ?
                """;

        try (PreparedStatement sentencia = conexion.prepareStatement(sqlEvento)) {
            sentencia.setString(1, eventoId);

            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) {
                    return null;
                }

                String id = resultado.getString("ID");
                String nombre = resultado.getString("NOMBRE");
                var fecha = resultado.getTimestamp("FECHA").toLocalDateTime();
                String lugar = resultado.getString("LUGAR");
                int aforo = resultado.getInt("AFORO");
                int bloqueados = resultado.getInt("BLOQUEADOS");

                List<Reserva> reservas = buscarReservas(id);

                return new Evento(
                        id, nombre, fecha, lugar, aforo, bloqueados, reservas);
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No fue posible consultar el evento en la base de datos", e);
        }
    }

    /**
     * Recupera todas las reservas de un evento, incluyendo sus asistencias.
     */
    private List<Reserva> buscarReservas(String eventoId) throws SQLException {
        String sql = """
                SELECT R.CODIGO, R.ESTUDIANTE_ID, R.VENCE_EN, R.ESTADO,
                       A.HORA AS HORA_ASISTENCIA
                FROM RESERVA R
                LEFT JOIN ASISTENCIA A
                    ON R.CODIGO = A.RESERVA_CODIGO
                WHERE R.EVENTO_ID = ?
                """;

        List<Reserva> reservas = new ArrayList<>();

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, eventoId);

            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    String codigo = resultado.getString("CODIGO");
                    String estudianteId = resultado.getString("ESTUDIANTE_ID");
                    var venceEn = resultado.getTimestamp("VENCE_EN").toLocalDateTime();
                    EstadoReserva estado = EstadoReserva.valueOf(
                            resultado.getString("ESTADO"));

                    Timestamp horaAsistencia =
                            resultado.getTimestamp("HORA_ASISTENCIA");

                    Asistencia asistencia = horaAsistencia == null
                            ? null
                            : new Asistencia(horaAsistencia.toLocalDateTime());

                    Reserva reserva = Reserva.reconstruir(
                            codigo, estudianteId, venceEn, estado, asistencia);

                    reservas.add(reserva);
                }
            }
        }

        return reservas;
    }

    /**
     * Inserta o actualiza un evento y todas sus reservas y asistencias.
     */
    @Override
    public void guardar(Evento evento) {
        try {
            boolean autocommitOriginal = conexion.getAutoCommit();
            Savepoint puntoGuardado = null;

            if (autocommitOriginal) {
                conexion.setAutoCommit(false);
            } else {
                puntoGuardado = conexion.setSavepoint();
            }

            try {
                guardarEvento(evento);

                for (Reserva reserva : evento.getReservas()) {
                    guardarReserva(evento.getId(), reserva);

                    if (reserva.getAsistencia() != null) {
                        guardarAsistencia(
                                reserva.getCodigo(), reserva.getAsistencia());
                    }
                }

                if (autocommitOriginal) {
                    conexion.commit();
                } else {
                    conexion.releaseSavepoint(puntoGuardado);
                }

            } catch (SQLException e) {
                try {
                    if (autocommitOriginal) {
                        conexion.rollback();
                    } else {
                        conexion.rollback(puntoGuardado);
                    }
                } catch (SQLException errorRollback) {
                    e.addSuppressed(errorRollback);
                }

                throw e;

            } finally {
                if (autocommitOriginal) {
                    conexion.setAutoCommit(true);
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No fue posible guardar el evento en la base de datos", e);
        }
    }

    private void guardarEvento(Evento evento) throws SQLException {
        String actualizar = """
                UPDATE EVENTO
                SET NOMBRE = ?, FECHA = ?, LUGAR = ?, AFORO = ?, BLOQUEADOS = ?
                WHERE ID = ?
                """;

        try (PreparedStatement sentencia = conexion.prepareStatement(actualizar)) {
            sentencia.setString(1, evento.getNombre());
            sentencia.setTimestamp(2, Timestamp.valueOf(evento.getFecha()));
            sentencia.setString(3, evento.getLugar());
            sentencia.setInt(4, evento.getAforo());
            sentencia.setInt(5, evento.getBloqueados());
            sentencia.setString(6, evento.getId());

            if (sentencia.executeUpdate() == 0) {
                String insertar = """
                        INSERT INTO EVENTO
                        (ID, NOMBRE, FECHA, LUGAR, AFORO, BLOQUEADOS)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """;

                try (PreparedStatement insercion =
                             conexion.prepareStatement(insertar)) {
                    insercion.setString(1, evento.getId());
                    insercion.setString(2, evento.getNombre());
                    insercion.setTimestamp(
                            3, Timestamp.valueOf(evento.getFecha()));
                    insercion.setString(4, evento.getLugar());
                    insercion.setInt(5, evento.getAforo());
                    insercion.setInt(6, evento.getBloqueados());
                    insercion.executeUpdate();
                }
            }
        }
    }

    private void guardarReserva(String eventoId, Reserva reserva)
            throws SQLException {

        String actualizar = """
                UPDATE RESERVA
                SET EVENTO_ID = ?, ESTUDIANTE_ID = ?, VENCE_EN = ?, ESTADO = ?
                WHERE CODIGO = ?
                """;

        try (PreparedStatement sentencia = conexion.prepareStatement(actualizar)) {
            sentencia.setString(1, eventoId);
            sentencia.setString(2, reserva.getEstudianteId());
            sentencia.setTimestamp(
                    3, Timestamp.valueOf(reserva.getVenceEn()));
            sentencia.setString(4, reserva.getEstado().name());
            sentencia.setString(5, reserva.getCodigo());

            if (sentencia.executeUpdate() == 0) {
                String insertar = """
                        INSERT INTO RESERVA
                        (CODIGO, EVENTO_ID, ESTUDIANTE_ID, VENCE_EN, ESTADO)
                        VALUES (?, ?, ?, ?, ?)
                        """;

                try (PreparedStatement insercion =
                             conexion.prepareStatement(insertar)) {
                    insercion.setString(1, reserva.getCodigo());
                    insercion.setString(2, eventoId);
                    insercion.setString(3, reserva.getEstudianteId());
                    insercion.setTimestamp(
                            4, Timestamp.valueOf(reserva.getVenceEn()));
                    insercion.setString(5, reserva.getEstado().name());
                    insercion.executeUpdate();
                }
            }
        }
    }

    private void guardarAsistencia(String codigoReserva, Asistencia asistencia)
            throws SQLException {

        String actualizar = """
                UPDATE ASISTENCIA
                SET HORA = ?
                WHERE RESERVA_CODIGO = ?
                """;

        try (PreparedStatement sentencia = conexion.prepareStatement(actualizar)) {
            sentencia.setTimestamp(
                    1, Timestamp.valueOf(asistencia.getHora()));
            sentencia.setString(2, codigoReserva);

            if (sentencia.executeUpdate() == 0) {
                String insertar = """
                        INSERT INTO ASISTENCIA (RESERVA_CODIGO, HORA)
                        VALUES (?, ?)
                        """;

                try (PreparedStatement insercion =
                             conexion.prepareStatement(insertar)) {
                    insercion.setString(1, codigoReserva);
                    insercion.setTimestamp(
                            2, Timestamp.valueOf(asistencia.getHora()));
                    insercion.executeUpdate();
                }
            }
        }
    }
}
