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

/**
 * 任务管理接口层。
 * Controller 不再直接操作 Repository，仅负责接收请求与返回响应；
 * 团队权限校验由 TaskService 在入口调用 SecurityUtil 完成；
 * 参数校验由 DTO + @Valid 自动完成；
 * 审计日志由 @Audited 注解 + AuditAspect 自动写入。
 */
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
        return ResponseEntity.ok(ApiResponse.success(taskService.listTasks(status, priority, assigneeId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskDTO>> getTask(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getTask(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TaskDTO>> createTask(@Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.ok(ApiResponse.success(taskService.createTask(request), "任务创建成功"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskDTO>> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request) {
        return ResponseEntity.ok(ApiResponse.success(taskService.updateTask(id, request), "任务更新成功"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TaskDTO>> updateTaskStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(taskService.updateStatus(id, request), "状态更新成功"));
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<TaskDTO>> assignTask(
            @PathVariable Long id,
            @RequestBody AssignTaskRequest request) {
        return ResponseEntity.ok(ApiResponse.success(taskService.assignTask(id, request), "任务分配成功"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCER')")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.ok(ApiResponse.success(null, "任务删除成功"));
    }
}
