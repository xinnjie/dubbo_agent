package com.alibaba.dubbo.performance.demo.nettyagent.ProviderAgentHandlers;

import com.alibaba.dubbo.performance.demo.nettyagent.*;
import com.alibaba.dubbo.performance.demo.nettyagent.codec.CacheRequestDecoder;
import com.alibaba.dubbo.performance.demo.nettyagent.codec.CacheResponseEncoder;
import com.alibaba.dubbo.performance.demo.nettyagent.codec.DubboRpcDecoder;
import com.alibaba.dubbo.performance.demo.nettyagent.codec.DubboRpcEncoder;
import com.alibaba.dubbo.performance.demo.nettyagent.model.InvocationResponse;
import com.alibaba.dubbo.performance.demo.nettyagent.util.CacheContext;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.logging.log4j.LogManager;


import java.net.CacheRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by gexinjie on 2018/6/1.
 */
public class PAInitializer extends ChannelInitializer<SocketChannel> {
    // 语义上来说这三份 cache 都属于 PA
    private final CacheContext cacheContext;
    private final ConcurrentHashMap<Long, Integer> requestToMethodFirstCache;
    org.apache.logging.log4j.Logger logger = LogManager.getLogger(LogManager.ROOT_LOGGER_NAME);
    volatile List<Channel> CAChannels = new ArrayList<>();
    ConcurrentLinkedQueue<Channel> actualCAChannels = new ConcurrentLinkedQueue<>();
    Channel providerChannel = null;
    Random random = new Random();
    AtomicInteger connectionCount = new AtomicInteger(0);


    /*
    在初始化阶段创建一个到 provider 的连接
     */
    public PAInitializer(CacheContext cacheContext, ConcurrentHashMap<Long, Integer> requestToMethodFirstCache, EventLoopGroup eventLoopGroup, int connectionNum) {
        this.cacheContext = cacheContext;
        this.requestToMethodFirstCache = requestToMethodFirstCache;
        try {
            ChannelFuture providerChannelFuture = bootstrapConnectToProvider(eventLoopGroup).sync();
            if (providerChannelFuture.isSuccess()) {
                providerChannel = providerChannelFuture.channel();
                logger.info("PA connected to provider: {}", providerChannel);
            } else {
                logger.error("PA can not connect to provider");
            }
        } catch (InterruptedException e) {
            logger.error("connection interrupted", e);
        }
    }

    /*
    PA 作为服务器的 pipeline （左边）
    PA 左边连接到 CA 的 channel 设置，包括收到消息自动转发给 Provider
    从 CA 到 PA 的连接保持固定，PA 每次收到 CA 的连接就也发起一个到 provider 的连接
     */
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();
//        p.addLast("accumulator", new Accumulator(AgentConfig.SEND_ONCE));
        p.addLast("registerChannel", new ChannelInboundHandlerAdapter() {
            @Override
            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                super.channelActive(ctx);
                actualCAChannels.add(ctx.channel());
                connectionCount.incrementAndGet();
                List<Channel> newList = new ArrayList<>(actualCAChannels);
                Collections.shuffle(newList);
                CAChannels = Collections.unmodifiableList(newList);
                logger.info("PA connected to CA: {}\n new CA channels: {}", ctx.channel(), CAChannels);
            }
            @Override
            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                super.channelInactive(ctx);
                actualCAChannels.remove(ctx.channel());
                connectionCount.decrementAndGet();
                List<Channel> newList = new ArrayList<>(actualCAChannels);
                Collections.shuffle(newList);
                CAChannels = Collections.unmodifiableList(newList);
                logger.warn("CA connection to CA is not active! {}\nnew CA channels: {}", ctx.channel(), CAChannels);
            }
        });
        p.addLast("responseEncoder", new CacheResponseEncoder(cacheContext, requestToMethodFirstCache));
        p.addLast("requestDecoder", new CacheRequestDecoder(cacheContext, requestToMethodFirstCache));

        /*
        当读取到 CA 的 request 数据后，将读到的 invocation 写入 provider 去
         */
        p.addLast("transmit2provider", new Transmit2provider(providerChannel));

    }
    /*
    PA 作为客户端向 Dubbo 请求的 pipeline 设置（右边）
     */
    public ChannelFuture bootstrapConnectToProvider(EventLoopGroup eventLoopGroup) {
        Bootstrap bootstrap = new Bootstrap()
                .group(eventLoopGroup)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)

                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                         // PA 连接到 P
                         @Override
                         protected void initChannel(SocketChannel ch) throws Exception {
                             ChannelPipeline pipeline = ch.pipeline();
//                             pipeline.addLast("accumulator", new Accumulator(AgentConfig.SEND_ONCE * Integer.parseInt(System.getProperty("connection.num"))));
//                             pipeline.addLast("accumulator", new Accumulator(AgentConfig.SEND_ONCE));
                             pipeline.addLast("DubboEncoder", new DubboRpcEncoder());
                             pipeline.addLast("DubboDecoder", new DubboRpcDecoder());
                             pipeline.addLast("send2CA", new ChannelInboundHandlerAdapter() {
                                 @Override
                                 public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                     InvocationResponse response = (InvocationResponse) msg;
                                     Channel comsumerChannel = selectCAChannel((int)response.getRequestID());
                                     comsumerChannel.writeAndFlush(response);
                                 }
                             });
                         }

                         }
                );
        int port = Integer.valueOf(System.getProperty("dubbo.protocol.port"));

        return bootstrap.connect("127.0.0.1", port);

    }

    private  Channel selectCAChannel(int sequenceID) {
        Channel channel = CAChannels.get(sequenceID % CAChannels.size());
        if (!channel.isActive()) {
            logger.error("channel is not active {} ", channel);
            return selectCAChannel(sequenceID + 1);
        }
        return channel;
//        do {
//            int index = (sequenceID % (AgentConfig.SEND_ONCE * CAChannels.size()) ) / AgentConfig.SEND_ONCE;
//            CAChannel = CAChannels.get(index);
//        } while (!CAChannel.isActive());
//        return CAChannel;
    }
}