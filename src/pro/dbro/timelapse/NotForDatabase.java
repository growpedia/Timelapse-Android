package pro.dbro.timelapse;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
 
// Gotcha! By default, annotations are not available at runtime
// you've gotta explicitly set RetentionPolicy.RUNTIME
@Retention(RetentionPolicy.RUNTIME)
public @interface NotForDatabase {

}