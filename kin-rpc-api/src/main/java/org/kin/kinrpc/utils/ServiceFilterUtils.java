package org.kin.kinrpc.utils;

import org.kin.kinrpc.Filter;
import org.kin.kinrpc.validation.ValidationFilter;

import java.util.Collections;
import java.util.List;

/**
 * @author huangjianqin
 * @date 2023/7/29
 */
public final class ServiceFilterUtils {
    private ServiceFilterUtils() {
    }

    /**
     * service内部前置filter
     */
    public static List<Filter> internalPreFilters() {
        return Collections.emptyList();
    }

    /**
     * service内部后置filter
     */
    public static List<Filter> internalPostFilters() {
        return Collections.singletonList(ValidationFilter.instance());
    }
}
