package unit;

/**
 * Created by 健勤 on 2017/2/13.
 */
public class HostPortRegexTest {
    public static void main(String[] args) {
        String address = "192.168.40.3:21811";
        System.out.println(address.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{1,5}"));
    }
}
