import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { MonitorService } from './services/monitor';
import { ServiceNode, CategoryGroup, HostStatus, MonitorResponse } from './models/service-node.model';
import { Subscription, timer, of } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';

const EMPTY_HOST_STATUS: HostStatus = {
  status: 'OFFLINE',
  cpuUsage: '0.0%',
  ramUsed: '0M',
  ramTotal: '0G',
  diskUsed: '0G / 0G'
};

@Component({
  selector: 'app-root',
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class App implements OnInit, OnDestroy {
  private monitor = inject(MonitorService);

  hasError = signal(false);
  hostStatus = signal<HostStatus>(EMPTY_HOST_STATUS);
  groupedServices = signal<CategoryGroup[]>([]);

  private pollingSub!: Subscription;

  ngOnInit() {
    this.pollingSub = timer(0, 5000).pipe(
      switchMap(() => this.monitor.getServices().pipe(
        catchError(() => {
          this.hasError.set(true);
          return of(null);
        })
      ))
    ).subscribe((data: MonitorResponse | null) => {
      if (data) {
        this.hasError.set(false);
        this.hostStatus.set(data.hostStatus ?? EMPTY_HOST_STATUS);
        this.groupedServices.set(this.agruparServicios(data.services ?? []));
      }
    });
  }

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
