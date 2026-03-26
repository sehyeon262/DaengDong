-- =============================================
-- 시연용 페르소나 더미데이터 (발표용 VS 구도)
--
-- 시연 위치: lat=35.0942, lon=128.8541 (김해시 내외동 일대)
-- 같은 위치에서 경로 추천 API를 호출하면
-- 개인화 알고리즘에 의해 서로 다른 경로가 추천되는 것을 시연
--
-- 페르소나 A (user_id=101): 콜드스타트 신규 유저
--   🐶 뽀미 (말티즈 / 3kg / 10살) → SMALL_SENIOR 세그먼트
--   신규 가입자 → 개인 데이터 없음 → CF 세그먼트 기반 추천
--   반경 0.7배 축소 → 짧고 안전한 평탄 경로
--
-- 페르소나 B (user_id=102): 1달 사용 헤비 유저
--   🐕 맥스 (골든리트리버 / 30kg / 2살) → LARGE_ADULT 세그먼트
--   자연/하천/공원 선호 → 개인화 가중치 적용
--   반경 1.3배 확대 → 넓은 탐험 코스
--   최근 3일 A공원 반복 방문 → 피로도 감점(-0.3) 시연
--
-- 실행 전 users.sql, init_places.sql이 먼저 적용되어 있어야 합니다.
--
-- 시연 API 호출 예시:
--   POST /api/v1/routes/recommend
--   Authorization: Bearer {뽀미 보호자 JWT}  → 근린공원 위주 짧은 경로
--   Authorization: Bearer {맥스 보호자 JWT}  → 하천/자연 중심 탐험 코스
-- =============================================


-- =============================================
-- 1. 시연용 유저 (2명)
-- =============================================
INSERT INTO users (id, email, password, nickname, phone)
VALUES
    (101, 'demo-a@test.com', '$2a$10$ZSFjsrHS5aGQS11lFxmYYuE22y9RSIoJHJFy6q8r8kuJuhKuVWnue', '뽀미맘', '010-0101-0101'),
    (102, 'demo-b@test.com', '$2a$10$ZSFjsrHS5aGQS11lFxmYYuE22y9RSIoJHJFy6q8r8kuJuhKuVWnue', '맥스아빠', '010-0102-0102')
ON CONFLICT (id) DO NOTHING;


-- =============================================
-- 2. 시연용 강아지
--    뽀미: 말티즈 3kg, 2016-03-01생 → 10살 → SMALL + SENIOR
--    맥스: 골든리트리버 30kg, 2024-01-15생 → 2살 → LARGE + ADULT
-- =============================================
INSERT INTO dogs (id, user_id, name, breed, birth_date, weight, gender, neutered_yn, profile_image_url)
VALUES
    (101, 101, '뽀미', '말티즈',       '2016-03-01', 3.0,  'FEMALE', true,  NULL),
    (102, 102, '맥스', '골든리트리버',   '2024-01-15', 30.0, 'MALE',   false, NULL)
ON CONFLICT (id) DO NOTHING;


-- =============================================
-- 3. breed_distance_config (체중별 적정 거리)
--    SMALL  → 반경 배율 0.7x (500m 기준)
--    LARGE  → 반경 배율 1.3x (1500m 기준)
-- =============================================
INSERT INTO breed_distance_config (id, size_category, min_weight_kg, max_weight_kg, recommended_distance_m, max_distance_m)
VALUES
    (1, 'SMALL',  0.00,  10.00, 500,  1000),
    (2, 'MEDIUM', 10.00, 25.00, 1000, 2000),
    (3, 'LARGE',  25.00, 100.00, 1500, 3000)
ON CONFLICT (id) DO NOTHING;


-- =============================================
-- 4. scoring_weights (ML 학습 가중치)
--    시연용: 카테고리 가중치를 높여 개인화 효과 극대화
-- =============================================
INSERT INTO scoring_weights (feature_name, weight, sample_count, created_at, updated_at)
VALUES
    ('DISTANCE', 0.35, 100, NOW(), NOW()),
    ('CATEGORY', 0.65, 100, NOW(), NOW())
ON CONFLICT (feature_name) DO UPDATE
SET weight = EXCLUDED.weight, sample_count = EXCLUDED.sample_count, updated_at = NOW();


