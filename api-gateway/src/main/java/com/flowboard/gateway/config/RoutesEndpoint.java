package com.flowboard.gateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Endpoint(id = "gateway-routes")
@RequiredArgsConstructor
public class RoutesEndpoint {

    private final RouteLocator routeLocator;

    @ReadOperation
    public List<Map<String, Object>> routes() {
        return routeLocator.getRoutes()
                .map(route -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("id",        route.getId());
                    info.put("uri",       route.getUri().toString());
                    info.put("order",     route.getOrder());
                    info.put("predicate", route.getPredicate().toString());
                    return info;
                })
                .collectList()
                .block();
    }
}