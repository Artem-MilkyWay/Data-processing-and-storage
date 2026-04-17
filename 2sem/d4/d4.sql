-- =========================================
-- 1. Анализ цен по сегментам
-- =========================================

SELECT
    s.flight_id,
    s.fare_conditions,
    COUNT(*) AS bookings_count,
    AVG(s.price) AS avg_price
FROM bookings.segments s
GROUP BY s.flight_id, s.fare_conditions;


-- =========================================
-- 2. Восстановление цен с учетом маршрута
-- =========================================

SELECT
    r.departure_airport,
    r.arrival_airport,
    s.fare_conditions,
    EXTRACT(DOW FROM f.scheduled_departure) AS day_of_week,
    COUNT(*) AS bookings_count,
    AVG(s.price) AS avg_price
FROM bookings.segments s
JOIN bookings.flights f ON s.flight_id = f.flight_id
JOIN bookings.routes r ON f.route_no = r.route_no
GROUP BY
    r.departure_airport,
    r.arrival_airport,
    s.fare_conditions,
    day_of_week;


-- =========================================
-- 3. Создание таблицы правил ценообразования
-- =========================================

DROP TABLE IF EXISTS pricing_rules;

CREATE TABLE pricing_rules AS
SELECT
    r.departure_airport,
    r.arrival_airport,
    s.fare_conditions,
    EXTRACT(DOW FROM f.scheduled_departure) AS day_of_week,
    COUNT(*) AS demand,
    AVG(s.price) AS base_price
FROM bookings.segments s
JOIN bookings.flights f ON s.flight_id = f.flight_id
JOIN bookings.routes r ON f.route_no = r.route_no
GROUP BY
    r.departure_airport,
    r.arrival_airport,
    s.fare_conditions,
    day_of_week;


-- =========================================
-- 4. Просмотр таблицы правил
-- =========================================

SELECT *
FROM pricing_rules
ORDER BY departure_airport, arrival_airport, fare_conditions;


-- =========================================
-- 5. Динамическое ценообразование
-- =========================================

SELECT
    departure_airport,
    arrival_airport,
    fare_conditions,
    day_of_week,
    base_price,
    demand,
    CASE
        WHEN demand > 100 THEN base_price * 1.2
        WHEN demand > 50 THEN base_price * 1.1
        ELSE base_price
    END AS final_price
FROM pricing_rules;


-- =========================================
-- 6. Применение к будущим рейсам
-- =========================================

SELECT
    f.flight_id,
    r.departure_airport,
    r.arrival_airport,
    f.scheduled_departure,
    pr.fare_conditions,
    pr.base_price
FROM bookings.flights f
JOIN bookings.routes r ON f.route_no = r.route_no
JOIN pricing_rules pr
  ON r.departure_airport = pr.departure_airport
 AND r.arrival_airport = pr.arrival_airport
 AND pr.fare_conditions = 'Economy'
 AND pr.day_of_week = EXTRACT(DOW FROM f.scheduled_departure);