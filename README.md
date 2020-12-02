# **kin-rpc**
    高性能RPC
   1. 支持基于zookeeper, redis和url直连的服务注册订阅方案
   2. 支持异步调用
   3. 支持使用javassist技术替代反射提高方法调用性能
   4. 支持负载均衡, 包括: 轮询, 随机, 一致性HASH等
   5. 支持服务路由
   6. 支持socket, http, grpc等协议
   7. KinRpc协议支持KRYO, HESSIAN, JAVA, JSON等序列化方案
   8. KinRpc协议下支持provider服务接口方法异步返回结果

### **进一步工作** 
    代码优化, 参考其他优秀的项目优化代码结构, 简化核心调用逻辑, 提高代码执行性能, 还有吸纳其他项目优秀且实用的功能
   1. 支持protobuf序列化
   2. 服务降级思考, 即优先级越低越晚执行, 甚至不执行(请求队列排序???)
   3. 支持service mesh