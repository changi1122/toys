# Olist DBMS 고급 기능 마스터 프로젝트

> 기준 DBMS: **PostgreSQL 15+** (Materialized View, Updatable View, `WITH CHECK OPTION`, PL/pgSQL 트리거/프로시저 지원)
> MySQL 사용 시: Phase 3(B)에서 Materialized View 대신 집계 테이블 + 이벤트 스케줄러를 사용합니다.

---

## Phase 0. 환경 준비

### [OLIST-001] 스키마 적재

- **목표**: Olist CSV 6종을 `olist` 스키마에 적재하고 PK/FK/인덱스를 설계합니다.
- **제약사항**
  - 적재는 `COPY`로 하고, 행 단위 `INSERT`는 쓰지 않습니다.
  - 적재 후에 FK와 인덱스를 생성합니다. 적재 전에 걸면 느립니다.
  - `order_items`의 PK는 `(order_id, order_item_id)` 복합키입니다.
  - `order_payments`의 PK는 `(order_id, payment_sequential)` 복합키입니다.
  - `customers`에서 `customer_id`(주문마다 새로 발급)와 `customer_unique_id`(실제 고객)는 다릅니다. 이 구분이 이후 모든 Phase의 함정입니다.

- **파일목록**

1. `olist_customers_dataset.csv`
2. `olist_orders_dataset.csv`
3. `olist_order_items_dataset.csv`
4. `olist_order_payments_dataset.csv`
5. `olist_products_dataset.csv`
6. `product_category_name_translation.csv`

- **힌트**

```sql
CREATE SCHEMA olist;
COPY olist.customers FROM '/data/olist_customers_dataset.csv' CSV HEADER;
```

- **셀프 테스트**
  - `SELECT count(*) FROM olist.orders;` → 약 99,441건이어야 합니다.
  - `SELECT count(DISTINCT customer_unique_id), count(*) FROM olist.customers;` → 두 값이 다르다는 점을 확인합니다.

---

## Phase 1. 시나리오 A: View 기반 보안 분리

### [OLIST-101] 마케팅팀용 마스킹 뷰 `v_customers_masked`

- **요구사항**: 마케팅팀이 원본 `customers`는 못 보고 마스킹된 뷰만 조회하게 합니다.
- **제약사항**
  - 원본 `customer_id`는 노출하지 않습니다. `md5(customer_id || '솔트')` 같은 가명 키로 대체합니다.
  - `zip_prefix`는 앞 3자리만, `city`는 그대로 노출합니다.
  - 뷰를 만들고 끝내면 안 됩니다. `marketing_role`을 만들고 `REVOKE ALL ON olist.customers`, `GRANT SELECT ON v_customers_masked`까지 해야 완료입니다.
- **안티패턴**: 뷰에 `SELECT *`를 쓰면 원본에 컬럼이 추가될 때 의도치 않게 노출됩니다. 컬럼을 명시하세요.
- **힌트**: PG의 보안 정석은 `CREATE VIEW ... WITH (security_barrier = true)`입니다. 뷰 소유자 권한으로 실행되는 특성도 함께 확인하세요.
- **셀프 테스트**
  - `SET ROLE marketing_role; SELECT * FROM olist.customers;` → permission denied
  - `SELECT * FROM v_customers_masked LIMIT 5;` → 성공

### [OLIST-102] VIP 고객 뷰 `v_vip_customers`

- **요구사항**: 누적 결제액 기준 이상인 `customer_unique_id`를 노출합니다. 원 기준(`$500`)이지만 데이터는 브라질 레알(BRL)이므로 기준을 BRL 500으로 명시하고 가정을 주석에 남기세요.
- **제약사항**
  - 집계 단위는 `customer_id`가 아니라 `customer_unique_id`입니다.
  - 취소(`canceled`)와 `unavailable` 주문은 제외합니다.
  - 결제는 분할 결제 때문에 `order_payments`에 여러 행이 있습니다. `order_items`와 먼저 JOIN하면 금액이 부풀려집니다(fan-out). 주문 단위로 먼저 집계한 뒤 JOIN하세요.
- **셀프 테스트**: 다음 두 값의 총합이 일치해야 합니다.
  - `SELECT sum(payment_value) FROM olist.order_payments;`
  - 같은 값을 주문 단위 CTE로 합산한 결과

### [OLIST-103] Updatable View 제약 검증

