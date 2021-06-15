package agency.highlysuspect.libs.nacl.v1.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Require the numeric value to be greater-than-or-equal-to a threshold value.
 * Only specify one type, please.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AtLeast {
	byte byteValue() default Byte.MIN_VALUE;
	short shortValue() default Short.MIN_VALUE;
	int intValue() default Integer.MIN_VALUE;
	long longValue() default Long.MIN_VALUE;
	float floatValue() default Float.NaN;
	double doubleValue() default Double.NaN;
}
