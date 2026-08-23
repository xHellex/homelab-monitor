package com.homelab.homelab_monitor_backend.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.homelab.homelab_monitor_backend.model.ServiceNode;
import com.homelab.homelab_monitor_backend.service.NetworkMonitorService;
import com.homelab.homelab_monitor_backend.service.PortainerService;
import com.homelab.homelab_monitor_backend.service.ProxmoxService;

@RestController
@RequestMapping("/api/v1/monitor")
@CrossOrigin(origins = "http://localhost:4200")
public class MonitorController {

    private final NetworkMonitorService monitorService;
    private final PortainerService portainerService;
    private final ProxmoxService proxmoxService;
    private final List<ServiceNode> configuredServices;

    public MonitorController(NetworkMonitorService monitorService, PortainerService portainerService, ProxmoxService proxmoxService) {
        this.monitorService = monitorService;
        this.portainerService = portainerService;
        this.proxmoxService = proxmoxService;
        
        // LISTA COMPLETA DE TODOS TUS SERVICIOS (Docker, Domótica y Servidores de Juegos)
        this.configuredServices = Arrays.asList(
            new ServiceNode("100", "Home Assistant", "Domótica", "TCP", "192.168.0.172:8123", "OFFLINE", 0, null, null),
            new ServiceNode("pihole", "Pi-hole", "Infraestructura", "TCP", "192.168.0.187:8081", "OFFLINE", 0, null, null),
            new ServiceNode("nginx-proxy-manager-app-1", "Nginx Proxy Manager", "Proxy Inverso", "TCP", "192.168.0.187:81", "OFFLINE", 0, null, null),
            new ServiceNode("jenkins", "Jenkins", "CI/CD", "TCP", "192.168.0.187:8083", "OFFLINE", 0, null, null),
            new ServiceNode("sonarqube", "SonarQube", "DevSecOps", "TCP", "192.168.0.187:9001", "OFFLINE", 0, null, null),
            new ServiceNode("portainer", "Portainer", "Gestión Docker", "TCP", "192.168.0.187:9443", "OFFLINE", 0, null, null),
            // Servidores de Juegos
            new ServiceNode("103", "Ark Server", "Server Juego", "UDP", "192.168.0.51:27015", "OFFLINE", 0, null, null),
            new ServiceNode("102", "Terraria Server", "Server Juego", "UDP", "192.168.0.50:7777", "OFFLINE", 0, null, null),
            new ServiceNode("104", "Palworld Server", "Server Juego", "UDP", "192.168.0.52:8211", "OFFLINE", 0, null, null)
        );
    }

    @GetMapping("/services")
    public Map<String, Object> getAllData() {
        Map<String, Object> response = new HashMap<>();
        
        // 1. RAM de Docker vía Portainer
        Map<String, String> dockerRamMap = portainerService.getContainersRamUsage();
        
        // 2. RAM de VMs vía Proxmox (Extraemos solo los IDs que son números, como "100", "102", etc.)
        List<String> vmIds = configuredServices.stream()
            .map(ServiceNode::getId)
            .filter(id -> id.matches("\\d+")) 
            .toList();
        Map<String, String> proxmoxRamMap = proxmoxService.getVmsRamUsage(vmIds);
        
        // 3. Escaneo de red EN PARALELO para todos los servicios
        List<ServiceNode> liveServices = configuredServices.parallelStream().map(node -> {
            ServiceNode checkedNode = monitorService.checkService(node);
            
            // Si está en Docker, inyectamos su RAM
            if (dockerRamMap.containsKey(checkedNode.getId())) {
                checkedNode.setRamUsage(dockerRamMap.get(checkedNode.getId()));
            }
            
            // Si es una VM de Proxmox y el escáner de red dice que está ONLINE, inyectamos su RAM
            if (proxmoxRamMap.containsKey(checkedNode.getId()) && "ONLINE".equals(checkedNode.getStatus())) {
                checkedNode.setRamUsage(proxmoxRamMap.get(checkedNode.getId()));
            }
            
            return checkedNode;
        }).toList();

        // 4. Métricas del Host Proxmox (Cabecera)
        Map<String, Object> hostStatus = proxmoxService.getNodeStatus();

        response.put("services", liveServices);
        response.put("hostStatus", hostStatus);

        return response;
    }
}