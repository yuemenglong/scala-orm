package io.github.yuemenglong.orm.api.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static io.github.yuemenglong.orm.api.anno.predef.Const.ANNOTATION_STRING_NULL;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExportDT {
    String value() default ANNOTATION_STRING_NULL;

    boolean ignore() default false;
}