- **요구사항**: 단순 뷰 `v_sp_customers`(`state='SP'`)를 만들고 이 뷰를 통한 `UPDATE`/`INSERT` 동작을 검증합니다.
- **미션**
  1. `UPDATE v_sp_customers SET state='RJ' ...`를 실행하면 뷰 조건에서 벗어난 행이 사라집니다. 이를 재현합니다.
  2. `WITH CHECK OPTION`을 추가한 뒤 같은 UPDATE가 어떻게 막히는지 확인합니다.
  3. `DISTINCT`, `GROUP BY`, 집계를 넣은 뷰(`v_vip_customers`)에 UPDATE가 왜 실패하는지 에러 메시지를 설명합니다.
  4. 해결책으로 `INSTEAD OF` 트리거를 설계합니다(구현은 Phase 4 이후 선택 과제).
- **셀프 테스트**: 단계별 에러 메시지를 캡처하고 "왜 막히는지"를 한 문장으로 적습니다. 설명할 수 있어야 통과입니다.

---

## Phase 2. 시나리오 B 전반부: 느린 통계 쿼리 만들기

### [OLIST-201] 월별 카테고리별 매출 쿼리 작성

- **요구사항**: `orders → order_items → products → order_payments`를 JOIN해 월별, 카테고리별 매출과 주문 수를 구합니다.
- **제약사항**
  - 카테고리명은 `product_category_name_translation`을 JOIN해 영어명으로 변환합니다(데이터셋에 포함된 6번째 테이블).
  - 매출 정의를 하나로 고정합니다. 권장은 `order_items.price` 합계이며 `payment_value`와 섞지 않습니다.
  - 월 기준은 `order_purchase_timestamp`이며 `date_trunc('month', ...)`를 사용합니다.
  - 이 쿼리는 일부러 인덱스 없이 먼저 실행해서 느린 기준선(baseline)을 만듭니다.
- **셀프 테스트**: `EXPLAIN (ANALYZE, BUFFERS)` 결과에서 실행 시간과 `Seq Scan` 대상 테이블 목록을 기록합니다.

### [OLIST-202] 일반 뷰로 감싸기

- **요구사항**: 201 쿼리를 `v_monthly_category_sales`로 만들고 대시보드 호출을 시뮬레이션합니다(같은 뷰를 10회 반복 조회).
- **미션**: 뷰는 저장된 쿼리일 뿐이라는 점을 실행 계획으로 확인합니다. 매번 전체 JOIN이 재실행되어야 합니다.

---

## Phase 3. 시나리오 B 후반부: 최적화

### [OLIST-301] 인덱스 튜닝

- **요구사항**: 실행 계획에서 병목이 된 JOIN/필터 컬럼에 인덱스를 추가하고 전후를 비교합니다.
- **제약사항**
  - 인덱스는 한 번에 하나씩 추가하고 매번 `EXPLAIN ANALYZE`를 기록합니다. 한꺼번에 추가하면 효과를 구분할 수 없습니다.
  - FK 컬럼(`order_items.order_id`, `order_items.product_id`)이 우선 후보입니다.
  - 인덱스를 만들었는데 안 쓰이는 경우가 있습니다. 이유(통계, 카디널리티, 함수 적용된 컬럼)를 분석해 기록합니다.
- **힌트**

```sql
CREATE INDEX idx_oi_order ON olist.order_items(order_id);
ANALYZE olist.order_items;
```

### [OLIST-302] Materialized View 전환

- **요구사항**: `mv_monthly_category_sales`를 만들고 201/202 대비 속도를 비교합니다.
- **제약사항**
  - `REFRESH MATERIALIZED VIEW CONCURRENTLY`를 쓰려면 MV에 UNIQUE 인덱스가 필요합니다. 이 인덱스를 설계해야 합니다.
  - MV는 자동 갱신되지 않습니다. 갱신 전략(수동, cron, 트리거)을 문서로 정하고 장단점을 3줄로 적습니다.
- **셀프 테스트**: 비교표를 작성합니다.

| 방식 | 실행 시간 | Seq Scan 수 | 데이터 신선도 |
|---|---|---|---|
| 일반 뷰 | | | |
| 인덱스 튜닝 후 | | | |
| MV | | | |

### [OLIST-303] 집계 테이블 방식 (심화)

- **요구사항**: `agg_monthly_category_sales` 일반 테이블을 만들고 증분 갱신 프로시저를 작성합니다.
- **미션**: 신규 주문 1건을 INSERT한 뒤 집계 테이블에 반영되는 흐름을 검증합니다. MV 전체 재계산과 증분 갱신의 비용 차이를 논하세요.

