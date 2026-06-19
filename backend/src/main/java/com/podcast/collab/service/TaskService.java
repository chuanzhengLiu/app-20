package com.podcast.collab.service;

import com.podcast.collab.audit.AuditContext;
import com.podcast.collab.audit.Audited;
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

/**
 * 任务管理业务服务。
 * 团队权限通过 SecurityUtil.requireCurrentTeamIdForRead/ForWrite 在 Service 入口主动校验；
 * 审计日志通过 @Audited 注解 + AuditAspect 自动写入，方法体不再嵌入 auditService 调用。
 */
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final AnnotationRepository annotationRepository;
    private final SecurityUtil securityUtil;

    @Transactional(readOnly = true)
    public List<TaskDTO> listTasks(Task.Status status, Task.Priority priority, Long assigneeId) {
        Long teamId = securityUtil.requireCurrentTeamIdForRead(null);
        return taskRepository.findByTeamIdWithFilters(teamId, status, priority, assigneeId).stream()
                .map(TaskDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TaskDTO getTask(Long id) {
        Long teamId = securityUtil.requireCurrentTeamIdForRead(null);
        Task task = taskRepository.findByIdAndTeamId(id, teamId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在"));
        return TaskDTO.fromEntity(task);
    }

    @Transactional
    @Audited(action = "CREATE_TASK", entityType = "TASK")
    public TaskDTO createTask(CreateTaskRequest request) {
        Long teamId = securityUtil.requireCurrentTeamIdForWrite(null);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("团队不存在"));
        User currentUser = securityUtil.getCurrentUser();

        Task task = Task.builder()
                .team(team)
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : Task.Priority.MEDIUM)
                .createdBy(currentUser)
                .dueDate(request.getDueDate())
                .build();

        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new IllegalArgumentException("被分配用户不存在"));
            task.setAssignee(assignee);
        }

        if (request.getAnnotationIds() != null) {
            task.setAnnotations(loadAnnotations(request.getAnnotationIds(), teamId));
        }

        return TaskDTO.fromEntity(taskRepository.save(task));
    }

    @Transactional
    @Audited(action = "UPDATE_TASK", entityType = "TASK", entityId = "#id")
    public TaskDTO updateTask(Long id, UpdateTaskRequest request) {
        Long teamId = securityUtil.requireCurrentTeamIdForWrite(null);
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
            if (request.getAssigneeId() == 0L) {
                task.setAssignee(null);
            } else {
                User assignee = userRepository.findById(request.getAssigneeId())
                        .orElseThrow(() -> new IllegalArgumentException("被分配用户不存在"));
                task.setAssignee(assignee);
            }
        }
        if (request.dueDateProvided()) {
            task.setDueDate(request.parseDueDate());
        }
        if (request.getAnnotationIds() != null) {
            task.setAnnotations(loadAnnotations(request.getAnnotationIds(), teamId));
        }

        return TaskDTO.fromEntity(taskRepository.save(task));
    }

    @Transactional
    @Audited(action = "UPDATE_TASK_STATUS", entityType = "TASK", entityId = "#id")
    public TaskDTO updateStatus(Long id, UpdateTaskStatusRequest request) {
        Long teamId = securityUtil.requireCurrentTeamIdForWrite(null);
        Task task = taskRepository.findByIdAndTeamId(id, teamId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在"));

        Task.Status oldStatus = task.getStatus();
        Task.Status newStatus = request.getStatus();

        AuditContext.put("oldStatus", oldStatus.name());
        AuditContext.put("newStatus", newStatus.name());

        task.setStatus(newStatus);
        return TaskDTO.fromEntity(taskRepository.save(task));
    }

    @Transactional
    @Audited(action = "ASSIGN_TASK", entityType = "TASK", entityId = "#id")
    public TaskDTO assignTask(Long id, AssignTaskRequest request) {
        Long teamId = securityUtil.requireCurrentTeamIdForWrite(null);
        Task task = taskRepository.findByIdAndTeamId(id, teamId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在"));

        Long assigneeId = request.getAssigneeId();
        if (assigneeId == null || assigneeId == 0L) {
            task.setAssignee(null);
        } else {
            User assignee = userRepository.findById(assigneeId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
            task.setAssignee(assignee);
        }

        AuditContext.put("assigneeId", assigneeId == null ? 0L : assigneeId);

        return TaskDTO.fromEntity(taskRepository.save(task));
    }

    @Transactional
    @Audited(action = "DELETE_TASK", entityType = "TASK", entityId = "#id")
    public void deleteTask(Long id) {
        Long teamId = securityUtil.requireCurrentTeamIdForWrite(null);
        Task task = taskRepository.findByIdAndTeamId(id, teamId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在"));
        taskRepository.delete(task);
    }

    private Set<Annotation> loadAnnotations(List<Long> annotationIds, Long teamId) {
        Set<Annotation> annotations = new HashSet<>();
        for (Long annId : annotationIds) {
            annotationRepository.findByIdAndTeamId(annId, teamId).ifPresent(annotations::add);
        }
        return annotations;
    }
}
