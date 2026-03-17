package com.e108.be.domain.home.service;

import com.e108.be.domain.auth.entity.Member;
import com.e108.be.domain.auth.repository.MemberRepository;
import com.e108.be.domain.dog.entity.Dog;
import com.e108.be.domain.dog.repository.DogRepository;
import com.e108.be.domain.home.dto.response.*;
import com.e108.be.domain.home.enums.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 홈 화면 통합 서비스
 *
 * - 날씨/미세먼지 외부 API 호출을 조합
 * - 등급 판정 + 메시지 생성
 * - 사용자/반려견 정보는 auth 완성 전까지 하드코딩
 * - 주간 통계는 산책 기록 완성 전까지 null
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class HomeService {

    private final WeatherService weatherService;
    private final AirQualityService airQualityService;
    private final MemberRepository memberRepository;
    private final DogRepository dogRepository;

    public HomeResponse getHomeData(Long memberId, double latitude, double longitude) {
        // 1. 외부 API 호출 (실패 시 기본값 반환)
        WeatherService.WeatherData weather = fetchWeatherSafely(latitude, longitude);
        AirQualityService.AirQualityData airQuality = fetchAirQualitySafely(latitude, longitude);

        // 2. 등급 판정
        SkyStatus skyStatus = SkyStatus.from(weather.skyCode(), weather.ptyCode());
        TemperatureGrade tempGrade = TemperatureGrade.from(weather.temperature());
        FineDustGrade dustGrade = FineDustGrade.from(airQuality.pm10Value());
        WindGrade windGrade = WindGrade.from(weather.windSpeed());
        WalkStatus walkStatus = WalkStatus.calculate(tempGrade, dustGrade, windGrade, skyStatus);
        Member member = memberRepository.findById(memberId).orElse(null);
        Dog dog = dogRepository.findFirstByUserId(memberId).orElse(null);
        String dogName = dog != null ? dog.getName() : "";

        // 3. 응답 조합
        return HomeResponse.builder()
                .user(buildUserInfo(member, dog))
                .location(buildLocationInfo(latitude, longitude))
                .weather(buildWeatherInfo(weather, airQuality, skyStatus, tempGrade, dustGrade, windGrade))
                .walk(buildWalkInfo(walkStatus, skyStatus, dogName))
                .weeklySummary(null) // TODO: 산책 기록 구현 후 연동
                .build();
    }

    private HomeUserInfo buildUserInfo(Member member, Dog dog) {
        return HomeUserInfo.builder()
                .nickname(member != null ? member.getNickname() : "")
                .dogName(dog != null ? dog.getName() : "")
                .dogProfileImageUrl(dog != null ? dog.getProfileImageUrl() : null)
                .build();
    }

    /**
     * 위치 정보
     * TODO: 역지오코딩 API 연동 후 실제 주소 반환
     */
    private HomeLocationInfo buildLocationInfo(double latitude, double longitude) {
        String address = getApproximateAddress(latitude, longitude);
        return HomeLocationInfo.builder()
                .address(address)
                .build();
    }

    private HomeWeatherInfo buildWeatherInfo(WeatherService.WeatherData weather,
                                             AirQualityService.AirQualityData airQuality,
                                             SkyStatus skyStatus,
                                             TemperatureGrade tempGrade,
                                             FineDustGrade dustGrade,
                                             WindGrade windGrade) {
        return HomeWeatherInfo.builder()
                .weatherCode(skyStatus.name())
                .weatherLabel(skyStatus.getLabel())
                .temperature(weather.temperature())
                .temperatureGrade(tempGrade.name())
                .temperatureLabel(tempGrade.getLabel())
                .feelsLike(weather.feelsLike())
                .fineDustValue(airQuality.pm10Value())
                .fineDustGrade(dustGrade.name())
                .fineDustLabel(dustGrade.getLabel())
                .windSpeed(weather.windSpeed())
                .windGrade(windGrade.name())
                .windLabel(windGrade.getLabel())
                .build();
    }

    private HomeWalkInfo buildWalkInfo(WalkStatus walkStatus, SkyStatus skyStatus, String dogName) {
        String message = generateWalkMessage(walkStatus, dogName);
        String characterType = generateCharacterType(skyStatus, walkStatus);

        return HomeWalkInfo.builder()
                .walkStatus(walkStatus.name())
                .walkMessage(message)
                .characterType(characterType)
                .build();
    }

    private String generateWalkMessage(WalkStatus walkStatus, String dogName) {
        String nameWith = withParticle(dogName, "와", "이와");
        return switch (walkStatus) {
            case GREAT -> "오늘 날씨가 좋아요. " + nameWith + " 함께 즐거운 산책 가볼까요?";
            case GOOD -> "산책하기 무난한 날이에요. " + nameWith + " 가볍게 다녀오기 좋아요.";
            case CAUTION -> "오늘은 환경이 조금 아쉬워요. " + nameWith + " 짧은 산책을 추천해요.";
            case BAD -> "오늘은 산책을 쉬거나 " + nameWith + " 실내 놀이를 추천해요.";
        };
    }

    /**
     * 한국어 조사 처리 (받침 유무에 따라 조사 선택)
     * 예: 와/이와, 가/이가, 을/를
     */
    private String withParticle(String name, String noFinal, String hasFinal) {
        if (name == null || name.isEmpty()) return name + noFinal;
        char lastChar = name.charAt(name.length() - 1);
        if (lastChar >= 0xAC00 && lastChar <= 0xD7A3) {
            int batchim = (lastChar - 0xAC00) % 28;
            return name + (batchim == 0 ? noFinal : hasFinal);
        }
        return name + noFinal;
    }

    /**
     * 캐릭터 타입 코드 생성
     * 프론트가 이 코드로 캐릭터 이미지 매핑
     * 형식: {날씨}_{적합도} 예: SUNNY_GREAT, RAINY_CAUTION
     */
    private String generateCharacterType(SkyStatus skyStatus, WalkStatus walkStatus) {
        return skyStatus.name() + "_" + walkStatus.name();
    }

    /**
     * 기상청 API 호출 (실패 시 기본값 반환)
     * TODO: 기상청 API 키 활성화 후 fallback 제거
     */
    private WeatherService.WeatherData fetchWeatherSafely(double latitude, double longitude) {
        try {
            return weatherService.getWeather(latitude, longitude);
        } catch (Exception e) {
            log.warn("기상청 API 호출 실패, 기본값 사용: {}", e.getMessage());
            return new WeatherService.WeatherData(18, 2.5, 55, "1", "0", 18);
        }
    }

    /**
     * 에어코리아 API 호출 (실패 시 기본값 반환)
     * TODO: 안정화 후 fallback 제거 검토
     */
    private AirQualityService.AirQualityData fetchAirQualitySafely(double latitude, double longitude) {
        try {
            return airQualityService.getAirQuality(latitude, longitude);
        } catch (Exception e) {
            log.warn("에어코리아 API 호출 실패, 기본값 사용: {}", e.getMessage());
            return new AirQualityService.AirQualityData(25, "측정소 정보 없음");
        }
    }

    /**
     * 위경도 → 대략적 주소 (시도 수준)
     * TODO: Kakao Local API 등으로 정확한 역지오코딩 구현
     */
    private String getApproximateAddress(double lat, double lng) {
        // 주요 도시 중심 좌표 기반 매핑
        if (lat >= 35.0 && lat <= 35.3 && lng >= 128.8 && lng <= 129.3) return "부산광역시";
        if (lat >= 37.4 && lat <= 37.7 && lng >= 126.8 && lng <= 127.2) return "서울특별시";
        if (lat >= 35.7 && lat <= 36.0 && lng >= 128.4 && lng <= 128.8) return "대구광역시";
        if (lat >= 37.3 && lat <= 37.6 && lng >= 126.5 && lng <= 126.8) return "인천광역시";
        if (lat >= 35.0 && lat <= 35.3 && lng >= 126.7 && lng <= 127.0) return "광주광역시";
        if (lat >= 36.2 && lat <= 36.5 && lng >= 127.2 && lng <= 127.6) return "대전광역시";
        if (lat >= 35.4 && lat <= 35.7 && lng >= 129.0 && lng <= 129.5) return "울산광역시";
        if (lat >= 33.0 && lat <= 34.0 && lng >= 126.0 && lng <= 127.0) return "제주특별자치도";
        return "현재 위치";
    }
}
