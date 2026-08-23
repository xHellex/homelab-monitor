package com.homelab.homelab_monitor_backend.service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.homelab.homelab_monitor_backend.model.ServiceNode;

@Service
public class NetworkMonitorService {

    public ServiceNode checkService(ServiceNode node) {
        // Separamos la IP del puerto (ej: "192.168.1.20" y "8080")
        String[] parts = node.getAddress().split(":");
        String ip = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 80;

        long startTime = System.currentTimeMillis();

        if ("HTTP".equalsIgnoreCase(node.getProtocol()) || "TCP".equalsIgnoreCase(node.getProtocol())) {
            // Prueba de conexión TCP profunda (Ideal para Nextcloud, n8n, Mattermost)
            try (Socket socket = new Socket()) {
                // Intentamos conectar con un tiempo límite de 2 segundos (2000 ms)
                socket.connect(new InetSocketAddress(ip, port), 2000);
                long endTime = System.currentTimeMillis();
                
                node.setStatus("ONLINE");
                node.setLatencyMs((int) (endTime - startTime));
            } catch (IOException e) {
                // Si la conexión es rechazada o hay timeout, marcamos como caído
                node.setStatus("OFFLINE");
                node.setLatencyMs(null);
            }
        } else if ("UDP".equalsIgnoreCase(node.getProtocol())) {
            // Prueba para servidores de juego (UDP). Hacemos un Ping a la máquina física.
            try {
                InetAddress inet = InetAddress.getByName(ip);
                if (inet.isReachable(2000)) {
                    long endTime = System.currentTimeMillis();
                    node.setStatus("ONLINE");
                    node.setLatencyMs((int) (endTime - startTime));
                } else {
                    node.setStatus("OFFLINE");
                    node.setLatencyMs(null);
                }
            } catch (Exception e) {
                node.setStatus("OFFLINE");
                node.setLatencyMs(null);
            }
        }

        node.setLastChecked(LocalDateTime.now());
        return node;
    }
}