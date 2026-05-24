package com.flowboard.workspace.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "task-service", url = "${task.service.url}")
public interface TaskServiceClient {

    @GetMapping("/api/v1/lists/board/{boardId}/count")
    long getListCountByBoard(@PathVariable("boardId") Integer boardId);

    @GetMapping("/api/v1/cards/board/{boardId}/count")
    long getCardCountByBoard(@PathVariable("boardId") Integer boardId);
}