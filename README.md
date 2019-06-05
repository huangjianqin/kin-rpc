# kinrpc
模仿Dubbo实现的一个基于Zookeeper的RPC工具,目前可以实现服务注册,发现和调用,还有一些更深层的细节和优化还没实现。

进一步工作(2019年度计划) : 代码重构并改进,结合新颖编程模型以及自身编程经验的提升(工作中,其他优秀项目源码).
1. 在模块化基础上，支持service mesh和url直连
2. 支持重载方法调用(使用hash(class+方法名+参数类型)作为唯一标识)
3. 模块化工作，分离底层网络传输，负载均衡，router，discove和consumer/provider核心封装 
4. 增加限流和降级策略
5. 利用netty api支持rest服务