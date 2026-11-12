-- ============================================================================
-- HostelSync Schema-Only DDL Migration (MySQL 8.0 -> TiDB Cloud Compatible)
-- Generated from local MySQL 8.0.46 `hostelsync` schema (DDL only, 0 data rows)
-- Tables: 23 | Views: 0 | Triggers: 0 | Routines: 0 | Events: 0
-- ============================================================================

SET NAMES utf8mb4;
SET TIME_ZONE = '+00:00';
SET FOREIGN_KEY_CHECKS = 0;

-- Table: `audit_logs`
CREATE TABLE IF NOT EXISTS `audit_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `action` varchar(100) NOT NULL,
  `action_timestamp` datetime(6) NOT NULL,
  `client_info` varchar(200) DEFAULT NULL,
  `entity_id` bigint NOT NULL,
  `entity_type` varchar(50) NOT NULL,
  `new_value` text,
  `previous_value` text,
  `reason` text,
  `user_id` bigint NOT NULL,
  `user_name` varchar(150) NOT NULL,
  `user_role` varchar(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `IDXg331fk4jerbycwntt412hxmev` (`entity_type`,`entity_id`),
  KEY `IDX415k17h694o8vs7gxeedrkhhj` (`action_timestamp`),
  KEY `IDX9n7hgmrobetlx9wpvbmu21acj` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: `complaint_assignments`
CREATE TABLE IF NOT EXISTS `complaint_assignments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `assigned_at` datetime(6) NOT NULL,
  `assigned_by_id` bigint NOT NULL,
  `reason` text,
  `assigned_warden_id` bigint NOT NULL,
  `complaint_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `IDX9tfxyjrhh63n8m76codbm2bka` (`complaint_id`),
  KEY `IDX6dridyrq4i4xbtispx9by2u0w` (`assigned_warden_id`),
  KEY `IDXr2chdgl6hitl94oa4q3vctx7w` (`assigned_at`),
  CONSTRAINT `FK8v2qmkgwtk3y1f0tqfl78kd5q` FOREIGN KEY (`assigned_warden_id`) REFERENCES `wardens` (`id`),
  CONSTRAINT `FKixdogb4e3kdkbmwxsyo8viuiq` FOREIGN KEY (`complaint_id`) REFERENCES `complaints` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: `complaint_attachments`
CREATE TABLE IF NOT EXISTS `complaint_attachments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content_type` varchar(100) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `file_size` bigint DEFAULT NULL,
  `original_file_name` varchar(255) NOT NULL,
  `stored_file_name` varchar(255) NOT NULL,
  `complaint_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKcu6acsjbmvd1iokn2ubdwn46i` (`stored_file_name`),
  KEY `FKa5olbvh24g2xtkoxul46txypl` (`complaint_id`),
  CONSTRAINT `FKa5olbvh24g2xtkoxul46txypl` FOREIGN KEY (`complaint_id`) REFERENCES `complaints` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: `complaint_escalations`
CREATE TABLE IF NOT EXISTS `complaint_escalations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dgm_response` text,
  `escalated_at` datetime(6) NOT NULL,
  `handled_at` datetime(6) DEFAULT NULL,
  `handled_by_dgm_id` bigint DEFAULT NULL,
  `reason` text NOT NULL,
  `status` enum('IN_REVIEW','PENDING','REJECTED','RESOLVED') NOT NULL,
  `supporting_details` text,
  `complaint_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `IDXq4chhdvt9hy8h5blc5elcjn9e` (`complaint_id`),
  KEY `IDXfn5mvyfp0nkcj8t3hfdjbk0l8` (`escalated_at`),
  KEY `FKsx5qrptvw07es5is41j4mdqbq` (`student_id`),
  CONSTRAINT `FKhapt309kydo5y32ny8xij7uhe` FOREIGN KEY (`complaint_id`) REFERENCES `complaints` (`id`),
  CONSTRAINT `FKsx5qrptvw07es5is41j4mdqbq` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: `complaint_timeline`
