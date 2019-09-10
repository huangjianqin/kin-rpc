package org.kin.kinrpc.rpc.invoker.proxy;

import org.kin.kinrpc.rpc.invoker.JavassistMethodInvoker;
import org.kin.kinrpc.rpc.utils.JavassistUtils;

import java.util.Map;

/**
 * @author huangjianqin
 * @date 2019-09-09
 */
public class JavassistTest {
    public static void main(String[] args) {
        Map<String, JavassistMethodInvoker> result = JavassistUtils.generateProviderMethodProxy(new AddableImpl(), Addable.class, Addable.class.getName());
        JavassistMethodInvoker javassistMethodInvoker = result.get("add(int,int)");
        try {
            System.out.println(javassistMethodInvoker.invoke(1, 2));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface Addable {
        int add(int a, int b);

        void print(int a);
    }

    public static class AddableImpl implements Addable {

        @Override
        public int add(int a, int b) {
            return a + b;
        }

        @Override
        public void print(int a) {
            System.out.println(a);
        }
    }
}
