package com.example.doitnow.dto;

import lombok.Data;

@Data
public class TaskDTO {
    private String id;
    private String title;
    private String description;
    private boolean completed;
}