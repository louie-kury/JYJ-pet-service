-- 격리된 테스트 전용 DB에서만 실행한다.
INSERT INTO species (uuid, name) VALUES
    ('61b179ee-8a43-3b1d-8d63-f355b72ddf67', '강아지'),
    ('33174886-9924-3da5-9126-7d292c31e15d', '고양이'),
    ('47b5fa2c-947f-41b5-9f04-b8923261d885', '토끼');
INSERT INTO breed (uuid, species_id, name)
SELECT '9fbe7e65-d3c5-3a72-8655-7150179023b9', id, '푸들' FROM species
WHERE uuid = '61b179ee-8a43-3b1d-8d63-f355b72ddf67';
