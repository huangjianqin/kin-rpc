package unit;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.kinrpc.common.Constants;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Created by 健勤 on 2017/2/16.
 */
public class ZookeeperUtil {
    private static CountDownLatch latch = new CountDownLatch(1);

    public static void main(String[] args) throws InterruptedException, IOException, KeeperException {
        ZooKeeper zooKeeper = new ZooKeeper("localhost", 5000, new Watcher() {
            public void process(WatchedEvent watchedEvent) {
                if(watchedEvent.getState() == Event.KeeperState.SyncConnected){
                    latch.countDown();
                }
            }
        });
        latch.await();

        printChilds(zooKeeper, "/");

        zooKeeper.close();
    }

    public static void printChilds(ZooKeeper zooKeeper, String path) throws KeeperException, InterruptedException {
        List<String> childs = zooKeeper.getChildren(path, false);

        for(String child: childs){
            System.out.println(child);
        }
    }

    public static void deleteZNode(ZooKeeper zooKeeper, String path) throws KeeperException, InterruptedException {
        zooKeeper.delete(path, -1);
    }

    /**
     * 在Zookeeper上删除root下的所有节点
     * @param zooKeeper
     * @param root
     */
    public static void deleteAllZNode(ZooKeeper zooKeeper, String root) throws KeeperException, InterruptedException {
        List<String> childs = zooKeeper.getChildren(root, false);

        for(String child: childs){
            if(zooKeeper.getChildren(child, false).size() > 0){
                deleteAllZNode(zooKeeper, child);
            }
            else{
                deleteZNode(zooKeeper, child);
            }
        }
    }
}