-- =============================================
-- 5. 세그먼트 선호도 (콜드스타트 CF 데이터)
--    ★ 핵심: 뽀미(SMALL_SENIOR)가 가입 즉시 추천받는 근거
--    age_group 포함하여 정확한 2차원 세그먼트 매칭
-- =============================================
INSERT INTO segment_preferences (weight_group, age_group, category_id, preference_score, sample_count, created_at, updated_at)
VALUES
    -- ─── SMALL_SENIOR (뽀미 세그먼트) ───
    -- 소형 노견 그룹: 가까운 공원, 평탄한 산책로, 카페 선호
    ('SMALL', 'SENIOR', 14, 0.95, 40, NOW(), NOW()),   -- 공원 (최고 선호 → 평탄한 근린공원)
    ('SMALL', 'SENIOR', 16, 0.80, 40, NOW(), NOW()),   -- 산책로 (안전한 산책로)
    ('SMALL', 'SENIOR', 6,  0.65, 40, NOW(), NOW()),   -- 카페 (실내 휴식)
    ('SMALL', 'SENIOR', 7,  0.50, 40, NOW(), NOW()),   -- 식당
    ('SMALL', 'SENIOR', 4,  0.35, 40, NOW(), NOW()),   -- 반려동물용품
    ('SMALL', 'SENIOR', 11, 0.20, 40, NOW(), NOW()),   -- 여행지 (먼 거리 비선호)

    -- ─── SMALL_ADULT ───
    ('SMALL', 'ADULT', 6,  0.85, 30, NOW(), NOW()),
    ('SMALL', 'ADULT', 7,  0.70, 30, NOW(), NOW()),
    ('SMALL', 'ADULT', 14, 0.60, 30, NOW(), NOW()),
    ('SMALL', 'ADULT', 4,  0.45, 30, NOW(), NOW()),
    ('SMALL', 'ADULT', 16, 0.40, 30, NOW(), NOW()),

    -- ─── SMALL_PUPPY ───
    ('SMALL', 'PUPPY', 14, 0.90, 20, NOW(), NOW()),
    ('SMALL', 'PUPPY', 16, 0.70, 20, NOW(), NOW()),
    ('SMALL', 'PUPPY', 6,  0.50, 20, NOW(), NOW()),
    ('SMALL', 'PUPPY', 4,  0.40, 20, NOW(), NOW()),

    -- ─── LARGE_ADULT (맥스 세그먼트 — 개인화 데이터 있으므로 CF 미사용) ───
    -- 대형 활동견 그룹: 넓은 야외, 자연, 여행지 선호
    ('LARGE', 'ADULT', 11, 0.95, 25, NOW(), NOW()),    -- 여행지
    ('LARGE', 'ADULT', 14, 0.85, 25, NOW(), NOW()),    -- 공원
    ('LARGE', 'ADULT', 16, 0.80, 25, NOW(), NOW()),    -- 산책로
    ('LARGE', 'ADULT', 12, 0.45, 25, NOW(), NOW()),    -- 펜션
    ('LARGE', 'ADULT', 6,  0.30, 25, NOW(), NOW()),    -- 카페

    -- ─── LARGE_SENIOR ───
    ('LARGE', 'SENIOR', 14, 0.80, 15, NOW(), NOW()),
    ('LARGE', 'SENIOR', 16, 0.70, 15, NOW(), NOW()),
    ('LARGE', 'SENIOR', 11, 0.60, 15, NOW(), NOW()),
    ('LARGE', 'SENIOR', 6,  0.35, 15, NOW(), NOW()),

    -- ─── LARGE_PUPPY ───
    ('LARGE', 'PUPPY', 14, 0.90, 10, NOW(), NOW()),
    ('LARGE', 'PUPPY', 16, 0.75, 10, NOW(), NOW()),
    ('LARGE', 'PUPPY', 11, 0.55, 10, NOW(), NOW()),

    -- ─── MEDIUM_ADULT ───
    ('MEDIUM', 'ADULT', 11, 0.85, 30, NOW(), NOW()),
    ('MEDIUM', 'ADULT', 14, 0.75, 30, NOW(), NOW()),
    ('MEDIUM', 'ADULT', 16, 0.65, 30, NOW(), NOW()),
    ('MEDIUM', 'ADULT', 8,  0.55, 30, NOW(), NOW()),
    ('MEDIUM', 'ADULT', 6,  0.40, 30, NOW(), NOW()),

    -- ─── MEDIUM_SENIOR ───
    ('MEDIUM', 'SENIOR', 14, 0.85, 20, NOW(), NOW()),
    ('MEDIUM', 'SENIOR', 16, 0.75, 20, NOW(), NOW()),
    ('MEDIUM', 'SENIOR', 6,  0.55, 20, NOW(), NOW()),
    ('MEDIUM', 'SENIOR', 11, 0.45, 20, NOW(), NOW()),

    -- ─── MEDIUM_PUPPY ───
    ('MEDIUM', 'PUPPY', 14, 0.85, 15, NOW(), NOW()),
    ('MEDIUM', 'PUPPY', 16, 0.70, 15, NOW(), NOW()),
    ('MEDIUM', 'PUPPY', 11, 0.50, 15, NOW(), NOW())

