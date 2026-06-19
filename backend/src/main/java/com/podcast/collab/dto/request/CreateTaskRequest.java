package com.podcast.collab.dto.request;

import com.podcast.collab.entity.Task;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 创建任务的请求体。
 * 替代原先 Controller 里通过 Map<String, Object> 手动取字段、手动转换的方式，
 * 由 Jackson + jakarta.validation 自动完成类型转换与校验。
 */
@Data
public class CreateTaskRequest {

    @NotBlank(message = "任务标题不能为空")
    @Size(max = 200, message = "任务标题长度不能超过 200")
    private String title;

    private String description;

    private Task.Priority priority;

    private Long assigneeId;

    private LocalDate dueDate;

    private List<Long> annotationIds;
}
