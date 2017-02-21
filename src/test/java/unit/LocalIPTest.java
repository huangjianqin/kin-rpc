package unit;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by 健勤 on 2017/2/13.
 */
public class LocalIPTest {
    public static void main(String[] args) throws UnknownHostException {
        System.out.println(InetAddress.getLocalHost());
    }
}
