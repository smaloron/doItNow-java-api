package com.example.doitnow.controller;

import com.example.doitnow.dto.CreateTaskDTO;
import com.example.doitnow.dto.TaskDTO;
import com.example.doitnow.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // GET /api/tasks — Récupérer toutes les tâches
    @GetMapping
    public ResponseEntity<List<TaskDTO>> getAllTasks() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    // GET /api/tasks/{id} — Récupérer une tâche par ID
    @GetMapping("/{id}")
    public ResponseEntity<TaskDTO> getTaskById(@PathVariable String id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    // POST /api/tasks — Créer une nouvelle tâche
    @PostMapping
    public ResponseEntity<TaskDTO> createTask(@Valid @RequestBody CreateTaskDTO createTaskDTO) {
        TaskDTO createdTask = taskService.createTask(createTaskDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
    }

    // PUT /api/tasks/{id} — Mettre à jour une tâche
    @PutMapping("/{id}")
    public ResponseEntity<TaskDTO> updateTask(@PathVariable String id, @Valid @RequestBody TaskDTO taskDTO) {
        return ResponseEntity.ok(taskService.updateTask(id, taskDTO));
    }

    // DELETE /api/tasks/{id} — Supprimer une tâche
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable String id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    // GET /api/tasks/search?completed=true — Filtrer par statut
    @GetMapping("/search")
    public ResponseEntity<List<TaskDTO>> searchTasks(
            @RequestParam(required = false) Boolean completed,
            @RequestParam(required = false) String keyword) {

        if (completed != null) {
            return ResponseEntity.ok(taskService.getTasksByCompleted(completed));
        }
        if (keyword != null && !keyword.isBlank()) {
            return ResponseEntity.ok(taskService.searchTasksByTitle(keyword));
        }
        return ResponseEntity.ok(taskService.getAllTasks());
    }
}
