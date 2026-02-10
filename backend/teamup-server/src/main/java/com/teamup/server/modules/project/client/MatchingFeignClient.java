package com.teamup.server.modules.project.client;

import com.teamup.server.modules.project.dto.matching.MatchRequest;
import com.teamup.server.modules.project.dto.matching.MatchResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "matching-service", url = "${matching-service.url}")
public interface MatchingFeignClient {

    @PostMapping("/api/matching/calculate")
    List<MatchResult> calculateMatch(@RequestBody MatchRequest request);
}
