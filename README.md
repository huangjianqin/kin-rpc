# **kin-rpc**
    高性能RPC

1. 支持基于zookeeper, redis和url直连的服务注册订阅方案
2. 支持异步调用
3. 支持使用javassist技术替代反射提高方法调用性能
4. 支持负载均衡, 包括: 轮询, 随机, 一致性HASH等
5. 支持服务路由
6. 支持kinrpc, http, grpc, rsocket等协议
7. kinrpc协议支持KRYO, HESSIAN, JAVA, JSON等序列化方案
8. kinrpc协议下支持provider服务接口方法异步返回结果
   * 通过```org.kin.kinrpc.AsyncContext```设置future
   * 参数返回值是Future

### **进一步工作** 
    代码优化, 参考其他优秀的项目优化代码结构, 简化核心调用逻辑, 提高代码执行性能, 还有吸纳其他项目优秀且实用的功能
1. 优化json序列化方式, 不带class信息, 但能正确序列化和反序列化, 参考kin-rsocket
2. 优化掉javassist, 采用bytebuddy
3. 优化兼容第三方协议
4. 优化配置方式
5. 优化代码逻辑
6. 服务降级思考, 即优先级越低越晚执行, 甚至不执行(请求队列排序???)
7. 支持service mesh