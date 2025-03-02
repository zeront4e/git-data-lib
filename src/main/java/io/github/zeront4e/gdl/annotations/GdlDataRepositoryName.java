package io.github.zeront4e.gdl.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to set a custom data repository name for data object instances to manage. If this annotation is not
 * present, the default repository name is created using the class name of the class to store.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
public @interface GdlDataRepositoryName {
    /**
     * The data repository name for data object instances to manage. If this annotation is not present,
     * the default repository name is created using the class name of the class to store.
     * @return The data repository name.
     */
    String value();
}
