-- 모집 참여자 테이블 (MariaDB)
--
-- 이 프로젝트는 spring.jpa.hibernate.ddl-auto=none 이므로 애플리케이션이 테이블을 만들지 않는다.
-- 모집 상세/목록의 "참여하기"를 쓰기 전에 이 스크립트를 한 번 실행해야 한다.
--
-- 작성자는 이 테이블에 넣지 않는다. board_mst.current_participants 는 작성자 1명을 포함한 값으로
-- 시작하고(글쓰기 시 1), 여기에 한 줄이 생길 때마다 +1, 참여 취소로 한 줄이 지워질 때마다 -1 된다.
-- 회원/게스트 모두 users.user_id 로 들어온다 (게스트 기록 가져오기 때 회원 아이디로 바뀐다).
CREATE TABLE IF NOT EXISTS board_participant (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    board_id    BIGINT       NOT NULL COMMENT 'board_mst.id (FK 없음)',
    user_id     VARCHAR(100) NOT NULL COMMENT '참여자 users.user_id (FK 없음)',
    user_name   VARCHAR(100) NOT NULL COMMENT '참여 당시 이름',
    insert_dts  DATETIME(6)  NOT NULL,
    update_dts  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    -- 같은 글에 두 번 참여할 수 없다
    UNIQUE KEY uk_board_participant (board_id, user_id),
    -- 내가 참여한 모집 조회 / 게스트 기록 가져오기용
    KEY idx_board_participant_user (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
