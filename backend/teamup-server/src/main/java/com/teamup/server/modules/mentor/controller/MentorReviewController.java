package com.teamup.server.modules.mentor.controller;

import com.teamup.server.common.api.Result;
import com.teamup.server.modules.mentor.dto.MentorReviewDTO;
import com.teamup.server.modules.mentor.entity.MentorReview;
import com.teamup.server.modules.mentor.service.MentorReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学员评价导师控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/mentor/reviews")
@RequiredArgsConstructor
public class MentorReviewController {
    
    private final MentorReviewService reviewService;
    
    /**
     * 提交评价
     */
    @PostMapping
    public Result<Long> submitReview(@RequestBody MentorReviewDTO dto) {
        try {
            Long reviewId = reviewService.submitReview(dto);
            return Result.success(reviewId);
        } catch (Exception e) {
            log.error("提交评价失败", e);
            return Result.error(500, e.getMessage());
        }
    }
    
    /**
     * 获取导师的所有评价
     */
    @GetMapping("/mentor/{mentorId}")
    public Result<List<MentorReview>> getMentorReviews(@PathVariable Long mentorId) {
        try {
            List<MentorReview> reviews = reviewService.getMentorReviews(mentorId);
            return Result.success(reviews);
        } catch (Exception e) {
            log.error("获取导师评价失败", e);
            return Result.error(500, e.getMessage());
        }
    }
    
    /**
     * 获取导师的评分统计
     */
    @GetMapping("/mentor/{mentorId}/stats")
    public Result<Map<String, Object>> getMentorReviewStats(@PathVariable Long mentorId) {
        try {
            BigDecimal avgRating = reviewService.getMentorAverageRating(mentorId);
            Integer reviewCount = reviewService.getMentorReviewCount(mentorId);
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("averageRating", avgRating);
            stats.put("reviewCount", reviewCount);
            
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取导师评分统计失败", e);
            return Result.error(500, e.getMessage());
        }
    }
}
