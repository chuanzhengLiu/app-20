package com.podcast.collab.controller;

import com.podcast.collab.dto.ApiResponse;
import com.podcast.collab.dto.TaskDTO;
import com.podcast.collab.dto.request.AssignTaskRequest;
import com.podcast.collab.dto.request.CreateTaskRequest;
import com.podcast.collab.dto.request.UpdateTaskRequest;
import com.podcast.collab.dto.request.UpdateTaskStatusRequest;
import com.podcast.collab.entity.Task;
import com.podcast.collab.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class TaskController {
    
    private final TaskService taskService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskDTO>>> getTasks(
            @RequestParam(required = false) Task.Status status,
            @RequestParam(required = false) Task.Priority priority,
            @RequestParam(required = false) Long assigneeId) {
        
        List<TaskDTO> dtos = taskService.getTasks(status, priority, assigneeId);
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskDTO>> getTask(@PathVariable Long id) {
        TaskDTO dto = taskService.getTask(id);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<TaskDTO>> createTask(
            @Valid @RequestBody CreateTaskRequest request) {
        
        TaskDTO dto = taskService.createTask(request);
        return ResponseEntity.ok(ApiResponse.success(dto, "任务创建成功"));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskDTO>> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request) {
        
        TaskDTO dto = taskService.updateTask(id, request);
        return ResponseEntity.ok(ApiResponse.success(dto, "任务更新成功"));
    }
    
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TaskDTO>> updateTaskStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskStatusRequest request) {
        
        TaskDTO dto = taskService.updateTaskStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(dto, "状态更新成功"));
    }
    
    @PatchMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<TaskDTO>> assignTask(
            @PathVariable Long id,
            @Valid @RequestBody AssignTaskRequest request) {
        
        TaskDTO dto = taskService.assignTask(id, request);
        return ResponseEntity.ok(ApiResponse.success(dto, "任务分配成功"));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCER')")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.ok(ApiResponse.success(null, "任务删除成功"));
    }
}
