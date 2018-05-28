package netty_agent;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import netty_agent.registry.Endpoint;
import netty_agent.registry.EtcdRegistry;
import netty_agent.registry.IRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by gexinjie on 2018/5/28.
 */
public class NettyAgent {
    static final int agentPort = Integer.parseInt(System.getProperty("server.port"));
    static final IRegistry registry = new EtcdRegistry(System.getProperty("etcd.url"));
    static List<Endpoint> endpoints = Collections.unmodifiableList(Arrays.asList(
            new Endpoint("provider-large",30000),
            new Endpoint("provider-medium",30000),
            new Endpoint("provider-small",30000)));

    private List<Endpoint> weightedEndpoints = null;
    static final Random random = new Random();

    private Endpoint selectEndpoint() {
        if (weightedEndpoints == null) {
            List<Integer> weight = Arrays.asList(3,2,1);
            assert endpoints.size()  == weight.size();
            weightedEndpoints = new ArrayList<>();
            for (int i = 0; i < weight.size(); ++i) {
                for (int j = 0; j < weight.get(i); j++) {
                    weightedEndpoints.add(endpoints.get(i));
                }
            }
            weightedEndpoints = Collections.unmodifiableList(weightedEndpoints);
        }
        return weightedEndpoints.get(random.nextInt(endpoints.size()));

    }

//    public static void main(String[] args) throws Exception{
//        // Configure the server.
//        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
//        EventLoopGroup workerGroup = new NioEventLoopGroup();
//        try {
//            ServerBootstrap b = new ServerBootstrap();
//            b.option(ChannelOption.SO_BACKLOG, 1024);
//            b.group(bossGroup, workerGroup)
//                    .channel(NioServerSocketChannel.class)
//                    .handler(new LoggingHandler(LogLevel.INFO))
//                    .childHandler(new (sslCtx));
//
//            Channel ch = b.bind(agentPort).sync().channel();
//
//
//            ch.closeFuture().sync();
//        } finally {
//            bossGroup.shutdownGracefully();
//            workerGroup.shutdownGracefully();
//        }
//    }

}
