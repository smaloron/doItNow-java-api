package com.example.doitnow.model;

import lombok.Data;

@Data
public class Task {

    private String id;
    private String title;
    private String description;
    private boolean completed;

}