ON CONFLICT (weight_group, age_group, category_id) DO UPDATE
SET preference_score = EXCLUDED.preference_score,
    sample_count = EXCLUDED.sample_count,
    updated_at = NOW();


-- =============================================
-- ★ 페르소나 A (뽀미맘, user_id=101) — 콜드스타트
--   개인 데이터 없음 → 이 섹션은 의도적으로 비어 있음
--   알고리즘 흐름:
--     1) user_category_preferences 조회 → 없음
--     2) segment_preferences(SMALL, SENIOR) CF 발동
--     3) 공원(0.95) > 산책로(0.80) > 카페(0.65) 순 추천
--     4) 반경 0.7배 → SHORT=350m, RECOMMEND=700m, EXPLORE=1050m
-- =============================================
-- (데이터 없음 — 콜드스타트 시연)


-- =============================================
-- ★ 페르소나 B (맥스아빠, user_id=102) — 1달 사용 헤비 유저
--   알고리즘 흐름:
--     1) user_category_preferences 조회 → 선택 5회 이상 → 개인화 활성
--     2) 여행지/공원/산책로 가중치 극대화
--     3) 반경 1.3배 → SHORT=650m, RECOMMEND=1300m, EXPLORE=1950m
--     4) 최근 3일 방문 장소(A공원) 피로도 -0.3 감점
-- =============================================

-- 6-1. 카테고리 선호도 (자연/야외 극단 선호)
INSERT INTO user_category_preferences (user_id, category_id, selection_count, preference_score, created_at, updated_at)
VALUES
    (102, 11, 25, 1.00, NOW(), NOW()),   -- 여행지: 25회 → 1.0 (최고 선호)
    (102, 14, 20, 0.80, NOW(), NOW()),   -- 공원: 20회 → 0.80
    (102, 16, 18, 0.72, NOW(), NOW()),   -- 산책로: 18회 → 0.72
    (102, 15,  8, 0.32, NOW(), NOW()),   -- 화장실: 8회 → 0.32 (경유)
    (102, 6,   3, 0.12, NOW(), NOW()),   -- 카페: 3회 → 0.12 (비선호)
    (102, 7,   2, 0.08, NOW(), NOW())    -- 식당: 2회 → 0.08 (비선호)
ON CONFLICT (user_id, category_id) DO UPDATE
SET selection_count = EXCLUDED.selection_count,
    preference_score = EXCLUDED.preference_score,
    updated_at = NOW();

-- 6-2. 경로 선택 로그 (1달간 EXPLORE 위주, 오전 산책)
INSERT INTO route_selection_logs (user_id, dog_id, selected_type, selected_distance_m, hour_of_day, day_of_week, weather_condition, temperature, created_at, updated_at)
VALUES
    -- 최근 3일: 매일 같은 공원 방문 (피로도 감점 시연 데이터)
    (102, 102, 'EXPLORE',     1800, 9,  4, 'CLEAR',  22.0, NOW() - INTERVAL '1 day',   NOW()),
    (102, 102, 'EXPLORE',     1900, 10, 3, 'CLEAR',  21.0, NOW() - INTERVAL '2 days',  NOW()),
    (102, 102, 'RECOMMENDED', 1300, 9,  2, 'CLOUDY', 19.0, NOW() - INTERVAL '3 days',  NOW()),
    -- 이전 기록: 다양한 자연 코스
    (102, 102, 'EXPLORE',     2100, 8,  7, 'CLEAR',  24.0, NOW() - INTERVAL '5 days',  NOW()),
    (102, 102, 'EXPLORE',     1700, 10, 6, 'CLEAR',  23.0, NOW() - INTERVAL '7 days',  NOW()),
    (102, 102, 'RECOMMENDED', 1400, 11, 5, 'CLOUDY', 20.0, NOW() - INTERVAL '9 days',  NOW()),
    (102, 102, 'EXPLORE',     2000, 9,  4, 'CLEAR',  25.0, NOW() - INTERVAL '12 days', NOW()),
    (102, 102, 'EXPLORE',     1600, 10, 3, 'CLEAR',  22.0, NOW() - INTERVAL '15 days', NOW()),
    (102, 102, 'EXPLORE',     2200, 8,  1, 'CLEAR',  26.0, NOW() - INTERVAL '18 days', NOW()),
    (102, 102, 'RECOMMENDED', 1500, 11, 7, 'CLOUDY', 18.0, NOW() - INTERVAL '21 days', NOW()),
    (102, 102, 'EXPLORE',     1900, 9,  6, 'CLEAR',  23.0, NOW() - INTERVAL '25 days', NOW()),
    (102, 102, 'EXPLORE',     2100, 10, 2, 'CLEAR',  24.0, NOW() - INTERVAL '28 days', NOW());

