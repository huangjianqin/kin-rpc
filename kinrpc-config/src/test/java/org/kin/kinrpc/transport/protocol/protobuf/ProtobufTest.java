//package org.kin.kinrpc.transport.protocol.protobuf;
//
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//
///**
// * Created by 健勤 on 2017/2/25.
// */
//public class ProtobufTest {
//    public static void main(String[] args) throws IOException {
//        PersonMsg.Person.Builder personBuilder = PersonMsg.Person.newBuilder();
//        personBuilder.setId(2);
//        personBuilder.setName("haha");
//        personBuilder.setEmail("@@");
//        personBuilder.addFriends("B");
//        PersonMsg.Person person = personBuilder.build();
//
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        person.writeTo(baos);
//        baos.close();
//        byte[] bytes = baos.toByteArray();
//
//        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
//        PersonMsg.Person person1 = PersonMsg.Person.parseFrom(bais);
//
//        System.out.println(person == person1);
//        System.out.println(person.equals(person1));
//
//    }
//}
