package com.example.doitnow.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "tasks")
public class Task {

    private String id;
    private String title;
    private String description;
    private boolean completed;

}
