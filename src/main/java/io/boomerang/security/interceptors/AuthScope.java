package io.boomerang.security.interceptors;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import io.boomerang.security.model.TokenAccess;
import io.boomerang.security.model.TokenObject;
import io.boomerang.security.model.TokenScope;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthScope {
 TokenObject object();
 TokenAccess access();
 TokenScope[] types();
}