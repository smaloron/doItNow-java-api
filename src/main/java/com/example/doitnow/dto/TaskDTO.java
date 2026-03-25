package com.example.doitnow.dto;

import com.example.doitnow.model.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskDTO {

    private String id;

    @NotBlank(message = "Le titre ne peut pas être vide.")
    private String title;

    private String description;

    @NotNull(message = "Le statut 'completed' est obligatoire.")
    private boolean completed;

    private String userId;

    private Priority priority;

    private List<String> tags;

    private LocalDate dueDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
