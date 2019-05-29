package org.kin.kinrpc.common;

/**
 * Created by 健勤 on 2017/2/12.
 */
public class LogUtil {
    public static String mkString(String separator,String... contents){
        if(contents != null && contents.length > 0){
            String result = contents[0];

            for(int i = 1; i < contents.length; i++){
                result += (", " + contents[i]);
            }

            return result;
        }

        return null;
    }
}
