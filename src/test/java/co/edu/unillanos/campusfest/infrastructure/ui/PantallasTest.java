package co.edu.unillanos.campusfest.infrastructure.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import co.edu.unillanos.campusfest.application.usecase.ConsultarCupos;
import co.edu.unillanos.campusfest.application.usecase.EditarEvento;
import co.edu.unillanos.campusfest.application.usecase.RegistrarIngreso;
import co.edu.unillanos.campusfest.application.usecase.ReservarEntrada;
import javafx.fxml.FXML;

/**
 * Revisión estática de las pantallas JavaFX: lee los archivos FXML y el
 * código de los controladores sin abrir ninguna ventana (el agente que
 * califica no tiene pantalla).
 *
 * Comprueba que cada pantalla está conectada con su controlador, que el
 * controlador recibe y usa su caso de uso, que solo lee la pantalla, llama
 * al caso de uso y muestra el resultado o el error, y que App ensambla todo.
 */
class PantallasTest {

    private static final String VISTAS = "src/main/resources/co/edu/unillanos/campusfest/vistas/";
    private static final String UI = "src/main/java/co/edu/unillanos/campusfest/infrastructure/ui/";
    private static final String CODIGO = "src/main/java/co/edu/unillanos/campusfest/";
    private static final String APP = CODIGO + "App.java";
    private static final String PAQUETE_UI = "co.edu.unillanos.campusfest.infrastructure.ui.";

    /** Una fila por caso de uso: FXML, controlador, caso de uso y operaciones que debe llamar. */
    private static final Pantalla[] PANTALLAS = {
        new Pantalla("pantalla-cupos.fxml", "PantallaCuposController", ConsultarCupos.class,
                new String[] {"consultar"}),
        new Pantalla("pantalla-reserva.fxml", "PantallaReservaController", ReservarEntrada.class,
                new String[] {"solicitarReserva", "confirmarReserva"}),
        new Pantalla("pantalla-evento.fxml", "PantallaEventoController", EditarEvento.class,
                new String[] {"obtener", "editar"}),
        new Pantalla("pantalla-ingreso.fxml", "PantallaIngresoController", RegistrarIngreso.class,
                new String[] {"registrar"})
    };

    // ---------- Archivos ----------

    @Test
    void cadaCasoDeUsoTieneSuFxmlYSuControlador() {
        List<String> faltas = new ArrayList<>();
        for (Pantalla p : PANTALLAS) {
            if (!new File(VISTAS + p.fxml).isFile()) {
                faltas.add("Falta " + VISTAS + p.fxml);
            }
            if (!new File(UI + p.controlador + ".java").isFile()) {
                faltas.add("Falta " + UI + p.controlador + ".java");
            }
        }
        assertTrue(faltas.isEmpty(), unir(faltas));
    }

    @Test
    void elCssEstaJuntoALasVistasYUsaPropiedadesConPrefijoFx() throws IOException {
        List<String> faltas = new ArrayList<>();
        List<File> hojas = new ArrayList<>();
        File[] archivos = new File(VISTAS).listFiles();
        if (archivos != null) {
            for (File archivo : archivos) {
                if (archivo.getName().endsWith(".css")) {
                    hojas.add(archivo);
                }
            }
        }
        if (hojas.isEmpty()) {
            faltas.add("No hay ningún archivo .css en " + VISTAS);
        }
        for (File hoja : hojas) {
            List<String> lineas = Files.readAllLines(hoja.toPath());
            for (int i = 0; i < lineas.size(); i++) {
                String linea = lineas.get(i).trim();
                if (linea.startsWith("/*") || linea.startsWith("*") || !linea.contains(":")
                        || !linea.endsWith(";")) {
                    continue;
                }
                String propiedad = linea.substring(0, linea.indexOf(':')).trim();
                if (!propiedad.startsWith("-fx-")) {
                    faltas.add(hoja.getName() + ":" + (i + 1) + "  la propiedad '" + propiedad
                            + "' no tiene el prefijo -fx- y JavaFX la ignora");
                }
            }
        }
        assertTrue(faltas.isEmpty(), unir(faltas));
    }

    // ---------- FXML <-> controlador ----------

