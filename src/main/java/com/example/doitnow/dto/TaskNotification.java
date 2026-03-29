package com.example.doitnow.dto;


public record TaskNotification(
        String type,    // "CREATED" | "UPDATED" | "DELETED"
        String taskId,
        TaskDTO task    // null pour DELETED
) {}