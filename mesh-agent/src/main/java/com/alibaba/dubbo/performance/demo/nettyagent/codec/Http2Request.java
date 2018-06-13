package com.alibaba.dubbo.performance.demo.nettyagent.codec;

import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import com.alibaba.dubbo.performance.demo.nettyagent.model.InvocationRequest;
import com.alibaba.dubbo.performance.demo.nettyagent.registry.Endpoint;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.util.ByteProcessor;
import org.apache.logging.log4j.LogManager;


import java.nio.charset.Charset;
import java.util.*;

/**
 * Created by gexinjie on 2018/5/28.
 */


public class Http2Request extends ChannelInboundHandlerAdapter{
org.apache.logging.log4j.Logger logger = LogManager.getLogger(LogManager.ROOT_LOGGER_NAME);
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        FullHttpRequest req = (FullHttpRequest) msg;
        try {
            if (req.method().equals(HttpMethod.POST)) {
                HttpPostRequestDecoder postDecoder = new HttpPostRequestDecoder(req);
                List<InterfaceHttpData> paramList = postDecoder.getBodyHttpDatas();
                Map<String, String> paramMap = new HashMap<>();
                for (InterfaceHttpData para : paramList) {
                    Attribute data = (Attribute) para;
                    paramMap.put(data.getName(), data.getValue());
                }

            /*
            TODO 解读http，构建对应 invocation 的重要部分，多检查一下
             */

                InvocationRequest request = new InvocationRequest(paramMap.get("parameter"),
                        paramMap.get("interface"), paramMap.get("method"), paramMap.get("parameterTypesString"));
                logger.debug("received from Consumer: {}", request);
                ctx.fireChannelRead(request);
            } else {
                logger.warn("CA received non-post request from consumer");
            }
        } finally {
             req.release();
        }
    }
    static ByteProcessor FIND_AND = new ByteProcessor() {
        @Override
        public boolean process(byte value) throws Exception {
            return value != (byte)'&';
        }
    };
    static ByteProcessor FIND_EQUAL = new ByteProcessor() {
        @Override
        public boolean process(byte value) throws Exception {
            return value != (byte)'=';
        }
    };


//    // 基本格式为  "interface=com.alibaba.dubbo.performance.demo.provider.IHelloService&method=hash&parameterTypesString=Ljava/lang/String;&parameter=adsadjknjkstrange";
//
//    private InvocationRequest extractRequest(ByteBuf byteBuf) {
//        InvocationRequest request = new InvocationRequest();
//        FuncType funcType = new FuncType();
//        int readEndIndex = byteBuf.writerIndex();
//        int beginIndex, endIndex;
//
//        int count = 0;
//        for (beginIndex = byteBuf.forEachByte(FIND_EQUAL),
//                     endIndex = byteBuf.forEachByte(beginIndex, readEndIndex-beginIndex, FIND_AND); count < 3; ++count) {
//            byteBuf.readerIndex(beginIndex);
//            //todo 这边可能会出现 indexoutofBound
//            String content = byteBuf.readCharSequence(endIndex - beginIndex, Charset.forName("utf-8")).toString();
//            switch (count) {
//                case 0: {
//                    funcType.setInterfaceName(content);
//                    break;
//                }
//                case 1: {
//                    funcType.setMethodName(content);
//                    break;
//                }
//                case 2: {
//                    funcType.setParameterTypes(content);
//                    break;
//                }
//                default: {
//                    // 这个错误不会出现
//                    logger.error("index out of bound");
//                }
//            }
//            beginIndex = endIndex + 1;
//        }
//
//        byteBuf.readerIndex(beginIndex);
//        ByteBuf argument = byteBuf.retainedSlice(beginIndex, endIndex-beginIndex);
//        request.setArgument(argument);
//        request.setFuncType(funcType);
//        return request;
//    }


}
