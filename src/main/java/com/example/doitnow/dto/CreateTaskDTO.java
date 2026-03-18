package com.example.doitnow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTaskDTO {

    @NotBlank( message = "Le titre est obligatoire.")
    @Size(min = 3, max = 100, message = "Le titre doit contenir entre 3 et 100 caractères.")
    private String title;

    @Size(max = 500, message = "La description ne peut pas dépasser 500 caractères.")
    private String description;
}