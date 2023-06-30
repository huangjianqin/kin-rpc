package org.kin.kinrpc.bootstrap;

import com.google.common.base.Preconditions;
import org.kin.framework.utils.CollectionUtils;
import org.kin.kinrpc.Exporter;
import org.kin.kinrpc.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 单service多server场景下, 仅对外暴露单个{@link Exporter}实现
 *
 * @author huangjianqin
 * @date 2023/6/30
 */
public class CompositeExporter<T> implements Exporter<T> {
    private static final Logger log = LoggerFactory.getLogger(CompositeExporter.class);

    /** service exporter */
    private final List<Exporter<T>> exporters = new ArrayList<>();

    public CompositeExporter(List<Exporter<T>> exporters) {
        Preconditions.checkArgument(CollectionUtils.isNonEmpty(exporters), "exporters must be not empty");
        this.exporters.addAll(exporters);
    }

    @Override
    public RpcService<T> service() {
        return exporters.get(0).service();
    }

    @Override
    public void unExport() {
        String service = service().service();
        for (Exporter<T> exporter : exporters) {
            try {
                exporter.unExport();
            } catch (Exception e) {
                log.error("service '{}' unExport error", service, e);
            }
        }
    }
}
