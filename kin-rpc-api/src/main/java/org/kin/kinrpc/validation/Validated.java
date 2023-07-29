package org.kin.kinrpc.validation;

import java.lang.annotation.*;

/**
 * Variant of JSR-303's {@link javax.validation.Valid}, supporting the
 * specification of validation groups. Designed for convenient use with
 * Spring's JSR-303 support but not JSR-303 specific.
 * <p>Can be used e.g. with Spring MVC handler methods arguments.
 * Supported through {@link org.springframework.validation.SmartValidator}'s
 * validation hint concept, with validation group classes acting as hint objects.
 * <p>Can also be used with method level validation, indicating that a specific
 * class is supposed to be validated at the method level (acting as a pointcut
 * for the corresponding validation interceptor), but also optionally specifying
 * the validation groups for method-level validation in the annotated class.
 * Applying this annotation at the method level allows for overriding the
 * validation groups for a specific method but does not serve as a pointcut;
 * a class-level annotation is nevertheless necessary to trigger method validation
 * for a specific bean to begin with. Can also be used as a meta-annotation on a
 * custom stereotype annotation or a custom group-specific validated annotation.
 *
 * @author huangjianqin
 * @date 2023/7/27
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Validated {
    /**
     * Specify one or more validation groups to apply to the validation step
     * kicked off by this annotation.
     * <p>JSR-303 defines validation groups as custom annotations which an application declares
     * for the sole purpose of using them as type-safe group arguments, as implemented in
     * {@link org.springframework.validation.beanvalidation.SpringValidatorAdapter}.
     * <p>Other {@link org.springframework.validation.SmartValidator} implementations may
     * support class arguments in other ways as well.
     */
    Class<?>[] value() default {};
}
