# CampusFest

Programa de registro y control de asistencias y creación de eventos estudiantiles universitarios.
Proyecto de semestre de Ingeniería de Software, Universidad de los Llanos.

## Integrantes

| Integrante | Usuario de GitHub | Rol |
| --- | --- | --- |
| Carlos Acebedo| `santiFst` | Integrador (único que hace merge a `main`) |
| _(completar)_ | _(completar)_ | Desarrollador |
| _(completar)_ | _(completar)_ | Desarrollador |
| _(completar)_ | _(completar)_ | Desarrollador |

## Requisitos

- Java 21
- Maven 3.9 o superior

JavaFX 21 y H2 se descargan solos con Maven; no hay pasos manuales.

## Cómo ejecutar

```bash
mvn compile        # compila
mvn test           # ejecuta las pruebas entregadas por los docentes
mvn javafx:run     # abre la aplicación
```

`mvn test` debe correrse desde la raíz del proyecto: `ReglaDeDependenciaTest` lee `src/main/java` con ruta relativa.

## Casos de uso implementados

| CU | Actor | Clase | Estado |
| --- | --- | --- | --- |
| CU1 Consultar cupos | Estudiante | `ConsultarCupos` | Lógica lista; falta pantalla |
| CU2 Reservar y confirmar una entrada | Estudiante | `ReservarEntrada` | Lógica lista; falta pantalla |
| CU3 Editar evento | Organizador | `EditarEvento` | Lógica lista; falta pantalla |
| CU4 Registrar el ingreso con QR | Encargado de la entrada | `RegistrarIngreso` | Lógica lista; falta pantalla |

## Arquitectura

Arquitectura limpia con un paquete por anillo. Las dependencias apuntan hacia adentro.

| Anillo | Paquete |
| --- | --- |
| Entidades | `co.edu.unillanos.campusfest.domain` |
| Casos de uso | `co.edu.unillanos.campusfest.application.usecase` |
| Puertos | `co.edu.unillanos.campusfest.application.port` |
| Adaptadores | `co.edu.unillanos.campusfest.infrastructure.persistence`, `.infrastructure.ui` y `.infrastructure.reloj` |
| Frameworks y drivers | `co.edu.unillanos.campusfest` (`App`, `Launcher`), vistas FXML, CSS y script SQL |

Recursos: vistas FXML y CSS en `src/main/resources/co/edu/unillanos/campusfest/vistas/` y esquema H2 en `src/main/resources/db/schema.sql`.

El contrato completo que deben cumplir las clases está en [`docs/CONTRATO.md`](docs/CONTRATO.md).
Las pruebas de `src/test/java` son las entregadas por el docente y no se modifican.

## Flujo de trabajo con Git

- Nadie hace push directo a `main`.
- Cada integrante trabaja en su rama (`feature/<caso-de-uso>-<nombre>`) y abre un pull request.
- Otro integrante revisa y aprueba el pull request.
- Solo el integrador (`santiFst`) hace el merge a `main`.
- Los mensajes de commit dicen qué cambió.

## Declaración de uso de IA

### Herramientas

| Herramienta | Quién la usó | Para qué |
| --- | --- | --- |
| Claude (Anthropic) | _(completar: Carlos Acebedo)_ | Asistente de ingeniería de software para analizar los documentos, proponer la arquitectura y escribir código. |
| _(otra herramienta, si se usó)_ | _(completar)_ | _(completar)_ |

Cada línea de código que escribió la IA fue revisada, aplicada y subida por un integrante mediante pull request. La IA no tuvo permiso de escritura sobre `main`.

### Qué hizo la IA

| Parte | Archivos | Commit en `main` |
| --- | --- | --- |
| Análisis del contrato, los criterios de evaluación y las hojas del taller | Ninguno (resumen y documento de trabajo) | — |
| Puertos, casos de uso y adaptador en memoria | `RepositorioEventos`, `Reloj`, `ConsultarCupos`, `ReservarEntrada`, `EditarEvento`, `RegistrarIngreso`, `RepositorioEventosEnMemoria` | `131e2f5` |
| Adaptador del reloj | `RelojDelSistema` | `b734ea8` |

Las pruebas de `src/test/java` son del docente y la IA no las modificó. Las pruebas del dominio y de los casos de uso se comprobaron con un ejecutor propio de la IA, y el equipo las confirmó con `mvn test` _(completar al verificarlo)_.

### Lo que hizo el equipo

_Completar con las decisiones propias: el diseño del taller en clase, el reparto de historias, las pantallas, H2 y lo que el equipo escribió sin IA._

### Propuestas de la IA que no cumplían lo exigido

La revisión con `ReglaDeDependenciaTest` y con la lista de la rúbrica no encontró imports de un anillo exterior ni llamadas a `now()` dentro de `domain` o `application`. Los casos que sí se detectaron fueron estos:

| Qué propuso la IA | Qué exigía el diseño | Cómo se resolvió |
| --- | --- | --- |
| `Reserva.registrarIngreso` rechazaba el segundo ingreso con un mensaje sin la hora. | El flujo alternativo 4b de CU4 (hoja 7 del taller) pide informar la hora del primer ingreso. | Se detectó al leer las hojas del taller. El mensaje ahora incluye `asistencia.getHora()`. Corregido en `67fca72`. |
| `EditarEvento.editar` llama a `cambiarDatos` y luego a `cambiarAforo`. Si el aforo se rechaza, los datos ya cambiaron en el objeto, aunque no se guarden. | La operación debería dejar el evento intacto cuando se rechaza. | Con H2 no ocurre, porque cada `buscar` crea un objeto nuevo; con el repositorio en memoria sí. |
