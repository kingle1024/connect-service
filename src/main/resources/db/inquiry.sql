-- 개선/버그 요청(문의) 테이블 (MariaDB)
--
-- 이 프로젝트는 spring.jpa.hibernate.ddl-auto=none 이므로 애플리케이션이 테이블을 만들지 않는다.
-- 앱의 "개선/버그 요청" 메뉴와 백오피스 "문의 관리" 탭을 쓰기 전에 이 스크립트를 한 번 실행해야 한다.
--
-- type   : InquiryType   (IMPROVEMENT / BUG / ETC)
-- status : InquiryStatus (RECEIVED / IN_PROGRESS / ANSWERED / REJECTED)
--
-- 로그인 없이도 접수할 수 있어서 작성자는 둘 중 하나다.
--   회원   : user_id 가 채워지고 guest_key 는 NULL
--   비회원 : guest_key(단말이 만들어 캐시에 보관하는 키)가 채워지고 user_id 는 NULL
CREATE TABLE IF NOT EXISTS inquiry_mst (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       VARCHAR(100) NULL COMMENT '작성자 로그인 아이디 (users.user_id, FK 없음). 비회원 문의면 NULL',
    guest_key     VARCHAR(64)  NULL COMMENT '비회원이 내 문의 내역을 다시 볼 때 쓰는 단말 키. 회원 문의면 NULL',
    guest_email   VARCHAR(255) NULL COMMENT '비회원이 답변을 메일로 받으려고 남긴 주소 (선택)',
    user_name     VARCHAR(100) NOT NULL COMMENT '접수 당시 작성자 이름 (비회원은 ''비회원'')',
    type          VARCHAR(20)  NOT NULL DEFAULT 'IMPROVEMENT',
    title         VARCHAR(200) NOT NULL,
    content       TEXT         NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'RECEIVED',
    answer        TEXT         NULL COMMENT '관리자 답변 (없으면 NULL)',
    answered_by   VARCHAR(100) NULL COMMENT '답변한 관리자 아이디',
    answered_dts  DATETIME(6)  NULL,
    is_deleted    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '관리자가 숨긴 문의 (소프트 삭제)',
    insert_dts    DATETIME(6)  NOT NULL,
    update_dts    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    -- 앱의 "내 문의 내역" 조회용
    KEY idx_inquiry_mst_user (user_id, is_deleted, id),
    -- 비회원의 "내 문의 내역" 조회용
    KEY idx_inquiry_mst_guest (guest_key, is_deleted, id),
    -- 백오피스 상태 필터/미답변 건수 조회용
    KEY idx_inquiry_mst_status (status, is_deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;


-- ---------------------------------------------------------------------------
-- 이미 inquiry_mst 를 만들어 쓰고 있던 환경이라면 아래 ALTER 를 한 번 실행한다.
-- (비로그인 접수 기능이 추가되면서 컬럼이 늘고 user_id 가 NULL 을 허용하게 됐다)
-- ---------------------------------------------------------------------------
-- ALTER TABLE inquiry_mst
--     MODIFY COLUMN user_id VARCHAR(100) NULL COMMENT '작성자 로그인 아이디. 비회원 문의면 NULL',
--     ADD COLUMN guest_key   VARCHAR(64)  NULL COMMENT '비회원 단말 키' AFTER user_id,
--     ADD COLUMN guest_email VARCHAR(255) NULL COMMENT '비회원이 답변받을 메일 주소 (선택)' AFTER guest_key,
--     ADD KEY idx_inquiry_mst_guest (guest_key, is_deleted, id);


-- 확인용
-- SELECT id, user_id, guest_key, guest_email, type, status, title, answered_by FROM inquiry_mst ORDER BY id DESC;
