## ⚙️ Configuración y Ejecución (Backend)

Este proyecto utiliza variables de entorno para proteger las credenciales de la infraestructura.

1. Renombra el archivo `src/main/resources/application.properties.example` a `application.properties` o inyecta las variables directamente.
2. Configura las siguientes variables de entorno en tu terminal:
   - `PORTAINER_URL`: La URL y puerto de tu Portainer (ej. `http://192.168.0.x:9000`)
   - `PORTAINER_TOKEN`: Tu Access Token de Portainer.
   - `PROXMOX_TOKEN`: Tu API Token de Proxmox (Rol `PVEAuditor`).
3. Ejecuta el proyecto:
   ```bash
   mvn clean spring-boot:run