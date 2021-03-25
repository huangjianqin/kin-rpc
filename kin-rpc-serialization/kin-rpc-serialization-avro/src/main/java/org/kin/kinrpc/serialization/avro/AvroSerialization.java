package org.kin.kinrpc.serialization.avro;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.kin.kinrpc.serialization.Serialization;
import org.kin.kinrpc.serialization.SerializationType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author huangjianqin
 * @date 2020/11/27
 */
public class AvroSerialization implements Serialization {
    /** encoder */
    private static final EncoderFactory ENCODER_FACTORY = EncoderFactory.get();
    /** decoder */
    private static final DecoderFactory DECODER_FACTORY = DecoderFactory.get();

    @Override
    public byte[] serialize(Object target) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BinaryEncoder encoder = ENCODER_FACTORY.binaryEncoder(baos, null);

        ReflectDatumWriter dd = new ReflectDatumWriter<>(target.getClass());
        dd.write(target, encoder);

        encoder.flush();
        baos.close();

        return baos.toByteArray();
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> targetClass) throws IOException {
        //不支持不确定类型的反序列化
        BinaryDecoder decoder = DECODER_FACTORY.binaryDecoder(bytes, null);

        ReflectDatumReader<T> reader = new ReflectDatumReader<>(targetClass);
        return reader.read(null, decoder);
    }

    @Override
    public int type() {
        return SerializationType.AVRO.getCode();
    }
}
