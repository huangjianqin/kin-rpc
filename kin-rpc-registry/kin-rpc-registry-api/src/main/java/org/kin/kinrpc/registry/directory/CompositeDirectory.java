package org.kin.kinrpc.registry.directory;

import org.kin.kinrpc.ReferenceInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 适用于多注册中心场景下, 仅对外暴露单个{@link Directory}实现
 *
 * @author huangjianqin
 * @date 2023/6/25
 */
public final class CompositeDirectory implements Directory {
    private static final Logger log = LoggerFactory.getLogger(CompositeDirectory.class);
    /** 订阅同一服务的{@link Directory}实例数组 */
    private final List<Directory> directories;

    public CompositeDirectory(List<Directory> directories) {
        Set<String> services = new HashSet<>();
        for (Directory directory : directories) {
            services.add(directory.service());
        }

        if (services.size() != 1) {
            throw new UnsupportedOperationException("directories must be not empty and subscribe same service");
        }

        this.directories = directories;
    }

    @Override
    public List<ReferenceInvoker<?>> list() {
        return directories.stream().flatMap(d -> d.list().stream()).distinct().collect(Collectors.toList());
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void destroy() {
        for (Directory directory : directories) {
            try {
                directory.destroy();
            } catch (Exception e) {
                log.error("directory destroy fail", e);
            }
        }
    }

    @Override
    public String service() {
        return directories.get(0).service();
    }
}
