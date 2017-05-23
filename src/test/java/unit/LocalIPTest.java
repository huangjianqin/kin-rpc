package unit;

import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by 健勤 on 2017/2/13.
 */
public class LocalIPTest {
    public static void main(String[] args) throws UnknownHostException {
        //window下可以
        //linux不可以
//        System.out.println(InetAddress.getLocalHost());
        System.out.println("操作系统为："+SystemOperate.fromCode(IPUtils.getOsType()+""));
        System.out.println("内网IP为："+IPUtils.getIP(2));
        System.out.println("外网IP为："+IPUtils.getIP(1));
    }
}
class IPUtils {
    private static String OS_NAME = null;
    /**
     * 查询本机外网IP网站
     */
    private static final String getWebIP = "http://www.ip138.com/ip2city.asp";
    /**
     * 默认值
     */
    private static String IP = "未知";
    static {
        System.out.println("初始化获取系统名称...");
        OS_NAME = System.getProperty("os.name");
    }

    public static String getIP(int queryFlag) {
        if (queryFlag == 1) {
            // 查询外网IP
            switch (IPUtils.getOsType()) {
                case 1:
                    IP = IPUtils.getWinOuterIP();
                    break;
                case 2:
                    IP = IPUtils.getLinuxIP(queryFlag);
                    break;
                default:
                    break;
            }
        } else {
            // 查询内网IP
            switch (IPUtils.getOsType()) {
                case 1:
                    IP = IPUtils.getWinInnerIP();
                    break;
                case 2:
                    IP = IPUtils.getLinuxIP(queryFlag);
                    break;
                default:
                    break;
            }
        }

        return IP;
    }

    /**
     * 获取window平台下外网IP
     *
     * @return IP
     */
    private static String getWinOuterIP() {
        try {
            URL url = new URL(getWebIP);
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            String s = "";
            StringBuffer sb = new StringBuffer("");
            String webContent = "";
            while ((s = br.readLine()) != null) {
                //System.err.println("---"+s);
                sb.append(s + "\r\n");
            }
            br.close();
            webContent = sb.toString();
            int start = webContent.indexOf("[") + 1;
            int end = webContent.indexOf("]");
            webContent = webContent.substring(start, end);
            return webContent;
        } catch (Exception e) {
            //e.printStackTrace();
            System.err.println("获取外网IP网站访问失败！");
            return IP;
        }

    }

    /**
     * 获取window平台下内网IP
     *
     * @return IP
     */
    private static String getWinInnerIP() {
        InetAddress[] inetAdds;
        try {
            inetAdds = InetAddress.getAllByName(InetAddress.getLocalHost()
                    .getHostName());
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return IP;
        }
        return inetAdds[0].getHostAddress();
    }

    /**
     * 获取linux下的IP
     * @param queryFlag
     * 1表示查询外网IP 2表示查询内网IP
     * @return IP
     * @throws IOException
     */
    private static String getLinuxIP(int queryFlag) {
        LineNumberReader input = null;
        String pathString = IPUtils.class.getResource("/").getPath();
        //类的路径
        //System.out.println(pathString);
        Process process=null;
        String line = "";
        try {
            Runtime.getRuntime().exec("dos2unix "+pathString+"test.sh");
            process = Runtime.getRuntime().exec("sh "+pathString+"test.sh "+(queryFlag==1?"1":"2"));
            InputStreamReader ir = new InputStreamReader(process.getInputStream());
            input = new LineNumberReader(ir);
            if((line = input.readLine()) != null) {
                IP = line;
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("linux下获取IP失败!");
        }
        //System.out.println("exec shell result:ip====>" + IP);
        return IP;
    }
    /**
     * 目前只支持window和linux两种平台
     *
     * @return 1 window 2 linux -1:未知
     */
    public static int getOsType() {
        // 将获取到的系统类型名称转为全部小写
        OS_NAME = OS_NAME.toLowerCase();
        if (OS_NAME.startsWith("win")) {
            return 1;
        }
        if (OS_NAME.startsWith("linux")) {
            return 2;
        }
        return -1;
    }
}

enum SystemOperate{
    WINDOWS(1,"windows系统"),LINUX(2,"linux系统"),UNKNOWN(3,"未知系统");

    private int operateType;
    private String operateName;

    private SystemOperate(int operateType, String operateName) {
        this.operateType = operateType;
        this.operateName = operateName;
    }
    public int getOperateType() {
        return operateType;
    }
    public void setOperateType(int operateType) {
        this.operateType = operateType;
    }
    public String getOperateName() {
        return operateName;
    }
    public void setOperateName(String operateName) {
        this.operateName = operateName;
    }
    private static final Map<String, SystemOperate> lookup = new HashMap<String, SystemOperate>();
    static {
        for (SystemOperate cp : values()) {
            lookup.put(cp.getOperateType()+"", cp);
        }
    }
    public static String fromCode(String code) {
        return lookup.get(code).operateName;
    }
}