package agency.highlysuspect.libs.nacl.v1.annotation;

import java.lang.annotation.*;

/**
 * Adds a blank line before this config option.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface BlankLine {
	int lines() default 1;
}