    @Test
    void losFxmlNoDeclaranFxControllerPorqueAppAsignaElControlador() throws Exception {
        List<String> faltas = new ArrayList<>();
        for (Pantalla p : PANTALLAS) {
            for (Element elemento : elementos(p.fxml)) {
                if (elemento.hasAttribute("fx:controller")) {
                    faltas.add(p.fxml + " declara fx:controller; App debe asignarlo con setController");
                }
            }
        }
        assertTrue(faltas.isEmpty(), unir(faltas));
    }

    @Test
    void cadaFxIdDelFxmlEsUnCampoPrivadoConFxmlEnElControlador() throws Exception {
        List<String> faltas = new ArrayList<>();
        for (Pantalla p : PANTALLAS) {
            Class<?> clase = cargar(p.controlador);
            for (Element elemento : elementos(p.fxml)) {
                String id = elemento.getAttribute("fx:id");
                if (id.isEmpty()) {
                    continue;
                }
                Field campo = campo(clase, id);
                if (campo == null) {
                    faltas.add(p.fxml + ": fx:id=\"" + id + "\" no es un campo de " + p.controlador);
                } else if (!campo.isAnnotationPresent(FXML.class) || !Modifier.isPrivate(campo.getModifiers())) {
                    faltas.add(p.controlador + "." + id + " debe ser private y llevar @FXML");
                }
            }
        }
        assertTrue(faltas.isEmpty(), unir(faltas));
    }

    @Test
    void cadaAccionDelFxmlEsUnMetodoConFxmlEnElControlador() throws Exception {
        List<String> faltas = new ArrayList<>();
        for (Pantalla p : PANTALLAS) {
            Class<?> clase = cargar(p.controlador);
            int acciones = 0;
            for (Element elemento : elementos(p.fxml)) {
                NamedNodeMap atributos = elemento.getAttributes();
                for (int i = 0; i < atributos.getLength(); i++) {
                    Node atributo = atributos.item(i);
                    String nombre = atributo.getNodeName();
                    String valor = atributo.getNodeValue();
                    if (!nombre.startsWith("on") || !valor.startsWith("#")) {
                        continue;
                    }
                    acciones++;
                    String metodo = valor.substring(1);
                    Method encontrado = metodo(clase, metodo);
                    if (encontrado == null) {
                        faltas.add(p.fxml + ": " + nombre + "=\"" + valor + "\" no es un método de "
                                + p.controlador);
                    } else if (!encontrado.isAnnotationPresent(FXML.class)) {
                        faltas.add(p.controlador + "." + metodo + "() debe llevar @FXML");
                    }
                }
            }
            if (acciones == 0) {
                faltas.add(p.fxml + " no tiene ninguna acción (onAction=\"#...\")");
            }
        }
        assertTrue(faltas.isEmpty(), unir(faltas));
    }

    @Test
    void cadaPantallaTieneLaEtiquetaDeMensajes() throws Exception {
        List<String> faltas = new ArrayList<>();
        for (Pantalla p : PANTALLAS) {
            boolean tiene = false;
            for (Element elemento : elementos(p.fxml)) {
                if ("etiquetaMensaje".equals(elemento.getAttribute("fx:id"))) {
                    tiene = "Label".equals(elemento.getTagName());
                }
            }
            if (!tiene) {
                faltas.add(p.fxml + " debe tener <Label fx:id=\"etiquetaMensaje\"/> para el resultado o el error");
            }
        }
        assertTrue(faltas.isEmpty(), unir(faltas));
    }

    // ---------- Controladores ----------

    @Test
    void cadaControladorRecibeSuCasoDeUsoEnElConstructor() {
        List<String> faltas = new ArrayList<>();
        for (Pantalla p : PANTALLAS) {
            Class<?> clase = cargar(p.controlador);
            if (clase == null) {
                faltas.add("No existe la clase " + PAQUETE_UI + p.controlador);
                continue;
            }
            try {
                Constructor<?> constructor = clase.getConstructor(p.casoDeUso);
                if (!Modifier.isPublic(constructor.getModifiers())) {
                    faltas.add(p.controlador + "(" + p.casoDeUso.getSimpleName() + ") debe ser público");
                }
            } catch (NoSuchMethodException e) {
                faltas.add(p.controlador + " debe tener el constructor público "
                        + p.controlador + "(" + p.casoDeUso.getSimpleName() + ")");
            }
        }
        assertTrue(faltas.isEmpty(), unir(faltas));
    }

