import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { MonitorService } from './services/monitor';
import { ServiceNode, CategoryGroup } from './models/service-node.model';
import { Subscription, timer, of } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class App implements OnInit, OnDestroy {
  hasError = signal(false);
  private pollingSub!: Subscription;

  // Restauramos las señales (Signals) que tu HTML necesita
  hostStatus = signal<any>({});
  groupedServices = signal<CategoryGroup[]>([]);

  constructor(private monitor: MonitorService) { }

  ngOnInit() {
    this.pollingSub = timer(0, 5000).pipe(
      switchMap(() => this.monitor.getServices().pipe(
        catchError(error => {
          this.hasError.set(true);
          // Retornamos null silencioso para no romper el timer
          return of(null);
        })
      ))
    ).subscribe((data: any) => {
      if (data) {
        this.hasError.set(false); // Hay conexión de nuevo

        // Dependiendo de cómo sea tu backend, mapeamos los datos a las señales:
        if (data.hostStatus) {
          this.hostStatus.set(data.hostStatus);
        }

        // Asumiendo que data o data.services es la lista de tus servicios
        const servicesArray = Array.isArray(data) ? data : (data.services || []);
        this.groupedServices.set(this.agruparServicios(servicesArray));
      }
    });
  }

  // Función para agrupar los servicios por categoría, necesaria para tu HTML
  private agruparServicios(services: ServiceNode[]): CategoryGroup[] {
    const agrupado = services.reduce((acc, curr) => {
      const cat = curr.category || 'Otros';
      if (!acc[cat]) acc[cat] = [];
      acc[cat].push(curr);
      return acc;
    }, {} as Record<string, ServiceNode[]>);

    return Object.keys(agrupado).map(key => ({
      categoryName: key,
      services: agrupado[key],
      // Ajusta el status según lo que envíe tu backend (ONLINE, running, etc.)
      onlineCount: agrupado[key].filter(s => s.status === 'ONLINE' || s.status === 'running').length,
      totalCount: agrupado[key].length
    }));
  }

  ngOnDestroy() {
    if (this.pollingSub) {
      this.pollingSub.unsubscribe();
    }
  }
}