---

## Phase 4. 시나리오 C: 트리거와 프로시저

### [OLIST-401] 배송 완료 타임스탬프 자동 기록 트리거

- **요구사항**: `orders.order_status`가 `'delivered'`로 바뀔 때 `order_delivered_customer_date`가 NULL이면 `now()`를 자동 기록합니다.
- **제약사항**
  - `BEFORE UPDATE` 트리거로 구현합니다. 값 보정은 `NEW`를 수정해서 하며, `AFTER`에서 다시 UPDATE하면 무한 루프 위험이 있습니다.
  - 상태가 바뀌었을 때만 동작해야 합니다. `WHEN (OLD.order_status IS DISTINCT FROM NEW.order_status)` 조건을 사용하세요.
  - 이미 값이 있으면 덮어쓰지 않습니다.
  - (보너스) 허용되지 않는 상태 전이(예: `delivered → created`)는 `RAISE EXCEPTION`으로 막습니다.
- **힌트**

```sql
CREATE FUNCTION olist.fn_set_delivered_ts() RETURNS trigger AS $$
BEGIN
  IF NEW.order_status = 'delivered' AND NEW.order_delivered_customer_date IS NULL THEN
    NEW.order_delivered_customer_date := now();
  END IF;
  RETURN NEW;
END; $$ LANGUAGE plpgsql;
```

- **셀프 테스트**
  - `shipped` 상태 주문을 `delivered`로 UPDATE하면 타임스탬프가 채워져야 합니다.
  - 이미 타임스탬프가 있는 주문은 값이 유지되어야 합니다.
  - 다른 컬럼만 UPDATE했을 때는 트리거가 동작하지 않아야 합니다.

### [OLIST-402] 휴면 회원 이관 프로시저 `sp_archive_dormant_customers(p_months int)`

- **요구사항**: 마지막 주문이 `p_months`개월 이전인 고객을 `customers_archive`로 이관하고 원본에서 삭제합니다.
- **제약사항**
  - 이 데이터셋의 `orders`가 `customers`를 FK로 참조하므로 그냥 DELETE하면 FK 위반이 납니다. 다음 중 하나를 선택하고 이유를 기록합니다.
    - (a) 주문 이력은 보존하고 고객만 익명화합니다.
    - (b) 관련 주문까지 함께 아카이빙합니다.
    - (c) FK를 `ON DELETE` 정책으로 재설계합니다.
  - 이관과 삭제는 한 트랜잭션에서 처리하며, 도중 실패 시 전체가 롤백되어야 합니다.
  - `BEGIN ... EXCEPTION WHEN OTHERS THEN` 블록으로 예외를 잡아 에러 로그 테이블에 기록한 뒤 `RAISE`로 다시 던집니다. 예외를 삼키지 마세요.
  - 대량 데이터는 배치 단위(예: 5,000건)로 나눠 처리하고 락 시간을 최소화합니다.
  - 프로시저 안에서 `COMMIT`이 필요하면 `CALL`로 호출해야 하며, 함수로는 안 됩니다.
- **셀프 테스트**
  1. 이관 전 `count(*)`를 기록합니다.
  2. 호출 후 `원본 감소 건수 = archive 증가 건수`여야 합니다.
  3. 중간에 의도적으로 에러를 일으킵니다(archive 테이블에 CHECK 위반 유도). 롤백되어 양쪽 건수가 변하지 않아야 합니다.
  4. 같은 프로시저를 2회 연속 호출해도 중복 이관이 없어야 합니다(멱등성).

---

## 최종 점검 체크리스트

- [ ] 권한 분리: `marketing_role`로 원본 접근이 차단된다.
- [ ] VIP 금액이 주문 단위 집계로 정확하다(fan-out 없음).
- [ ] `WITH CHECK OPTION` 동작을 설명할 수 있다.
- [ ] 일반 뷰, 인덱스, MV 비교표를 완성했다.
- [ ] 트리거가 상태 변경 시에만 동작하고 기존 값을 덮어쓰지 않는다.
- [ ] 프로시저가 롤백과 멱등성을 모두 통과한다.

## 진행 방식

Phase 0 → 101 순서로 하나씩 진행하고, 작성한 SQL과 `EXPLAIN` 결과를 티켓 단위로 리뷰받습니다.
