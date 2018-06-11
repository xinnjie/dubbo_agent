package com.alibaba.dubbo.performance.demo.nettyagent.ConsumerAgentUtil;

import com.alibaba.dubbo.performance.demo.nettyagent.model.InvocationRequest;
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


public class Http2Request extends ChannelInboundHandlerAdapter{
    private Logger logger = LoggerFactory.getLogger(Http2Request.class);
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


}
