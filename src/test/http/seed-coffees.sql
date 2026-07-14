-- 커피 메뉴 시드 데이터
-- coffee-order-api.http 실행 전, coffees 테이블에 메뉴를 채운다. (메뉴 생성 API는 과제 범위 밖)
--
-- 실행 방법 (택1):
--   docker exec -i coffee-mysql mysql -uroot -proot coffee < src/test/http/seed-coffees.sql
--   docker exec coffee-mysql mysql -uroot -proot coffee -e "INSERT INTO coffees(name,price) VALUES('아메리카노',4000),('카페라떼',4500);"
--
-- 재실행 시 중복 삽입을 피하려면 아래 두 줄의 주석을 풀어 초기화 후 삽입한다.
--   (orders가 이 메뉴를 참조 중이면 FK 때문에 TRUNCATE가 막히므로 체크를 잠시 끈다.)

-- SET FOREIGN_KEY_CHECKS = 0;
-- TRUNCATE TABLE coffees;
-- SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO coffees (name, price) VALUES
  ('아메리카노', 4000),
  ('카페라떼', 4500),
  ('바닐라라떼', 5000),
  ('콜드브루', 4800),
  ('카푸치노', 4500),
  ('카라멜마키아토', 5500);
