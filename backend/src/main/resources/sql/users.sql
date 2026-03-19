-- =============================================
-- 1. users (3명)
-- =============================================
INSERT INTO users (id, email, password, nickname, phone) VALUES
(1, 'user1@test.com', '$2a$10$ZSFjsrHS5aGQS11lFxmYYuE22y9RSIoJHJFy6q8r8kuJuhKuVWnue', '김지훈', '010-1111-1111'),
(2, 'user2@test.com', '$2a$10$tx0f5Nl9M/ZVKEIg4LBzbOahGuEwhE6dA827jEdh/S4uO8RJMZkpK', '이서연', '010-2222-2222'),
(3, 'user3@test.com', '$2a$10$vdSHKjpymiHQqW6LqnkgsOY.v2IdgZjNexyS5WorD0GnGDIq4vVA.', '박민재', '010-3333-3333');

-- =============================================
-- 2. personality_tags (6개)
-- =============================================
INSERT INTO personality_tags (id, tag_name) VALUES
(1, '에너자이저'),
(2, '먹보'),
(3, '겁쟁이'),
(4, '사람좋아'),
(5, '친구좋아'),
(6, '짖음많음');

-- =============================================
-- 3. dogs (유저당 1마리)
-- =============================================
INSERT INTO dogs (id, user_id, name, breed, birth_date, weight, gender, neutered_yn, profile_image_url) VALUES
(1, 1, '오월', '프렌치불독', '2018-03-15', 9.5, 'FEMALE', true, NULL),
(2, 2, '영숙', '토이푸들', '2020-10-17', 5.7, 'FEMALE', true, NULL),
(3, 3, '까미', '시바이누', '2022-08-10', 10.1, 'MALE', false, NULL);

-- =============================================
-- 4. dog_personality_tags
--    1번 오월: 먹보, 사람좋아, 친구좋아 (긍정)
--    2번 영숙: 에너자이저, 사람좋아 (긍정)
--    3번 까미: 겁쟁이, 짖음많음 (부정)
-- =============================================
INSERT INTO dog_personality_tags (id, dog_id, tag_id) VALUES
(1, 1, 2),  -- 오월 - 먹보
(2, 1, 4),  -- 오월 - 사람좋아
(3, 1, 5),  -- 오월 - 친구좋아
(4, 2, 1),  -- 영숙 - 에너자이저
(5, 2, 4),  -- 영숙 - 사람좋아
(6, 3, 3),  -- 까미 - 겁쟁이
(7, 3, 6);  -- 까미 - 짖음많음
