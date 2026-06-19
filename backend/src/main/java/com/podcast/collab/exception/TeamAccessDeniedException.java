package com.podcast.collab.exception;

public class TeamAccessDeniedException extends RuntimeException {
    
    public TeamAccessDeniedException(String message) {
        super(message);
    }
}
