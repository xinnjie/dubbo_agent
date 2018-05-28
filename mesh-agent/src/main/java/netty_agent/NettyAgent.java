package netty_agent;

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
    private IRegistry registry = new EtcdRegistry(System.getProperty("etcd.url"));
    private List<Endpoint> endpoints = Collections.unmodifiableList(Arrays.asList(
            new Endpoint("provider-large",30000),
            new Endpoint("provider-medium",30000),
            new Endpoint("provider-small",30000)));

    private List<Endpoint> weightedEndpoints = null;
    private Random random = new Random();

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
