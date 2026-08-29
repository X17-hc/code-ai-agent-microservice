package com.hechang.codeagent.ratelimiter.annotation;

import com.hechang.codeagent.ratelimiter.enums.RateLimitType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 限流注解
 */
@Target(ElementType.METHOD)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 限流key前缀
     */
    String key() default "";

    /**
     * 限流类型
     */
    int rate() default 10;

    /**
     * 限流时间间隔
     */
    int rateInterval() default 1;

    /**
     * 限流类型
     */
    RateLimitType limitType() default RateLimitType.USER;

    /**
     * 限流提示信息
     */
    String message() default"请求过于频繁，请稍后再试";

}
