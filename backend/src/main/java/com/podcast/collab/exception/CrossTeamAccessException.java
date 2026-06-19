package com.podcast.collab.exception;

/**
 * 跨团队访问/操作时抛出的业务异常。
 * 由 GlobalExceptionHandler 转换为 HTTP 400 + ApiResponse.error(message)，
 * 与改造前 Controller 中 ResponseEntity.badRequest().body(ApiResponse.error(...)) 的响应体完全一致，
 * 不携带 code 字段，保持前端契约不变。
 */
public class CrossTeamAccessException extends RuntimeException {

    public CrossTeamAccessException(String message) {
        super(message);
    }
}
