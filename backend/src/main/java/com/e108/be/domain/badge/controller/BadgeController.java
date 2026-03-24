package com.e108.be.domain.badge.controller;

import com.e108.be.domain.badge.dto.response.BadgeProgressResponse;
import com.e108.be.domain.badge.dto.response.BadgeResponse;
import com.e108.be.domain.badge.dto.response.UserBadgeResponse;
import com.e108.be.domain.badge.service.BadgeService;
import com.e108.be.global.common.template.ResTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/badges")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeService badgeService;

    @GetMapping
    public ResTemplate<List<BadgeResponse>> getAllBadges() {
        List<BadgeResponse> response = badgeService.getAllBadges();
        return ResTemplate.success(HttpStatus.OK, "배지 목록 조회 성공", response);
    }

    @GetMapping("/my")
    public ResTemplate<List<UserBadgeResponse>> getMyBadges(
            @AuthenticationPrincipal Long memberId) {
        List<UserBadgeResponse> response = badgeService.getMyBadges(memberId);
        return ResTemplate.success(HttpStatus.OK, "내 배지 조회 성공", response);
    }

    @GetMapping("/progress")
    public ResTemplate<List<BadgeProgressResponse>> getBadgeProgress(
            @AuthenticationPrincipal Long memberId) {
        List<BadgeProgressResponse> response = badgeService.getBadgeProgress(memberId);
        return ResTemplate.success(HttpStatus.OK, "배지 진행도 조회 성공", response);
    }
}
