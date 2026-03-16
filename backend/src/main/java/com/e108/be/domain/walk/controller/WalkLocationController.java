package com.e108.be.domain.walk.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GPS 좌표 관련 API
 * POST /walks/{walkId}/locations
 */
@RestController
@RequestMapping("/walks")
@RequiredArgsConstructor
public class WalkLocationController {

    // TODO: WalkLocationService 주입 후 API 구현
}
