package com.podcast.collab.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TeamScope {
    
    String teamIdParam() default "teamId";
    
    boolean validateTeamIdParam() default true;
    
    String message() default "无权访问其他团队数据";
}