    @Test
    void cadaControladorLlamaALasOperacionesDeSuCasoDeUso() throws IOException {
        List<String> faltas = new ArrayList<>();
        for (Pantalla p : PANTALLAS) {
            String codigo = codigoSinComentarios(new File(UI + p.controlador + ".java"));
            for (String operacion : p.operaciones) {
                if (!codigo.contains("." + operacion + "(")) {
                    faltas.add(p.controlador + " no llama a " + p.casoDeUso.getSimpleName() + "." + operacion + "(...)");
                }
            }
        }
        assertTrue(faltas.isEmpty(), unir(faltas));
    }

    @Test
    void losControladoresAtrapanElErrorYMuestranSuMensaje() throws IOException {
        List<String> faltas = new ArrayList<>();
        for (Pantalla p : PANTALLAS) {
            String codigo = codigoSinComentarios(new File(UI + p.controlador + ".java"));
            if (!codigo.contains("catch") || !codigo.contains("getMessage()")) {
                faltas.add(p.controlador + " debe atrapar el error del caso de uso y mostrar getMessage()");
            }
        }
        assertTrue(faltas.isEmpty(), unir(faltas));
    }

    @Test
    void losControladoresNoTienenReglasDelNegocio() throws IOException {
        String[] prohibidos = {
            "cuposDisponibles(", "ocupaCupo(", "getEstado()", "EstadoReserva",
            "getReservas(", "getBloqueados(", "isBefore(", "isAfter(", "plusMinutes(",
            ".reservar(", ".confirmar(", ".cancelar(", ".registrarIngreso(",
            "buscarReservaPorCodigo(", "cambiarAforo(", "cambiarDatos("
        };
        List<String> faltas = buscarEnControladores(prohibidos);
        assertTrue(faltas.isEmpty(),
                "Reglas o llamadas a entidades dentro de los controladores (van en domain):\n" + unir(faltas));
    }

    @Test
    void losControladoresNoConocenLaPersistenciaNiElReloj() throws IOException {
        String[] prohibidos = {
            "import co.edu.unillanos.campusfest.infrastructure.persistence",
            "import java.sql.", "import org.h2.", "RepositorioEventos", "RelojDelSistema",
            "Reloj ", ".now("
        };
        List<String> faltas = buscarEnControladores(prohibidos);
        assertTrue(faltas.isEmpty(),
                "Los controladores solo hablan con su caso de uso:\n" + unir(faltas));
    }

    @Test
    void losControladoresNoCreanLosCasosDeUso() throws IOException {
        String[] prohibidos = {
            "new ConsultarCupos(", "new ReservarEntrada(", "new EditarEvento(", "new RegistrarIngreso("
        };
        List<String> faltas = buscarEnControladores(prohibidos);
        assertTrue(faltas.isEmpty(),
                "El caso de uso llega por el constructor; lo crea App:\n" + unir(faltas));
    }

    // ---------- App ----------

    @Test
    void appEnsamblaH2ElRelojYLasCuatroPantallas() throws IOException {
        List<String> faltas = new ArrayList<>();
        File archivo = new File(APP);
        if (!archivo.isFile()) {
            faltas.add("Falta " + APP);
        } else {
            String codigo = codigoSinComentarios(archivo);
            String[] requeridos = {
                "new RepositorioEventosH2(", "new RelojDelSistema(", "setController(",
                "new ConsultarCupos(", "new ReservarEntrada(", "new EditarEvento(", "new RegistrarIngreso("
            };
            for (String requerido : requeridos) {
                if (!codigo.contains(requerido)) {
                    faltas.add("App no contiene " + requerido + "...)");
                }
            }
            for (Pantalla p : PANTALLAS) {
                if (!codigo.contains("new " + p.controlador + "(")) {
                    faltas.add("App no crea " + p.controlador);
                }
                if (!codigo.contains(p.fxml)) {
                    faltas.add("App no carga " + p.fxml);
                }
            }
        }
        assertTrue(faltas.isEmpty(), unir(faltas));
    }

