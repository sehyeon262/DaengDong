-- =============================================
-- 시연용 페르소나 더미데이터
--
-- 시연 위치: lat=35.0942, lon=128.8541 (김해시 내외동 일대)
-- 같은 위치에서 경로 추천 API를 호출하면
-- 개인화 알고리즘에 의해 서로 다른 경로가 추천되는 것을 시연
--
-- 페르소나 A (user_id=1, 김지훈): "카페 탐방러" - 소형견, 카페/식당 선호
-- 페르소나 B (user_id=3, 박민재): "자연 탐험가" - 중형견, 여행지/공원 선호
--
-- 실행 전 users.sql, init_places.sql이 먼저 적용되어 있어야 합니다.
--
-- 시연 API 호출 예시:
--   GET /api/v1/routes/recommend?lat=35.0942&lon=128.8541
--   Authorization: Bearer {김지훈 JWT}  → 카페/식당 위주 경로
--   Authorization: Bearer {박민재 JWT}  → 여행지/공원 위주 경로
-- =============================================

-- =============================================
-- 1. breed_distance_config (체중별 적정 거리)
-- =============================================
INSERT INTO breed_distance_config (id, size_category, min_weight_kg, max_weight_kg, recommended_distance_m, max_distance_m)
VALUES
    (1, 'SMALL',  0.00,  10.00, 500,  1000),
    (2, 'MEDIUM', 10.00, 25.00, 1000, 2000),
    (3, 'LARGE',  25.00, 100.00, 1500, 3000)
ON CONFLICT (id) DO NOTHING;

-- =============================================
-- 2. scoring_weights (ML 학습 가중치)
--    시연용: 카테고리 가중치를 높여 개인화 효과 극대화
-- =============================================
INSERT INTO scoring_weights (feature_name, weight, sample_count, created_at, updated_at)
VALUES
    ('DISTANCE', 0.40, 100, NOW(), NOW()),
    ('CATEGORY', 0.60, 100, NOW(), NOW())
ON CONFLICT (feature_name) DO UPDATE
SET weight = EXCLUDED.weight, sample_count = EXCLUDED.sample_count, updated_at = NOW();

-- =============================================
-- 3. 페르소나 A: 김지훈 (user_id=1)
--    소형견 오월 (dog_id=1, 9.5kg -> SMALL)
--    선호: 카페(6) > 식당(7) > 반려동물용품(4)
--    패턴: 저녁 시간대, SHORT 선호, 맑은 날 산책
-- =============================================

-- 카테고리 선호도 (선택 횟수 + 정규화 점수)
INSERT INTO user_category_preferences (user_id, category_id, selection_count, preference_score, created_at, updated_at)
VALUES
    (1, 6,  15, 1.00, NOW(), NOW()),   -- 카페: 15회 -> 1.0 (최고 선호)
    (1, 7,  10, 0.67, NOW(), NOW()),   -- 식당: 10회 -> 0.67
    (1, 4,   6, 0.40, NOW(), NOW()),   -- 반려동물용품: 6회 -> 0.40
    (1, 3,   3, 0.20, NOW(), NOW()),   -- 미용: 3회 -> 0.20
    (1, 11,  1, 0.07, NOW(), NOW())    -- 여행지: 1회 -> 0.07
ON CONFLICT (user_id, category_id) DO UPDATE
SET selection_count = EXCLUDED.selection_count,
    preference_score = EXCLUDED.preference_score,
    updated_at = NOW();

