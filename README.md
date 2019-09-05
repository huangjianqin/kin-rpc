# kin-rpc
模仿Dubbo实现的一个基于Zookeeper的RPC工具,目前可以实现服务注册,发现和调用,还有一些更深层的细节和优化还没实现。

进一步工作: 代码重构并改进,结合新颖编程模型以及自身编程经验的提升(工作中,其他优秀项目源码).
-1. 使用javassist字节码技术优化服务端(比较重要)及客户端调用速度, 考虑增加invoker池, 减少invoker client数.
0. 服务降级思考, 即优先级越低越晚执行, 甚至不执行(请求队列排序???)
1. 支持service mesh
2. 利用netty api支持rest服务
3. 支持redis作为注册中心