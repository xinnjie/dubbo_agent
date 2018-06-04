package ConsumerAgent;

import ConsumerAgent.ConsumerAgentInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import netty_agent.registry.EtcdRegistry;
import netty_agent.registry.IRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * Created by gexinjie on 2018/5/28.
 */
public class NettyConsumerAgent {
    static final int agentPort = Integer.parseInt(System.getProperty("server.port"));
    static final IRegistry registry = new EtcdRegistry(System.getProperty("etcd.url"));

    private Logger logger = LoggerFactory.getLogger(NettyConsumerAgent.class);

    public static void main(String[] args) throws Exception{
        // Configure the server.
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        //  TODO 要给 worker 分配几个线程
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 1024);
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(agentPort))
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ConsumerAgentInitializer());

            Channel ch = b.bind().sync().channel();
            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

}
