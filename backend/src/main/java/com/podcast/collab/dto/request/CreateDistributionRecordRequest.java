package com.podcast.collab.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class CreateDistributionRecordRequest {

    @NotNull(message = "节目ID不能为空")
    private Long episodeId;

    @NotNull(message = "平台ID不能为空")
    private Long platformId;

    private Map<String, Object> metadata;
}
