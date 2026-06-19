package com.podcast.collab.dto.request;

import com.podcast.collab.entity.Task;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateTaskStatusRequest {
    
    @NotNull(message = "状态不能为空")
    private Task.Status status;
}
