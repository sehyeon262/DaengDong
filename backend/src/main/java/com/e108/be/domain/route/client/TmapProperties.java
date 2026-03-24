package com.e108.be.domain.route.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * TMAP API 설정
 */
@Component
@ConfigurationProperties(prefix = "api.tmap")
@Getter
@Setter
public class TmapProperties {

    /**
     * TMAP API 앱 키
     */
    private String appKey;

    /**
     * TMAP 도보 길찾기 API base URL
     */
    private String baseUrl = "https://apis.openapi.sk.com/tmap/routes/pedestrian";

    /**
     * 연결 타임아웃 (초)
     */
    private int connectTimeout = 5;

    /**
     * 읽기 타임아웃 (초)
     */
    private int readTimeout = 30;

    /**
     * TMAP API 사용 여부 (false면 직선 경로 사용)
     */
    private boolean enabled = true;
}
