-- 앱 설정(키-값) 테이블 (MariaDB)
-- 백오피스에서 바꾸는 앱 전역 설정을 저장한다. 예) theme.primary = 제품(브랜드) 색
-- ddl-auto=none 이므로 배포 전에 직접 한 번 실행한다. 여러 번 실행해도 안전하다.
CREATE TABLE IF NOT EXISTS app_setting (
    setting_key   VARCHAR(100) NOT NULL,
    setting_value VARCHAR(1000) NOT NULL,
    insert_dts    DATETIME(6)  NOT NULL,
    update_dts    DATETIME(6)  NOT NULL,
    PRIMARY KEY (setting_key)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
