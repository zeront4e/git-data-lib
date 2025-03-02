package io.github.zeront4e.gdl.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a string field (property) that should be encrypted.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
public @interface GdlSecretProperty {
    /**
     * The name of the secret to decrypt the property with.
     * @return The secret name.
     */
    String value() default "secret";
}
