package com.podcast.collab.dto.request;

import lombok.Data;

import java.util.Map;

@Data
public class UpdatePlatformRequest {
    
    private String name;
    
    private Map<String, Object> config;
}
