package com.alibaba.dubbo.performance.demo.nettyagent;

import com.alibaba.dubbo.performance.demo.nettyagent.ConsumerAgentUtil.*;
import com.alibaba.dubbo.performance.demo.nettyagent.registry.Endpoint;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import com.alibaba.dubbo.performance.demo.nettyagent.registry.EtcdRegistry;
import com.alibaba.dubbo.performance.demo.nettyagent.registry.IRegistry;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.apache.logging.log4j.LogManager;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;

/**
 * Created by gexinjie on 2018/5/28.
 */
public class NettyConsumerAgent {
    static final int agentPort = Integer.parseInt(System.getProperty("server.port"));
    static final IRegistry registry = new EtcdRegistry(System.getProperty("etcd.url"), 0);

org.apache.logging.log4j.Logger logger = LogManager.getLogger(LogManager.ROOT_LOGGER_NAME);
    Map<Endpoint, Integer> endpointsAndPortion;
//    private List<Endpoint> endpointsAndPortion = Collections.unmodifiableList(Arrays.asList(
//            new Endpoint("provider-large",30000),
//            new Endpoint("provider-medium",30000),
//            new Endpoint("provider-small",30000)));

    public static void main(String[] args) throws Exception{
        new NettyConsumerAgent().run();
    }
    public void run() {
        try {
            int workThreadsNum = Integer.parseInt(System.getProperty("worker.threads"));

            // Configure the server.
            EventLoopGroup bossGroup = new NioEventLoopGroup(1);
            EventLoopGroup workerGroup = new NioEventLoopGroup(workThreadsNum);
            endpointsAndPortion = registry.find("com.alibaba.dubbo.performance.demo.provider.IHelloService");

            waitProviderAgent();
            logger.debug("all providers has been found: {}", endpointsAndPortion);
            /*
            connectManager 创建从 CA 到 PA 的连接，连接数由 PA 在 etcd 中注册的连接数量决定
             */
            ConnectManager connectManager = new ConnectManager(workerGroup, endpointsAndPortion);
            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup)
                        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                        .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        .childOption(ChannelOption.TCP_NODELAY, true)
                        .channel(NioServerSocketChannel.class)
                        .localAddress(new InetSocketAddress(agentPort))
                        .handler(new LoggingHandler(LogLevel.WARN))
                        .childHandler(new CAInitializer(connectManager));

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

    private void waitProviderAgent() {
        boolean scanning=true;

        for (Map.Entry<Endpoint, Integer> entry : this.endpointsAndPortion.entrySet()) {
            Endpoint endpoint = entry.getKey();
            while (scanning) {
                Socket sock = new Socket();
                try {
                    InetSocketAddress address = new InetSocketAddress(endpoint.getHost(), endpoint.getPort());
                    sock.connect(address);
                    scanning = false;
                    logger.debug("Connected to provider, so provider must have been started");
                } catch (IOException e) {
                    logger.debug("Connect to provider at " + endpoint.getHost() + ":" + endpoint.getPort() + " failed (the provider may have not started) waiting and trying again");
                    try {
                        Thread.sleep(2000);//2 seconds
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                } finally {
                    try {
                        sock.close();
                    } catch (IOException e) {
                        logger.error("can not disconnect to provider. error message:" + e.getMessage());
                    }
                }
            }

        }

    }
}
