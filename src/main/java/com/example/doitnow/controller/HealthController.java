package com.example.doitnow.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, String> checkHealth() {
        // Crée une simple map pour la réponse JSON
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        return response; // Spring la convertira en {"status": "UP"}
    }
}
