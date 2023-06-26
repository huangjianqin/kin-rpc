package org.kin.kinrpc.registry.directory;

import com.google.common.base.Preconditions;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.ExtensionLoader;
import org.kin.kinrpc.ReferenceInvoker;
import org.kin.kinrpc.ServiceInstance;
import org.kin.kinrpc.rpc.AsyncInvoker;
import org.kin.kinrpc.rpc.Invoker;
import org.kin.kinrpc.rpc.Protocol;
import org.kin.kinrpc.rpc.common.Url;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author huangjianqin
 * @date 2019/6/11
 */
public class DefaultDirectory implements Directory {
    private static final Logger log = LoggerFactory.getLogger(DefaultDirectory.class);

    /** 订阅的服务名 */
    private final String serviceName;
    /** 所有directory的discover和destroy操作都是单线程操作, 利用copy-on-write思想更新可用invokers, 提高list效率 */
    private volatile List<AsyncInvoker> invokers = Collections.emptyList();

    private volatile boolean isStopped;

    public DefaultDirectory(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * 获取活跃的ReferenceInvoker
     */
    private List<AsyncInvoker> getActiveReferenceInvoker() {
        return invokers.stream().filter(Invoker::isAvailable).collect(Collectors.toList());
    }

    /**
     * 更新可用ReferenceInvoker列表
     */
    private void updateInvokers(List<AsyncInvoker> newInvokers) {
        invokers = Collections.unmodifiableList(newInvokers);
    }

    @Override
    public List<ReferenceInvoker<?>> list() {
        //Directory关闭中调用该方法会返回一个size=0的列表
        if (!isStopped) {
            return getActiveReferenceInvoker();
        }
        return Collections.emptyList();
    }

    @Override
    public void discover(List<ServiceInstance> serviceInstances) {
        if (!isStopped) {
            //利用consumer url覆盖provider url
            urls = urls.stream().map(url -> Url.mergeUrl(referenceUrl, url)).collect(Collectors.toList());

            ArrayList<AsyncInvoker> originInvokers = new ArrayList<>(invokers);

            StringBuilder sb = new StringBuilder();
            List<String> addresses = new ArrayList<>(urls.size());
            if (CollectionUtils.isNonEmpty(urls)) {
                for (Url url : urls) {
                    String address = url.getAddress();

                    addresses.add(address);
                    sb.append(address).append(", ");
                }
            }
            log.info("discover service '{}'..."
                    .concat("current service address: ")
                    .concat(sb.toString()), getServiceName());

            List<AsyncInvoker> validInvokers = new ArrayList<>(urls.size());
            List<AsyncInvoker> invalidInvokers = new ArrayList<>(originInvokers.size());
            if (CollectionUtils.isNonEmpty(urls)) {
                for (AsyncInvoker originInvoker : originInvokers) {
                    Url url = originInvoker.url();
                    String address = url.getAddress();

                    if (!addresses.contains(address)) {
                        //无效invoker
                        invalidInvokers.add(originInvoker);
                    } else {
                        //invoker仍然有效
                        validInvokers.add(originInvoker);
                        urls.removeIf(item -> item.getAddress().equals(address));
                    }
                }
            } else {
                //如果服务取消注册或者没有子节点(注册了但没有启动完连接),关闭所有现有的invoker
                invalidInvokers.addAll(originInvokers);
            }

            //new ReferenceInvokers
            for (Url url : urls) {
                String protocolName = url.getProtocol();
                Protocol protocol = ExtensionLoader.getExtension(Protocol.class, protocolName);

                Preconditions.checkNotNull(protocol, String.format("unknown protocol: %s", protocolName));

                AsyncInvoker referenceInvoker = null;
                try {
                    referenceInvoker = protocol.refer(url);
                } catch (Throwable throwable) {
                    ExceptionUtils.throwExt(throwable);
                }
                validInvokers.add(referenceInvoker);
            }

            //remove invalid ReferenceInvokers
            for (Invoker<?> invoker : invalidInvokers) {
                invoker.destroy();
            }

            //update cache
            updateInvokers(validInvokers);

            log.info("discover service '{}' finished", getServiceName());
        }
    }

    @Override
    public void destroy() {
        if (!isStopped) {
            isStopped = true;
            for (AsyncInvoker<?> invoker : invokers) {
                invoker.destroy();
            }
            invokers = null;
            log.info("zookeeper directory destroyed");
        }
    }

    @Override
    public String service() {
        // TODO: 2023/6/25
        return null;
    }
}
