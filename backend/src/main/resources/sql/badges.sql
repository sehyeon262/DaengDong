-- 배지 중복 데이터 정리 (badge_name 기준 최소 ID만 남기고 삭제)
DELETE FROM badges WHERE id NOT IN (
    SELECT MIN(id) FROM badges GROUP BY badge_name
);

-- unique 인덱스 생성 (없으면)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_badges_badge_name_unique') THEN
        CREATE UNIQUE INDEX idx_badges_badge_name_unique ON badges (badge_name);
    END IF;
END $$;

-- 배지 마스터 데이터 시딩
INSERT INTO badges (badge_name, condition_value, description, created_at, updated_at) VALUES
('발자국 콩콩', '5', '장소 5곳 방문', NOW(), NOW()),
('세상은 넓다개', '20', '장소 20곳 방문', NOW(), NOW()),
('길 위의 모험가', '5', '새로운 코스 5회 완주', NOW(), NOW()),
('우리 동네 지킴이', '3', '위험 신고 3회', NOW(), NOW()),
('추억 수집가', '10', '사진과 일기가 함께한 산책 10회', NOW(), NOW()),
('오늘도 찰칵', '10', '산책 사진 10장 촬영', NOW(), NOW()),
('우리집 스타', '1', '반려견 프로필 등록 완료', NOW(), NOW()),
('우리는 이제 친구', '1', '다른 강아지와 첫 만남', NOW(), NOW()),
('우리 동네 인싸', '5', '5마리 이상의 다른 강아지와 만남', NOW(), NOW())
ON CONFLICT (badge_name) DO NOTHING;
