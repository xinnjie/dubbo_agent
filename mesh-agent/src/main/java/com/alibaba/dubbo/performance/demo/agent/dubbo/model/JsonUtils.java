package com.alibaba.dubbo.performance.demo.agent.dubbo.model;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.io.*;

/**
 * @author ken.lj
 * @date 02/04/2018
 */
public class JsonUtils {

    public static void writeObject(Object obj, PrintWriter writer) throws IOException {
        SerializeWriter out = new SerializeWriter();
        JSONSerializer serializer = new JSONSerializer(out);
        serializer.config(SerializerFeature.WriteEnumUsingToString, true);
        serializer.write(obj);
        out.writeTo(writer);
        out.close(); // for reuse SerializeWriter buf
        writer.println();
        writer.flush();
    }

    public static void writeBytes(byte[] b, PrintWriter writer) {
        writer.print(new String(b));
        writer.flush();
    }

    public static void main(String[] args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
        try {
            JsonUtils.writeObject("hello", writer);
            JsonUtils.writeBytes(new byte[]{'1','3'}, writer);
            JsonUtils.writeObject("hello", writer);
            JsonUtils.writeBytes(new byte[]{'1','3'}, writer);


            System.out.println(new String(out.toByteArray()));
        } catch (IOException e) {

        }
    }
}
