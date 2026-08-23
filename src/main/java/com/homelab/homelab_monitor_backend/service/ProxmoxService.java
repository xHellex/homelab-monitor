package com.homelab.homelab_monitor_backend.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ProxmoxService {

    @Value("${proxmox.url}")
    private String proxmoxUrl;

    @Value("${proxmox.node}")
    private String proxmoxNode;

    @Value("${proxmox.token}")
    private String proxmoxToken;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ProxmoxService() {
        this.restTemplate = createTrustAllRestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    private RestTemplate createTrustAllRestTemplate() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory() {
                @Override
                protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                    if (connection instanceof HttpsURLConnection) {
                        ((HttpsURLConnection) connection).setHostnameVerifier((hostname, session) -> true);
                        ((HttpsURLConnection) connection).setSSLSocketFactory(sslContext.getSocketFactory());
                    }
                    super.prepareConnection(connection, httpMethod);
                }
            };

            return new RestTemplate(requestFactory);
        } catch (Exception e) {
            e.printStackTrace();
            return new RestTemplate();
        }
    }

    // Método para obtener métricas globales del nodo Proxmox (CPU, RAM del Host)
    public Map<String, Object> getNodeStatus() {
        Map<String, Object> statusData = new HashMap<>();
        try {
            // Consultamos la lista de nodos del clúster para ser dinámicos
            String nodesUrl = proxmoxUrl + "/nodes";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", proxmoxToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(nodesUrl, HttpMethod.GET, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.get("data");

            if (data != null && data.isArray() && data.size() > 0) {
                // Tomamos el primer nodo activo del clúster automáticamente
                String nodeName = data.get(0).path("node").asText("proxmox");

                // Consultamos su estatus real
                String statusUrl = proxmoxUrl + "/nodes/" + nodeName + "/status";
                ResponseEntity<String> statusResp = restTemplate.exchange(statusUrl, HttpMethod.GET, entity,
                        String.class);
                JsonNode statusRoot = objectMapper.readTree(statusResp.getBody());
                JsonNode statusDataNode = statusRoot.get("data");

                if (statusDataNode != null) {
                    double cpu = statusDataNode.path("cpu").asDouble(0.0) * 100;
                    long memTotal = statusDataNode.path("memory").path("total").asLong(0);
                    long memUsed = statusDataNode.path("memory").path("used").asLong(0);
                    long diskTotal = statusDataNode.path("rootfs").path("total").asLong(0);
                    long diskUsed = statusDataNode.path("rootfs").path("used").asLong(0);

                    statusData.put("cpuUsage", String.format("%.1f%%", cpu));
                    statusData.put("ramTotal", formatBytes(memTotal));
                    statusData.put("ramUsed", formatBytes(memUsed));
                    // Concatenamos el disco en un solo string
                    statusData.put("diskUsed", formatBytes(diskUsed) + " / " + formatBytes(diskTotal));
                    statusData.put("status", "ONLINE");
                    return statusData;
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error conectando a Proxmox API: " + e.getMessage());
        }

        // Valores de respaldo en caso de que Proxmox esté apagado o falle la conexión
        statusData.put("cpuUsage", "0.0%");
        statusData.put("ramTotal", "0G");
        statusData.put("ramUsed", "0M");
        statusData.put("status", "OFFLINE");
        // CORRECCIÓN: Usamos texto estático aquí porque las variables de arriba no
        // existen en este bloque
        statusData.put("diskUsed", "0G / 0G");

        return statusData;
    }

    // Método para obtener la RAM de VMs (QEMU) o Contenedores LXC específicos
    public Map<String, String> getVmsRamUsage(List<String> vmIds) {
        Map<String, String> ramData = new HashMap<>();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", proxmoxToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Usamos "proxmox" como el nombre de tu nodo base según vimos en tus capturas
            String nodeName = "proxmox";

            for (String vmid : vmIds) {
                // Solo procesamos los IDs que son números (los de Proxmox)
                if (!vmid.matches("\\d+"))
                    continue;

                // 1. Intentamos buscar la RAM asumiendo que es una Máquina Virtual (QEMU)
                String qemuUrl = proxmoxUrl + "/nodes/" + nodeName + "/qemu/" + vmid + "/status/current";
                try {
                    ResponseEntity<String> response = restTemplate.exchange(qemuUrl, HttpMethod.GET, entity,
                            String.class);
                    JsonNode data = objectMapper.readTree(response.getBody()).get("data");

                    if (data != null && data.has("mem") && data.path("status").asText().equals("running")) {
                        long memUsed = data.path("mem").asLong(0);
                        ramData.put(vmid, formatBytes(memUsed));
                        continue; // Si lo encontró y está corriendo, pasa al siguiente ID
                    }
                } catch (Exception e) {
                    // 2. Si falla (error 404), intentamos buscarlo como Contenedor LXC
                    String lxcUrl = proxmoxUrl + "/nodes/" + nodeName + "/lxc/" + vmid + "/status/current";
                    try {
                        ResponseEntity<String> lxcResponse = restTemplate.exchange(lxcUrl, HttpMethod.GET, entity,
                                String.class);
                        JsonNode lxcData = objectMapper.readTree(lxcResponse.getBody()).get("data");

                        if (lxcData != null && lxcData.has("mem")
                                && lxcData.path("status").asText().equals("running")) {
                            long memUsed = lxcData.path("mem").asLong(0);
                            ramData.put(vmid, formatBytes(memUsed));
                        }
                    } catch (Exception ex) {
                        // El servicio está apagado o no existe, lo ignoramos
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error obteniendo RAM de VMs Proxmox: " + e.getMessage());
        }
        return ramData;
    }

    // Método que convierte bytes a "454.8M" o "1.2G"
    private String formatBytes(long bytes) {
        if (bytes < 1024 * 1024) {
            return (bytes / 1024) + "K";
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1fM", bytes / (1024.0 * 1024.0)).replace(",", ".");
        } else {
            return String.format("%.2fG", bytes / (1024.0 * 1024.0 * 1024.0)).replace(",", ".");
        }
    }
}