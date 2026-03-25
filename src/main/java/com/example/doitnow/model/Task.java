package com.example.doitnow.model;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "tasks")
public class Task {

    @Id
    private String id;

    @Indexed
    private String title;
    private String description;
    private boolean completed;

    @Indexed
    private String userId;

    @NotNull(message =
            "La priorité est obligatoire")
    private Priority priority = Priority.MEDIUM;

    private List<String> tags = new ArrayList<>();

    @FutureOrPresent(message =
            "La date d'échéance ne peut pas "
                    + "être dans le passé")
    private LocalDate dueDate;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

}
