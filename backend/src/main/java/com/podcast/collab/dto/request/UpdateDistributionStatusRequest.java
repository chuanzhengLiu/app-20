package com.podcast.collab.dto.request;

import com.podcast.collab.entity.DistributionRecord;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateDistributionStatusRequest {

    @NotNull(message = "状态不能为空")
    private DistributionRecord.Status status;

    private String publishUrl;

    private String errorMessage;
}