CREATE TABLE IF NOT EXISTS `complaint_timeline` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `note` text,
  `status` enum('In_Progress','Overdue','Rejected','Resolved','Submitted','Under_Review') NOT NULL,
  `timestamp` datetime(6) NOT NULL,
  `complaint_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK407lpqq2obh9rmuiq6rs6w9vg` (`complaint_id`),
  CONSTRAINT `FK407lpqq2obh9rmuiq6rs6w9vg` FOREIGN KEY (`complaint_id`) REFERENCES `complaints` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: `complaints`
CREATE TABLE IF NOT EXISTS `complaints` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `admin_notes` text,
  `complaint_number` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` text NOT NULL,
  `location` varchar(300) NOT NULL,
  `priority` enum('High','Low','Medium','Urgent') NOT NULL,
  `resolved_at` datetime(6) DEFAULT NULL,
  `status` enum('In_Progress','Overdue','Rejected','Resolved','Submitted','Under_Review') NOT NULL,
  `subject` varchar(300) NOT NULL,
  `type` enum('Cleaning','Electricity','Mess','Other','Plumbing','Room_Maintenance','Water') NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `student_id` bigint NOT NULL,
  `escalated` bit(1) NOT NULL,
  `escalated_at` datetime(6) DEFAULT NULL,
  `overdue_at` datetime(6) DEFAULT NULL,
  `reminder_sent_at` datetime(6) DEFAULT NULL,
  `reopen_count` int NOT NULL,
  `reopened` bit(1) NOT NULL,
  `reopened_at` datetime(6) DEFAULT NULL,
  `resolution_notes` text,
  `sla_deadline` datetime(6) DEFAULT NULL,
  `assigned_warden_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK348qxg8v14l5t9lp4a9sxxyt6` (`complaint_number`),
  KEY `FKh1cahj0r1cwkhoc2evv4y83el` (`student_id`),
  KEY `FKscrmbb4js50q3chwwy8i77vx4` (`assigned_warden_id`),
  CONSTRAINT `FKh1cahj0r1cwkhoc2evv4y83el` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`),
  CONSTRAINT `FKscrmbb4js50q3chwwy8i77vx4` FOREIGN KEY (`assigned_warden_id`) REFERENCES `wardens` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: `dgms`
