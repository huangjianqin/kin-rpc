package org.kin.kinrpc.config.test.consumer;


import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.References;
import org.kin.kinrpc.config.test.service.Addable;
import org.kin.kinrpc.transport.statistic.InOutBoundStatisicService;

/**
 * Created by 健勤 on 2017/2/16.
 */
public class AddableConsumer {
    public static void main(String[] args) {
        ReferenceConfig<Addable> referenceConfig =
                References.reference(Addable.class).urls("127.0.0.1:16888").retry(0);
        Addable service = referenceConfig.get();
        System.out.println("结果" + service.add(1, 1));
        referenceConfig.disable();
        InOutBoundStatisicService.instance().close();
    }
}
