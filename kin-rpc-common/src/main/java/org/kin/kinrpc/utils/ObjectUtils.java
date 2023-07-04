package org.kin.kinrpc.utils;

import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2023/7/4
 */
public final class ObjectUtils {
    private ObjectUtils() {
    }

    /**
     * 如果{@code obj}非null, 则返回s, 否则返回""
     *
     * @param obj 目标对象
     * @param s   目标字符串
     * @return return s if {@code obj} is not null
     */
    public static String toStringIfNonNull(Object obj, String s) {
        return toStringIfPredicate(Objects.nonNull(obj), s);
    }

    /**
     * 如果{@code expression}为true, 则返回s, 否则返回""
     *
     * @param expression 条件表达式结果
     * @param s          目标字符串
     * @return return s if {@code expression} is true
     */
    public static String toStringIfPredicate(boolean expression, String s) {
        return expression ? s : "";
    }
}