CREATE TABLE IF NOT EXISTS `dgms` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `contact_extension` varchar(20) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `designation` varchar(150) DEFAULT NULL,
  `employee_id` varchar(50) DEFAULT NULL,
  `office_location` varchar(150) DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKlto3regpg6hgt31rg3qfh1nyi` (`user_id`),
  CONSTRAINT `FKqqrecamcvbgnd6sy5xv1ra6v6` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: `feedback_tasks`
CREATE TABLE IF NOT EXISTS `feedback_tasks` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category` varchar(50) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` varchar(255) NOT NULL,
  `description` text NOT NULL,
  `priority` varchar(20) DEFAULT NULL,
  `resolution_notes` text,
  `resolved_at` datetime(6) DEFAULT NULL,
  `status` enum('IN_PROGRESS','PENDING','RESOLVED') NOT NULL,
  `title` varchar(200) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `hostel_id` bigint NOT NULL,
  `survey_id` bigint DEFAULT NULL,
  `warden_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKjhjijc994q4q6cdg4h8q5bjio` (`hostel_id`),
  KEY `FKdc5kcf98irdy7clnvq22886rs` (`survey_id`),
  KEY `FK7jrprxquh43wht1tr241u715k` (`warden_id`),
  CONSTRAINT `FK7jrprxquh43wht1tr241u715k` FOREIGN KEY (`warden_id`) REFERENCES `wardens` (`id`),
  CONSTRAINT `FKdc5kcf98irdy7clnvq22886rs` FOREIGN KEY (`survey_id`) REFERENCES `surveys` (`id`),
  CONSTRAINT `FKjhjijc994q4q6cdg4h8q5bjio` FOREIGN KEY (`hostel_id`) REFERENCES `hostels` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: `hostel_dgm_assignments`
CREATE TABLE IF NOT EXISTS `hostel_dgm_assignments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `assigned_at` datetime(6) NOT NULL,
  `assigned_by` varchar(255) NOT NULL,
  `dgm_id` bigint NOT NULL,
  `hostel_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK8esxvwynno0cmrm8k9hvl8i81` (`hostel_id`,`dgm_id`),
  KEY `FKa164p7x0jx800vkdew1nbu3pk` (`dgm_id`),
  CONSTRAINT `FK15cq1xw7bsrt9j10nxqkboc3h` FOREIGN KEY (`hostel_id`) REFERENCES `hostels` (`id`),
  CONSTRAINT `FKa164p7x0jx800vkdew1nbu3pk` FOREIGN KEY (`dgm_id`) REFERENCES `dgms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: `hostels`
CREATE TABLE IF NOT EXISTS `hostels` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `address` varchar(300) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `name` varchar(150) NOT NULL,
  `active` bit(1) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: `notices`
CREATE TABLE IF NOT EXISTS `notices` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `author_name` varchar(255) NOT NULL,
  `author_role` varchar(255) NOT NULL,
  `category` enum('EMERGENCY','EVENT','GENERAL','MAINTENANCE','MESS','SECURITY') NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) DEFAULT NULL,
  `message` text NOT NULL,
  `priority` enum('HIGH','LOW','MEDIUM','URGENT') NOT NULL,
  `title` varchar(200) NOT NULL,
  `hostel_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKspuarls3ixuxpeh017k5wqbpu` (`hostel_id`),
  CONSTRAINT `FKspuarls3ixuxpeh017k5wqbpu` FOREIGN KEY (`hostel_id`) REFERENCES `hostels` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: `notifications`
CREATE TABLE IF NOT EXISTS `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `message` text NOT NULL,
  `read_at` datetime(6) DEFAULT NULL,
  `related_entity_id` bigint DEFAULT NULL,
  `related_entity_type` varchar(50) DEFAULT NULL,
  `type` enum('COMPLAINT_APPROACHING_DEADLINE','COMPLAINT_ASSIGNED','COMPLAINT_ESCALATED','COMPLAINT_OVERDUE','COMPLAINT_REASSIGNED','COMPLAINT_REOPENED','COMPLAINT_RESOLVED','COMPLAINT_STATUS_CHANGED','DGM_ACTION_TAKEN','ROOM_ALLOCATION_CHANGED','SLA_REMINDER','SYSTEM_ANNOUNCEMENT') NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `IDXmw5q27cokspshaftu6oi178q6` (`user_id`,`read_at`),
  KEY `IDXjxjfmj284tk4uw3xfdp6d8210` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: `room_allocation_history`
