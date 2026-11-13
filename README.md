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
| CU1 Consultar cupos | Estudiante | `ConsultarCupos` | Pendiente |
| CU2 Reservar y confirmar una entrada | Estudiante | `ReservarEntrada` | Pendiente |
| CU3 Editar evento | Organizador | `EditarEvento` | Pendiente |
| CU4 Registrar el ingreso con QR | Encargado de la entrada | `RegistrarIngreso` | Pendiente |

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
Las pruebas de `src/test/java` son las entregadas por los docentes y no se modifican.

## Flujo de trabajo con Git

- Nadie hace push directo a `main`.
- Cada integrante trabaja en su rama (`feature/<caso-de-uso>-<nombre>`) y abre un pull request.
- Otro integrante revisa y aprueba el pull request.
- Solo el integrador (`santiFst`) hace el merge a `main`.
- Los mensajes de commit dicen qué cambió.

## Declaración de uso de IA

_(Completar antes de la entrega. Debe decir qué herramientas se usaron, para qué y en qué clases, y describir al menos una propuesta de la IA que violaba el diseño y cómo se corrigió.)_

| Herramienta | Para qué se usó | Clases o archivos |
| --- | --- | --- |
| Claude | Esqueleto del proyecto (`pom.xml`, estructura de paquetes, README) | `pom.xml`, `README.md`, `.gitignore` |

### Propuestas de la IA que violaban el diseño

_(Registrar aquí cada caso: qué propuso la IA, qué regla violaba, cómo se corrigió y en qué commit.)_
