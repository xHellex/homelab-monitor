export interface ServiceNode {
  id: string;
  name: string;
  category: string;
  protocol: string;
  address: string;
  status: string;
  latencyMs: number;
  lastChecked: string;
  ramUsage?: string; // El nuevo campo que viene del backend
}

// Agregamos la interfaz para los grupos aquí mismo
export interface CategoryGroup {
  categoryName: string;
  services: ServiceNode[];
  onlineCount: number;
  totalCount: number;
}