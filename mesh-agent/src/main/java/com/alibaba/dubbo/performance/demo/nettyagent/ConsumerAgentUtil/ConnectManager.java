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
import org.apache.logging.log4j.EventLogger;
import org.apache.logging.log4j.LogManager;


import javax.annotation.Nullable;
import java.awt.*;
import java.util.*;
import java.util.List;
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
//    private List<Channel> PAChannels = new ArrayList<>();
    private HashMap<EventLoop, List<Channel>> PAchannelFromEventloop = new HashMap<>();
    final private ConcurrentHashMap<Long, Channel> request2CAChannel= new ConcurrentHashMap<>();
    
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
        int connectionsPerPA = 4;
        cacheContexts = new HashMap<>();

        EventLoop startLoop = this.eventLoopGroup.next();
        this.PAchannelFromEventloop.put(startLoop, new ArrayList<>());
        for (EventLoop loop = this.eventLoopGroup.next(); !loop.equals(startLoop); loop = this.eventLoopGroup.next()) {
            this.PAchannelFromEventloop.put(loop, new ArrayList<>());
        }
        int eventLoopSize = this.PAchannelFromEventloop.size();

        for (Endpoint PAendpoint :
                this.endpointsAndPortion.keySet()) {
            cacheContexts.put(PAendpoint, new CacheContext());
            // this.PAchannelFromEventloop.size() 为 eventloop 的数量，对每个 PA 的连接数分散到各个 eventloop 中去,
            // 所以 connectionsPerPA / this.PAchannelFromEventloop.size() 为每个 eventloop 中连接数量
            for (EventLoop loop : this.PAchannelFromEventloop.keySet()) {
                int connectionsPerEventloop = connectionsPerPA / eventLoopSize;
                for (int i = 0; i < connectionsPerEventloop; i++) {
                    Channel thePAchannel = connectToPA(PAendpoint, loop);
                    if (thePAchannel == null) {
                        logger.error("CA to PA connection not established!");
                    } else {
                        for (int j = 0; j < this.endpointsAndPortion.get(PAendpoint) / eventLoopSize / connectionsPerEventloop; j++) {
                            this.PAchannelFromEventloop.get(loop).add(thePAchannel);
                        }
                    }
                }
            }
        }

        for (EventLoop loop: this.PAchannelFromEventloop.keySet()) {
            List<Channel> channels = this.PAchannelFromEventloop.get(loop);
            Collections.shuffle(channels);
            this.PAchannelFromEventloop.replace(loop, Collections.unmodifiableList(channels));
        }

        logger.info("CA to PA connection portion info: {}", this.PAchannelFromEventloop);
    }


    @Nullable
    private Channel connectToPA(Endpoint PAendpoint, EventLoop eventLoop) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.channel(NioSocketChannel.class)
                .group(eventLoop)
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
                        pipeline.addLast("WriteToConsumer", new PATransmit2CA(ConnectManager.this));
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

    public Channel getAccordingConsumerChannel(long requestID) {
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
        List<Channel> PAchannels = PAchannelFromEventloop.get(consumerChannel.eventLoop());
        int index = (count / PAchannelFromEventloop.size()) %  PAchannels.size();
        return PAchannels.get(index);
    }


}
