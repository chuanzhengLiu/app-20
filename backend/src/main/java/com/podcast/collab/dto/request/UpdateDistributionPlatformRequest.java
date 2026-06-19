package com.podcast.collab.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
public class UpdateDistributionPlatformRequest {
    
    @Size(max = 100, message = "平台名称长度不能超过100个字符")
    private String name;
    
    private Map<String, Object> config;
}
