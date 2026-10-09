-- V3: Create short_code_seq sequence for Base62 short code generation
-- Compatible with H2 2.x and PostgreSQL
CREATE SEQUENCE IF NOT EXISTS short_code_seq START WITH 1 INCREMENT BY 1;
