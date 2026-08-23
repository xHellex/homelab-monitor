package com.homelab.homelab_monitor_backend.model;

import java.time.LocalDateTime;

public class ServiceNode {
    private String id;
    private String name;
    private String category;
    private String protocol;
    private String address;
    private String status;
    private Integer latencyMs; // Usamos Integer en vez de int para que permita valores null
    private LocalDateTime lastChecked;
    private String ramUsage; //CAMPO PARA LA RAM

    // Constructor vacío (obligatorio para Spring Boot)
    public ServiceNode() {}

    // Constructor con parámetros
    public ServiceNode(String id, String name, String category, String protocol, String address, String status, Integer latencyMs, LocalDateTime lastChecked, String ramUsage) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.protocol = protocol;
        this.address = address;
        this.status = status;
        this.latencyMs = latencyMs;
        this.lastChecked = lastChecked;
        this.ramUsage = ramUsage;
    }

    // Getters y Setters (Spring Boot los necesita para generar el JSON)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Integer getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Integer latencyMs) { this.latencyMs = latencyMs; }
    
    public LocalDateTime getLastChecked() { return lastChecked; }
    public void setLastChecked(LocalDateTime lastChecked) { this.lastChecked = lastChecked; }

    public String getRamUsage() { return ramUsage; }
    public void setRamUsage(String ramUsage) { this.ramUsage = ramUsage; }
}