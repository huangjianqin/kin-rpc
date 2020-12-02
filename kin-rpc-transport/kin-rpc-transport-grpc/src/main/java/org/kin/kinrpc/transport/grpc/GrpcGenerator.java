package org.kin.kinrpc.transport.grpc;

import com.google.common.base.Strings;
import com.google.common.html.HtmlEscapers;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileOptions;
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto;
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto;
import com.google.protobuf.DescriptorProtos.SourceCodeInfo.Location;
import com.google.protobuf.compiler.PluginProtos;
import com.salesforce.jprotoc.Generator;
import com.salesforce.jprotoc.GeneratorException;
import com.salesforce.jprotoc.ProtoTypeMap;
import com.salesforce.jprotoc.ProtocPlugin;
import org.kin.framework.utils.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 生成XXXGrpc代码
 *
 * @author huangjianqin
 * @date 2020/12/2
 */
public class GrpcGenerator extends Generator {
    private static final int SERVICE_NUMBER_OF_PATHS = 2;
    private static final int METHOD_NUMBER_OF_PATHS = 4;

    public static void main(String[] args) {
        ProtocPlugin.generate(new GrpcGenerator());
    }

    /**
     * 类名前缀
     */
    protected String getClassPrefix() {
        return "KinRpc";
    }

    /**
     * 类名后缀
     */
    protected String getClassSuffix() {
        return "Grpc";
    }

    /**
     * 4个空格
     */
    private String getServiceJavaDocPrefix() {
        return "    ";
    }

    /**
     * 8个空格
     */
    private String getMethodJavaDocPrefix() {
        return "        ";
    }

    /**
     * 生成代码源文件
     */
    @Override
    public List<PluginProtos.CodeGeneratorResponse.File> generateFiles(PluginProtos.CodeGeneratorRequest request) throws GeneratorException {
        //proto文件
        final ProtoTypeMap typeMap = ProtoTypeMap.of(request.getProtoFileList());

        List<FileDescriptorProto> protosToGenerate = request.getProtoFileList().stream()
                .filter(protoFile -> request.getFileToGenerateList().contains(protoFile.getName()))
                .collect(Collectors.toList());

        //service信息
        List<ServiceContext> services = findServices(protosToGenerate, typeMap);

        //生成代码
        return generateFiles(services);
    }

    /**
     * 遍历service proto定义, 构建service信息
     */
    private List<ServiceContext> findServices(List<FileDescriptorProto> protos, ProtoTypeMap typeMap) {
        List<ServiceContext> contexts = new ArrayList<>();

        for (FileDescriptorProto fileProto : protos) {
            //遍历proto文件
            for (int serviceNumber = 0; serviceNumber < fileProto.getServiceCount(); serviceNumber++) {
                //遍历service proto定义
                ServiceContext serviceContext = buildServiceContext(
                        fileProto.getService(serviceNumber),
                        typeMap,
                        fileProto.getSourceCodeInfo().getLocationList(),
                        serviceNumber
                );
                serviceContext.protoName = fileProto.getName();
                serviceContext.packageName = extractPackageName(fileProto);
                contexts.add(serviceContext);
            }
        }

        return contexts;
    }

    /**
     * 获取包名
     * 优先级:
     * 1. option java_package="XX.XX.XX"
     * 2. package XX.XX.XX
     */
    private String extractPackageName(FileDescriptorProto proto) {
        FileOptions options = proto.getOptions();
        if (options != null) {
            String javaPackage = options.getJavaPackage();
            if (!Strings.isNullOrEmpty(javaPackage)) {
                return javaPackage;
            }
        }

        return Strings.nullToEmpty(proto.getPackage());
    }