-- 경로 선택 로그 (최근 30일)
INSERT INTO route_selection_logs (user_id, dog_id, selected_type, selected_distance_m, hour_of_day, day_of_week, weather_condition, temperature, created_at, updated_at)
VALUES
    (1, 1, 'SHORT',       400,  18, 6, 'CLEAR',  22.0, NOW() - INTERVAL '1 day',   NOW()),
    (1, 1, 'RECOMMENDED', 800,  19, 7, 'CLEAR',  21.0, NOW() - INTERVAL '2 days',  NOW()),
    (1, 1, 'SHORT',       350,  17, 1, 'CLOUDY', 18.0, NOW() - INTERVAL '3 days',  NOW()),
    (1, 1, 'RECOMMENDED', 750,  18, 2, 'CLEAR',  23.0, NOW() - INTERVAL '5 days',  NOW()),
    (1, 1, 'SHORT',       450,  20, 3, 'RAIN',   15.0, NOW() - INTERVAL '7 days',  NOW()),
    (1, 1, 'RECOMMENDED', 900,  19, 5, 'CLEAR',  20.0, NOW() - INTERVAL '9 days',  NOW()),
    (1, 1, 'SHORT',       380,  18, 6, 'CLOUDY', 19.0, NOW() - INTERVAL '11 days', NOW()),
    (1, 1, 'RECOMMENDED', 820,  17, 7, 'CLEAR',  24.0, NOW() - INTERVAL '14 days', NOW());

-- 선택 장소: 시연 좌표(35.0942, 128.8541) 반경 3km 이내 카페/식당
INSERT INTO route_selection_places (selection_log_id, place_id, visit_order)
SELECT sl.id, sub.place_id, sub.rn
FROM route_selection_logs sl
JOIN LATERAL (
    SELECT p.id AS place_id,
           ROW_NUMBER() OVER (ORDER BY RANDOM()) AS rn
    FROM places p
    WHERE p.category_id IN (6, 7)
      AND p.is_active = true
      AND ST_DWithin(
          p.location,
          ST_SetSRID(ST_MakePoint(128.8541, 35.0942), 4326)::geography,
          3000
      )
    LIMIT 3
) sub ON true
WHERE sl.user_id = 1;

-- =============================================
-- 4. 페르소나 B: 박민재 (user_id=3)
--    중형견 까미 (dog_id=3, 10.1kg -> MEDIUM)
--    선호: 여행지(11) > 박물관(8) > 미술관(9)
--    패턴: 오전 시간대, EXPLORE 선호, 맑은 날 탐험
-- =============================================

-- 카테고리 선호도
INSERT INTO user_category_preferences (user_id, category_id, selection_count, preference_score, created_at, updated_at)
VALUES
    (3, 11, 14, 1.00, NOW(), NOW()),   -- 여행지: 14회 -> 1.0 (최고 선호)
    (3, 8,   9, 0.64, NOW(), NOW()),   -- 박물관: 9회 -> 0.64
    (3, 9,   7, 0.50, NOW(), NOW()),   -- 미술관: 7회 -> 0.50
    (3, 10,  4, 0.29, NOW(), NOW()),   -- 문예회관: 4회 -> 0.29
    (3, 6,   2, 0.14, NOW(), NOW())    -- 카페: 2회 -> 0.14
ON CONFLICT (user_id, category_id) DO UPDATE
SET selection_count = EXCLUDED.selection_count,
    preference_score = EXCLUDED.preference_score,
    updated_at = NOW();

-- 경로 선택 로그 (탐험형 - EXPLORE 선호, 긴 거리)
INSERT INTO route_selection_logs (user_id, dog_id, selected_type, selected_distance_m, hour_of_day, day_of_week, weather_condition, temperature, created_at, updated_at)
VALUES
    (3, 3, 'EXPLORE',     1500, 10, 6, 'CLEAR',  25.0, NOW() - INTERVAL '1 day',   NOW()),
    (3, 3, 'EXPLORE',     1800, 9,  7, 'CLEAR',  23.0, NOW() - INTERVAL '2 days',  NOW()),
    (3, 3, 'RECOMMENDED', 1200, 11, 1, 'CLOUDY', 20.0, NOW() - INTERVAL '4 days',  NOW()),
    (3, 3, 'EXPLORE',     2000, 10, 3, 'CLEAR',  22.0, NOW() - INTERVAL '6 days',  NOW()),
    (3, 3, 'EXPLORE',     1700, 8,  5, 'CLEAR',  24.0, NOW() - INTERVAL '8 days',  NOW()),
    (3, 3, 'RECOMMENDED', 1300, 11, 6, 'CLOUDY', 19.0, NOW() - INTERVAL '10 days', NOW()),
    (3, 3, 'EXPLORE',     1900, 9,  7, 'CLEAR',  21.0, NOW() - INTERVAL '13 days', NOW()),
    (3, 3, 'EXPLORE',     2100, 10, 2, 'CLEAR',  26.0, NOW() - INTERVAL '15 days', NOW());

