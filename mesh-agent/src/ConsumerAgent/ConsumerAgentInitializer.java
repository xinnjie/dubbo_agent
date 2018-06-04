package ConsumerAgent;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

/**
 * Created by gexinjie on 2018/5/28.
 */
public class ConsumerAgentInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();
        p.addLast("http_codc", new HttpServerCodec());
        p.addLast("aggregator", new HttpObjectAggregator(100000));
        p.addLast("agent_handler", new ReceiveConsumerHandler());

    }
}
