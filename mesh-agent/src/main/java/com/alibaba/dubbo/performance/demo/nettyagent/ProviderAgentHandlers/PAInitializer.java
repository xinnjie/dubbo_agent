package com.alibaba.dubbo.performance.demo.nettyagent.ProviderAgentHandlers;

import com.alibaba.dubbo.performance.demo.nettyagent.*;
import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import com.alibaba.dubbo.performance.demo.nettyagent.util.CacheContext;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by gexinjie on 2018/6/1.
 */
public class PAInitializer extends ChannelInitializer<SocketChannel> {
    // 语义上来说这三份 cache 都属于 PA
    private final CacheContext cacheContext;
    private final ConcurrentHashMap<Long, Integer> requestToMethodFirstCache;
    private Logger logger = LoggerFactory.getLogger(PAInitializer.class);


    public PAInitializer(CacheContext cacheContext, ConcurrentHashMap<Long, Integer> requestToMethodFirstCache) {
        this.cacheContext = cacheContext;
        this.requestToMethodFirstCache = requestToMethodFirstCache;
    }

    /*
        PA 作为服务器的 pipeline （左边）
        PA 左边连接到 CA 的 channel 设置，包括一步收到消息自动转发给 Provider
        从 CA 到 PA 的连接保持固定，PA 每次收到 CA 的连接就也发起一个到 provider 的连接
         */
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();
        // PA 连接到 provider
        ChannelFuture providerFuture =  bootstrapConnectToProvider(ch);
        p.addLast("cacheEncoder", new CacheEncoder(cacheContext, requestToMethodFirstCache));
        p.addLast("cacheDecoder", new CacheDecoder(cacheContext, requestToMethodFirstCache));

        /*
        当读取到 CA 的 request 数据后，将读到的 invocation 写入 provider 去
         */
        p.addLast("transmit2provider", new Transmit2provider(providerFuture));

    }
    /*
    PA 作为客户端向 Dubbo 请求的 pipeline 设置（右边）
     */
    public ChannelFuture bootstrapConnectToProvider(SocketChannel leftChannel) {
        Bootstrap bootstrap = new Bootstrap()
                .group(leftChannel.eventLoop())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
//                .option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(600, 1200))

//                .option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                         final Channel PALeftChannel = leftChannel;
                         // (todo 高亮) PA 和 CA 的连接部分
                         @Override
                         protected void initChannel(SocketChannel ch) throws Exception {
                             ChannelPipeline pipeline = ch.pipeline();
                             pipeline.addLast("DubboEncoder", new DubboRpcEncoder());
                             pipeline.addLast("DubboDecoder", new DubboRpcDecoder());
                             pipeline.addLast("send2CA", new ChannelInboundHandlerAdapter() {
                                 @Override
                                 public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                     Invocation invocation = (Invocation) msg;
//                                     if (PALeftChannel.isActive()) {
                                         PALeftChannel.writeAndFlush(invocation);
//                                     } else {
//                                         logger.error("connection between CA and PA is broken");
//                                     }
                                 }
                             });
                         }

                         }
                );
        int port = Integer.valueOf(System.getProperty("dubbo.protocol.port"));

        return bootstrap.connect("127.0.0.1", port);

    }
}