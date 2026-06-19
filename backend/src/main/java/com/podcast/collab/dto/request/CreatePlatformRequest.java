package com.podcast.collab.dto.request;

import com.podcast.collab.entity.DistributionPlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
public class CreatePlatformRequest {

    @NotBlank(message = "平台名称不能为空")
    @Size(max = 100, message = "平台名称长度不能超过 100")
    private String name;

    @NotNull(message = "平台类型不能为空")
    private DistributionPlatform.PlatformType type;

    private Map<String, Object> config;
}