CREATE TABLE IF NOT EXISTS `room_allocation_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `action` enum('ASSIGNED','CANCELLED','DEPARTED','TRANSFERRED') NOT NULL,
  `action_timestamp` datetime(6) NOT NULL,
  `performed_by_id` bigint NOT NULL,
  `reason` text,
  `room_id` bigint DEFAULT NULL,
  `student_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `IDXrbecqn47ibc6b4aasnuic92xn` (`student_id`),
  KEY `IDXs9y7agewbinovrqaomevjpujm` (`room_id`),
  KEY `IDXlvgi4yv7qcf6rjo2m6q0jvuu0` (`action_timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: `room_furniture`
CREATE TABLE IF NOT EXISTS `room_furniture` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `damaged_count` int NOT NULL,
  `furniture_type` enum('BED','LOCKER','STUDY_TABLE') NOT NULL,
  `missing_count` int NOT NULL,
  `serviceable_count` int NOT NULL,
  `under_repair_count` int NOT NULL,
  `room_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK3ketf3suo9320c6603b2xmvdr` (`room_id`,`furniture_type`),
  CONSTRAINT `FKpujrqrt8ce5ca6w3onaxk9rfc` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: `room_furniture_history`
CREATE TABLE IF NOT EXISTS `room_furniture_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `action_type` enum('CAPACITY_ADJUSTED','DAMAGE_REPORTED','INITIAL_SETUP','INVENTORY_AUDIT','REPAIRED','REPLACED') NOT NULL,
  `furniture_type` enum('BED','LOCKER','STUDY_TABLE') NOT NULL,
  `notes` varchar(500) DEFAULT NULL,
  `quantity` int NOT NULL,
  `recorded_at` datetime(6) NOT NULL,
  `recorded_by` varchar(255) NOT NULL,
  `room_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKm46ann4kid1p9cdg8r4cxnxt` (`room_id`),
  CONSTRAINT `FKm46ann4kid1p9cdg8r4cxnxt` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: `rooms`
CREATE TABLE IF NOT EXISTS `rooms` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `block` varchar(50) NOT NULL,
  `capacity` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `floor` int NOT NULL,
  `room_number` varchar(20) NOT NULL,
  `hostel_id` bigint NOT NULL,
  `cooling_equipment` enum('AC','COOLER','NONE') NOT NULL,
  `cooling_maintenance_status` enum('MAINTENANCE_REQUIRED','OPERATIONAL','REPLACEMENT_NEEDED') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKh5btkgvsab2ftimf52ggcsm6t` (`room_number`,`hostel_id`),
  KEY `FKtain3h0alg45872odwm85r9kb` (`hostel_id`),
  CONSTRAINT `FKtain3h0alg45872odwm85r9kb` FOREIGN KEY (`hostel_id`) REFERENCES `hostels` (`id`),
  CONSTRAINT `rooms_chk_1` CHECK (((`capacity` <= 20) and (`capacity` >= 1))),
  CONSTRAINT `rooms_chk_2` CHECK ((`floor` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: `sla_configurations`
CREATE TABLE IF NOT EXISTS `sla_configurations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `deadline_hours` int NOT NULL,
  `priority` enum('High','Low','Medium','Urgent') NOT NULL,
  `reminder_threshold_percent` int NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKado3paq2l1d4peo44sb50bo1t` (`priority`),
  CONSTRAINT `sla_configurations_chk_1` CHECK ((`deadline_hours` >= 0)),
  CONSTRAINT `sla_configurations_chk_2` CHECK ((`reminder_threshold_percent` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: `students`
CREATE TABLE IF NOT EXISTS `students` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course` varchar(150) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `gender` varchar(10) NOT NULL,
  `guardian_name` varchar(120) DEFAULT NULL,
  `guardian_phone` varchar(20) DEFAULT NULL,
  `phone` varchar(20) NOT NULL,
  `status` enum('Active','Inactive','Suspended') NOT NULL,
  `student_id` varchar(50) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `year` int NOT NULL,
  `room_id` bigint DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK5mbus2m1tm2acucrp6t627jmx` (`student_id`),
  UNIQUE KEY `UKg4fwvutq09fjdlb4bb0byp7t` (`user_id`),
  KEY `FKq8l9dnbc3y02t87sh1d88408j` (`room_id`),
  CONSTRAINT `FKdt1cjx5ve5bdabmuuf3ibrwaq` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKq8l9dnbc3y02t87sh1d88408j` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `students_chk_1` CHECK (((`year` <= 6) and (`year` >= 1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: `survey_submissions`
CREATE TABLE IF NOT EXISTS `survey_submissions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `comments` varchar(500) DEFAULT NULL,
  `q10_complaint_resolution` int NOT NULL,
  `q1_cleanliness` int NOT NULL,
  `q2_washrooms` int NOT NULL,
  `q3_furniture` int NOT NULL,
  `q4_drinking_water` int NOT NULL,
  `q5_wifi` int NOT NULL,
  `q6_electricity` int NOT NULL,
  `q7_food` int NOT NULL,
  `q8_security` int NOT NULL,
  `q9_warden_support` int NOT NULL,
  `submitted_at` datetime(6) NOT NULL,
  `student_id` bigint NOT NULL,
  `survey_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKgh6o8cscbdpbwk3ufqv2o1w9h` (`survey_id`,`student_id`),
  KEY `FK5cn9spdohfqp9u2buos4btt9p` (`student_id`),
  CONSTRAINT `FK5b996avj0a9mq8h9xi54slo6a` FOREIGN KEY (`survey_id`) REFERENCES `surveys` (`id`),
  CONSTRAINT `FK5cn9spdohfqp9u2buos4btt9p` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`),
  CONSTRAINT `survey_submissions_chk_1` CHECK (((`q10_complaint_resolution` <= 5) and (`q10_complaint_resolution` >= 1))),
  CONSTRAINT `survey_submissions_chk_10` CHECK (((`q9_warden_support` <= 5) and (`q9_warden_support` >= 1))),
  CONSTRAINT `survey_submissions_chk_2` CHECK (((`q1_cleanliness` <= 5) and (`q1_cleanliness` >= 1))),
  CONSTRAINT `survey_submissions_chk_3` CHECK (((`q2_washrooms` <= 5) and (`q2_washrooms` >= 1))),
  CONSTRAINT `survey_submissions_chk_4` CHECK (((`q3_furniture` <= 5) and (`q3_furniture` >= 1))),
  CONSTRAINT `survey_submissions_chk_5` CHECK (((`q4_drinking_water` <= 5) and (`q4_drinking_water` >= 1))),
  CONSTRAINT `survey_submissions_chk_6` CHECK (((`q5_wifi` <= 5) and (`q5_wifi` >= 1))),
  CONSTRAINT `survey_submissions_chk_7` CHECK (((`q6_electricity` <= 5) and (`q6_electricity` >= 1))),
  CONSTRAINT `survey_submissions_chk_8` CHECK (((`q7_food` <= 5) and (`q7_food` >= 1))),
  CONSTRAINT `survey_submissions_chk_9` CHECK (((`q8_security` <= 5) and (`q8_security` >= 1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: `surveys`
CREATE TABLE IF NOT EXISTS `surveys` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `end_date` datetime(6) DEFAULT NULL,
  `start_date` datetime(6) DEFAULT NULL,
  `title` varchar(200) NOT NULL,
  `target_hostel_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKbvcsbwj1c5iawsrwky15xkv1p` (`target_hostel_id`),
  CONSTRAINT `FKbvcsbwj1c5iawsrwky15xkv1p` FOREIGN KEY (`target_hostel_id`) REFERENCES `hostels` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: `users`
CREATE TABLE IF NOT EXISTS `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `avatar_url` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(200) NOT NULL,
  `name` varchar(120) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `role` enum('DGM','STUDENT','WARDEN') NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: `warden_floor_assignments`
CREATE TABLE IF NOT EXISTS `warden_floor_assignments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `assigned_at` datetime(6) NOT NULL,
  `floor` int NOT NULL,
  `hostel_id` bigint NOT NULL,
  `warden_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKor5lxn4h04grj5cgjlimv8yva` (`warden_id`,`hostel_id`,`floor`),
  KEY `FK83i49jgsx8k9ewwndn106kmxd` (`hostel_id`),
  CONSTRAINT `FK83i49jgsx8k9ewwndn106kmxd` FOREIGN KEY (`hostel_id`) REFERENCES `hostels` (`id`),
  CONSTRAINT `FKmsma5818tgs82n3kbgpyp5xkg` FOREIGN KEY (`warden_id`) REFERENCES `wardens` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: `wardens`
CREATE TABLE IF NOT EXISTS `wardens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `employee_id` varchar(50) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `active` bit(1) NOT NULL,
  `phone` varchar(30) DEFAULT NULL,
  `hostel_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKm5sux809y59vaq0rsrn59qkae` (`user_id`),
  KEY `FKt9k668rw6ygucs84674ppv7gv` (`hostel_id`),
  CONSTRAINT `FK9ftb5slxj2d9hu30ytxqqrh4x` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKt9k668rw6ygucs84674ppv7gv` FOREIGN KEY (`hostel_id`) REFERENCES `hostels` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
