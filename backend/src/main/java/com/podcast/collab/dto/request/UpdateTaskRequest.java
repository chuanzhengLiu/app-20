package com.podcast.collab.dto.request;

import com.podcast.collab.entity.Task;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

@Data
public class UpdateTaskRequest {

    @Size(max = 200, message = "任务标题不能超过200个字符")
    private String title;

    private String description;

    private Task.Status status;

    private Task.Priority priority;

    private Long assigneeId;

    private LocalDate dueDate;

    private Set<Long> annotationIds;
}
