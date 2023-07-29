package org.kin.kinrpc.cluster.utils;

import org.kin.kinrpc.Filter;
import org.kin.kinrpc.cache.CacheFilter;
import org.kin.kinrpc.validation.ValidationFilter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author huangjianqin
 * @date 2023/7/29
 */
public final class ReferenceFilterUtils {
    private ReferenceFilterUtils() {
    }

    /**
     * reference内部前置filter
     */
    public static List<Filter> internalPreFilters() {
        return Collections.emptyList();
    }

    /**
     * reference内部后置filter
     */
    public static List<Filter> internalPostFilters() {
        return Arrays.asList(ValidationFilter.instance(), CacheFilter.instance());
    }
}
