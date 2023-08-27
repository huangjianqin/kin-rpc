package org.kin.kinrpc.common;

public final class Version {
    private static String VERSION = "0.2.0.0";
    private static String BUILD_TIME = "2023-08-27 13:43:14";


    public static String getVersion() {
        return VERSION;
    }

    public static String getBuildTime() {
        return BUILD_TIME;
    }
}