-- 선택 장소: 시연 좌표 반경 5km 이내 여행지/박물관/미술관
-- EXPLORE 유저이므로 넓은 범위(5km)에서 탐색
INSERT INTO route_selection_places (selection_log_id, place_id, visit_order)
SELECT sl.id, sub.place_id, sub.rn
FROM route_selection_logs sl
JOIN LATERAL (
    SELECT p.id AS place_id,
           ROW_NUMBER() OVER (ORDER BY RANDOM()) AS rn
    FROM places p
    WHERE p.category_id IN (11, 8, 9)
      AND p.is_active = true
      AND ST_DWithin(
          p.location,
          ST_SetSRID(ST_MakePoint(128.8541, 35.0942), 4326)::geography,
          5000
      )
    LIMIT 3
) sub ON true
WHERE sl.user_id = 3;

-- =============================================
-- 5. 세그먼트 선호도 (콜드스타트 CF 데이터)
--    새 사용자가 가입해도 체중 그룹 기반 추천 가능
-- =============================================
INSERT INTO segment_preferences (weight_group, category_id, preference_score, sample_count, created_at, updated_at)
VALUES
    -- SMALL (소형견 그룹) - 실내/가까운 장소 선호
    ('SMALL', 6,  0.85, 30, NOW(), NOW()),   -- 카페
    ('SMALL', 7,  0.70, 30, NOW(), NOW()),   -- 식당
    ('SMALL', 4,  0.45, 30, NOW(), NOW()),   -- 반려동물용품
    ('SMALL', 11, 0.30, 30, NOW(), NOW()),   -- 여행지
    ('SMALL', 3,  0.25, 30, NOW(), NOW()),   -- 미용

    -- MEDIUM (중형견 그룹) - 야외/문화 활동 선호
    ('MEDIUM', 11, 0.90, 25, NOW(), NOW()),  -- 여행지
    ('MEDIUM', 8,  0.65, 25, NOW(), NOW()),  -- 박물관
    ('MEDIUM', 9,  0.55, 25, NOW(), NOW()),  -- 미술관
    ('MEDIUM', 6,  0.40, 25, NOW(), NOW()),  -- 카페
    ('MEDIUM', 10, 0.35, 25, NOW(), NOW()),  -- 문예회관

    -- LARGE (대형견 그룹) - 넓은 공간/야외 선호
    ('LARGE', 11, 0.95, 15, NOW(), NOW()),   -- 여행지
    ('LARGE', 12, 0.50, 15, NOW(), NOW()),   -- 펜션
    ('LARGE', 13, 0.45, 15, NOW(), NOW()),   -- 호텔
    ('LARGE', 6,  0.30, 15, NOW(), NOW()),   -- 카페
    ('LARGE', 8,  0.25, 15, NOW(), NOW())    -- 박물관
ON CONFLICT (weight_group, category_id) DO UPDATE
SET preference_score = EXCLUDED.preference_score,
    sample_count = EXCLUDED.sample_count,
    updated_at = NOW();