    /**
     * 构建service proto定义信息
     */
    private ServiceContext buildServiceContext(
            ServiceDescriptorProto serviceProto,
            ProtoTypeMap typeMap,
            List<Location> locations,
            int serviceNumber) {
        ServiceContext serviceContext = new ServiceContext();
        serviceContext.fileName = getClassPrefix() + serviceProto.getName() + getClassSuffix() + ".java";
        serviceContext.className = getClassPrefix() + serviceProto.getName() + getClassSuffix();
        serviceContext.serviceName = serviceProto.getName();
        serviceContext.deprecated = serviceProto.getOptions() != null && serviceProto.getOptions().getDeprecated();

        //该service出现的Location
        List<Location> allLocationsForService = locations.stream()
                .filter(location ->
                        location.getPathCount() >= 2 &&
                                location.getPath(0) == FileDescriptorProto.SERVICE_FIELD_NUMBER &&
                                location.getPath(1) == serviceNumber
                )
                .collect(Collectors.toList());

        //寻找service Location
        Location serviceLocation = allLocationsForService.stream()
                .filter(location -> location.getPathCount() == SERVICE_NUMBER_OF_PATHS)
                .findFirst()
                .orElseGet(Location::getDefaultInstance);
        serviceContext.javaDoc = getJavaDoc(getComments(serviceLocation), getServiceJavaDocPrefix());

        //遍历service定义的接口方法
        for (int methodNumber = 0; methodNumber < serviceProto.getMethodCount(); methodNumber++) {
            MethodContext methodContext = buildMethodContext(
                    serviceProto.getMethod(methodNumber),
                    typeMap,
                    locations,
                    methodNumber
            );

            serviceContext.methods.add(methodContext);
            serviceContext.methodTypes.add(methodContext.inputType);
            serviceContext.methodTypes.add(methodContext.outputType);
        }
        return serviceContext;
    }

    /**
     * 构建service 方法 proto定义信息
     */
    private MethodContext buildMethodContext(
            MethodDescriptorProto methodProto,
            ProtoTypeMap typeMap,
            List<Location> locations,
            int methodNumber) {
        MethodContext methodContext = new MethodContext();
        methodContext.methodName = lowerCaseFirst(methodProto.getName());
        methodContext.inputType = typeMap.toJavaTypeName(methodProto.getInputType());
        methodContext.outputType = typeMap.toJavaTypeName(methodProto.getOutputType());
        methodContext.deprecated = methodProto.getOptions() != null && methodProto.getOptions().getDeprecated();
        methodContext.isManyInput = methodProto.getClientStreaming();
        methodContext.isManyOutput = methodProto.getServerStreaming();
        methodContext.methodNumber = methodNumber;

        //方法Location
        Location methodLocation = locations.stream()
                .filter(location ->
                        location.getPathCount() == METHOD_NUMBER_OF_PATHS &&
                                location.getPath(METHOD_NUMBER_OF_PATHS - 1) == methodNumber
                )
                .findFirst()
                .orElseGet(Location::getDefaultInstance);
        methodContext.javaDoc = getJavaDoc(getComments(methodLocation), getMethodJavaDocPrefix());

        //根据类型, 确定方法名
        if (!methodProto.getClientStreaming() && !methodProto.getServerStreaming()) {
            methodContext.reactiveCallsMethodName = "oneToOne";
            methodContext.grpcCallsMethodName = "asyncUnaryCall";
        }
        if (!methodProto.getClientStreaming() && methodProto.getServerStreaming()) {
            methodContext.reactiveCallsMethodName = "oneToMany";
            methodContext.grpcCallsMethodName = "asyncServerStreamingCall";
        }
        if (methodProto.getClientStreaming() && !methodProto.getServerStreaming()) {
            methodContext.reactiveCallsMethodName = "manyToOne";
            methodContext.grpcCallsMethodName = "asyncClientStreamingCall";
        }
        if (methodProto.getClientStreaming() && methodProto.getServerStreaming()) {
            methodContext.reactiveCallsMethodName = "manyToMany";
            methodContext.grpcCallsMethodName = "asyncBidiStreamingCall";
        }
        return methodContext;
    }

    /**
     * 首字母小写
     */
    private String lowerCaseFirst(String s) {
        return StringUtils.firstLowerCase(s);
    }

    /**
     * 生成文件
     */
    private List<PluginProtos.CodeGeneratorResponse.File> generateFiles(List<ServiceContext> services) {
        return services.stream()
                .map(this::buildFile)
                .collect(Collectors.toList());
    }

    /**
     * 生成文件
     */
    private PluginProtos.CodeGeneratorResponse.File buildFile(ServiceContext context) {
        String content = applyTemplate(getClassPrefix() + getClassSuffix() + "Stub.mustache", context);
        return PluginProtos.CodeGeneratorResponse.File
                .newBuilder()
                .setName(absoluteFileName(context))
                .setContent(content)
                .build();
    }

