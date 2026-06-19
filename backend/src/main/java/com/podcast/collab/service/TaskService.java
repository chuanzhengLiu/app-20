package com.podcast.collab.service;

import com.podcast.collab.annotation.AuditLog;
import com.podcast.collab.aspect.AuditLogContext;
import com.podcast.collab.dto.TaskDTO;
import com.podcast.collab.dto.request.AssignTaskRequest;
import com.podcast.collab.dto.request.CreateTaskRequest;
import com.podcast.collab.dto.request.UpdateTaskRequest;
import com.podcast.collab.dto.request.UpdateTaskStatusRequest;
import com.podcast.collab.entity.Annotation;
import com.podcast.collab.entity.Task;
import com.podcast.collab.entity.Team;
import com.podcast.collab.entity.User;
import com.podcast.collab.repository.AnnotationRepository;
import com.podcast.collab.repository.TaskRepository;
import com.podcast.collab.repository.TeamRepository;
import com.podcast.collab.repository.UserRepository;
import com.podcast.collab.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {
    
    private final TaskRepository taskRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final AnnotationRepository annotationRepository;
    private final SecurityUtil securityUtil;
    
    @Transactional(readOnly = true)
    public List<TaskDTO> getTasks(Task.Status status, Task.Priority priority, Long assigneeId) {
        Long teamId = securityUtil.getCurrentTeamId();
        List<Task> tasks = taskRepository.findByTeamIdWithFilters(teamId, status, priority, assigneeId);
        return tasks.stream()
                .map(TaskDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public TaskDTO getTask(Long id) {
        Long teamId = securityUtil.getCurrentTeamId();
        Task task = taskRepository.findByIdAndTeamId(id, teamId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在"));
        return TaskDTO.fromEntity(task);
    }
    
    @Transactional
    @AuditLog(action = "CREATE_TASK", entityType = "TASK")
    public TaskDTO createTask(CreateTaskRequest request) {
        Long teamId = securityUtil.getCurrentTeamId();
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("团队不存在"));
        
        User currentUser = securityUtil.getCurrentUser();
        
        Task task = Task.builder()
                .team(team)
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : Task.Priority.MEDIUM)
                .createdBy(currentUser)
                .build();
        
        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new IllegalArgumentException("被分配用户不存在"));
            task.setAssignee(assignee);
        }
        
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }
        
        if (request.getAnnotationIds() != null && !request.getAnnotationIds().isEmpty()) {
            Set<Annotation> annotations = new HashSet<>();
            for (Long annId : request.getAnnotationIds()) {
                annotationRepository.findByIdAndTeamId(annId, teamId)
                        .ifPresent(annotations::add);
            }
            task.setAnnotations(annotations);
        }
        
        task = taskRepository.save(task);
        return TaskDTO.fromEntity(task);
    }
    
    @Transactional
    @AuditLog(action = "UPDATE_TASK", entityType = "TASK", entityIdParam = "id")
    public TaskDTO updateTask(Long id, UpdateTaskRequest request) {
        Long teamId = securityUtil.getCurrentTeamId();
        Task task = taskRepository.findByIdAndTeamId(id, teamId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在"));
        
        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getAssigneeId() != null) {
            Long assigneeId = request.getAssigneeId();
            if (assigneeId == 0) {
                task.setAssignee(null);
            } else {
                User assignee = userRepository.findById(assigneeId)
                        .orElseThrow(() -> new IllegalArgumentException("被分配用户不存在"));
                task.setAssignee(assignee);
            }
        }
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }
        if (request.getAnnotationIds() != null) {
            Set<Annotation> annotations = new HashSet<>();
            for (Long annId : request.getAnnotationIds()) {
                annotationRepository.findByIdAndTeamId(annId, teamId)
                        .ifPresent(annotations::add);
            }
            task.setAnnotations(annotations);
        }
        
        task = taskRepository.save(task);
        return TaskDTO.fromEntity(task);
    }
    
    @Transactional
    @AuditLog(action = "UPDATE_TASK_STATUS", entityType = "TASK", entityIdParam = "id")
    public TaskDTO updateTaskStatus(Long id, UpdateTaskStatusRequest request) {
        Long teamId = securityUtil.getCurrentTeamId();
        Task task = taskRepository.findByIdAndTeamId(id, teamId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在"));
        
        Task.Status oldStatus = task.getStatus();
        task.setStatus(request.getStatus());
        task = taskRepository.save(task);
        
        AuditLogContext.addDetail("oldStatus", oldStatus.name());
        AuditLogContext.addDetail("newStatus", request.getStatus().name());
        
        return TaskDTO.fromEntity(task);
    }
    
    @Transactional
    @AuditLog(action = "ASSIGN_TASK", entityType = "TASK", entityIdParam = "id")
    public TaskDTO assignTask(Long id, AssignTaskRequest request) {
        Long teamId = securityUtil.getCurrentTeamId();
        Task task = taskRepository.findByIdAndTeamId(id, teamId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在"));
        
        Long assigneeId = request.getAssigneeId();
        if (assigneeId == null || assigneeId == 0) {
            task.setAssignee(null);
        } else {
            User assignee = userRepository.findById(assigneeId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
            task.setAssignee(assignee);
        }
        
        task = taskRepository.save(task);
        
        AuditLogContext.addDetail("assigneeId", assigneeId);
        
        return TaskDTO.fromEntity(task);
    }
    
    @Transactional
    @AuditLog(action = "DELETE_TASK", entityType = "TASK", entityIdParam = "id")
    public void deleteTask(Long id) {
        Long teamId = securityUtil.getCurrentTeamId();
        Task task = taskRepository.findByIdAndTeamId(id, teamId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在"));
        
        taskRepository.delete(task);
    }
}
