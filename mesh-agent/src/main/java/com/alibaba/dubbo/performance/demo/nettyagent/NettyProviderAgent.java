/**
 * Created by gexinjie on 2018/5/29.
 */
package com.alibaba.dubbo.performance.demo.nettyagent;

import com.alibaba.dubbo.performance.demo.nettyagent.ProviderAgentHandlers.PAInitializer;
import com.alibaba.dubbo.performance.demo.nettyagent.registry.EtcdRegistry;
import com.alibaba.dubbo.performance.demo.nettyagent.registry.IRegistry;
import com.alibaba.dubbo.performance.demo.nettyagent.util.CacheContext;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.ResourceLeakDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class NettyProviderAgent {
    static final int agentPort = Integer.parseInt(System.getProperty("server.port"));
    static final IRegistry registry = new EtcdRegistry(System.getProperty("etcd.url"), 50);

    private Logger logger = LoggerFactory.getLogger(NettyConsumerAgent.class);



    public static void main(String[] args) throws Exception{
        new NettyConsumerAgent().run();
    }


    public void run() {
        try {
            // Configure the server.
            EventLoopGroup bossGroup = new NioEventLoopGroup(1);
            //  TODO 要给 worker 分配几个线程?
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            waitProvider();

            CacheContext cacheContext = new CacheContext();
            ConcurrentHashMap<Long, Integer>  requestToMethodFirstCache = new ConcurrentHashMap<>();

            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup)
                        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                        .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        .childOption(ChannelOption.TCP_NODELAY, true)
//                        .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(600, 1200))
                        .channel(NioServerSocketChannel.class)
                        .localAddress(new InetSocketAddress(agentPort))
                        .handler(new LoggingHandler(LogLevel.WARN))
                        .childHandler(new PAInitializer(cacheContext, requestToMethodFirstCache));

                Channel ch = b.bind().sync().channel();
                logger.info("bound to port " + agentPort);
                ch.closeFuture().sync();
            } finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void waitProvider() {
        boolean scanning=true;
        int port = Integer.valueOf(System.getProperty("dubbo.protocol.port"));

        while(scanning) {
            Socket sock = new Socket();
            try {
                InetSocketAddress address =  new InetSocketAddress("127.0.0.1", port);
                sock.connect(address);
                scanning=false;
                logger.info("Connected to provider, so provider must have been started");
            }
            catch(IOException e) {
                logger.info("Connect to provider at port " + port + " failed (the provider may have not started, waiting and trying again");
                try {
                    Thread.sleep(2000);//2 seconds
                } catch(InterruptedException ie){
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
