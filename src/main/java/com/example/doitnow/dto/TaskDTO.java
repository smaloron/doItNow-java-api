package com.example.doitnow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskDTO {

    private String id;

    @NotBlank(message = "Le titre ne peut pas être vide.")
    private String title;

    private String description;

    @NotNull(message = "Le statut 'completed' est obligatoire.")
    private boolean completed;
}