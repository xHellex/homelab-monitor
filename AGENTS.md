# Homelab Monitor

Monitor de infraestructura self-hosted. Muestra el estado de nodos, VMs y
contenedores consultando las APIs de Proxmox y Portainer.

## Estructura
- homelab-monitor-frontend/: Angular 21 (standalone, signals), pnpm.
- homelab-monitor-backend/: Spring Boot 4, Java 21, Maven wrapper.
- docs/: documentación del proyecto.

## Comandos
Frontend (desde homelab-monitor-frontend/):
- pnpm install · pnpm start · pnpm test · pnpm build

Backend (desde homelab-monitor-backend/):
- Windows: .\mvnw.cmd test · .\mvnw.cmd spring-boot:run
- Linux:   ./mvnw test · ./mvnw spring-boot:run

Variables de entorno requeridas: ver homelab-monitor-backend/.env.example.

## Reglas propias de este proyecto
- El monitor es de SOLO LECTURA: el backend solo hace consultas GET a
  Proxmox y Portainer. Nunca agregar operaciones que modifiquen la
  infraestructura (iniciar, detener, borrar) sin una decisión documentada.
- Tokens de Proxmox y Portainer solo por variables de entorno. Nunca en
  código, tests, logs, mensajes de error ni respuestas de la API.
- Los .example contienen solo marcadores, nunca valores reales.
- No desactivar la verificación TLS de forma global. Si Proxmox usa
  certificado autofirmado, confiar solo en ese certificado.
- El frontend nunca llama a Proxmox o Portainer directamente: siempre a
  través del backend.

## Generales
- Antes de cambiar código, propone un plan corto y espera aprobación.
- Cambios pequeños y enfocados; no refactorizar lo que no se pidió.
- No agregar dependencias sin explicar por qué y qué alternativa se descartó.
- Validar toda entrada en el backend.
- Accesibilidad WCAG 2.2 AA: semántica, foco visible, labels, contraste,
  aria-live en estados que se actualizan solos.
- Todo cambio de lógica va con su test.
- Explica el porqué de cada decisión técnica y la alternativa descartada.

## Git
- Ramas: feature/<nombre>, fix/<nombre>, chore/<nombre>. Nunca trabajar en main.
- Conventional Commits en español: feat:, fix:, refactor:, test:, docs:, chore:.
- Nunca hacer push, merge, rebase ni borrar ramas o archivos sin confirmación.

## TypeScript y Angular
- strict; prohibido any (usar unknown y estrechar).
- Componentes standalone, ChangeDetectionStrategy.OnPush.
- Estado con signal() y computed(); input() y output() como funciones.
- Inyección con inject().
- Control flow nativo (@if, @for con track, @switch).
- RxJS para HTTP y polling; convertir con toSignal(); takeUntilDestroyed().
- Validar respuestas del backend antes de usarlas.
- Lógica en servicios; componentes delgados.

## Java y Spring Boot
- Capas: controller -> service -> cliente HTTP externo.
- Inyección por constructor con campos final.
- DTOs como record; no exponer respuestas crudas de Proxmox/Portainer:
  mapear a DTOs propios con solo los campos necesarios.
- Errores centralizados con @RestControllerAdvice y ProblemDetail.
- Timeouts explícitos en toda llamada externa; una API caída no debe
  bloquear las demás.
- Logs con SLF4J sin tokens ni cabeceras de autorización.

## Testing
- Front: tests unitarios de servicios y componentes; probar estados de
  carga, error y vacío, no solo el caso exitoso.
- Back: JUnit 5 + Mockito; @WebMvcTest para controllers; simular las APIs
  externas, nunca llamar a Proxmox o Portainer reales en tests.