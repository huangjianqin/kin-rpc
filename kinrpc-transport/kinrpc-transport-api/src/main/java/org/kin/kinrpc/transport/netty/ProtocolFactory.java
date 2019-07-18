package org.kin.kinrpc.transport.netty;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.kin.framework.utils.ClassUtils;
import org.kin.kinrpc.transport.netty.exception.UnknowProtocolException;
import org.kin.kinrpc.transport.netty.protocol.AbstractProtocol;
import org.kin.kinrpc.transport.netty.protocol.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Set;

/**
 * @author huangjianqin
 * @date 2019/7/4
 */
public class ProtocolFactory {
    private static final Logger log = LoggerFactory.getLogger(ProtocolFactory.class);
    private static final Cache<Integer, Class<AbstractProtocol>> PROTOCOL_CACHE = CacheBuilder.newBuilder().build();

    private ProtocolFactory() {
    }

    /**
     * 仅仅append
     */
    public static void init(String scanPath){
        synchronized (ProtocolFactory.class){
            Set<Class<AbstractProtocol>> protocolClasses = ClassUtils.getSubClass(scanPath, AbstractProtocol.class, false);
            for(Class<AbstractProtocol> protocolClass: protocolClasses){
                Protocol protocol = protocolClass.getAnnotation(Protocol.class);
                if(protocol != null){
                    PROTOCOL_CACHE.put(protocol.id(), protocolClass);
                    log.info("find protocol(id={}) >>> {}", protocol.id(), protocolClass);
                }
            }
        }
    }

    /**
     * 根据id创建protocol, 并依照field定义顺序设置field value, 从子类开始算
     */
    public static <T extends AbstractProtocol> T createProtocol(int id, Object... fieldValues){
        Class<AbstractProtocol> claxx = PROTOCOL_CACHE.getIfPresent(id);
        if(claxx != null){
            AbstractProtocol protocol = ClassUtils.instance(claxx);
            if(protocol != null){
                //设置协议id
                Field[] fields = ClassUtils.getAllFields(claxx).toArray(new Field[0]);
                for(Field field: fields){
                    if("protocolId".equals(field.getName())){
                        ClassUtils.setFieldValue(protocol, field, id);
                        break;
                    }
                }
                if (fieldValues.length > 0) {
                    for(int i = 0; i < fieldValues.length; i++){
                        Field field = fields[i];
                        ClassUtils.setFieldValue(protocol, field, fieldValues[i]);
                    }
                }

                return (T) protocol;
            }

            return null;
        }

        throw new UnknowProtocolException(id);
    }
}
