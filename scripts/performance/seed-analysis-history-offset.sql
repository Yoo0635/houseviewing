SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE post_reports;
TRUNCATE TABLE pre_reports;
TRUNCATE TABLE post_analyses;
TRUNCATE TABLE pre_analyses;
TRUNCATE TABLE contracts;
TRUNCATE TABLE houses;
TRUNCATE TABLE subscriptions;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO users (user_id, created_at, updated_at, email, login_id, name, password)
VALUES (1, NOW(6), NOW(6), 'analysis-perf@example.com', 'analysis-perf', '성능 측정',
        '$2y$10$EILk.v3ibkGc5OL9u.REleYJT7G9EnApZeHKTYOrK986gJaHlWXae');

INSERT INTO subscriptions (
    subscription_id, user_id, created_at, updated_at, plan_type
) VALUES (1, 1, NOW(6), NOW(6), 'PREMIUM');

DROP PROCEDURE IF EXISTS seed_analysis_history;
DELIMITER //
CREATE PROCEDURE seed_analysis_history()
BEGIN
    DECLARE i INT DEFAULT 1;
    WHILE i <= 25 DO
        INSERT INTO houses (
            house_id, user_id, created_at, updated_at, nickname, address_name, monitoring_status
        ) VALUES (
            i, 1, TIMESTAMP('2026-01-01 00:00:00') + INTERVAL i SECOND,
            TIMESTAMP('2026-01-01 00:00:00') + INTERVAL i SECOND,
            CONCAT('집-', i), CONCAT('서울시 테스트로 ', i), 'LIVE'
        );
        INSERT INTO contracts (
            contract_id, house_id, created_at, updated_at, confirm_date, move_date,
            deposit, maintenance_fee, monthly_amount, contract_type
        ) VALUES (
            i, i, NOW(6), NOW(6), '2026-01-01', '2026-02-01', 100000000, 100000, 0, 'JEONSE'
        );
        INSERT INTO post_analyses (
            analysis_id, house_id, contract_id, created_at, updated_at, analysis_type,
            snapshot_hash, risk_level, main_reason, ltv_score, raw_data
        ) VALUES (
            i, i, i, TIMESTAMP('2026-02-01 00:00:00') + INTERVAL i SECOND,
            TIMESTAMP('2026-02-01 00:00:00') + INTERVAL i SECOND,
            IF(MOD(i, 2) = 0, 'DIFF', 'BASIC'), SHA2(CONCAT('snapshot-', i), 256),
            ELT(MOD(i, 3) + 1, 'SAFE', 'WARNING', 'DANGER'), CONCAT('사후 원인-', i),
            MOD(i, 101), JSON_OBJECT('payload', REPEAT('x', 10000))
        );
        INSERT INTO pre_analyses (
            analysis_id, user_id, created_at, updated_at, nickname, address_name,
            main_reason, raw_data, risk_level, ltv_score
        ) VALUES (
            i, 1, TIMESTAMP('2026-01-01 00:00:00') + INTERVAL i SECOND,
            TIMESTAMP('2026-01-01 00:00:00') + INTERVAL i SECOND,
            CONCAT('사전-', i), CONCAT('서울시 사전로 ', i), CONCAT('사전 원인-', i),
            JSON_OBJECT('payload', REPEAT('x', 10000)),
            ELT(MOD(i, 3) + 1, 'SAFE', 'WARNING', 'DANGER'), MOD(i, 101)
        );
        IF MOD(i, 10) <> 0 THEN
            INSERT INTO post_reports (
                report_id, analysis_id, created_at, updated_at, pdf_key, pdf_name, pdf_path, pdf_size_bytes
            ) VALUES (i, i, NOW(6), NOW(6), CONCAT('post-', i), CONCAT('post-', i, '.pdf'),
                      CONCAT('/post/', i), 1024);
            INSERT INTO pre_reports (
                report_id, analysis_id, created_at, updated_at, pdf_key, pdf_name, pdf_path, pdf_size_bytes
            ) VALUES (i, i, NOW(6), NOW(6), CONCAT('pre-', i), CONCAT('pre-', i, '.pdf'),
                      CONCAT('/pre/', i), 1024);
        END IF;
        SET i = i + 1;
    END WHILE;
END//
DELIMITER ;
CALL seed_analysis_history();
DROP PROCEDURE seed_analysis_history;
