package unit;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * Created by 健勤 on 2017/3/20.
 */
public class MyLocalIPTest {
    public static void main(String[] args) throws SocketException {
        //linux获取内网ip
//        Enumeration netInterfaces = NetworkInterface.getNetworkInterfaces();
//        InetAddress ip = null;
//        while(netInterfaces.hasMoreElements()){
//            NetworkInterface ni = (NetworkInterface)netInterfaces.nextElement();
//            System.out.println(ni.getName());
//            ip = (InetAddress)ni.getInetAddresses().nextElement();
//            System.out.println(ip);
//            if(!ip.isSiteLocalAddress() && !ip.isLinkLocalAddress() && ip.getHostAddress().indexOf(":") == -1){
//                System.out.println("本机IP >>>> " + ip.getHostAddress());
//                break;
//            }
//            else{
//                ip = null;
//            }
//        }
        //windows获取内网ip
        try {
            System.out.println(InetAddress.getAllByName(InetAddress.getLocalHost().getHostName())[0].getHostAddress());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
