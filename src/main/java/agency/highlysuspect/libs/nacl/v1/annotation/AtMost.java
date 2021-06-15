package agency.highlysuspect.libs.nacl.v1.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Require the numeric value to be less-than-or-equal-to a threshold value.
 * Only specify one type, please.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AtMost {
	byte byteValue() default Byte.MAX_VALUE;
	short shortValue() default Short.MAX_VALUE;
	int intValue() default Integer.MAX_VALUE;
	long longValue() default Long.MAX_VALUE;
	float floatValue() default Float.NaN;
	double doubleValue() default Double.NaN;
}