-- 6-3. 선택 장소: 최근 3일 로그에 "A공원" 연결 (피로도 감점 대상)
--      시연 좌표(35.0942, 128.8541) 반경 3km 이내 공원(category_id=14) 중
--      하나를 골라 최근 3일 방문 기록에 모두 연결 → -0.3 감점 발동
INSERT INTO route_selection_places (selection_log_id, place_id, visit_order)
SELECT sl.id, park.id, 1
FROM route_selection_logs sl
CROSS JOIN LATERAL (
    SELECT p.id
    FROM places p
    WHERE p.category_id = 14            -- 공원
      AND p.is_active = true
      AND ST_DWithin(
          p.location,
          ST_SetSRID(ST_MakePoint(128.8541, 35.0942), 4326)::geography,
          3000
      )
    ORDER BY ST_Distance(
        p.location,
        ST_SetSRID(ST_MakePoint(128.8541, 35.0942), 4326)::geography
    )
    LIMIT 1                              -- 가장 가까운 공원 = "A공원"
) park
WHERE sl.user_id = 102
  AND sl.created_at > NOW() - INTERVAL '3 days';

-- 6-4. 이전 기록에는 다양한 자연/여행지 장소 연결
INSERT INTO route_selection_places (selection_log_id, place_id, visit_order)
SELECT sl.id, sub.place_id, sub.rn
FROM route_selection_logs sl
CROSS JOIN LATERAL (
    SELECT p.id AS place_id,
           ROW_NUMBER() OVER (ORDER BY RANDOM()) AS rn
    FROM places p
    WHERE p.category_id IN (11, 14, 16)  -- 여행지, 공원, 산책로
      AND p.is_active = true
      AND ST_DWithin(
          p.location,
          ST_SetSRID(ST_MakePoint(128.8541, 35.0942), 4326)::geography,
          5000                            -- 대형견 → 넓은 탐색 범위
      )
    LIMIT 4
) sub
WHERE sl.user_id = 102
  AND sl.created_at <= NOW() - INTERVAL '3 days'
ON CONFLICT DO NOTHING;