    @Test
    void soloAppCreaLasImplementacionesDeLosPuertos() throws IOException {
        String[] prohibidos = {
            "new RepositorioEventosH2(", "new RepositorioEventosEnMemoria(", "new RelojDelSistema("
        };
        List<File> archivos = new ArrayList<>();
        recolectarJava(new File(CODIGO), archivos);
        List<String> faltas = new ArrayList<>();
        for (File archivo : archivos) {
            if (archivo.getName().equals("App.java")) {
                continue;
            }
            faltas.addAll(buscar(archivo, prohibidos));
        }
        assertTrue(faltas.isEmpty(), "Solo App nombra las implementaciones concretas:\n" + unir(faltas));
    }

    // ---------- Utilidades ----------

    private static final class Pantalla {
        final String fxml;
        final String controlador;
        final Class<?> casoDeUso;
        final String[] operaciones;

        Pantalla(String fxml, String controlador, Class<?> casoDeUso, String[] operaciones) {
            this.fxml = fxml;
            this.controlador = controlador;
            this.casoDeUso = casoDeUso;
            this.operaciones = operaciones;
        }
    }

    private List<Element> elementos(String fxml) throws Exception {
        List<Element> resultado = new ArrayList<>();
        File archivo = new File(VISTAS + fxml);
        if (!archivo.isFile()) {
            return resultado;
        }
        Document documento = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(archivo);
        NodeList nodos = documento.getElementsByTagName("*");
        for (int i = 0; i < nodos.getLength(); i++) {
            resultado.add((Element) nodos.item(i));
        }
        return resultado;
    }

    private Class<?> cargar(String controlador) {
        try {
            return Class.forName(PAQUETE_UI + controlador, false, getClass().getClassLoader());
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private Field campo(Class<?> clase, String nombre) {
        if (clase == null) {
            return null;
        }
        try {
            return clase.getDeclaredField(nombre);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private Method metodo(Class<?> clase, String nombre) {
        if (clase == null) {
            return null;
        }
        for (Method m : clase.getDeclaredMethods()) {
            if (m.getName().equals(nombre)) {
                return m;
            }
        }
        return null;
    }

    private List<String> buscarEnControladores(String[] textos) throws IOException {
        List<String> faltas = new ArrayList<>();
        for (Pantalla p : PANTALLAS) {
            File archivo = new File(UI + p.controlador + ".java");
            if (archivo.isFile()) {
                faltas.addAll(buscar(archivo, textos));
            }
        }
        return faltas;
    }

    private List<String> buscar(File archivo, String[] textos) throws IOException {
        List<String> faltas = new ArrayList<>();
        List<String> lineas = Files.readAllLines(archivo.toPath());
        for (int i = 0; i < lineas.size(); i++) {
            String linea = lineas.get(i).trim();
            if (esComentario(linea)) {
                continue;
            }
            for (String texto : textos) {
                if (linea.contains(texto)) {
                    faltas.add(archivo.getName() + ":" + (i + 1) + "  " + linea);
                }
            }
        }
        return faltas;
    }

    private String codigoSinComentarios(File archivo) throws IOException {
        if (!archivo.isFile()) {
            return "";
        }
        StringBuilder codigo = new StringBuilder();
        for (String linea : Files.readAllLines(archivo.toPath())) {
            if (!esComentario(linea.trim())) {
                codigo.append(linea).append('\n');
            }
        }
        return codigo.toString();
    }

    private boolean esComentario(String linea) {
        return linea.startsWith("//") || linea.startsWith("*") || linea.startsWith("/*");
    }

    private void recolectarJava(File directorio, List<File> archivos) {
        File[] hijos = directorio.listFiles();
        if (hijos == null) {
            return;
        }
        for (File hijo : hijos) {
            if (hijo.isDirectory()) {
                recolectarJava(hijo, archivos);
            } else if (hijo.getName().endsWith(".java")) {
                archivos.add(hijo);
            }
        }
    }

    private String unir(List<String> faltas) {
        StringBuilder texto = new StringBuilder();
        for (String falta : faltas) {
            texto.append(falta).append('\n');
        }
        return texto.toString();
    }
}
