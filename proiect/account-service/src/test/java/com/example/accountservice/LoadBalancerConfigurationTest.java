package com.example.accountservice;

import com.example.accountservice.client.UserFeignClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.RoundRobinLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.SimpleObjectProvider;
import org.springframework.cloud.openfeign.FeignClient;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LoadBalancerConfigurationTest {

    @Test
    @DisplayName("UserFeignClient uses service name 'user-service' and no hardcoded URL")
    void testFeignClientUsesServiceName() {
        FeignClient feignClient = UserFeignClient.class.getAnnotation(FeignClient.class);
        assertThat(feignClient).isNotNull();
        assertThat(feignClient.name()).isEqualTo("user-service");
        assertThat(feignClient.url()).isBlank();
    }

    @Test
    @DisplayName("Spring Cloud RoundRobinLoadBalancer distributes requests across two service instances")
    void testRoundRobinLoadBalancerDistributesBetweenTwoReplicas() {
        ServiceInstance inst1 = new DefaultServiceInstance(
                "user-service:8081", "user-service", "localhost", 8081, false);
        ServiceInstance inst2 = new DefaultServiceInstance(
                "user-service:8181", "user-service", "localhost", 8181, false);

        List<ServiceInstance> instances = List.of(inst1, inst2);
        ServiceInstanceListSupplier supplier = new ServiceInstanceListSupplier() {
            @Override
            public String getServiceId() {
                return "user-service";
            }

            @Override
            public Flux<List<ServiceInstance>> get() {
                return Flux.just(instances);
            }
        };
        var loadBalancer = new RoundRobinLoadBalancer(new SimpleObjectProvider<>(supplier), "user-service");

        var response1 = loadBalancer.choose().block();
        var response2 = loadBalancer.choose().block();
        var response3 = loadBalancer.choose().block();
        var response4 = loadBalancer.choose().block();

        assertThat(response1).isNotNull();
        assertThat(response2).isNotNull();
        assertThat(response3).isNotNull();
        assertThat(response4).isNotNull();

        int port1 = response1.getServer().getPort();
        int port2 = response2.getServer().getPort();
        int port3 = response3.getServer().getPort();
        int port4 = response4.getServer().getPort();

        assertThat(port1).isNotEqualTo(port2);
        assertThat(port1).isEqualTo(port3);
        assertThat(port2).isEqualTo(port4);
    }
}
