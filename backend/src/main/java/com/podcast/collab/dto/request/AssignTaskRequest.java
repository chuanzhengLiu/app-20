package com.podcast.collab.dto.request;

import lombok.Data;

@Data
public class AssignTaskRequest {

    /**
     * 0 或 null 表示清空指派人。
     */
    private Long assigneeId;
}
