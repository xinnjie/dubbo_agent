package ConsumerAgent;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import netty_agent.registry.Endpoint;

import java.util.*;



/**
 * Created by gexinjie on 2018/5/28.
 */


/*
   consumer agent 接收到 consumer 的连接后，需要选择一个 provider agent 进行连接
   CA 在接收到 consumer 的请求后
   1. 将 HTTP 形式的请求解码为 Invocation 对象
   2. TODO 编码为用合适的形式发送给 PA
   3. PA 以合适的形式返回结果
   4. CA 将结果编码为 HTTP 返回给 consumer
 */
public class ReceiveConsumerHandler extends ChannelInboundHandlerAdapter{
    ChannelFuture connectedProviderFuture = null;

    /**
     * 当 consumer agent 接收到来自 consumer 的连接时，CA 需要选择一个 PA 进行连接
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        addProviderChannel(ctx.channel().eventLoop());
    }

    /*
      创建一个连接到 provider agent 的连接
     */
    private ChannelFuture addProviderChannel(EventLoop et) {
        if (connectedProviderFuture == null) {
            Endpoint selectedProvider = selectEndpoint();
            Bootstrap bootstrap = new Bootstrap();

            bootstrap.channel(NioSocketChannel.class)
                    .group(et)
                    .handler(new ConsumerAgentInitializer2());

            connectedProviderFuture = bootstrap.connect(selectedProvider.getHost(), selectedProvider.getPort());
        }
        return connectedProviderFuture;
    }

    /**
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            HttpRequest req = (FullHttpRequest) msg;
            if (req.method().equals(HttpMethod.POST)) {
                HttpPostRequestDecoder postDecoder = new HttpPostRequestDecoder(req);
                List<InterfaceHttpData> paramList = postDecoder.getBodyHttpDatas();

//                RpcInvocation

                if (!HttpUtil.isKeepAlive(req)) {
                    ctx.write(response).addListener(ChannelFutureListener.CLOSE);
                }
            }

        }
    }

    //todo 释放与 Consumer 断开连接时也要断开与 PA 的连接
//    @Override
//    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
//        connectedProviderFuture.channel().deregister();
//        super.channelInactive(ctx);
//    }

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

}
