package com.homelab.homelab_monitor_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class PortainerService {

    @Value("${portainer.url}")
    private String portainerUrl;

    @Value("${portainer.endpoint.id}")
    private String endpointId;

    @Value("${portainer.token}")
    private String token;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public PortainerService() {
        // Configuramos un RestTemplate normal, pero con TIMEOUT ESTRICTO DE 2 SEGUNDOS
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000); // Déjalo en 2s (conexión inicial)
        factory.setReadTimeout(5000); // Súbelo a 5s (tiempo para descargar los datos)

        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
    }

    public Map<String, String> getContainersRamUsage() {
        Map<String, String> ramData = new HashMap<>();

        try {
            String url = portainerUrl + "/api/endpoints/" + endpointId + "/docker/containers/json";
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-Key", token);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            JsonNode containers = objectMapper.readTree(response.getBody());

            for (JsonNode container : containers) {
                String id = container.get("Id").asText();
                String name = container.get("Names").get(0).asText().replace("/", "");

                String statsUrl = portainerUrl + "/api/endpoints/" + endpointId + "/docker/containers/" + id
                        + "/stats?stream=false";
                ResponseEntity<String> statsResponse = restTemplate.exchange(statsUrl, HttpMethod.GET, entity,
                        String.class);
                JsonNode stats = objectMapper.readTree(statsResponse.getBody());

                long usage = stats.path("memory_stats").path("usage").asLong(0);
                long cache = stats.path("memory_stats").path("stats").path("cache").asLong(0);
                long finalRamBytes = usage - cache;

                if (finalRamBytes < 0)
                    finalRamBytes = usage;

                ramData.put(name, formatBytes(finalRamBytes));
            }
        } catch (Exception e) {
            System.err.println("❌ Error obteniendo RAM de Portainer: " + e.getMessage());
        }

        return ramData;
    }

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