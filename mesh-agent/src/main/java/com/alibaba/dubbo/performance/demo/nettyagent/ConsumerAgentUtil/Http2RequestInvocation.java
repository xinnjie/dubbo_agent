package com.alibaba.dubbo.performance.demo.nettyagent.ConsumerAgentUtil;

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


public class Http2RequestInvocation extends ChannelInboundHandlerAdapter{
    private Logger logger = LoggerFactory.getLogger(Http2RequestInvocation.class);
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        FullHttpRequest req = (FullHttpRequest) msg;
        if (req.method().equals(HttpMethod.POST)) {
            HttpPostRequestDecoder postDecoder = new HttpPostRequestDecoder(req);
            List<InterfaceHttpData> paramList = postDecoder.getBodyHttpDatas();
            Map<String, String> paramMap = new HashMap<>();
            for (InterfaceHttpData para : paramList) {
                Attribute data = (Attribute) para;
                paramMap.put(data.getName(), data.getValue());
            }
            final Invocation invocation = new Invocation();

            /*
            TODO 解读http，构建对应 invocation 的重要部分，多检查一下
             */
            invocation.setMethodName(paramMap.get("method"));
            invocation.setParameterTypes(paramMap.get("parameterTypesString"));
            invocation.setArguments(paramMap.get("parameter"));
            invocation.setInterfaceName(paramMap.get("interface"));
            //  todo 在 attachment 中设置 path 主要是为了和 dubbo 兼容, 有点冗余
//                invocation.setAttachment("path", paramMap.get("interface"));
            ctx.fireChannelRead(invocation);
        } else {
            logger.warn("CA received non-post request from consumer");
        }
    }


}
