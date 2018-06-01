package com.alibaba.dubbo.performance.demo.nettyagent.ConsumerAgentHandlers;

import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by gexinjie on 2018/5/28.
 */


/*
   consumer agent 接收到 consumer 的连接后，需要选择一个 provider agent 进行连接
   CA 在接收到 consumer 的请求后
   1. 将 HTTP 形式的请求解码为 Invocation 对象
   2. TODO 编码为用合适的形式发送给 PA
   3. PA 以合适的形式返回结果
   4. CA 将结果编码为 HTTP 返回给 consumer
 */
public class ReceiveConsumerHandler extends ChannelInboundHandlerAdapter{
    private Logger logger = LoggerFactory.getLogger(ReceiveConsumerHandler.class);

    ChannelFuture providerChannelFuture = null;

    public ReceiveConsumerHandler(ChannelFuture providerChannelFuture) {
        this.providerChannelFuture = providerChannelFuture;
    }

    /**
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest req = (FullHttpRequest) msg;
            if (req.method().equals(HttpMethod.POST)) {
                HttpPostRequestDecoder postDecoder = new HttpPostRequestDecoder(req);
                List<InterfaceHttpData> paramList = postDecoder.getBodyHttpDatas();
                Map<String, String> paramMap = new HashMap<String, String>();
                for (InterfaceHttpData para : paramList) {
                    Attribute data = (Attribute) para;
                    paramMap.put(data.getName(), data.getValue());
                }
                final Invocation invocation = new Invocation();

                logger.info("CA received from Consumer, now sending to Provider.  method: "
                        + paramMap.get("method" +
                        "  parameterTypesString: "+paramMap.get("parameterTypesString")+
                        "   parameter: " + paramMap.get("parameter")));

                invocation.setMethodName(paramMap.get("method"));
                invocation.setParameterTypes(paramMap.get("parameterTypesString"));
                invocation.setArguments(paramMap.get("parameter"));


                if (providerChannelFuture.isDone()) {
                    Channel providerChannel = providerChannelFuture.channel();
                    providerChannel.writeAndFlush(invocation);
                } else {
                    providerChannelFuture.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            future.channel().writeAndFlush(invocation);
                        }
                    });
                }

            }

        }
    }


}
