package org.kin.kinrpc.registry;

import com.google.common.base.Preconditions;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.kinrpc.rpc.AsyncInvoker;
import org.kin.kinrpc.rpc.Invoker;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.transport.Protocol;
import org.kin.kinrpc.transport.Protocols;
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
public final class Directory {
    private static final Logger log = LoggerFactory.getLogger(Directory.class);

    /** 订阅的服务名 */
    private final String serviceName;
    /** 所有directory的discover和destroy操作都是单线程操作, 利用copy-on-write思想更新可用invokers, 提高list效率 */
    private volatile List<AsyncInvoker> invokers = Collections.emptyList();

    private volatile boolean isStopped;

    public Directory(String serviceName) {
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

    /**
     * 获取当前可用invokers
     */
    public List<AsyncInvoker> list() {
        //Directory关闭中调用该方法会返回一个size=0的列表
        if (!isStopped) {
            return getActiveReferenceInvoker();
        }
        return Collections.emptyList();
    }

    /**
     * 发现可用服务的address, 并构建reference invoker
     */
    public void discover(Url referenceUrl, List<Url> urls) {
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
                Protocol protocol = Protocols.INSTANCE.getExtension(protocolName);

                Preconditions.checkNotNull(protocol, String.format("unknown protocol: %s", protocolName));

                AsyncInvoker referenceInvoker = null;
                try {
                    referenceInvoker = protocol.reference(url);
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

    /**
     * @return 服务名
     */
    public String getServiceName() {
        return serviceName;
    }
}
