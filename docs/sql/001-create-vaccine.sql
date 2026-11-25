-- 새 테이블을 준비할 때만 실행합니다. 기존 테이블은 구조와 데이터를 먼저 확인합니다.
CREATE TABLE vaccine (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uuid CHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_vaccine_uuid UNIQUE (uuid),
    CONSTRAINT uk_vaccine_name UNIQUE (name)
);
