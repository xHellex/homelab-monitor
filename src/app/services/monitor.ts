import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ServiceNode } from '../models/service-node.model';
import { environment } from '../../environments/environments';

@Injectable({
  providedIn: 'root'
})
export class MonitorService {
  private http = inject(HttpClient);

  // 1. Ya no hay URL hardcodeada. Apunta al environment.
  private apiUrl = environment.apiUrl;

  // 2. Adiós al 'any'. Tipado estricto con ServiceNode[]
  getServices(): Observable<ServiceNode[]> {
    return this.http.get<ServiceNode[]>(`${this.apiUrl}/services`);
  }
}