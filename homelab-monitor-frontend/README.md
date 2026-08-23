# Homelab Monitor — Frontend

Dashboard en Angular para el [Homelab Monitor](../README.md). Consume la API del backend y muestra el estado de servicios agrupados por categoría, con una cabecera de métricas del host y una señal de error visible cuando se pierde la conexión.

**Stack:** Angular 21 (standalone components, Signals) · RxJS · Tailwind CSS · TypeScript

---

## Estructura

```
src/app/
├── app.ts                        # Componente raíz: polling, estado (signals)
├── app.html                      # Template del dashboard
├── models/service-node.model.ts  # ServiceNode, HostStatus, MonitorResponse, CategoryGroup
└── services/monitor.ts           # MonitorService — llamada tipada a la API
```

## Configuración

La URL del backend se define en `src/environments/environments.ts`:

```ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8081/api/v1/monitor'
};
```

## Ejecutar

```bash
pnpm install
pnpm start
```

Disponible en `http://localhost:4200`. Requiere el backend corriendo (ver [homelab-monitor-backend](../homelab-monitor-backend/README.md)) para tener datos reales; sin él, el dashboard muestra el banner de error y sigue reintentando cada 5 segundos.

## Comandos

```bash
pnpm start   # servidor de desarrollo
pnpm build   # build de producción
pnpm test    # tests unitarios (vitest)
```

## Decisiones técnicas

- **Signals + `timer()`/`switchMap`** para el polling: cada ciclo cancela la petición anterior si aún no respondió, evitando que las respuestas lleguen fuera de orden. La suscripción se limpia en `ngOnDestroy`.
- **Tipado estricto de la respuesta**: `MonitorService.getServices()` devuelve `Observable<MonitorResponse>` (`{ services: ServiceNode[], hostStatus: HostStatus }`), reflejando exactamente el JSON que entrega el backend — sin `any` de por medio.
- **Degradación visible ante fallos**: un `catchError` marca `hasError` y devuelve `null` sin romper el timer, así la UI se recupera sola cuando el backend vuelve.

Para la arquitectura completa (frontend + backend) y las notas de seguridad, ver el [README raíz](../README.md).
