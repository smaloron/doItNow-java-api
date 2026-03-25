package com.example.doitnow.dto;

import com.example.doitnow.model.Priority;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateTaskDTO {

    @NotBlank(message = "Le titre est obligatoire.")
    @Size(min = 3, max = 100, message = "Le titre doit contenir entre 3 et 100 caractères.")
    private String title;

    @Size(max = 500, message = "La description ne peut pas dépasser 500 caractères.")
    private String description;

    private Priority priority;

    private List<String> tags;

    @FutureOrPresent(message = "La date d'échéance ne peut pas être dans le passé.")
    private LocalDate dueDate;
}
