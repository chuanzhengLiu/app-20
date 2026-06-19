package com.podcast.collab.dto.request;

import com.podcast.collab.entity.Task;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 更新任务的请求体。所有字段均为可选，未提供时不修改对应字段。
 */
@Data
public class UpdateTaskRequest {

    @Size(max = 200, message = "任务标题长度不能超过 200")
    private String title;

    private String description;

    private Task.Status status;

    private Task.Priority priority;

    /**
     * 0 表示清空指派人。
     */
    private Long assigneeId;

    /**
     * 字符串形式，空字符串表示清空截止日期；其它情况按 ISO yyyy-MM-dd 解析。
     * 保持与原有前端契约一致。
     */
    private String dueDate;

    private List<Long> annotationIds;

    public LocalDate parseDueDate() {
        if (dueDate == null) {
            return null;
        }
        if (dueDate.isEmpty()) {
            return null;
        }
        return LocalDate.parse(dueDate);
    }

    public boolean dueDateProvided() {
        return dueDate != null;
    }
}
