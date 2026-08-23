import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MonitorResponse } from '../models/service-node.model';
import { environment } from '../../environments/environments';

@Injectable({
  providedIn: 'root'
})
export class MonitorService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  getServices(): Observable<MonitorResponse> {
    return this.http.get<MonitorResponse>(`${this.apiUrl}/services`);
  }
}
