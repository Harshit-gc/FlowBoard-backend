package com.flowboard.collaboration.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CardClient {

    private final RestTemplate restTemplate;

    @Value("${app.services.task-service:http://localhost:8083}")
    private String taskServiceUrl;

    public Map<String, Object> getCardInfo(Integer cardId) {
        try {
            return restTemplate.getForObject(
                    taskServiceUrl + "/api/v1/cards/" + cardId + "/owner",
                    Map.class
            );
        } catch (Exception e) {
            log.warn("Could not fetch card info for cardId={}: {}",
                    cardId, e.getMessage());
            return null;
        }
    }
}