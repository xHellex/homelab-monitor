# Homelab Monitor — Backend

API en Spring Boot para el [Homelab Monitor](../README.md). Orquesta el sondeo de red directo con las APIs de Portainer y Proxmox, y expone un único endpoint con el estado unificado de todos los servicios.

**Stack:** Java 21 · Spring Boot 4 · Maven

---

## Estructura

```
src/main/java/com/homelab/homelab_monitor_backend/
├── controller/MonitorController.java   # Orquesta las 3 fuentes y expone /api/v1/monitor/services
├── model/ServiceNode.java
└── service/
    ├── NetworkMonitorService.java      # Sondeo TCP/HTTP y UDP/ICMP en paralelo
    ├── PortainerService.java           # RAM por contenedor Docker
    └── ProxmoxService.java             # RAM por VM/LXC y métricas del nodo host
```

## Configuración

Copia el ejemplo y rellena tus valores:

```bash
cp .env.example .env
```

```properties
PORTAINER_URL=http://TU_HOST:9000
PORTAINER_TOKEN=tu_token_de_portainer
PROXMOX_URL=https://TU_HOST:8006/api2/json
PROXMOX_NODE=nombre_de_tu_nodo
PROXMOX_TOKEN=PVEAPIToken=usuario@pam!token-id=uuid
```

`application.properties` lee estas variables (`${PORTAINER_URL}`, etc.) — nunca hay credenciales en el código ni en el repo. Usa un token de Proxmox con rol `PVEAuditor` (solo lectura), no `root@pam`.

## Ejecutar

```bash
./mvnw spring-boot:run
```

La API queda en `http://localhost:8081/api/v1/monitor/services`.

## Endpoint

`GET /api/v1/monitor/services`

```json
{
  "services": [
    {
      "id": "jenkins",
      "name": "Jenkins",
      "category": "CI/CD",
      "protocol": "TCP",
      "status": "ONLINE",
      "latencyMs": 12,
      "ramUsage": "412 MB",
      "lastChecked": "2026-08-23T14:32:10"
    }
  ],
  "hostStatus": {
    "status": "ONLINE",
    "cpuUsage": "14%",
    "ramUsed": "18.2 GB",
    "ramTotal": "32 GB",
    "diskUsed": "41%"
  }
}
```

La lista de servicios a monitorear vive en el constructor de `MonitorController` (id, nombre, categoría, protocolo, dirección).

## Decisiones técnicas

- **Sondeo en paralelo** (`parallelStream()`): el tiempo total del escaneo es el del servicio más lento, no la suma de todos.
- **Timeouts estrictos** en las llamadas a Portainer/Proxmox (2s conexión / 5s lectura) para que un contenedor lento no congele la respuesta completa.
- **TLS deshabilitado hacia Proxmox** (`ProxmoxService`) por el certificado autofirmado de Proxmox VE — aceptable solo en LAN cerrada. Ver la nota de seguridad completa en el [README raíz](../README.md#️-notas-de-seguridad).

Para la arquitectura completa (frontend + backend), ver el [README raíz](../README.md).
