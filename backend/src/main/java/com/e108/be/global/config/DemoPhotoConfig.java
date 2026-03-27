package com.e108.be.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 시연(데모) 모드용 사진 설정.
 * application.yml에서 demo.photo.* 프로퍼티로 설정 가능.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "demo")
public class DemoPhotoConfig {

    /**
     * 데모 모드 활성화 여부
     */
    private boolean enabled = false;

    /**
     * 산책 종료 시 자동 주입할 사진 URL 목록
     */
    private List<String> photoUrls = new ArrayList<>();

    /**
     * 감정 분석 오버라이드 (파일명 -> 감정키)
     * 예: {"dog1.jpg": "happy", "dog2.jpg": "relaxed"}
     */
    private Map<String, String> emotionOverrides = new HashMap<>();
}
