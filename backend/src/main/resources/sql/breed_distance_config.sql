-- 견종/체중 구간별 적정 산책 거리 설정
-- 체중 구간: minWeightKg(포함) ~ maxWeightKg(미포함)
--
-- 소형견 (0~10kg): 권장 500m, 최대 1000m → 반경 축소 (0.5배)
-- 중형견 (10~25kg): 권장 1000m, 최대 2000m → 기본 (1.0배)
-- 대형견 (25~100kg): 권장 1500m, 최대 3000m → 반경 확대 (1.5배)

INSERT INTO breed_distance_config (size_category, min_weight_kg, max_weight_kg, recommended_distance_m, max_distance_m, created_at, updated_at, is_deleted)
VALUES
    ('SMALL', 0.00, 10.00, 500, 1000, NOW(), NOW(), false),
    ('MEDIUM', 10.00, 25.00, 1000, 2000, NOW(), NOW(), false),
    ('LARGE', 25.00, 100.00, 1500, 3000, NOW(), NOW(), false)
ON CONFLICT DO NOTHING;
