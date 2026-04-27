package com.zhangyichuang.medicine.common.core.validation;

import com.zhangyichuang.medicine.common.core.annotation.PowerOfTwo;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 2 的次方数值校验器。
 */
public class PowerOfTwoValidator implements ConstraintValidator<PowerOfTwo, Number> {

    @Override
    public boolean isValid(Number value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        long number = value.longValue();
        return number > 0 && (number & (number - 1)) == 0;
    }
}
