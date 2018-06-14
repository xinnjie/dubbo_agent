package com.alibaba.dubbo.performance.demo.nettyagent.ConsumerAgentUtil;

import com.alibaba.dubbo.performance.demo.nettyagent.codec.Http2Request;
import com.alibaba.dubbo.performance.demo.nettyagent.codec.Result2Http;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import org.apache.logging.log4j.LogManager;


/**
 * Created by gexinjie on 2018/5/28.
 */
public class CAInitializer extends ChannelInitializer<SocketChannel> {
    private final ConnectManager connectManager;
    org.apache.logging.log4j.Logger logger = LogManager.getLogger(LogManager.ROOT_LOGGER_NAME);


    public CAInitializer(ConnectManager manager) {
        this.connectManager = manager;
    }

    /*
   consumer agent 接收到 consumer 的连接后，需要选择一个 provider agent 进行连接
   CA 在接收到 consumer 的请求后
   1. 将 HTTP 形式的请求解码为 Invocation 对象
   2. 编码为用合适的形式发送给 PA
   3. PA 以合适的形式返回结果
   4. CA 将结果编码为 HTTP 返回给 consumer
 */

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        logger.debug("CAInitializer.initChannel 被调用（确保服务端发起几次连接，被调用几次）");
        ChannelPipeline p = ch.pipeline();

        p.addLast("httpCodec", new HttpServerCodec());
        p.addLast("aggregator", new HttpObjectAggregator(1400));

        /*
        这个当 consumer 传来请求时，将 HTTP POST 中内容提取出来构造一个 invocation，并写入到 PA 中
         */
        p.addLast("http2invocation", new Http2Request());
        // Transmit2PA 必须把 invocation 的 requestID 传给 connectManager
        p.addLast("transmit2PA", new Transmit2PA(connectManager));

        /*
           当写入 Invocation 到 Consumer 时，
           下面的这个 handler将 invocation 的 result 提取出来，构造出一个 POST 请求，传递个 http encoder
        */
        p.addLast("responseInvocation2http", new Result2Http());
    }


}
