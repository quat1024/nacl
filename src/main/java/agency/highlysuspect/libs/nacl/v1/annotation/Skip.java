package agency.highlysuspect.libs.nacl.v1.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This field does not represent a value in the config file.
 * Alternatively, you may mark fields with the "static", "final", or "transient" modifiers.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Skip {
}
