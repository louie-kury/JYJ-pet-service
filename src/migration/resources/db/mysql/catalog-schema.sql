-- ERD 기준 초기 설치용. 기존 테이블에는 실행하지 않는다.
CREATE TABLE species (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL,
    name VARCHAR(50) NOT NULL,
    CONSTRAINT uk_species_uuid UNIQUE (uuid),
    CONSTRAINT uk_species_name UNIQUE (name)
);
CREATE TABLE breed (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL,
    species_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    CONSTRAINT uk_breed_uuid UNIQUE (uuid),
    CONSTRAINT uk_breed_name UNIQUE (species_id, name)
);
