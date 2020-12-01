package org.kin.kinrpc.transport.grpc;

/**
 * @author huangjianqin
 * @date 2020/12/1
 */
public class Grpcs {
    /**
     * This method mimics the upper-casing method ogf gRPC to ensure compatibility
     * See https://github.com/grpc/grpc-java/blob/v1.8.0/compiler/src/java_plugin/cpp/java_generator.cpp#L58
     */
    public static String methodNameUpperUnderscore(String methodName) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < methodName.length(); i++) {
            char c = methodName.charAt(i);
            s.append(Character.toUpperCase(c));
            if ((i < methodName.length() - 1) && Character.isLowerCase(c) && Character.isUpperCase(methodName.charAt(i + 1))) {
                s.append('_');
            }
        }
        return s.toString();
    }

    public static String methodNamePascalCase(String methodName) {
        String mn = methodName.replace("_", "");
        return String.valueOf(Character.toUpperCase(mn.charAt(0))) + mn.substring(1);
    }

    public static String methodNameCamelCase(String methodName) {
        String mn = methodName.replace("_", "");
        return String.valueOf(Character.toLowerCase(mn.charAt(0))) + mn.substring(1);
    }
}
