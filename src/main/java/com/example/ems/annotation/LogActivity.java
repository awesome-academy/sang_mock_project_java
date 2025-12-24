package com.example.ems.annotation;

import com.example.ems.constant.EntityType;
import com.example.ems.constant.LogAction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogActivity {
    LogAction action();
    EntityType entityType();
    Class<?> entityClass();
}
