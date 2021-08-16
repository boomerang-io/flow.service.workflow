package io.boomerang.security.interceptors;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.*;
import io.boomerang.mongo.model.TokenScope;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthenticationScope {
  TokenScope[] scopes();
}

