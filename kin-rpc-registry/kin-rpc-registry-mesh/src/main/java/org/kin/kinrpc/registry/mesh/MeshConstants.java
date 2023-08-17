package org.kin.kinrpc.registry.mesh;

import org.kin.kinrpc.config.DefaultConfig;
import org.kin.kinrpc.config.ProtocolType;

/**
 * @author huangjianqin
 * @date 2023/8/17
 */
public final class MeshConstants {
    /** mesh service protocol */
    public static final String PROTOCOL_KEY = "MESH_PROTOCOL";
    /** mesh service port */
    public static final String PORT_KEY = "MESH_PORT";
    /** mesh service pod namespace */
    public static final String POD_NAMESPACE_KEY = "MESH_POD_NAMESPACE";
    /** mesh cluster domain */
    public static final String CLUSTER_DOMAIN_KEY = "MESH_CLUSTER_DOMAIN";
    /** mesh service token */
    public static final String TOKEN_KEY = "MESH_TOKEN";
    //-----------------------------------------------------------------------------------------------default
    /** default mesh service protocol */
    public static final String DEFAULT_PROTOCOL = ProtocolType.KINRPC.getName();
    /** default mesh service port */
    public static final int DEFAULT_PORT = DefaultConfig.DEFAULT_SERVER_PORT;
    /** default mesh service pod namespace */
    public static final String DEFAULT_POD_NAMESPACE = "default";
    /** default mesh cluster domain */
    public static final String DEFAULT_CLUSTER_DOMAIN = "cluster.local";

    private MeshConstants() {
    }
}
