package co.edu.unillanos.campusfest;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Revisa el código fuente para comprobar la regla de dependencia de la
 * arquitectura limpia: lo de adentro no conoce lo de afuera.
 *
 * Entidades     -> co.edu.unillanos.campusfest.domain
 * Casos de uso  -> co.edu.unillanos.campusfest.application (usecase y port)
 * Adaptadores   -> co.edu.unillanos.campusfest.infrastructure
 */
class ReglaDeDependenciaTest {

    private static final String RAIZ = "src/main/java/co/edu/unillanos/campusfest/";

    private static final String[] PROHIBIDOS_ADENTRO = {
        "import javafx.",
        "import java.sql.",
        "import org.h2.",
        "import co.edu.unillanos.campusfest.infrastructure."
    };

    @Test
    void lasEntidadesNoDependenDeNingunAnilloExterior() throws IOException {
        List<String> faltas = buscar(RAIZ + "domain", PROHIBIDOS_ADENTRO);
        faltas.addAll(buscar(RAIZ + "domain",
                new String[] {"import co.edu.unillanos.campusfest.application."}));

        assertTrue(faltas.isEmpty(), "Imports prohibidos en domain:\n" + unir(faltas));
    }

    @Test
    void losCasosDeUsoNoDependenDeAdaptadoresNiFrameworks() throws IOException {
        List<String> faltas = buscar(RAIZ + "application", PROHIBIDOS_ADENTRO);

        assertTrue(faltas.isEmpty(), "Imports prohibidos en application:\n" + unir(faltas));
    }

    @Test
    void niLasEntidadesNiLosCasosDeUsoLeenElRelojDelSistema() throws IOException {
        String[] llamadas = {"LocalDateTime.now(", "LocalDate.now(", "Instant.now(",
                "System.currentTimeMillis("};
        List<String> faltas = buscar(RAIZ + "domain", llamadas);
        faltas.addAll(buscar(RAIZ + "application", llamadas));

        assertTrue(faltas.isEmpty(),
                "La hora debe llegar por el puerto Reloj, no por now():\n" + unir(faltas));
    }

    // ---------- Utilidades ----------

    private List<String> buscar(String carpeta, String[] textos) throws IOException {
        File directorio = new File(carpeta);
        assertTrue(directorio.isDirectory(), "No existe la carpeta " + carpeta);
        List<String> faltas = new ArrayList<>();
        List<File> archivos = new ArrayList<>();
        recolectarJava(directorio, archivos);
        for (File archivo : archivos) {
            List<String> lineas = Files.readAllLines(archivo.toPath());
            for (int i = 0; i < lineas.size(); i++) {
                String linea = lineas.get(i).trim();
                if (linea.startsWith("//") || linea.startsWith("*")) {
                    continue;
                }
                for (String texto : textos) {
                    if (linea.contains(texto)) {
                        faltas.add(archivo.getPath() + ":" + (i + 1) + "  " + linea);
                    }
                }
            }
        }
        return faltas;
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