    /**
     * 获取文件绝对路径
     */
    private String absoluteFileName(ServiceContext ctx) {
        String dir = ctx.packageName.replace('.', '/');
        if (Strings.isNullOrEmpty(dir)) {
            return ctx.fileName;
        } else {
            return dir + "/" + ctx.fileName;
        }
    }

    /**
     * 获取proto文件service的注释
     */
    private String getComments(Location location) {
        return location.getLeadingComments().isEmpty() ? location.getTrailingComments() : location.getLeadingComments();
    }

    /**
     * 生成java注释
     */
    private String getJavaDoc(String comments, String prefix) {
        if (!comments.isEmpty()) {
            StringBuilder builder = new StringBuilder("/**\n")
                    .append(prefix).append(" * <pre>\n");
            Arrays.stream(HtmlEscapers.htmlEscaper().escape(comments).split("\n"))
                    .map(line -> line.replace("*/", "&#42;&#47;").replace("*", "&#42;"))
                    .forEach(line -> builder.append(prefix).append(" * ").append(line).append("\n"));
            builder
                    .append(prefix).append(" * </pre>\n")
                    .append(prefix).append(" */");
            return builder.toString();
        }
        return null;
    }

    /**
     * Template class for proto Service objects.
     */
    private class ServiceContext {
        // CHECKSTYLE DISABLE VisibilityModifier FOR 8 LINES
        /** .java文件 */
        public String fileName;
        /** proto定义的名字 */
        public String protoName;
        /** 包名 */
        public String packageName;
        /** java类名 */
        public String className;
        /** proto定义的名字 */
        public String serviceName;
        /** 是否deprecated */
        public boolean deprecated;
        /** java注释 */
        public String javaDoc;
        /** service 方法定义 */
        public List<MethodContext> methods = new ArrayList<>();
        /** 缓存所有方法的请求和返回类型 */
        public Set<String> methodTypes = new HashSet<>();

        /**
         * 单请求方法
         */
        public List<MethodContext> unaryRequestMethods() {
            return methods.stream().filter(m -> !m.isManyInput).collect(Collectors.toList());
        }

        /**
         * 单请求单返回方法
         */
        public List<MethodContext> unaryMethods() {
            return methods.stream().filter(m -> (!m.isManyInput && !m.isManyOutput)).collect(Collectors.toList());
        }

        /**
         * 单请求多返回方法
         */
        public List<MethodContext> serverStreamingMethods() {
            return methods.stream().filter(m -> !m.isManyInput && m.isManyOutput).collect(Collectors.toList());
        }

        /**
         * 多请求方法
         */
        public List<MethodContext> biStreamingMethods() {
            return methods.stream().filter(m -> m.isManyInput).collect(Collectors.toList());
        }
    }

    /**
     * Template class for proto RPC objects.
     */
    private class MethodContext {
        // CHECKSTYLE DISABLE VisibilityModifier FOR 10 LINES
        /** 方法名 */
        public String methodName;
        /** 请求类型 java类型 */
        public String inputType;
        /** 返回类型 java类型 */
        public String outputType;
        /** 方法是否是deprecated */
        public boolean deprecated;
        /** client streaming */
        public boolean isManyInput;
        /** server streaming */
        public boolean isManyOutput;
        /**
         *
         */
        public String reactiveCallsMethodName;
        /** grpc调用的方法名 */
        public String grpcCallsMethodName;
        /** 第n个方法 */
        public int methodNumber;
        /** java注释 */
        public String javaDoc;

        /**
         * This method mimics the upper-casing method ogf gRPC to ensure compatibility
         * See https://github.com/grpc/grpc-java/blob/v1.8.0/compiler/src/java_plugin/cpp/java_generator.cpp#L58
         */
        public String methodNameUpperUnderscore() {
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < methodName.length(); i++) {
                char c = methodName.charAt(i);
                s.append(Character.toUpperCase(c));
                if ((i < methodName.length() - 1) && Character.isLowerCase(c) && Character.isUpperCase(methodName.charAt(i + 1))) {
                    s.append('_');
                }
            }
            return s.toString();
        }

        /**
         *
         */
        public String methodNamePascalCase() {
            String mn = methodName.replace("_", "");
            return String.valueOf(Character.toUpperCase(mn.charAt(0))) + mn.substring(1);
        }

        /**
         *
         */
        public String methodNameCamelCase() {
            String mn = methodName.replace("_", "");
            return String.valueOf(Character.toLowerCase(mn.charAt(0))) + mn.substring(1);
        }
    }
}


