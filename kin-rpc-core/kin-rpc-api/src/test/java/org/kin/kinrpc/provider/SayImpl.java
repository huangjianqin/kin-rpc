package org.kin.kinrpc.provider;

import java.util.Random;

/**
 * @author huangjianqin
 * @date 2019-08-21
 */
public class SayImpl extends AbstractSay {
    @Override
    public void say() {
        long sum = 0;
        for (int i = 0; i < 100000; i++) {
            sum += i;
        }
    }
}
