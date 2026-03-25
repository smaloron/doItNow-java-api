package com.example.doitnow.dto;

import lombok.Data;
import java.util.Map;

@Data
public class TaskStatsDTO {
    private long total;
    private long completed;
    private long pending;
    private long overdue;
    private double completionRate;
    private Map<String, Long> tasksByPriority;
}