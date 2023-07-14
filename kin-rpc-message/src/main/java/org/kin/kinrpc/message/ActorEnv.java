package org.kin.kinrpc.message;

import org.kin.framework.concurrent.Dispatcher;
import org.kin.framework.concurrent.EventBasedDispatcher;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.utils.SysUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * actor环境
 *
 * @author huangjianqin
 * @date 2020-06-08
 */
public class ActorEnv {
    private static final Logger log = LoggerFactory.getLogger(ActorEnv.class);

    /** thread local {@link ActorEnv}实例 */
    private static final ThreadLocal<ActorEnv> THREAD_LOCAL_ACTOR_ENV = new ThreadLocal<>();

    /** current thread local actor env */
    static ActorEnv current() {
        return THREAD_LOCAL_ACTOR_ENV.get();
    }

    /** update current thread local actor env */
    static void update(ActorEnv actorEnv) {
        THREAD_LOCAL_ACTOR_ENV.set(actorEnv);
    }
    //----------------------------------------------------------------------------------------------------------------
    /** 公用线程池, 除了dispatcher以外, 都用这个线程池, 会存在io/加锁操作 */
    final ExecutionContext commonExecutors = ExecutionContext.elastic(SysUtils.getSuitableThreadNum(), SysUtils.CPU_NUM * 5,
            "kinrpc-message", 2);

    /** 消息调度 */
    private final Dispatcher<String, ActorContext> dispatcher;
    /** 标识是否terminated */
    private volatile boolean terminated = false;
    /** actor ref provider */
    private final ActorRefProvider actorRefProvider;
    /** key -> {@link Actor}实例, value -> {@link Actor}对应的{@link ActorRef}实例 */
    private final Map<Actor, ActorRef> actorRefMap = new ConcurrentHashMap<>();

    protected ActorEnv(int parallelism) {
        this(parallelism, ActorRefProvider.LOCAL);
    }

    protected ActorEnv(int parallelism, ActorRefProvider actorRefProvider) {
        this.dispatcher = new EventBasedDispatcher<>(parallelism);
        this.actorRefProvider = actorRefProvider;
    }

    /**
     * 检查是否已经terminated
     */
    protected final void checkTerminated() {
        if (!isTerminated()) {
            return;
        }

        throw new IllegalStateException("actor env is already terminated");
    }

    /**
     * 注册actor
     *
     * @param name  actor name
     * @param actor actor instance
     */
    @SuppressWarnings("unchecked")
    public final void newActor(String name, Actor actor) {
        checkTerminated();

        if (dispatcher.isRegistered(name)) {
            throw new IllegalStateException(String.format("actor '%s' has been registered", name));
        }

        ActorRef actorRef = actorRefProvider.actorOf(this, ActorAddress.of(name));
        actor.internalInit(actorRef);
        actorRefMap.put(actor, actorRef);
        dispatcher.register(name, new ActorReceiver(this, actor), !actor.threadSafe());
    }

    /**
     * 注销actor
     *
     * @param name  actor name
     * @param actor actor instance
     */
    public final void removeActor(String name, Actor actor) {
        checkTerminated();

        dispatcher.unregister(name);
        actorRefMap.remove(actor);
    }

    /**
     * create remote actor reference
     *
     * @param actorName actor name
     * @return actor reference
     */
    public final ActorRef actorOf(String actorName) {
        return actorOf(Address.LOCAL, actorName);
    }

    /**
     * create remote actor reference
     *
     * @param actorName actor name
     * @param address   actor address
     * @return actor reference
     */
    public ActorRef actorOf(Address address, String actorName) {
        checkTerminated();

        return actorRefProvider.actorOf(this, ActorAddress.of(address, actorName));
    }

    /**
     * destroy actor env
     */
    public final void destroy() {
        if (isTerminated()) {
            return;
        }

        terminated = true;
        doDestroy();

        //关闭dispatcher
        dispatcher.shutdown();

        //shutdown 线程资源
        commonExecutors.shutdown();
    }

    /**
     * 允许实现类自定义destroy逻辑
     */
    protected void doDestroy() {
        //default do nothing
    }

    //setter && getter
    public final boolean isTerminated() {
        return terminated;
    }

    //----------------------------------------------------------------------internal

    /**
     * 分派并处理消息
     *
     * @param actorContext actor context
     */
    void postMessage(ActorContext actorContext) {
        actorContext.setEventTime(System.currentTimeMillis());
        dispatcher.postMessage(actorContext.getToActorAddress().getName(), actorContext);
    }

    //-----------------------------------------------------------------------------
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        /** 并行数 */
        private int parallelism = SysUtils.DOUBLE_CPU;

        public Builder parallelism(int parallelism) {
            this.parallelism = parallelism;
            return this;
        }

        public ActorEnv build() {
            return new ActorEnv(parallelism);
        }
    }
}
