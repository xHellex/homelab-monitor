import { TestBed } from '@angular/core/testing';
import { MonitorService } from './monitor'; // <-- Cambia a tu clase real

describe('MonitorService', () => {
  let service: MonitorService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(MonitorService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});