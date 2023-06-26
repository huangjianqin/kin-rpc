package org.kin.kinrpc.registry.directory;

import org.kin.kinrpc.ReferenceInvoker;
import org.kin.kinrpc.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 多注册中心场景下, 对外invoker暴露的{@link Directory}实现
 *
 * @author huangjianqin
 * @date 2023/6/25
 */
public final class MultiDirectory implements Directory {
    private static final Logger log = LoggerFactory.getLogger(MultiDirectory.class);

    private final List<Directory> directories;

    public MultiDirectory(List<Directory> directories) {
        this.directories = directories;
    }

    @Override
    public List<ReferenceInvoker<?>> list() {
        return directories.stream().flatMap(d -> d.list().stream()).distinct().collect(Collectors.toList());
    }

    @Override
    public void discover(List<ServiceInstance> serviceInstances) {
        throw new UnsupportedOperationException();
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
