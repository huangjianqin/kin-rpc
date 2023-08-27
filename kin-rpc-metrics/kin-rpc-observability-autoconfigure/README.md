# Tracing

基于Micrometer Observation作为门面, 桥接Brave/otel, 并将Tracing信息发送给Zipkin或otlp的链路跟踪

## Tracing相关概念

* Span：基本工作单元。例如，发送 RPC 是一个新的 span，发送对 RPC 的响应也是如此。Span还有其他数据，例如description、带时间戳的事件、键值注释（标签）、导致它们的跨度的
  ID 和进程 ID（通常是 IP 地址）。跨度可以启动和停止，并且它们会跟踪它们的时间信息。创建跨度后，您必须在将来的某个时间点停止它。
* Trace：一组形成树状结构的跨度。例如，如果您运行分布式大数据存储，则可能会通过请求形成跟踪PUT。
* Annotation/Event : 用于及时记录一个事件的存在。
* Tracing context：为了使分布式跟踪工作，跟踪上下文（跟踪标识符、跨度标识符等）必须通过进程（例如通过线程）和网络传播。
* Log correlation：部分跟踪上下文（例如跟踪标识符、跨度标识符）可以填充到给定应用程序的日志中。然后可以将所有日志收集到一个存储中，并通过跟踪
  ID 对它们进行分组。这样就可以从所有按时间顺序排列的服务中获取单个业务操作（跟踪）的所有日志。
* Latency analysis tools：一种收集导出跨度并可视化整个跟踪的工具。允许轻松进行延迟分析。
* Tracer: 处理span生命周期的库（Dubbo 目前支持 Opentelemetry 和 Brave）。它可以通过 Exporter 创建、启动、停止和报告 Spans
  到外部系统（如 Zipkin、Jagger 等）。
* Exporter: 将产生的 Trace 信息通过 http 等接口上报到外部系统，比如上报到 Zipkin。

## Starters

Opentelemetry 作为 Tracer，将 Trace 信息 export 到 Zipkin

```xml

<dependency>
    <groupId>org.kin</groupId>
    <artifactId>kin-rpc-tracing-otel-zipkin-starter</artifactId>
    <version>${version}</version>
</dependency>
```

Brave 作为 Tracer，将 Trace 信息 export 到 Zipkin

```xml

<dependency>
    <groupId>org.kin</groupId>
    <artifactId>kin-rpc-tracing-brave-zipkin-starter</artifactId>
    <version>${version}</version>
</dependency>
```

Opentelemetry 作为 Tracer，将 Trace 信息 export 到 Otlp Collector

```xml

<dependency>
    <groupId>org.kin</groupId>
    <artifactId>kin-rpc-tracing-otel-otlp-starter</artifactId>
    <version>${version}</version>
</dependency>
```

## 自由组装 Tracer 和 Exporter

```xml

<dependency>
    <groupId>org.kin</groupId>
    <artifactId>kin-rpc-observability-autoconfigure</artifactId>
    <version>${version}</version>
</dependency>

        <!-- 使用micrometer官方提供的bridge, 比如下面以brave作为tracer  -->
<dependency>
<groupId>io.micrometer</groupId>
<artifactId>micrometer-tracing-bridge-brave</artifactId>
<version>${version}</version>
</dependency>

        <!-- 目前支持将Tracing信息上报brave或otlp -->
        <!-- export到zipkin -->
<dependency>
<groupId>io.zipkin.reporter2</groupId>
<artifactId>zipkin-reporter-brave</artifactId>
<version>${version}</version>
</dependency>

        <!-- export到otlp -->
<dependency>
<groupId>io.opentelemetry</groupId>
<artifactId>opentelemetry-exporter-otlp</artifactId>
<version>${version}</version>
</dependency>
```