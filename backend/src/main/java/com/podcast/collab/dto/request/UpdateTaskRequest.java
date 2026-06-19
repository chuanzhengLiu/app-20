package com.podcast.collab.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.podcast.collab.config.CustomLocalDateDeserializer;
import com.podcast.collab.entity.Task;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

@Data
public class UpdateTaskRequest {
    
    @Size(max = 200, message = "任务标题长度不能超过200个字符")
    private String title;
    
    private String description;
    
    private Task.Status status;
    
    private Task.Priority priority;
    
    private Long assigneeId;
    
    @JsonDeserialize(using = CustomLocalDateDeserializer.class)
    private LocalDate dueDate;
    
    private Set<Long> annotationIds;
}
