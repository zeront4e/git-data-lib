package io.github.zeront4e.gdl.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a data-object class to indicate that the whole data should be encrypted.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
public @interface GdlSecretDataRepository {
    /**
     * The name of the secret to encrypt/decrypt the data with.
     * @return The secret name.
     */
    String value() default "secret";
}
