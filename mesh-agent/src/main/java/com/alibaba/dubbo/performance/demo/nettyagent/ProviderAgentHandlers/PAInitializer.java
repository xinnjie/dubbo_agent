package com.alibaba.dubbo.performance.demo.nettyagent.ProviderAgentHandlers;

import com.alibaba.dubbo.performance.demo.nettyagent.CacheDecoder;
import com.alibaba.dubbo.performance.demo.nettyagent.CacheEncoder;
import com.alibaba.dubbo.performance.demo.nettyagent.DubboRpcDecoder;
import com.alibaba.dubbo.performance.demo.nettyagent.DubboRpcEncoder;
import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import com.alibaba.dubbo.performance.demo.nettyagent.model.Invocation;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * Created by gexinjie on 2018/6/1.
 */
public class PAInitializer extends ChannelInitializer<SocketChannel> {
    // 语义上来说这三份 cache 都属于 PA
    // 每个 PA 内部所有 handler 共用一组 Cache, 由于一个服务器上只运行一个PA,这组 cache 可以是全类共享
    static private final HashMap<FuncType, Integer> methodIDsCache = new HashMap<>();
    static private final HashMap<Integer, FuncType> methodsCache = new HashMap<>();
    static private final HashMap<Long, Integer> requestToMethodFirstCache = new HashMap<>();
    private Logger logger = LoggerFactory.getLogger(PAInitializer.class);


    /*
    PA 左边连接到 CA 的 channel 设置，包括一步收到消息自动转发给 Provider
     */
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();
        // PA 连接到 provider
        ChannelFuture providerFuture =  bootstrapConnectToProvider(ch);
        p.addLast("cacheEncoder", new CacheEncoder(PAInitializer.methodIDsCache, PAInitializer.requestToMethodFirstCache));
        p.addLast("cacheDecoder", new CacheDecoder(PAInitializer.methodsCache, PAInitializer.requestToMethodFirstCache));

        /*
        当读取到 CA 的数据后，将读到的 invocation 写入 provider 去
         */
        p.addLast("transmit2provider", new ChannelInboundHandlerAdapter() {
            final ChannelFuture providerChannelFuture = providerFuture;

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                Invocation invocation = (Invocation) msg;

                if (providerChannelFuture.isSuccess()) {
                    Channel providerChannel = providerChannelFuture.channel();
                    logger.info("is about to send to provider");
                    providerChannel.writeAndFlush(invocation);
                } else {
                    providerChannelFuture.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            Channel providerChannel = future.channel();
                            providerChannel.writeAndFlush(invocation);
                        }
                    });
                }

            }
        });

    }
    public ChannelFuture bootstrapConnectToProvider(SocketChannel leftChannel) {
        Bootstrap bootstrap = new Bootstrap()
                .group(leftChannel.eventLoop())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
//                .option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                         final Channel PALeftChannel = leftChannel;
                         @Override
                         protected void initChannel(SocketChannel ch) throws Exception {
                             ChannelPipeline pipeline = ch.pipeline();
                             pipeline.addLast("DubboEncoder", new DubboRpcEncoder());
                             pipeline.addLast("DubboDecoder", new DubboRpcDecoder());
                             pipeline.addLast("sendToCAHandler", new ChannelInboundHandlerAdapter() {
                                 @Override
                                 public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                     Invocation invocation = (Invocation) msg;
                                     PALeftChannel.writeAndFlush(invocation);
                                 }
                             });
                         }
                     }
                );
        int port = Integer.valueOf(System.getProperty("dubbo.protocol.port"));

        logger.info("connection to provider established");
        return bootstrap.connect("127.0.0.1", port);

    }
}