-- =============================================
-- 6. 산책 기록 (완주율/이탈률 지표 + 패턴 분석용)
-- =============================================
INSERT INTO walk_records (dog_id, walk_status, start_time, end_time, total_distance, total_duration, calories, route_type, created_at, updated_at)
VALUES
    -- 페르소나 A (dog_id=1): SHORT 위주, 저녁 산책
    (1, 'COMPLETED', NOW() - INTERVAL '2 days'  + INTERVAL '18 hours', NOW() - INTERVAL '2 days'  + INTERVAL '18 hours 20 minutes', 380.00, 1200, 2.9, 'SHORT',       NOW(), NOW()),
    (1, 'COMPLETED', NOW() - INTERVAL '5 days'  + INTERVAL '19 hours', NOW() - INTERVAL '5 days'  + INTERVAL '19 hours 25 minutes', 750.00, 1500, 5.7, 'RECOMMENDED', NOW(), NOW()),
    (1, 'COMPLETED', NOW() - INTERVAL '9 days'  + INTERVAL '17 hours', NOW() - INTERVAL '9 days'  + INTERVAL '17 hours 15 minutes', 420.00, 900,  3.2, 'SHORT',       NOW(), NOW()),
    (1, 'COMPLETED', NOW() - INTERVAL '14 days' + INTERVAL '18 hours', NOW() - INTERVAL '14 days' + INTERVAL '18 hours 30 minutes', 850.00, 1800, 6.5, 'RECOMMENDED', NOW(), NOW()),
    (1, 'COMPLETED', NOW() - INTERVAL '20 days' + INTERVAL '20 hours', NOW() - INTERVAL '20 days' + INTERVAL '20 hours 10 minutes', 350.00, 600,  2.7, 'SHORT',       NOW(), NOW()),

    -- 페르소나 B (dog_id=3): EXPLORE 위주, 오전 산책
    (3, 'COMPLETED', NOW() - INTERVAL '1 day'   + INTERVAL '10 hours', NOW() - INTERVAL '1 day'   + INTERVAL '10 hours 50 minutes', 1800.00, 3000, 14.5, 'EXPLORE',     NOW(), NOW()),
    (3, 'COMPLETED', NOW() - INTERVAL '4 days'  + INTERVAL '9 hours',  NOW() - INTERVAL '4 days'  + INTERVAL '9 hours 45 minutes',  1500.00, 2700, 12.1, 'EXPLORE',     NOW(), NOW()),
    (3, 'COMPLETED', NOW() - INTERVAL '8 days'  + INTERVAL '11 hours', NOW() - INTERVAL '8 days'  + INTERVAL '11 hours 35 minutes', 1200.00, 2100, 9.7,  'RECOMMENDED', NOW(), NOW()),
    (3, 'COMPLETED', NOW() - INTERVAL '13 days' + INTERVAL '10 hours', NOW() - INTERVAL '13 days' + INTERVAL '10 hours 55 minutes', 2000.00, 3300, 16.2, 'EXPLORE',     NOW(), NOW()),
    (3, 'COMPLETED', NOW() - INTERVAL '18 days' + INTERVAL '8 hours',  NOW() - INTERVAL '18 days' + INTERVAL '8 hours 40 minutes',  1700.00, 2400, 13.7, 'EXPLORE',     NOW(), NOW());


-- =============================================
-- 확인 쿼리 (시연 전 데이터 검증용 - 필요 시 주석 해제)
-- =============================================

-- 페르소나별 선호도 확인
-- SELECT '페르소나 A (김지훈)' AS persona, pc.name, ucp.selection_count, ucp.preference_score
-- FROM user_category_preferences ucp JOIN place_category pc ON pc.id = ucp.category_id
-- WHERE ucp.user_id = 1 ORDER BY ucp.preference_score DESC;
--
-- SELECT '페르소나 B (박민재)' AS persona, pc.name, ucp.selection_count, ucp.preference_score
-- FROM user_category_preferences ucp JOIN place_category pc ON pc.id = ucp.category_id
-- WHERE ucp.user_id = 3 ORDER BY ucp.preference_score DESC;

-- 시연 좌표 반경 3km 장소 카테고리 분포 확인
-- SELECT pc.name AS category, COUNT(*) AS cnt
-- FROM places p JOIN place_category pc ON pc.id = p.category_id
-- WHERE ST_DWithin(p.location, ST_SetSRID(ST_MakePoint(128.8541, 35.0942), 4326)::geography, 3000)
-- GROUP BY pc.name ORDER BY cnt DESC;
