package com.alibaba.dubbo.performance.demo.nettyagent.ProviderAgentHandlers;

import com.alibaba.dubbo.performance.demo.nettyagent.CacheDecoder;
import com.alibaba.dubbo.performance.demo.nettyagent.CacheEncoder;
import com.alibaba.dubbo.performance.demo.nettyagent.ConsumerAgentHandlers.CAInitializer;
import com.alibaba.dubbo.performance.demo.nettyagent.ConsumerAgentHandlers.SendConsumerHandler;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by gexinjie on 2018/6/1.
 */
public class PAInitializer extends ChannelInitializer<SocketChannel> {
    // 每个 PA 内部所有 handler 共用一组 Cache，这组 Cache 由 ProviderAgent 类提供
    private HashMap<FuncType, Integer> methodIDsCache = null;
    private HashMap<Integer, FuncType> methodsCache = null;

    private Logger logger = LoggerFactory.getLogger(PAInitializer.class);


    public PAInitializer(HashMap<FuncType, Integer> methodIDsCache, HashMap<Integer, FuncType> methodsCache) {
        this.methodIDsCache = methodIDsCache;
        this.methodsCache = methodsCache;

    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();
        ChannelFuture providerFuture =  bootstrapConnectToProvider(ch);
        p.addLast("cacheEncoder", new CacheEncoder(this.methodIDsCache));
        p.addLast("cacheDecoder", new CacheDecoder(this.methodsCache));


        /*
        当读取到 CA 的数据后，将读到的 invocation 写入 provider 去
         */
        p.addLast("sendToProvider", new ChannelInboundHandlerAdapter() {
            final ChannelFuture providerChannelFuture = providerFuture;

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                Invocation invocation = (Invocation) msg;

                logger.info("requestId=" + invocation.getRequestID());

                if (providerChannelFuture.isDone()) {
                    Channel providerChannel = providerChannelFuture.channel();
                    providerChannel.write(invocation);
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
    public ChannelFuture bootstrapConnectToProvider(SocketChannel channel) {
        final Channel finalChannel = channel;
        Bootstrap bootstrap = new Bootstrap()
                .group(channel.eventLoop())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
//                .option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                         Channel providerChannel = finalChannel;
                         @Override
                         protected void initChannel(SocketChannel ch) throws Exception {
                             ChannelPipeline pipeline = ch.pipeline();
                             pipeline.addLast("DubboEncoder", new DubboRpcEncoder());
                             pipeline.addLast("DubboDecoder", new DubboRpcDecoder());
                             pipeline.addLast("sendToCAHandler", new ChannelInboundHandlerAdapter() {
                                 @Override
                                 public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                     Invocation invocation = (Invocation) msg;
                                     providerChannel.write(invocation);
                                 }
                             });
                         }
                     }
                );
        int port = Integer.valueOf(System.getProperty("dubbo.protocol.port"));

        logger.info("connecting to provider");

        return bootstrap.connect("127.0.0.1", port);

    }
}