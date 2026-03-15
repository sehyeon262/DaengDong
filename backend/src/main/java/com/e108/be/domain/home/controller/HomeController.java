package com.e108.be.domain.home.controller;

import com.e108.be.domain.home.dto.response.HomeResponse;
import com.e108.be.domain.home.service.HomeService;
import com.e108.be.global.common.template.ResTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 홈 화면 API
 *
 * GET /api/v1/home?latitude=35.1796&longitude=129.0756
 *
 * 홈 화면에 필요한 사용자 정보, 반려견 정보, 날씨, 미세먼지,
 * 산책 적합도, 주간 통계를 통합 조회한다.
 */
@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping
    public ResTemplate<HomeResponse> getHome(
            @RequestParam double latitude,
            @RequestParam double longitude) {

        HomeResponse response = homeService.getHomeData(latitude, longitude);
        return ResTemplate.success(HttpStatus.OK, "홈 화면 조회 성공", response);
    }
}
