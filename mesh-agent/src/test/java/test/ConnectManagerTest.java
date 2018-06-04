package test;

import com.alibaba.dubbo.performance.demo.nettyagent.ConsumerAgentUtil.ConnectManager;
import com.alibaba.dubbo.performance.demo.nettyagent.registry.Endpoint;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by gexinjie on 2018/6/4.
 */
public class ConnectManagerTest {
    private List<Endpoint> endpoints = Collections.unmodifiableList(Arrays.asList(
            new Endpoint("provider-large",30000),
            new Endpoint("provider-medium",30000),
            new Endpoint("provider-small",30000)));

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void connectConstruct() throws Exception {
        EmbeddedChannel dummyChannel = new EmbeddedChannel();
        ConnectManager connectManager = new ConnectManager(dummyChannel.eventLoop(), endpoints);
    }

}