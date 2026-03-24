package com.e108.be.domain.diary.service;

import com.e108.be.domain.dog.entity.Dog;
import com.e108.be.domain.home.enums.SkyStatus;
import com.e108.be.domain.home.service.WeatherService;
import com.e108.be.domain.walk.entity.WalkRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class DiaryPromptBuilder {

    public static final String DEVELOPER_PROMPT = """
            당신은 어린 강아지예요. 오늘 산책을 다녀온 뒤 일기를 쓰려고 해요.
            어린아이가 쓴 것처럼 순수하고 귀엽게 써주세요.
            아래 규칙을 지켜주세요:
            - 한국어, 반말, 강아지가 직접 쓰는 1인칭 시점
            - 보호자를 "누나", "형아", "언니", "오빠" 같은 친근한 가족 호칭으로 불러요 (하나만 골라서 일관되게 사용)
            - "나", "나는" 대신 자기 이름을 쓰거나 "나" 를 자연스럽게 섞어요
            - 숫자(거리, 칼로리, 정확한 시간)는 절대 쓰지 마세요
            - 산책한 장소가 있으면 자연스럽게 언급해주세요 (예: "오늘 행복공원에 갔는데~", "누나랑 한강공원에서~")
            - 함께 산책한 강아지가 있으면 이름을 넣어서 "(이름)이랑 같이 뛰어다녔다!" 같이 써주세요
            - 아래 "상황 힌트"를 반드시 활용해서, 이 산책에서만 느낄 수 있는 구체적인 장면을 만들어주세요
            - 3~5문장, 100~180자
            - 제공된 데이터 기반으로만 쓰고, 없는 사건은 만들지 마세요
            - 마지막에 "멍멍!", "킁킁!", "꼬리 흔들흔들!" 같은 귀여운 마무리를 넣어주세요
            - 마크다운 서식 없이 순수 텍스트로만 작성
            """;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy년 M월 d일");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("a h시 mm분");

    /**
     * 하위 호환: 감정 분석 없이도 호출 가능
     */
    public String buildUserPrompt(Dog dog, WalkRecord walk, WeatherService.WeatherData weather, List<String> nearbyPlaceNames) {
        return buildUserPrompt(dog, walk, weather, nearbyPlaceNames, Map.of());
    }

    public String buildUserPrompt(Dog dog, WalkRecord walk, WeatherService.WeatherData weather,
                                  List<String> nearbyPlaceNames, Map<String, EmotionResult> photoResults) {
        StringBuilder sb = new StringBuilder();
        sb.append("아래 산책 정보를 바탕으로 일기를 작성해주세요:\n\n");

        // 기본 정보
        String name = dog.getName();
        boolean hasBatchim = hasFinalConsonant(name);
        sb.append("- 강아지 이름: ").append(name);
        sb.append(" (자기 이름을 부를 때: ").append(hasBatchim ? name + "이는" : name + "는").append(")");
        if (dog.getBreed() != null) {
            sb.append(", 견종: ").append(dog.getBreed());
        }
        sb.append("\n");

        if (walk.getStartTime() != null) {
            sb.append("- 산책 날짜: ").append(walk.getStartTime().format(DATE_FMT)).append("\n");
            sb.append("- 산책 시작: ").append(walk.getStartTime().format(TIME_FMT)).append("\n");
        }

        if (walk.getTotalDuration() != null) {
            sb.append("- 산책 시간: ").append(walk.getTotalDuration() / 60).append("분\n");
        }

        if (walk.getTotalDistance() != null) {
            double distanceKm = walk.getTotalDistance().doubleValue() / 1000.0;
            sb.append("- 이동 거리: ").append(String.format("%.1f", distanceKm)).append("km\n");
        }

        if (walk.getCalories() != null) {
            sb.append("- 소모 칼로리: ").append(walk.getCalories()).append("kcal\n");
        }

        if (walk.getPhotoUrls() != null && !walk.getPhotoUrls().isEmpty()) {
            sb.append("- 사진 촬영: ").append(walk.getPhotoUrls().size()).append("장\n");
        }

        if (weather != null) {
            SkyStatus sky = SkyStatus.from(weather.skyCode(), weather.ptyCode());
            sb.append("- 날씨: ").append(sky.getLabel())
                    .append(", 기온 ").append(weather.temperature()).append("°C")
                    .append(", 체감 ").append(weather.feelsLike()).append("°C\n");
        }

        if (nearbyPlaceNames != null && !nearbyPlaceNames.isEmpty()) {
            sb.append("- 산책 장소: ").append(String.join(", ", nearbyPlaceNames)).append("\n");
        }

        // 강아지 감정 분석 결과 (사진별)
        if (photoResults != null && !photoResults.isEmpty()) {
            sb.append("- 사진 속 강아지 표정 분석:\n");
            int idx = 1;
            for (Map.Entry<String, EmotionResult> entry : photoResults.entrySet()) {
                EmotionResult r = entry.getValue();
                sb.append("  - 사진").append(idx++).append(": ")
                  .append(r.emotionTag())
                  .append(" (").append(r.emotion()).append(", ")
                  .append(String.format("신뢰도 %.0f%%", r.confidence() * 100)).append(")\n");
            }
        }

        // 상황 힌트 생성 (대표 감정 사용)
        EmotionResult bestEmotion = photoResults != null ? photoResults.values().stream()
                .max((a, b) -> Float.compare(a.confidence(), b.confidence()))
                .orElse(null) : null;
        List<String> hints = buildSituationHints(walk, weather, bestEmotion);
        if (!hints.isEmpty()) {
            sb.append("\n[상황 힌트 - 아래 내용을 활용해서 구체적인 장면을 만들어주세요]\n");
            for (String hint : hints) {
                sb.append("- ").append(hint).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * 산책 데이터 조합으로 그 산책에서만 느낄 수 있는 구체적 상황 힌트 생성
     */
    private List<String> buildSituationHints(WalkRecord walk, WeatherService.WeatherData weather, EmotionResult emotionResult) {
        List<String> hints = new ArrayList<>();

        // 1. 시간대별 감각
        if (walk.getStartTime() != null) {
            int hour = walk.getStartTime().getHour();
            if (hour >= 5 && hour < 9) {
                hints.add("이른 아침 산책: 풀잎에 이슬이 맺혀있고, 공기가 시원하고 상쾌해요. 아직 조용해서 새소리가 잘 들려요");
            } else if (hour >= 9 && hour < 12) {
                hints.add("오전 산책: 햇살이 점점 따뜻해지고, 동네 사람들이 하나둘 나와요");
            } else if (hour >= 12 && hour < 15) {
                hints.add("한낮 산책: 햇볕이 뜨겁고 그림자가 짧아요. 그늘을 찾아다녀요");
            } else if (hour >= 15 && hour < 18) {
                hints.add("오후 산책: 햇살이 부드러워지고, 바람이 살랑살랑 불어요");
            } else if (hour >= 18 && hour < 21) {
                hints.add("저녁 산책: 노을이 예쁘고, 가로등이 하나둘 켜져요. 저녁밥 냄새가 솔솔 나요");
            } else {
                hints.add("밤 산책: 별이 반짝반짝, 가로등 불빛 아래서 걸어요. 밤공기가 시원해요");
            }
        }

        // 2. 계절별 감각
        if (walk.getStartTime() != null) {
            int month = walk.getStartTime().getMonthValue();
            if (month >= 3 && month <= 5) {
                hints.add("봄 산책: 꽃냄새가 킁킁 나고, 나비가 팔랑팔랑 날아다녀요. 풀이 새로 올라왔어요");
            } else if (month >= 6 && month <= 8) {
                hints.add("여름 산책: 혀를 내밀고 헥헥거려요. 매미가 맴맴 울고, 그늘이 시원해요");
            } else if (month >= 9 && month <= 11) {
                hints.add("가을 산책: 낙엽이 바스락바스락 밟혀요. 바람이 시원하고 하늘이 높아요");
            } else {
                hints.add("겨울 산책: 입에서 하얀 김이 뿜뿜 나와요. 발이 시려서 깡충깡충 뛰어요");
            }
        }

        // 3. 날씨 특수 상황
        if (weather != null) {
            SkyStatus sky = SkyStatus.from(weather.skyCode(), weather.ptyCode());
            String label = sky.getLabel();

            if (label.contains("비")) {
                hints.add("비 오는 날: 발이 축축해지고, 웅덩이에 첨벙! 빗방울이 코에 톡톡 떨어져요");
            } else if (label.contains("눈")) {
                hints.add("눈 오는 날: 하얀 눈 위에 발자국이 콕콕 찍혀요. 눈을 킁킁 맡아봤어요");
            }

            double temp = weather.temperature();
            if (temp >= 30) {
                hints.add("아주 더운 날: 혀가 쭉 나오고 물이 너무 마셔요. 그늘에서 쉬고 싶어요");
            } else if (temp <= 0) {
                hints.add("아주 추운 날: 몸이 부르르 떨려요. 빨리 집에 가서 이불 속에 들어가고 싶어요");
            } else if (temp >= 20 && temp <= 25) {
                hints.add("딱 좋은 날씨: 산책하기 너무 좋아서 기분이 최고예요!");
            }
        }

        // 4. 산책 길이에 따른 감정
        if (walk.getTotalDuration() != null) {
            int minutes = walk.getTotalDuration() / 60;
            if (minutes <= 15) {
                hints.add("짧은 산책: 금방 끝나서 아쉬웠어요. 더 걷고 싶었는데~");
            } else if (minutes >= 60) {
                hints.add("긴 산책: 다리가 후들후들, 발바닥이 뜨끈뜨끈해요. 근데 너무 행복했어요!");
            }
        }

        // 5. 사진 찍힌 경우
        if (walk.getPhotoUrls() != null && !walk.getPhotoUrls().isEmpty()) {
            int count = walk.getPhotoUrls().size();
            if (count == 1) {
                hints.add("사진 찍힘: 가만히 앉아서 포즈! 잘 나왔을까?");
            } else {
                hints.add("사진 여러 장 찍힘: 자꾸 찍어서 가만히 있느라 힘들었지만, 나중에 보면 귀여울 거야!");
            }
        }

        // 6. 강아지 감정 분석 결과 기반 힌트
        if (emotionResult != null) {
            switch (emotionResult.emotion()) {
                case "happy" -> hints.add("사진 속 표정이 활짝 웃고 있어요! 꼬리를 신나게 흔들며 세상에서 제일 행복한 표정이에요");
                case "relaxed" -> hints.add("사진 속 표정이 편안해요. 눈이 살짝 감기고 입꼬리가 올라가서 여유로운 모습이에요");
                case "sad" -> hints.add("사진 속 표정이 조금 축 처져 있어요. 눈이 축축하고 귀가 살짝 내려가 있어요");
                case "angry" -> hints.add("사진 속 표정이 긴장되어 있어요. 뭔가 경계하는 듯한 눈빛이에요");
            }
        }

        return hints;
    }

    /**
     * 한글 이름의 마지막 글자에 받침(종성)이 있는지 판별
     */
    private boolean hasFinalConsonant(String name) {
        if (name == null || name.isEmpty()) return false;
        char last = name.charAt(name.length() - 1);
        if (last < 0xAC00 || last > 0xD7A3) return false;
        return (last - 0xAC00) % 28 != 0;
    }
}
