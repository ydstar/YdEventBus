package com.eventbus.write;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Subscribe {
    ThreadMode threadMode() default ThreadMode.POSTING;

    /**
     * 是否是黏性事件
     * @return
     */
    boolean sticky() default false;

    /**
     * 优先级,值越大优先级越高
     * @return
     */
    int priority() default 0;
}

