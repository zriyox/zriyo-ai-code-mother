/*
 Source Schema         : zriyo_user_db
 Target Server Type    : MySQL 8.0
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 用户基础表
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `userAccount` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '账号',
  `userPassword` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码',
  `userName` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户昵称',
  `userAvatar` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户头像',
  `userProfile` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户简介',
  `userRole` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'user' COMMENT '用户角色：user/admin',
  `editTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
  `authing_sub` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_userAccount` (`userAccount`),
  KEY `idx_userName` (`userName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户';

-- ----------------------------
-- 2. 用户积分账户表
-- ----------------------------
DROP TABLE IF EXISTS `user_points`;
CREATE TABLE `user_points` (
  `user_id` bigint NOT NULL COMMENT '用户ID，逻辑外键，关联 users.id',
  `total_points` int NOT NULL DEFAULT '0' COMMENT '累计获得总积分（不可减少）',
  `available_points` int NOT NULL DEFAULT '0' COMMENT '当前可用积分（可用于兑换或扣减）',
  `used_points` int NOT NULL DEFAULT '0' COMMENT '已使用/已消耗积分（total - available = used）',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号，用于防止并发更新覆盖',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户积分账户（逻辑外键 + 乐观锁）';

-- ----------------------------
-- 3. 积分变动日志表
-- ----------------------------
DROP TABLE IF EXISTS `points_log`;
CREATE TABLE `points_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID，逻辑外键，关联 users.id',
  `change_amount` int NOT NULL COMMENT '本次积分变动值（正数为增加，负数为扣除）',
  `balance_after` int NOT NULL COMMENT '本次变动后的可用积分余额',
  `reason` varchar(50) NOT NULL COMMENT '变动原因编码',
  `related_id` varchar(64) DEFAULT NULL COMMENT '关联业务ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='积分变动流水日志';

-- ----------------------------
-- 4. 积分兑换码表
-- ----------------------------
DROP TABLE IF EXISTS `points_code`;
CREATE TABLE `points_code` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` varchar(32) NOT NULL COMMENT '积分兑换码',
  `points` int NOT NULL DEFAULT '0' COMMENT '该码可兑换的积分数',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '使用状态：0=未使用，1=已使用，2=已过期',
  `used_by_user_id` bigint DEFAULT NULL COMMENT '兑换用户ID',
  `used_at` datetime DEFAULT NULL COMMENT '实际兑换时间',
  `expired_at` datetime NOT NULL COMMENT '过期时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建者用户ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='积分兑换码管理表';

-- ----------------------------
-- 5. 用户签到表
-- ----------------------------
DROP TABLE IF EXISTS `user_sign_in`;
CREATE TABLE `user_sign_in` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `sign_date` date NOT NULL COMMENT '签到日期',
  `continuous_days` int NOT NULL DEFAULT '1' COMMENT '连续签到天数',
  `reward_points` int NOT NULL DEFAULT '0' COMMENT '本次签到获得的积分数',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户签到历史记录';

SET FOREIGN_KEY_CHECKS = 1;
