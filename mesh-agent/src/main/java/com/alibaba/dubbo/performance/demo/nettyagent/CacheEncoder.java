package com.alibaba.dubbo.performance.demo.nettyagent;
import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import com.alibaba.dubbo.performance.demo.nettyagent.util.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by gexinjie on 2018/5/30.
 */
public class CacheEncoder extends MessageToByteEncoder{
    //    public static final String NAME = "cache";
    protected static final short MAGIC = (short) 0xdacc;

    private HashMap<FuncType, Integer> methodIDs = null;
//    private HashMap<Integer, FuncType> me
    protected  static final int HEADER_LENGTH_MIN = 16;
    protected  static final int HEADER_LENGTH_MAX = 20;

    protected  static final int REQEUST_ID_INDEX = 4;
    protected  static final int DATA_LENGTH_INDEX = 12;
    protected  static final int METHOD_ID_INDEX = 16;





    protected static final byte FLAG_REQUEST = (byte) 0x80;
    protected static final byte FLAG_CACHE = (byte) 0x40;
    protected static final byte FLAG_VALID = (byte) 0x20;
    protected static final byte FLAG_NULL = (byte) 0x10;



    public CacheEncoder(HashMap<FuncType, Integer> methodIDsCache) {
        super();
        this.methodIDs = methodIDsCache;
    }


    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        assert  this.methodIDs != null;
        Invocation invocation = (Invocation)msg;

        int startWriteIndex = out.writerIndex();

        boolean isRequest = invocation.getResult() == null,
            isCache = false;
        int HeaderLength = 0;

        // encoding request
        if (isRequest) {
            if (methodIDs.containsKey(invocation)) {
                isCache = true;
            }
            HeaderLength = isCache ? HEADER_LENGTH_MAX : HEADER_LENGTH_MIN;

            byte[] header = new byte[HeaderLength];
            Bytes.short2bytes(MAGIC, header);
            header[2] |= FLAG_REQUEST;
            // set request id
            // request 放需要分配一个 reqeustID 给每次 encode
            // 启动方面只要使用这个 ID，不能再分配
            Bytes.long2bytes(Invocation.getUniqueReqeustID(), header, 4);

            if (isCache) {
                header[2] |= FLAG_CACHE;
                int methodID = methodIDs.get(invocation);
                Bytes.int2bytes(methodID, header, METHOD_ID_INDEX);
            } else {
                // 写入 body, 先更改 bytebuf 的 index
                out.writerIndex(startWriteIndex + HeaderLength);

                out.writeCharSequence(invocation.getMethodName() + "\n", Charset.forName("utf-8"));
                out.writeCharSequence(invocation.getParameterTypes() + "\n", Charset.forName("utf-8"));
            }
            out.writeCharSequence(invocation.getArguments() + "\n", Charset.forName("utf-8"));
            int totalIndex = out.writerIndex();
            Bytes.int2bytes(totalIndex-startWriteIndex, header, DATA_LENGTH_INDEX);
            out.writerIndex(startWriteIndex);
            out.writeBytes(header);
            out.writerIndex(totalIndex);
            return;
        }
        // encoding response
        Boolean isValid = false;
        if (!methodIDs.containsKey(invocation)) {
            invocation.setMethodID(Invocation.getUniqueMethodID());
            synchronized (methodIDs) {
                methodIDs.put(invocation.shallowCopy(), invocation.getMethodID());
            }
            isValid = true;
        }
        isCache = true;

        //headers
        HeaderLength = HEADER_LENGTH_MAX;
        byte[] header = new byte[HeaderLength];
        Bytes.short2bytes(MAGIC, header);
        //todo remove assert
        assert invocation.getRequestID() != -1;
        Bytes.long2bytes(invocation.getRequestID(), header, REQEUST_ID_INDEX);
        if (isCache) {
            header[2] |= FLAG_CACHE;
        }
        out.writerIndex(startWriteIndex + HeaderLength);
        if (isValid) {
            header[2] |= FLAG_VALID;
            out.writeCharSequence(invocation.getMethodName()+ "\n", Charset.forName("utf-8"));
            out.writeCharSequence(invocation.getParameterTypes()+ "\n", Charset.forName("utf-8"));
        }
        out.writeCharSequence(invocation.getResult()+"\n", Charset.forName("utf-8"));
        int totalIndex = out.writerIndex();
        Bytes.int2bytes(totalIndex-startWriteIndex, header, DATA_LENGTH_INDEX);
        out.writerIndex(startWriteIndex);
        out.writeBytes(header);
        out.writerIndex(totalIndex);

    }

    enum DecodeResult {
        NEED_MORE_INPUT, SKIP_INPUT
    }

    public static void main(String[] args) {
        ByteBuf b = Unpooled.buffer(2);
        byte[] bytes = b.array();
        bytes[0] = 'a';
        bytes[1] = 'b';
        System.out.print(new String(b.array()));
        HashMap<FuncType, Integer>  map = new HashMap<>();



        String aa = "aa",
            bb = "bb";
        map.put(new FuncType(aa, bb), 1);
        Invocation invocation = new Invocation();
        invocation.setMethodName("aa");
        invocation.setParameterTypes("bb");
        assert invocation.hashCode()== new FuncType(aa, bb).hashCode();
        assert (map.containsKey(invocation));
        assert map.containsKey(new FuncType(aa, bb));

        for (String s:
             "hello\nhi\nhelo".split("\n")) {
            System.out.println(s + "!!!");

        }

        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            final int n = random.nextInt(10);
            System.out.println(n);
        }
    }
}
