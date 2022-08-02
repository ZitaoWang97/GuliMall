package com.zitao.common.valid;


import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


@Documented
// 将注解与自定义的校验器进行关联
@Constraint(validatedBy = {ListValueConstraintValidator.class}) // 可以适配多个校验器
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface ListValue {
    // 以下这三个属性是必备的

    /**
     * 校验出错信息放在 ValidationMessages.properties
     * @return
     */
    String message() default "{com.zitao.common.valid.ListValue.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int[] vals() default {};
}