-- =============================================
-- 7. 산책 기록 (완주율/이탈률 지표 + 패턴 분석)
-- =============================================
INSERT INTO walk_records (dog_id, walk_status, start_time, end_time, total_distance, total_duration, calories, route_type, created_at, updated_at)
VALUES
    -- ★ 페르소나 A (뽀미, dog_id=101): 기록 없음 — 콜드스타트
    -- (의도적으로 비어 있음)

    -- ★ 페르소나 B (맥스, dog_id=102): EXPLORE 위주, 오전 산책, 활발
    (102, 'COMPLETED', NOW() - INTERVAL '1 day'   + INTERVAL '9 hours',  NOW() - INTERVAL '1 day'   + INTERVAL '9 hours 55 minutes',  1800.00, 3300, 22.5, 'EXPLORE',     NOW(), NOW()),
    (102, 'COMPLETED', NOW() - INTERVAL '2 days'  + INTERVAL '10 hours', NOW() - INTERVAL '2 days'  + INTERVAL '10 hours 50 minutes', 1900.00, 3000, 23.8, 'EXPLORE',     NOW(), NOW()),
    (102, 'COMPLETED', NOW() - INTERVAL '3 days'  + INTERVAL '9 hours',  NOW() - INTERVAL '3 days'  + INTERVAL '9 hours 40 minutes',  1300.00, 2400, 16.3, 'RECOMMENDED', NOW(), NOW()),
    (102, 'COMPLETED', NOW() - INTERVAL '5 days'  + INTERVAL '8 hours',  NOW() - INTERVAL '5 days'  + INTERVAL '8 hours 60 minutes',  2100.00, 3600, 26.3, 'EXPLORE',     NOW(), NOW()),
    (102, 'COMPLETED', NOW() - INTERVAL '7 days'  + INTERVAL '10 hours', NOW() - INTERVAL '7 days'  + INTERVAL '10 hours 45 minutes', 1700.00, 2700, 21.3, 'EXPLORE',     NOW(), NOW()),
    (102, 'COMPLETED', NOW() - INTERVAL '9 days'  + INTERVAL '11 hours', NOW() - INTERVAL '9 days'  + INTERVAL '11 hours 35 minutes', 1400.00, 2100, 17.5, 'RECOMMENDED', NOW(), NOW()),
    (102, 'COMPLETED', NOW() - INTERVAL '12 days' + INTERVAL '9 hours',  NOW() - INTERVAL '12 days' + INTERVAL '9 hours 50 minutes',  2000.00, 3000, 25.0, 'EXPLORE',     NOW(), NOW()),
    (102, 'COMPLETED', NOW() - INTERVAL '15 days' + INTERVAL '10 hours', NOW() - INTERVAL '15 days' + INTERVAL '10 hours 40 minutes', 1600.00, 2400, 20.0, 'EXPLORE',     NOW(), NOW()),
    (102, 'COMPLETED', NOW() - INTERVAL '18 days' + INTERVAL '8 hours',  NOW() - INTERVAL '18 days' + INTERVAL '8 hours 45 minutes',  2200.00, 3300, 27.5, 'EXPLORE',     NOW(), NOW()),
    (102, 'COMPLETED', NOW() - INTERVAL '21 days' + INTERVAL '11 hours', NOW() - INTERVAL '21 days' + INTERVAL '11 hours 30 minutes', 1500.00, 2100, 18.8, 'RECOMMENDED', NOW(), NOW()),
    (102, 'COMPLETED', NOW() - INTERVAL '25 days' + INTERVAL '9 hours',  NOW() - INTERVAL '25 days' + INTERVAL '9 hours 55 minutes',  1900.00, 3000, 23.8, 'EXPLORE',     NOW(), NOW()),
    (102, 'COMPLETED', NOW() - INTERVAL '28 days' + INTERVAL '10 hours', NOW() - INTERVAL '28 days' + INTERVAL '10 hours 50 minutes', 2100.00, 3000, 26.3, 'EXPLORE',     NOW(), NOW());


-- =============================================
-- 확인 쿼리 (시연 전 데이터 검증용 — 필요 시 주석 해제)
-- =============================================

-- 페르소나 A: 뽀미 (콜드스타트 → 세그먼트 CF 확인)
-- SELECT '뽀미 (SMALL_SENIOR CF)' AS persona, pc.name, sp.preference_score
-- FROM segment_preferences sp JOIN place_category pc ON pc.id = sp.category_id
-- WHERE sp.weight_group = 'SMALL' AND sp.age_group = 'SENIOR'
-- ORDER BY sp.preference_score DESC;

-- 페르소나 B: 맥스 (개인화 선호도 확인)
-- SELECT '맥스 (개인화)' AS persona, pc.name, ucp.selection_count, ucp.preference_score
-- FROM user_category_preferences ucp JOIN place_category pc ON pc.id = ucp.category_id
-- WHERE ucp.user_id = 102 ORDER BY ucp.preference_score DESC;

-- 피로도 감점 대상 확인 (최근 3일 방문 장소)
-- SELECT p.name, pc.name AS category, rsp.visit_order, sl.created_at
-- FROM route_selection_places rsp
-- JOIN route_selection_logs sl ON sl.id = rsp.selection_log_id
-- JOIN places p ON p.id = rsp.place_id
-- JOIN place_category pc ON pc.id = p.category_id
-- WHERE sl.user_id = 102 AND sl.created_at > NOW() - INTERVAL '3 days';

-- 시연 좌표 반경 장소 카테고리 분포 확인
-- SELECT pc.name AS category, COUNT(*) AS cnt
-- FROM places p JOIN place_category pc ON pc.id = p.category_id
-- WHERE ST_DWithin(p.location, ST_SetSRID(ST_MakePoint(128.8541, 35.0942), 4326)::geography, 3000)
-- GROUP BY pc.name ORDER BY cnt DESC;
