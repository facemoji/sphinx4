package edu.cmu.sphinx.util.props;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * * A double property.
 *
 * @author Holger Brandl
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@S4Property
public @interface S4Double {

    /**
     * Default value to return
     */
    double NOT_DEFINED = -918273645.12345; // not bullet-proof, but should work in most cases

    double defaultValue() default NOT_DEFINED;

}
