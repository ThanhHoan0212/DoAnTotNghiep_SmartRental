-- ==============================================================================
-- Flyway Migration V4: Chuyen kieu du lieu price va area sang DOUBLE PRECISION
-- de tuong thich hoan toan voi kieu Java Double trong Hibernate 6
-- ==============================================================================

ALTER TABLE rooms ALTER COLUMN price TYPE DOUBLE PRECISION;
ALTER TABLE rooms ALTER COLUMN area TYPE DOUBLE PRECISION;
