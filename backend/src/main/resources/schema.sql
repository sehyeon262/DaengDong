-- PostGIS 확장 활성화 (Hibernate ddl-auto 테이블 생성 전에 실행됨)
-- geometry(Point, 4326) 타입을 사용하는 places 테이블보다 반드시 먼저 실행되어야 함
CREATE EXTENSION IF NOT EXISTS postgis;
