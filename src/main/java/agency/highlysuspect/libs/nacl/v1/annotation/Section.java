package agency.highlysuspect.libs.nacl.v1.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Prints a big comment that looks like this:
 * 
 *    ################
 *    ## My Section ##
 *    ################
 *    
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Section {
	String value();
}
