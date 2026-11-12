# HostelSync Database Schema Migration (MySQL 8.0 -> TiDB Cloud)

## Overview
- **Artifact:** `deployment/mysql/hostelsync_schema_tidb.sql`
- **Source:** Schema-only DDL export from local MySQL `8.0.46` (`hostelsync` database)
- **Contents:** **23** tables (including primary keys, unique keys, secondary indexes, foreign keys, `ENUM` types, and `CHECK` constraints), **0** views, **0** triggers, **0** stored routines, **0** events, and **0** application data rows.

---

## Transformations Applied for TiDB Cloud Compatibility
1. **Collation portability:** Replaced MySQL 8 default `COLLATE=utf8mb4_0900_ai_ci` with `COLLATE=utf8mb4_unicode_ci` across all 23 tables for universal compatibility with TiDB Cloud Starter and MySQL 8.
2. **Local counter cleanup:** Removed local `AUTO_INCREMENT=<n>` table offsets so IDs start cleanly in a fresh target database.
3. **Safe idempotent table creation:** Converted `DROP TABLE IF EXISTS` + `CREATE TABLE` into `CREATE TABLE IF NOT EXISTS` wrapped in `SET FOREIGN_KEY_CHECKS = 0;` / `1;` so importing into a new database is non-destructive and order-independent.
4. **Dump boilerplate stripped:** Removed `localhost` dump comments, `/*!50503 ... */` versioned client character-set directives, and `GTID` state commands.
5. **No credentials or seed records:** Contains DDL only; no users, students, wardens, DGMs, complaints, or notices are included.

---

## TiDB Compatibility Notes
- **Foreign Keys:** TiDB v6.6+ / TiDB Cloud Starter supports foreign key constraints (and `SET FOREIGN_KEY_CHECKS = 0` allows creation in any table order).
- **CHECK Constraints:** DDL includes MySQL 8 `CHECK` constraints (`rooms`, `sla_configurations`, `students`, `survey_submissions`). Depending on the TiDB cluster `tidb_enable_check_constraint` variable, TiDB may parse `CHECK` constraints without enforcing them at storage level; HostelSync's Spring Boot Jakarta Validation annotations already enforce these bounds in application code.
- **AUTO_INCREMENT:** TiDB allocates `AUTO_INCREMENT` IDs in distributed batches (guaranteed unique and monotonic, not necessarily gap-free), which is compatible with JPA `GenerationType.IDENTITY`.
- **Hibernate Production Validation:** `backend/src/main/resources/application-prod.properties` uses `spring.jpa.hibernate.ddl-auto=validate`. Always apply `hostelsync_schema_tidb.sql` to the target `hostelsync` database before starting the production Spring Boot service.
- **Note on `.gitignore`:** Root `.gitignore` currently ignores `*.sql`. The clean SQL artifact is stored locally at `deployment/mysql/hostelsync_schema_tidb.sql` (and the raw mysqldump copy is stored outside the repo in your backup directory).

---

## Applying to a Target Empty Database (When Ready in Phase 5)
1. Create the empty database in TiDB Cloud (e.g., `CREATE DATABASE IF NOT EXISTS hostelsync CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`).
2. Import `hostelsync_schema_tidb.sql` using the MySQL CLI with TLS against `hostelsync`.
3. Verify `SHOW TABLES;` returns all **23** tables before booting the Render backend with `SPRING_PROFILES_ACTIVE=prod`.

