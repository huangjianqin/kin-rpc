package org.kin.kinrpc.transport.statistic;

import org.kin.framework.Closeable;
import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.concurrent.ThreadManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by huangjianqin on 2019/6/3.
 *
 * important 要手动close
 */
public class InOutBoundStatisicService implements Closeable{
    private static final Logger reqStatisticLog = LoggerFactory.getLogger("org.kin.kinrpc.transport.reqStatistic");
    private static final Logger respStatisticLog = LoggerFactory.getLogger("org.kin.kinrpc.transport.respStatistic");
    private static final InOutBoundStatisicService INSTANCE = new InOutBoundStatisicService();
    static {
        JvmCloseCleaner.DEFAULT().add(INSTANCE);
    }

    private InOutBoundStatisticHolder reqHolder = new InOutBoundStatisticHolder();
    private InOutBoundStatisticHolder respHolder = new InOutBoundStatisticHolder();
    private ThreadManager threadManager = new ThreadManager(null,
            new ScheduledThreadPoolExecutor(3,
                    new SimpleThreadFactory("inoutbound-statisic")));

    private InOutBoundStatisicService() {
        //一分钟打印一次
        threadManager.scheduleAtFixedRate(() -> {
            logReqStatistic();
            logRespStatistic();
        }, 1, 1, TimeUnit.MINUTES);
    }

    public static InOutBoundStatisicService instance() {
        return INSTANCE;
    }

    @Override
    public void close() {
        threadManager.shutdown();
    }
    //-------------------------------------------------------------------------------------------------------
    private void logReqStatistic() {
        InOutBoundStatisticHolder origin = reqHolder;
        reqHolder = new InOutBoundStatisticHolder();
        logReqStatistic0(origin);
    }

    private void logReqStatistic0(InOutBoundStatisticHolder origin) {
        if (origin != null) {
            if (origin.getRef() > 0) {
                threadManager.schedule(() -> logReqStatistic0(origin), 50, TimeUnit.MILLISECONDS);
                return;
            }
            logReqStatistic1(origin);
        }
    }

    private void logReqStatistic1(InOutBoundStatisticHolder origin) {
        String content = origin.logContent();
        reqStatisticLog.info(content);
    }

    //-------------------------------------------------------------------------------------------------------
    private void logRespStatistic() {
        InOutBoundStatisticHolder origin = respHolder;
        respHolder = new InOutBoundStatisticHolder();
        logRespStatistic0(origin);
    }

    private void logRespStatistic0(InOutBoundStatisticHolder origin) {
        if (origin != null) {
            if (origin.getRef() > 0) {
                threadManager.schedule(() -> logRespStatistic0(origin), 50, TimeUnit.MILLISECONDS);
                return;
            }
            logRespStatistic1(origin);
        }
    }

    private void logRespStatistic1(InOutBoundStatisticHolder origin) {
        String content = origin.logContent();
        respStatisticLog.info(content);
    }

    //-------------------------------------------------------------------------------------------------------
    public void statisticReq(String uuid, long size) {
        reqHolder.reference();
        InOutBoundStatistic statistic = reqHolder.getstatistic(uuid);
        statistic.incr(size);
        reqHolder.release();
    }

    public void statisticResp(String uuid, long size) {
        respHolder.reference();
        InOutBoundStatistic statistic = respHolder.getstatistic(uuid);
        statistic.incr(size);
        respHolder.release();
    }
}
