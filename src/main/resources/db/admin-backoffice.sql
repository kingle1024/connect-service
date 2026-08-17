-- 관리자 백오피스용 스키마 변경 스크립트 (MariaDB)
--
-- 이 프로젝트는 spring.jpa.hibernate.ddl-auto=none 이므로 애플리케이션이 테이블을 만들지 않는다.
-- 백오피스를 쓰기 전에 이 스크립트를 DB에 직접 한 번 실행해야 한다.
--
-- !! 중요 !!
-- 3) 의 users.enabled 컬럼은 Users 엔티티에 매핑되어 있어서, 이 스크립트를 실행하지 않으면
-- 회원 조회가 들어가는 모든 기능(로그인 포함)이 SQL 오류로 실패한다. 배포 전에 먼저 실행해야 한다.


-- ---------------------------------------------------------------------------
-- 1) 게시판 카테고리 관리 테이블
--    board_mst.category 는 지금처럼 문자열을 그대로 유지한다(FK 없음).
--    이 테이블은 "앱에 어떤 카테고리를 어떤 순서로 노출할지"를 관리하는 용도다.
--
--    테이블명이 category_mst 가 아닌 이유: 같은 스키마에 이 앱과 무관한
--    category_mst (id int / name / is_visible / rank) 테이블이 이미 존재한다.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS board_category_mst (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(100) NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    is_active  TINYINT(1)   NOT NULL DEFAULT 1,
    insert_dts DATETIME(6)  NOT NULL,
    update_dts DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_board_category_mst_name (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;


-- ---------------------------------------------------------------------------
-- 2) 기존 게시글에 쓰인 카테고리를 초기 데이터로 이관
--    이미 있는 이름은 건드리지 않으므로 여러 번 실행해도 안전하다.
-- ---------------------------------------------------------------------------
INSERT INTO board_category_mst (name, sort_order, is_active, insert_dts, update_dts)
SELECT TRIM(b.category), 0, 1, NOW(6), NOW(6)
FROM board_mst b
WHERE b.category IS NOT NULL
  AND TRIM(b.category) <> ''
GROUP BY TRIM(b.category)
ON DUPLICATE KEY UPDATE update_dts = update_dts;


-- ---------------------------------------------------------------------------
-- 3) 회원 활성/정지 상태
--    Users.enabled 와 매핑된다. 0 이면 로그인도, 기존 토큰 사용도 막힌다.
-- ---------------------------------------------------------------------------
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS enabled TINYINT(1) NOT NULL DEFAULT 1;


-- ---------------------------------------------------------------------------
-- 4) 최초 관리자 계정 지정
--    백오피스는 users 테이블을 그대로 쓰고 ROLE_ADMIN 권한으로 접근을 통제한다.
--    관리자로 쓸 계정을 먼저 일반 회원가입으로 만든 뒤, 아래 'CHANGE_ME' 를
--    그 계정의 로그인 아이디로 바꿔서 한 번만 실행한다.
--    (users.user_id = 로그인 아이디 / user_roles.user_id = users.id 이므로 주의)
--    이후 추가 관리자는 백오피스 > 회원 관리 화면에서 부여할 수 있다.
-- ---------------------------------------------------------------------------
-- INSERT INTO user_roles (user_id, roles)
-- SELECT u.id, 'ROLE_ADMIN'
-- FROM users u
-- WHERE u.user_id = 'CHANGE_ME';


-- 확인용
-- SELECT * FROM board_category_mst ORDER BY sort_order, name;
-- SELECT u.user_id, u.name, u.enabled, r.roles
-- FROM users u LEFT JOIN user_roles r ON r.user_id = u.id
-- WHERE r.roles = 'ROLE_ADMIN';
