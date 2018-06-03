package com.alibaba.dubbo.performance.demo.nettyagent;

import com.alibaba.dubbo.performance.demo.nettyagent.ConsumerAgentHandlers.CAInitializer;
import com.alibaba.dubbo.performance.demo.nettyagent.registry.Endpoint;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import com.alibaba.dubbo.performance.demo.nettyagent.registry.EtcdRegistry;
import com.alibaba.dubbo.performance.demo.nettyagent.registry.IRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by gexinjie on 2018/5/28.
 */
public class NettyConsumerAgent {
    static final int agentPort = Integer.parseInt(System.getProperty("server.port"));
    static final IRegistry registry = new EtcdRegistry(System.getProperty("etcd.url"));

    private Logger logger = LoggerFactory.getLogger(NettyConsumerAgent.class);
    private List<Endpoint> endpoints = Collections.unmodifiableList(Arrays.asList(
            new Endpoint("provider-large",30000),
            new Endpoint("provider-medium",30000),
            new Endpoint("provider-small",30000)));

    public static void main(String[] args) throws Exception{
        new NettyConsumerAgent().run();
    }
    public void run() {
        try {
            // Configure the server.
            EventLoopGroup bossGroup = new NioEventLoopGroup(1);
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup)
//                        .option(ChannelOption.SO_KEEPALIVE, true)
//                        .option(ChannelOption.TCP_NODELAY, true)
                        .channel(NioServerSocketChannel.class)
                        .localAddress(new InetSocketAddress(agentPort))
                        .handler(new LoggingHandler(LogLevel.INFO))
                        .childHandler(new CAInitializer(endpoints));

                Channel ch = b.bind().sync().channel();
                ch.closeFuture().sync();
            } finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }

        }catch (Exception e) {
            e.printStackTrace();
        }
    }

}
