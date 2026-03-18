package com.example.doitnow.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WelcomeController {

    @GetMapping("/api/welcome")
    public String getWelcomeMessage() {
        return "Bienvenue sur l'API du Gestionnaire de Tâches !";
    }
}