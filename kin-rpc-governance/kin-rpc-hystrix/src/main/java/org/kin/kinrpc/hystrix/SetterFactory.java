package org.kin.kinrpc.hystrix;

import com.netflix.hystrix.HystrixCommand;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.SPI;
import org.kin.kinrpc.Invocation;

import java.util.List;

/**
 * user自定义构造{@link HystrixCommand.Setter}工厂方法
 *
 * @author huangjianqin
 * @date 2023/8/9
 */
@SPI(value = "setterFactory")
public interface SetterFactory {
    /**
     * user自定义构造{@link HystrixCommand.Setter}实例
     *
     * @param invocation rpc call info
     * @return {@link HystrixCommand.Setter}实例
     */
    HystrixCommand.Setter createSetter(Invocation invocation);

    /**
     * 返回合适的{@link SetterFactory}实例, 如果没有, 则返回{@link DefaultSetterFactory}
     *
     * @return 合适的{@link SetterFactory}实例
     */
    static SetterFactory getSuitableSetterFactory() {
        List<SetterFactory> setterFactoryList = ExtensionLoader.getExtensions(SetterFactory.class);
        if (CollectionUtils.isNonEmpty(setterFactoryList)) {
            return setterFactoryList.get(0);
        } else {
            return DefaultSetterFactory.instance();
        }
    }
}
