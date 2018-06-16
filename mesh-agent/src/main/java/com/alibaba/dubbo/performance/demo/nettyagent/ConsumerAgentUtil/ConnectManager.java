package com.alibaba.dubbo.performance.demo.nettyagent.ConsumerAgentUtil;

import com.alibaba.dubbo.performance.demo.nettyagent.codec.CacheRequestEncoder;
import com.alibaba.dubbo.performance.demo.nettyagent.codec.CacheResponseDecoder;
import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;
import com.alibaba.dubbo.performance.demo.nettyagent.model.InvocationResponse;
import com.alibaba.dubbo.performance.demo.nettyagent.registry.Endpoint;
import com.alibaba.dubbo.performance.demo.nettyagent.util.CacheContext;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.logging.log4j.LogManager;


import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by gexinjie on 2018/6/4.
 */
/*
 ConnectManager 负责创建从 CA 到 3个 PA 的所有连接
 */
public class ConnectManager {
    private final EventLoopGroup eventLoopGroup;
org.apache.logging.log4j.Logger logger = LogManager.getLogger(LogManager.ROOT_LOGGER_NAME);
    static final Random random = new Random();
    private final Map<Endpoint, Integer> endpointsAndPortion;
    private List<Channel> PAChannels = new ArrayList<>();
    //    final private Map<Long, Channel> request2CAChannel= Collections.synchronizedMap(new HashMap<>());
    // TODO  request2CAChannel 可以进行优化
    // 使用桶装的容器，桶内设n 个板子，对 requestID 进行分流，减少修改时的阻塞
    final private ConcurrentHashMap<Long, Channel> request2CAChannel= new ConcurrentHashMap<>();

    final AtomicInteger count = new AtomicInteger(0);

    /*
    语义上讲这两份 cache 是属于 CA 的
     */
    private HashMap<Endpoint, CacheContext> cacheContexts = null;
    private HashMap<Endpoint, ConcurrentHashMap<Integer, FuncType>> endpointMethods = null;

    public ConnectManager(EventLoopGroup eventLoopGroup, Map<Endpoint, Integer> endpointsAndPortion) {
        this.eventLoopGroup = eventLoopGroup;
        this.endpointsAndPortion = endpointsAndPortion;
        initVariables();
    }

    private void initVariables() {
        // 对每个 PA 发起两条连接，按照 portion 数量加权轮询,初始化 cacheContexts 变量
        cacheContexts = new HashMap<>();
        for (Endpoint PAendpoint :
                this.endpointsAndPortion.keySet()) {
            cacheContexts.put(PAendpoint, new CacheContext());
            for (int i = 0; i < 2; i++) {
                Channel thePAchannel = connectToPA(PAendpoint);
                if (thePAchannel == null) {
                    logger.error("CA to PA connection not established!");
                } else {
                    for (int j = 0; j < this.endpointsAndPortion.get(PAendpoint); j++) {
                        PAChannels.add(thePAchannel);
                    }
                }
            }
        }
        Collections.shuffle(PAChannels);
        PAChannels = Collections.unmodifiableList(PAChannels);
    }


    @Nullable
    private Channel connectToPA(Endpoint PAendpoint) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.channel(NioSocketChannel.class)
                .group(this.eventLoopGroup)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("RequestEncoder", new CacheRequestEncoder(cacheContexts.get(PAendpoint)));
                        pipeline.addLast("ResponseDecoder", new CacheResponseDecoder(cacheContexts.get(PAendpoint)));
                        // 当读入 PA 的返回结果时，继续引发 CA 写结果回 consumer      C <-- CA <-- PA （时间开始事件为 CA 读入PA的返回结果）
                        pipeline.addLast("WriteToConsumer", new ChannelInboundHandlerAdapter() {
                            /*
                            dubbo 返回的response，能使用的包括：返回值，request ID
                            todo 是哪里 consumerChannel 需要在运行channelRead 这个方法时决定，而不是在初始化时就决定。
                             */
                            @Override
                            public void channelRead(ChannelHandlerContext ctx_, Object msg) throws Exception {
                                InvocationResponse response = (InvocationResponse) msg;
                                // getAccordingConsumerChannel 将会返回对应于 reqeustID 的 consumerChannel （ps *****requestID 和 consumerChannel有对应关系）
                                Channel consumerChannel = getAccordingConsumerChannel(response.getRequestID());
                                if (consumerChannel != null) {
                                    logger.debug("received result from PA， find the right consumer channel for request " + response.getRequestID() + ": " + consumerChannel.toString());
                                    // 将来自 PA 的 response 发回给 Consumer
                                    consumerChannel.writeAndFlush(response);
                                } else {
                                    logger.error("request ID: {}  is duplicated! 肯定还有问题", response.getRequestID());
                                }
                            }
                        });
                    }
                });
        try {
            Channel channel = bootstrap.connect(PAendpoint.getHost(), PAendpoint.getPort()).sync().channel();
            logger.info("CA connected to PA: {}", channel);
            return channel;
        } catch (InterruptedException e) {
            logger.error(e);
        }
        return null;
    }

    private Channel getAccordingConsumerChannel(long requestID) {
        Channel consumerChannel = this.request2CAChannel.get(requestID);
        if (consumerChannel == null) {
            logger.error("request not in request table, maybe already processed? requestID is :" + requestID);
        }
        this.request2CAChannel.remove(requestID);
        return consumerChannel;
    }

    /**
     * @param requestID 利用这个传入的 requestID 记录下 requestID 和 consumer Channel的对应关系
     * @return
     */
    public Channel getProviderChannel(Channel consumerChannel, long requestID) {
        int count = (int)requestID;
        this.request2CAChannel.put(requestID, consumerChannel);
        Channel selected = PAChannels.get(count % PAChannels.size());
        return selected;
    }


}
