# skywalking

## Start Skywalking OAP

[skywalking oap setup](https://skywalking.apache.org/docs/main/v9.3.0/en/setup/backend/backend-setup/)

## Start Application With Skywalking Agent

可选择配置`/path/to/skywalking-agent/agent.config`, 然后使用以下cmd启动应用

`java -javaagent:/path/to/skywalking-agent/skywalking-agent.jar -jar kin-rpc-demo-provider-starter.jar`
