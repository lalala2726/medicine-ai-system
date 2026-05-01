/*
 Navicat Premium Dump SQL

 Source Server         : 开发服务器
 Source Server Type    : MySQL
 Source Server Version : 80403 (8.4.3)
 Source Host           : localhost:3306
 Source Schema         : medicine

 Target Server Type    : MySQL
 Target Server Version : 80403 (8.4.3)
 File Encoding         : 65001

 Date: 27/04/2026 22:18:45
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for agent_prompt_config
-- ----------------------------
DROP TABLE IF EXISTS `agent_prompt_config`;
CREATE TABLE `agent_prompt_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `prompt_key` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '提示词业务键',
  `prompt_content` longtext COLLATE utf8mb4_general_ci NOT NULL COMMENT '当前生效提示词正文',
  `prompt_version` bigint NOT NULL COMMENT '当前生效版本号',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人账号',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最后更新人账号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_prompt_config_key` (`prompt_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Agent提示词当前配置';

-- ----------------------------
-- Records of agent_prompt_config
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for agent_prompt_history
-- ----------------------------
DROP TABLE IF EXISTS `agent_prompt_history`;
CREATE TABLE `agent_prompt_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `prompt_key` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '提示词业务键',
  `prompt_version` bigint NOT NULL COMMENT '历史版本号',
  `prompt_content` longtext COLLATE utf8mb4_general_ci NOT NULL COMMENT '历史提示词正文',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人账号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_prompt_history_key_version` (`prompt_key`,`prompt_version`),
  KEY `idx_agent_prompt_history_key` (`prompt_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Agent提示词历史版本';

-- ----------------------------
-- Records of agent_prompt_history
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for agent_prompt_key
-- ----------------------------
DROP TABLE IF EXISTS `agent_prompt_key`;
CREATE TABLE `agent_prompt_key` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `prompt_key` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '提示词业务键',
  `description` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '提示词用途说明',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人账号',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最后更新人账号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_prompt_key` (`prompt_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Agent提示词业务键配置';

-- ----------------------------
-- Records of agent_prompt_key
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for conversation
-- ----------------------------
DROP TABLE IF EXISTS `conversation`;
CREATE TABLE `conversation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `uuid` char(36) NOT NULL COMMENT '会话UUID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(256) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(256) DEFAULT NULL COMMENT '更新人',
  `delete_time` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of conversation
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for coupon_activation_batch
-- ----------------------------
DROP TABLE IF EXISTS `coupon_activation_batch`;
CREATE TABLE `coupon_activation_batch` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '激活码批次ID',
  `batch_no` varchar(64) NOT NULL COMMENT '批次号',
  `template_id` bigint NOT NULL COMMENT '优惠券模板ID',
  `redeem_rule_type` varchar(32) NOT NULL COMMENT '兑换规则类型（SHARED_PER_USER_ONCE-每用户一次共享码，UNIQUE_SINGLE_USE-全局一次唯一码）',
  `validity_type` varchar(32) NOT NULL COMMENT '有效期类型（ONCE-一次性，AFTER_ACTIVATION-激活后计算）',
  `fixed_effective_time` datetime DEFAULT NULL COMMENT '固定生效时间',
  `fixed_expire_time` datetime DEFAULT NULL COMMENT '固定失效时间',
  `relative_valid_days` int DEFAULT NULL COMMENT '激活后有效天数',
  `status` varchar(32) NOT NULL COMMENT '批次状态',
  `generate_count` int NOT NULL COMMENT '生成数量',
  `success_use_count` int NOT NULL DEFAULT '0' COMMENT '成功使用次数',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_coupon_activation_batch_no` (`batch_no`),
  KEY `idx_coupon_activation_batch_template_id` (`template_id`),
  KEY `idx_coupon_activation_batch_rule_type` (`redeem_rule_type`),
  KEY `idx_coupon_activation_batch_status` (`status`),
  KEY `idx_coupon_activation_batch_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='优惠券激活码批次表';

-- ----------------------------
-- Records of coupon_activation_batch
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for coupon_activation_code
-- ----------------------------
DROP TABLE IF EXISTS `coupon_activation_code`;
CREATE TABLE `coupon_activation_code` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '激活码ID',
  `batch_id` bigint NOT NULL COMMENT '激活码批次ID',
  `code_hash` varchar(128) NOT NULL COMMENT '激活码哈希值',
  `plain_code` varchar(64) NOT NULL COMMENT '激活码明文',
  `status` varchar(32) NOT NULL COMMENT '激活码状态',
  `success_use_count` int NOT NULL DEFAULT '0' COMMENT '成功使用次数',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_coupon_activation_code_hash` (`code_hash`),
  UNIQUE KEY `uk_coupon_activation_code_plain` (`plain_code`),
  KEY `idx_coupon_activation_code_batch_id` (`batch_id`),
  KEY `idx_coupon_activation_code_batch_status` (`batch_id`,`status`),
  KEY `idx_coupon_activation_code_status` (`status`),
  KEY `idx_coupon_activation_code_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='优惠券激活码明细表';

-- ----------------------------
-- Records of coupon_activation_code
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for coupon_activation_grant_log
-- ----------------------------
DROP TABLE IF EXISTS `coupon_activation_grant_log`;
CREATE TABLE `coupon_activation_grant_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '激活码发券日志ID',
  `redeem_log_id` bigint DEFAULT NULL COMMENT '兑换日志ID',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `activation_code_id` bigint DEFAULT NULL COMMENT '激活码ID',
  `template_id` bigint DEFAULT NULL COMMENT '优惠券模板ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `coupon_id` bigint DEFAULT NULL COMMENT '用户优惠券ID',
  `source_type` varchar(64) DEFAULT NULL COMMENT '发券来源类型',
  `source_biz_no` varchar(128) DEFAULT NULL COMMENT '发券来源业务号',
  `grant_mode` varchar(32) NOT NULL COMMENT '发券方式',
  `grant_status` varchar(32) NOT NULL COMMENT '发券状态',
  `grant_error_code` varchar(64) DEFAULT NULL COMMENT '发券错误编码',
  `grant_error_message` varchar(255) DEFAULT NULL COMMENT '发券错误信息',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_coupon_activation_grant_log_redeem_log_id` (`redeem_log_id`),
  KEY `idx_coupon_activation_grant_log_batch_id` (`batch_id`),
  KEY `idx_coupon_activation_grant_log_code_id` (`activation_code_id`),
  KEY `idx_coupon_activation_grant_log_user_id` (`user_id`),
  KEY `idx_coupon_activation_grant_log_coupon_id` (`coupon_id`),
  KEY `idx_coupon_activation_grant_log_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='优惠券激活码发券日志表';

-- ----------------------------
-- Records of coupon_activation_grant_log
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for coupon_activation_log
-- ----------------------------
DROP TABLE IF EXISTS `coupon_activation_log`;
CREATE TABLE `coupon_activation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '激活码兑换日志ID',
  `request_id` varchar(64) NOT NULL COMMENT '兑换请求ID',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `activation_code_id` bigint DEFAULT NULL COMMENT '激活码ID',
  `plain_code_snapshot` varchar(64) NOT NULL COMMENT '激活码明文快照',
  `redeem_rule_type` varchar(32) NOT NULL COMMENT '兑换规则类型',
  `result_status` varchar(32) NOT NULL COMMENT '结果状态',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `coupon_id` bigint DEFAULT NULL COMMENT '用户优惠券ID',
  `client_ip` varchar(64) DEFAULT NULL COMMENT '客户端IP',
  `fail_code` varchar(64) DEFAULT NULL COMMENT '失败编码',
  `fail_message` varchar(255) DEFAULT NULL COMMENT '失败信息',
  `success_lock_key` varchar(128) DEFAULT NULL COMMENT '成功占位键',
  `grant_mode` varchar(32) DEFAULT NULL COMMENT '发券方式',
  `grant_status` varchar(32) DEFAULT NULL COMMENT '发券状态',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_coupon_activation_log_success_lock_key` (`success_lock_key`),
  KEY `idx_coupon_activation_log_request_id` (`request_id`),
  KEY `idx_coupon_activation_log_batch_id` (`batch_id`),
  KEY `idx_coupon_activation_log_code_id` (`activation_code_id`),
  KEY `idx_coupon_activation_log_user_id` (`user_id`),
  KEY `idx_coupon_activation_log_rule_type` (`redeem_rule_type`),
  KEY `idx_coupon_activation_log_result_status` (`result_status`),
  KEY `idx_coupon_activation_log_create_time` (`create_time`),
  KEY `idx_coupon_activation_log_code_time` (`activation_code_id`,`create_time`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='优惠券激活码兑换日志表';

-- ----------------------------
-- Records of coupon_activation_log
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for coupon_log
-- ----------------------------
DROP TABLE IF EXISTS `coupon_log`;
CREATE TABLE `coupon_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '优惠券日志ID',
  `coupon_id` bigint NOT NULL COMMENT '用户优惠券ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `order_no` varchar(64) DEFAULT NULL COMMENT '关联订单号',
  `change_type` varchar(32) NOT NULL COMMENT '变更类型',
  `change_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '变更金额',
  `deduct_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '订单抵扣金额',
  `waste_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '浪费金额',
  `before_available_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '变更前可用金额',
  `after_available_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '变更后可用金额',
  `source_type` varchar(32) NOT NULL COMMENT '来源类型',
  `source_biz_no` varchar(64) DEFAULT NULL COMMENT '来源业务号',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `operator_id` varchar(64) DEFAULT NULL COMMENT '操作人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_coupon_log_coupon` (`coupon_id`),
  KEY `idx_coupon_log_order` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='优惠券变更日志表';

-- ----------------------------
-- Records of coupon_log
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for coupon_template
-- ----------------------------
DROP TABLE IF EXISTS `coupon_template`;
CREATE TABLE `coupon_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '优惠券模板ID',
  `coupon_type` varchar(32) NOT NULL COMMENT '优惠券类型',
  `name` varchar(128) NOT NULL COMMENT '优惠券名称',
  `threshold_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '使用门槛金额',
  `face_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '面额',
  `continue_use_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否允许继续使用：1-允许，0-不允许',
  `stackable_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否允许叠加：1-允许，0-不允许',
  `status` varchar(32) NOT NULL COMMENT '模板状态',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='优惠券模板表';

-- ----------------------------
-- Records of coupon_template
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for drug_detail
-- ----------------------------
DROP TABLE IF EXISTS `drug_detail`;
CREATE TABLE `drug_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `product_id` bigint NOT NULL COMMENT '关联商城商品ID（mall_product.id）',
  `common_name` varchar(255) DEFAULT NULL COMMENT '药品通用名，例如"喉咙清颗粒"',
  `composition` text COMMENT '成分（如"土牛膝、马兰草、车前草、天名精…"）',
  `characteristics` varchar(500) DEFAULT NULL COMMENT '性状（如"棕褐色的颗粒；味甜、微苦…"）',
  `packaging` varchar(255) DEFAULT NULL COMMENT '包装规格（如"复合膜包装，12袋/盒"）',
  `validity_period` varchar(100) DEFAULT NULL COMMENT '有效期（如"24个月"）',
  `storage_conditions` varchar(255) DEFAULT NULL COMMENT '贮藏条件（如"密封，置阴凉处（不超过20°C）"）',
  `production_unit` varchar(255) DEFAULT NULL COMMENT '生产单位（如"湖南时代阳光药业股份有限公司"）',
  `approval_number` varchar(100) DEFAULT NULL COMMENT '批准文号（如"国药准字Z20090802"）',
  `executive_standard` varchar(255) DEFAULT NULL COMMENT '执行标准（如"国家食品药品监督管理局标准YBZ13322009"）',
  `origin_type` varchar(50) DEFAULT NULL COMMENT '产地类型（如"国产"或"进口"）',
  `is_outpatient_medicine` tinyint(1) DEFAULT '0' COMMENT '是否外用药（0-否，1-是）',
  `warm_tips` text COMMENT '温馨提示',
  `drug_category` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '药品分类编码: 0-OTC绿(乙类非处方药), 1-Rx(处方药), 2-OTC红(甲类非处方药)',
  `brand` varchar(128) DEFAULT NULL COMMENT '品牌名称',
  `efficacy` text COMMENT '功能主治',
  `usage_method` text COMMENT '用法用量',
  `adverse_reactions` text COMMENT '不良反应',
  `precautions` text COMMENT '注意事项',
  `taboo` text COMMENT '禁忌',
  `instruction` text COMMENT '药品说明书全文（可选）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(0未删除1已删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_id` (`product_id`),
  KEY `idx_common_name` (`common_name`),
  KEY `idx_approval_number` (`approval_number`),
  CONSTRAINT `fk_medicine_product` FOREIGN KEY (`product_id`) REFERENCES `mall_product` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='药品详细信息表';

-- ----------------------------
-- Records of drug_detail
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for kb_base
-- ----------------------------
DROP TABLE IF EXISTS `kb_base`;
CREATE TABLE `kb_base` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `knowledge_name` varchar(128) NOT NULL COMMENT '知识库唯一名称（业务键）',
  `cover` varchar(256) DEFAULT NULL COMMENT '封面',
  `display_name` varchar(255) NOT NULL COMMENT '知识库展示名称',
  `description` varchar(1024) NOT NULL DEFAULT '' COMMENT '知识库描述',
  `embedding_model` varchar(128) NOT NULL DEFAULT '' COMMENT '向量模型标识',
  `embedding_dim` int unsigned NOT NULL DEFAULT '0' COMMENT '向量维度',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '记录状态',
  `create_by` varchar(64) NOT NULL DEFAULT '' COMMENT '创建人账号',
  `update_by` varchar(64) NOT NULL DEFAULT '' COMMENT '最后更新人账号',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `deleted_at` datetime(3) DEFAULT NULL COMMENT '逻辑删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_kb_name_deleted` (`knowledge_name`,`is_deleted`),
  KEY `idx_kb_status_deleted` (`status`,`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库主数据表';

-- ----------------------------
-- Records of kb_base
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for kb_document
-- ----------------------------
DROP TABLE IF EXISTS `kb_document`;
CREATE TABLE `kb_document` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `knowledge_base_id` bigint unsigned NOT NULL COMMENT '知识库ID',
  `file_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `file_url` varchar(2048) NOT NULL COMMENT '文件 URL',
  `file_type` varchar(64) DEFAULT 'Unknown' COMMENT '文件类型',
  `file_size` int unsigned DEFAULT '0' COMMENT '文件大小',
  `chunk_mode` varchar(32) DEFAULT NULL COMMENT '切片模式',
  `chunk_size` int DEFAULT NULL COMMENT '切片长度',
  `chunk_overlap` int DEFAULT NULL COMMENT '切片重叠长度',
  `stage` varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT '文档阶段',
  `last_error` varchar(2000) NOT NULL DEFAULT '' COMMENT '最近一次处理失败错误信息',
  `create_by` varchar(64) NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(64) NOT NULL DEFAULT '' COMMENT '最后更新人',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_doc_kb_status` (`knowledge_base_id`,`stage`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库文档元数据表';

-- ----------------------------
-- Records of kb_document
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for kb_document_chunk
-- ----------------------------
DROP TABLE IF EXISTS `kb_document_chunk`;
CREATE TABLE `kb_document_chunk` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `knowledge_base_id` bigint unsigned NOT NULL COMMENT '知识库 ID',
  `document_id` bigint NOT NULL COMMENT '文档ID',
  `chunk_index` int NOT NULL COMMENT '切片序号（从0开始）',
  `content` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '切片内容',
  `vector_id` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '向量库记录ID（Milvus主键）',
  `char_count` int NOT NULL DEFAULT '0' COMMENT '切片字符数',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0启用，1禁用（禁用后不参与向量检索）',
  `stage` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '阶段',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_document_chunk_index` (`document_id`,`chunk_index`),
  KEY `idx_document_id` (`document_id`),
  KEY `idx_vector_id` (`vector_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文档切片表';

-- ----------------------------
-- Records of kb_document_chunk
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for kb_document_chunk_history
-- ----------------------------
DROP TABLE IF EXISTS `kb_document_chunk_history`;
CREATE TABLE `kb_document_chunk_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '历史记录主键',
  `document_id` bigint NOT NULL COMMENT '文档ID',
  `chunk_id` bigint NOT NULL COMMENT '业务切片ID，对应 knowledge_document_chunk.id',
  `knowledge_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '知识库名称',
  `vector_id` bigint NOT NULL COMMENT 'Milvus 向量主键ID',
  `old_content` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '修改前旧内容',
  `task_id` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '本次编辑对应任务ID',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID，可为空',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_chunk_id_task_id` (`chunk_id`,`task_id`),
  KEY `idx_document_id` (`document_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文档切片历史表';

-- ----------------------------
-- Records of kb_document_chunk_history
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for llm_provider
-- ----------------------------
DROP TABLE IF EXISTS `llm_provider`;
CREATE TABLE `llm_provider` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `provider_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '提供商显示名称',
  `provider_type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '提供商类型：openai、aliyun、volcengine',
  `base_url` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '基础请求地址',
  `api_key` varchar(2048) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '加密后的API Key密文',
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '提供商描述',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0启用 1停用',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序值，值越小越靠前',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人账号',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最后更新人账号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `enabled_unique_guard` tinyint GENERATED ALWAYS AS ((case when (`status` = 1) then 1 else NULL end)) STORED,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_llm_provider_name` (`provider_name`),
  UNIQUE KEY `uk_llm_provider_single_enabled` (`enabled_unique_guard`),
  KEY `idx_llm_provider_type_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='大模型提供商配置表';

-- ----------------------------
-- Records of llm_provider
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for llm_provider_model
-- ----------------------------
DROP TABLE IF EXISTS `llm_provider_model`;
CREATE TABLE `llm_provider_model` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `provider_id` bigint NOT NULL COMMENT '提供商ID，对应 llm_provider.id',
  `model_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模型显示名称',
  `model_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模型类型：CHAT/RERANK/EMBEDDING',
  `support_reasoning` tinyint NOT NULL DEFAULT '0' COMMENT '是否支持深度思考：0否 1是',
  `support_vision` tinyint NOT NULL DEFAULT '0' COMMENT '是否支持图片识别：0否 1是',
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '模型描述',
  `enabled` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0启用 1停用',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序值，值越小越靠前',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人账号',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最后更新人账号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_llm_provider_model_unique` (`provider_id`,`model_name`,`model_type`),
  KEY `idx_llm_provider_model_provider` (`provider_id`),
  KEY `idx_llm_provider_model_type_status` (`model_type`,`enabled`),
  KEY `idx_llm_provider_model_provider_type` (`provider_id`,`model_type`,`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='大模型提供商模型配置表';

-- ----------------------------
-- Records of llm_provider_model
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for mall_after_sale
-- ----------------------------
DROP TABLE IF EXISTS `mall_after_sale`;
CREATE TABLE `mall_after_sale` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '售后申请ID',
  `after_sale_no` varchar(64) NOT NULL COMMENT '售后单号(业务唯一标识)',
  `order_id` bigint NOT NULL COMMENT '关联订单ID',
  `order_no` varchar(64) NOT NULL COMMENT '订单编号(冗余字段)',
  `order_item_id` bigint NOT NULL COMMENT '关联订单项ID',
  `user_id` bigint NOT NULL COMMENT '申请用户ID',
  `after_sale_type` varchar(32) NOT NULL COMMENT '售后类型(REFUND_ONLY-仅退款, RETURN_REFUND-退货退款, EXCHANGE-换货)',
  `after_sale_status` varchar(32) NOT NULL COMMENT '售后状态(PENDING-待审核, APPROVED-已通过, REJECTED-已拒绝, PROCESSING-处理中, COMPLETED-已完成, CANCELLED-已取消)',
  `refund_amount` decimal(10,2) DEFAULT '0.00' COMMENT '退款金额',
  `apply_reason` varchar(64) NOT NULL COMMENT '申请原因(ADDRESS_ERROR-收货地址填错了, NOT_AS_DESCRIBED-与描述不符, INFO_ERROR-信息填错了重新拍, DAMAGED-收到商品损坏了, DELAYED-未按预定时间发货, OTHER-其它原因)',
  `apply_description` varchar(500) DEFAULT NULL COMMENT '详细说明',
  `evidence_images` text COMMENT '凭证图片(JSON数组格式)',
  `receive_status` varchar(32) DEFAULT NULL COMMENT '收货状态(RECEIVED-已收到货, NOT_RECEIVED-未收到货)',
  `reject_reason` varchar(500) DEFAULT NULL COMMENT '拒绝原因(审核拒绝时填写)',
  `admin_remark` varchar(500) DEFAULT NULL COMMENT '管理员备注',
  `apply_time` datetime NOT NULL COMMENT '申请时间',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `complete_time` datetime DEFAULT NULL COMMENT '完成时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(0未删除1已删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_after_sale_no` (`after_sale_no`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_order_item_id` (`order_item_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`after_sale_status`),
  KEY `idx_apply_time` (`apply_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='售后申请表';

-- ----------------------------
-- Records of mall_after_sale
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for mall_after_sale_timeline
-- ----------------------------
DROP TABLE IF EXISTS `mall_after_sale_timeline`;
CREATE TABLE `mall_after_sale_timeline` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '时间线ID',
  `after_sale_id` bigint NOT NULL COMMENT '售后申请ID',
  `event_type` varchar(64) NOT NULL COMMENT '事件类型',
  `event_status` varchar(32) NOT NULL COMMENT '事件状态',
  `operator_type` varchar(32) NOT NULL COMMENT '操作人类型(USER-用户, ADMIN-管理员, SYSTEM-系统)',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `description` varchar(500) NOT NULL COMMENT '事件描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(0未删除1已删除)',
  PRIMARY KEY (`id`),
  KEY `idx_after_sale_id` (`after_sale_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='售后时间线表';

-- ----------------------------
-- Records of mall_after_sale_timeline
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for mall_cart
-- ----------------------------
DROP TABLE IF EXISTS `mall_cart`;
CREATE TABLE `mall_cart` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '购物车ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `product_id` bigint unsigned NOT NULL COMMENT '商品ID',
  `product_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '商品名称',
  `product_image` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '商品图片（主图）',
  `cart_num` int NOT NULL DEFAULT '1' COMMENT '购买数量',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_id` (`user_id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商城购物车表';

-- ----------------------------
-- Records of mall_cart
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for mall_category
-- ----------------------------
DROP TABLE IF EXISTS `mall_category`;
CREATE TABLE `mall_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` varchar(100) NOT NULL COMMENT '分类名称',
  `parent_id` bigint DEFAULT '0' COMMENT '父分类ID，0表示顶级分类',
  `description` varchar(255) DEFAULT NULL COMMENT '分类描述',
  `cover` varchar(256) DEFAULT NULL COMMENT '封面',
  `sort` int DEFAULT '0' COMMENT '排序值，越小越靠前',
  `status` tinyint DEFAULT '0' COMMENT '状态（0-启用，1-禁用）',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=66 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商城商品分类表';

-- ----------------------------
-- Records of mall_category
-- ----------------------------
BEGIN;
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (1, '感冒发烧', 0, '感冒、发热相关用药', NULL, 1, 0, '2025-12-23 02:14:05', '2026-04-19 14:10:51', NULL, 'zhangchuang');
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (2, '五官用药', 0, '耳鼻喉口眼相关用药', NULL, 2, 0, '2025-12-23 02:14:05', '2026-04-19 17:16:19', NULL, 'zhangchuang');
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (3, '慢病用药', 0, '长期慢性疾病用药', NULL, 3, 0, '2025-12-23 02:14:05', '2026-04-19 17:16:11', NULL, 'zhangchuang');
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (4, '胃肠肝胆', 0, '胃肠及肝胆系统用药', NULL, 4, 0, '2025-12-23 02:14:05', '2026-04-19 17:16:03', NULL, 'zhangchuang');
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (5, '妇科用药', 0, '女性相关疾病用药', NULL, 5, 0, '2025-12-23 02:14:05', '2026-04-19 17:15:50', NULL, 'zhangchuang');
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (6, '男科用药', 0, '男性相关疾病用药', NULL, 6, 0, '2025-12-23 02:14:05', '2026-04-22 13:05:18', NULL, 'zhangchuang');
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (7, '皮肤用药', 0, '皮肤疾病及外用药', NULL, 7, 0, '2025-12-23 02:14:05', '2026-04-19 17:21:02', NULL, 'zhangchuang');
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (8, '风湿骨伤', 0, '风湿及骨关节损伤', NULL, 8, 0, '2025-12-23 02:14:05', '2026-04-19 17:20:30', NULL, 'zhangchuang');
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (9, '儿童用药', 0, '儿童专用药品', NULL, 9, 0, '2025-12-23 02:14:05', '2026-04-19 17:20:12', NULL, 'zhangchuang');
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (10, '抗菌消炎', 0, '抗菌及消炎类药品', NULL, 10, 0, '2025-12-23 02:14:05', '2026-04-19 17:19:08', NULL, 'zhangchuang');
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (11, '母婴商品', 0, '母婴相关商品', NULL, 11, 0, '2025-12-23 02:14:05', '2026-04-19 17:18:41', NULL, 'zhangchuang');
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (12, '医疗器械', 0, '医疗器械及辅助用品', NULL, 12, 0, '2025-12-23 02:14:05', '2026-04-19 17:18:12', NULL, 'zhangchuang');
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (13, '营养保健食品', 0, '营养及保健类产品', NULL, 13, 0, '2025-12-23 02:14:05', '2026-04-19 17:14:44', NULL, 'zhangchuang');
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (14, '滋补养生', 0, '滋补及养生类产品', NULL, 14, 0, '2025-12-23 02:14:05', '2026-04-19 17:17:43', NULL, 'zhangchuang');
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (15, '清热', 1, '清热类感冒用药', NULL, 1, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (16, '咳嗽', 1, '止咳化痰类', NULL, 2, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (17, '退烧', 1, '退热降温', NULL, 3, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (18, '流行性感冒', 1, '流感相关用药', NULL, 4, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (19, '普通感冒', 1, '普通感冒用药', NULL, 5, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (20, '风寒感冒', 1, '风寒型感冒', NULL, 6, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (21, '风热感冒', 1, '风热型感冒', NULL, 7, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (22, '预防感冒', 1, '增强免疫预防感冒', NULL, 8, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (23, '鼻炎用药', 2, '鼻炎及鼻腔疾病', NULL, 1, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (24, '咽喉用药', 2, '咽炎扁桃体相关', NULL, 2, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (25, '口腔用药', 2, '口腔溃疡牙龈炎', NULL, 3, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (26, '眼科用药', 2, '眼部疾病', NULL, 4, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (27, '耳科用药', 2, '耳部相关疾病', NULL, 5, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (28, '高血压', 3, '高血压用药', NULL, 1, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (29, '糖尿病', 3, '血糖控制', NULL, 2, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (30, '高血脂', 3, '降血脂', NULL, 3, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (31, '冠心病', 3, '心血管疾病', NULL, 4, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (32, '痛风', 3, '尿酸相关疾病', NULL, 5, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (33, '胃炎', 4, '急慢性胃炎', NULL, 1, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (34, '消化不良', 4, '助消化', NULL, 2, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (35, '腹泻便秘', 4, '肠道功能异常', NULL, 3, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (36, '肝胆用药', 4, '肝胆系统', NULL, 4, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (37, '胃溃疡', 4, '胃部溃疡', NULL, 5, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (38, '月经不调', 5, '月经周期异常', NULL, 1, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (39, '妇科炎症', 5, '妇科感染', NULL, 2, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (40, '避孕用药', 5, '避孕相关', NULL, 3, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (41, '前列腺', 6, '前列腺相关', NULL, 1, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (42, '性功能调理', 6, '功能调理', NULL, 2, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (43, '男科炎症', 6, '感染类疾病', NULL, 3, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (44, '皮炎湿疹', 7, '湿疹皮炎', NULL, 1, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (45, '真菌感染', 7, '真菌性皮肤病', NULL, 2, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (46, '皮肤过敏', 7, '过敏反应', NULL, 3, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (47, '关节疼痛', 8, '关节疼痛', NULL, 1, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (48, '风湿类', 8, '风湿免疫', NULL, 2, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (49, '跌打损伤', 8, '外伤损伤', NULL, 3, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (50, '儿童感冒', 9, '儿童感冒用药', NULL, 1, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (51, '儿童退烧', 9, '儿童退热', NULL, 2, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (52, '儿童止咳', 9, '儿童咳嗽', NULL, 3, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (53, '抗生素', 10, '抗菌类药物', NULL, 1, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (54, '消炎药', 10, '消炎镇痛', NULL, 2, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (55, '奶粉', 11, '婴幼儿奶粉', NULL, 1, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (56, '婴儿护理', 11, '护理用品', NULL, 2, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (57, '血压计', 12, '血压监测', NULL, 1, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (58, '血糖仪', 12, '血糖监测', NULL, 2, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (59, '医用口罩', 12, '防护用品', NULL, 3, 0, '2025-12-23 02:14:05', '2025-12-23 02:14:05', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (60, '维生素', 13, '维生素补充', NULL, 1, 0, '2025-12-23 02:14:06', '2025-12-23 02:14:06', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (61, '矿物质', 13, '矿物质补充', NULL, 2, 0, '2025-12-23 02:14:06', '2025-12-23 02:14:06', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (62, '增强免疫', 13, '免疫调节', NULL, 3, 0, '2025-12-23 02:14:06', '2025-12-23 02:14:06', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (63, '补气养血', 14, '补气养血', NULL, 1, 0, '2025-12-23 02:14:06', '2025-12-23 02:14:06', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (64, '滋阴补肾', 14, '滋阴补肾', NULL, 2, 0, '2025-12-23 02:14:06', '2025-12-23 02:14:06', NULL, NULL);
INSERT INTO `mall_category` (`id`, `name`, `parent_id`, `description`, `cover`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (65, '安神助眠', 14, '改善睡眠', NULL, 3, 0, '2025-12-23 02:14:06', '2025-12-23 02:14:06', NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for mall_order
-- ----------------------------
DROP TABLE IF EXISTS `mall_order`;
CREATE TABLE `mall_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` varchar(64) NOT NULL COMMENT '订单编号（业务唯一标识）',
  `user_id` bigint NOT NULL COMMENT '下单用户ID',
  `total_amount` decimal(10,2) NOT NULL COMMENT '订单总金额（含运费）',
  `pay_amount` decimal(10,2) NOT NULL COMMENT '实际支付金额',
  `freight_amount` decimal(10,2) DEFAULT '0.00' COMMENT '运费金额',
  `pay_type` varchar(64) DEFAULT NULL COMMENT '支付方式',
  `order_status` varchar(64) DEFAULT NULL COMMENT '订单状态',
  `delivery_type` varchar(64) DEFAULT NULL COMMENT '配送方式',
  `address_id` bigint DEFAULT NULL COMMENT '用户收货地址ID',
  `receiver_name` varchar(64) DEFAULT NULL COMMENT '收货人姓名',
  `receiver_phone` varchar(32) DEFAULT NULL COMMENT '收货人电话',
  `receiver_detail` varchar(255) DEFAULT NULL COMMENT '收货详细地址',
  `note` varchar(255) DEFAULT NULL COMMENT '用户留言',
  `pay_expire_time` datetime DEFAULT NULL COMMENT '支付过期时间',
  `refund_status` varchar(64) DEFAULT NULL COMMENT '退款状态',
  `refund_time` datetime DEFAULT NULL COMMENT '退款时间',
  `refund_price` decimal(10,2) DEFAULT NULL COMMENT '退款金额',
  `after_sale_flag` varchar(64) DEFAULT '0' COMMENT '是否存在售后',
  `paid` tinyint NOT NULL DEFAULT '0' COMMENT '是否支付(0否,1是)',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `deliver_time` datetime DEFAULT NULL COMMENT '发货时间',
  `receive_time` datetime DEFAULT NULL COMMENT '确认收货时间',
  `finish_time` datetime DEFAULT NULL COMMENT '完成时间',
  `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
  `close_reason` varchar(256) DEFAULT NULL COMMENT '关闭原因',
  `close_time` datetime DEFAULT NULL COMMENT '关闭时间',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号(更新时自增)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` int DEFAULT NULL COMMENT '修改人',
  `update_by` varchar(256) DEFAULT NULL COMMENT '修改人',
  `remark` varchar(1024) DEFAULT NULL COMMENT '备注',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(0未删除1已删除)',
  `items_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '商品原始总金额',
  `coupon_id` bigint DEFAULT NULL COMMENT '使用的用户优惠券ID',
  `coupon_name` varchar(128) DEFAULT NULL COMMENT '优惠券名称快照',
  `coupon_deduct_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '订单抵扣金额',
  `coupon_consume_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '优惠券消耗金额',
  `coupon_waste_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '优惠券浪费金额',
  `coupon_snapshot_json` text COMMENT '优惠券快照JSON',
  PRIMARY KEY (`id`),
  UNIQUE KEY `order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_order_status` (`order_status`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_status_deliver_time` (`order_status`,`deliver_time`),
  KEY `idx_deliver_time` (`deliver_time`) COMMENT '发货时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商城订单表（主订单）';

-- ----------------------------
-- Records of mall_order
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for mall_order_item
-- ----------------------------
DROP TABLE IF EXISTS `mall_order_item`;
CREATE TABLE `mall_order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单项ID',
  `order_id` bigint NOT NULL COMMENT '关联 mall_order.id',
  `product_id` bigint NOT NULL COMMENT '商城商品ID（mall_product）',
  `product_name` varchar(128) NOT NULL COMMENT '商品名称',
  `quantity` int NOT NULL COMMENT '购买数量',
  `price` decimal(10,2) NOT NULL COMMENT '单价',
  `total_price` decimal(10,2) NOT NULL COMMENT '小计金额',
  `image_url` varchar(255) DEFAULT NULL COMMENT '商品图片',
  `after_sale_status` varchar(32) DEFAULT 'NONE' COMMENT '售后状态(NONE-无售后, IN_PROGRESS-售后中, COMPLETED-售后完成)',
  `refunded_amount` decimal(10,2) DEFAULT '0.00' COMMENT '已退款金额',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(0未删除1已删除)',
  `coupon_deduct_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '订单项分摊优惠金额',
  `payable_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '订单项应付金额',
  PRIMARY KEY (`id`),
  KEY `fk_order_item_order` (`order_id`),
  KEY `idx_after_sale_status` (`after_sale_status`),
  CONSTRAINT `fk_order_item_order` FOREIGN KEY (`order_id`) REFERENCES `mall_order` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商城订单明细表（商品项）';

-- ----------------------------
-- Records of mall_order_item
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for mall_order_shipping
-- ----------------------------
DROP TABLE IF EXISTS `mall_order_shipping`;
CREATE TABLE `mall_order_shipping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '物流记录ID',
  `order_id` bigint NOT NULL COMMENT '关联 mall_order.id',
  `shipping_no` varchar(64) DEFAULT NULL COMMENT '物流单号',
  `shipping_company` varchar(64) DEFAULT NULL COMMENT '物流公司',
  `delivery_type` varchar(50) DEFAULT NULL COMMENT '配送方式（EXPRESS-快递配送,SELF_PICKUP-到店自提,PHARMACY_PICKUP_LOCKER-药店智能柜自提）',
  `sender_name` varchar(64) DEFAULT NULL COMMENT '发货人姓名',
  `sender_phone` varchar(32) DEFAULT NULL COMMENT '发货人电话',
  `status` varchar(64) DEFAULT NULL COMMENT '配送状态',
  `shipping_info` json DEFAULT NULL COMMENT '物流信息详情(JSON结构)',
  `deliver_time` datetime DEFAULT NULL COMMENT '发货时间',
  `receive_time` datetime DEFAULT NULL COMMENT '签收时间',
  `shipment_note` varchar(255) DEFAULT NULL COMMENT '发货备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `fk_shipping_order` (`order_id`),
  KEY `idx_order_id` (`order_id`),
  CONSTRAINT `fk_shipping_order` FOREIGN KEY (`order_id`) REFERENCES `mall_order` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单配送物流表';

-- ----------------------------
-- Records of mall_order_shipping
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for mall_order_timeline
-- ----------------------------
DROP TABLE IF EXISTS `mall_order_timeline`;
CREATE TABLE `mall_order_timeline` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `event_type` varchar(32) NOT NULL COMMENT '事件类型',
  `event_status` varchar(32) NOT NULL COMMENT '事件状态',
  `operator_type` varchar(16) DEFAULT 'SYSTEM' COMMENT '操作方(USER/ADMIN/SYSTEM)',
  `description` varchar(255) DEFAULT NULL COMMENT '事件描述',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '事件时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(0未删除1已删除)',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_event` (`event_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单时间线记录表';

-- ----------------------------
-- Records of mall_order_timeline
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for mall_product
-- ----------------------------
DROP TABLE IF EXISTS `mall_product`;
CREATE TABLE `mall_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `name` varchar(200) NOT NULL COMMENT '商品名称',
  `category_id` bigint NOT NULL COMMENT '商品分类ID，关联 mall_category',
  `unit` varchar(50) DEFAULT '件' COMMENT '商品单位（件、盒、瓶等）',
  `price` decimal(10,2) NOT NULL COMMENT '展示价/兜底价：单规格=唯一SKU价，多规格=最小SKU价；结算以SKU价为准',
  `stock` int DEFAULT '0' COMMENT '独立库存数量（仅当未绑定药品时生效）',
  `sort` int DEFAULT '0' COMMENT '排序值，越小越靠前',
  `status` tinyint DEFAULT '1' COMMENT '状态（1-上架，0-下架）',
  `delivery_type` tinyint DEFAULT '0' COMMENT '0咨询商家,1自提,2快递配送,3同城配送,4药店自送,5冷链配送,6智能药柜取药',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号(更新时自增)',
  `shipping_id` bigint DEFAULT NULL COMMENT '运费模板ID，关联 mall_product_shipping',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(0未删除1已删除)',
  `coupon_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否允许使用优惠券：1-允许，0-不允许',
  PRIMARY KEY (`id`),
  KEY `fk_product_category` (`category_id`),
  CONSTRAINT `fk_product_category` FOREIGN KEY (`category_id`) REFERENCES `mall_category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商城商品主表（支持绑定药品库存）';

-- ----------------------------
-- Records of mall_product
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for mall_product_category_rel
-- ----------------------------
DROP TABLE IF EXISTS `mall_product_category_rel`;
CREATE TABLE `mall_product_category_rel` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `category_id` bigint NOT NULL COMMENT '分类ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '更新人',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0-否，1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_category_active` (`product_id`,`category_id`,`is_deleted`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商城商品分类关联表';

-- ----------------------------
-- Records of mall_product_category_rel
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for mall_product_image
-- ----------------------------
DROP TABLE IF EXISTS `mall_product_image`;
CREATE TABLE `mall_product_image` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '图片ID',
  `product_id` bigint NOT NULL COMMENT '商品ID，关联 mall_product',
  `image_url` varchar(500) NOT NULL COMMENT '图片URL',
  `sort` int DEFAULT '0' COMMENT '排序值',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(0未删除1已删除)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商城商品图片表（轮播图、详情图）';

-- ----------------------------
-- Records of mall_product_image
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for mall_product_tag
-- ----------------------------
DROP TABLE IF EXISTS `mall_product_tag`;
CREATE TABLE `mall_product_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `name` varchar(64) NOT NULL COMMENT '标签名称',
  `type_id` bigint NOT NULL COMMENT '标签类型ID',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序值，越小越靠前',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1-启用，0-禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0-否，1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mall_product_tag_type_id_name` (`type_id`,`name`),
  KEY `idx_mall_product_tag_status` (`status`),
  KEY `idx_mall_product_tag_type_sort` (`type_id`,`sort`,`id`),
  CONSTRAINT `fk_mall_product_tag_type_id` FOREIGN KEY (`type_id`) REFERENCES `mall_product_tag_type` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=196 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品标签表';

-- ----------------------------
-- Records of mall_product_tag
-- ----------------------------
BEGIN;
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (1, '退烧', 1, 10, 1, '2026-03-21 19:56:53', '2026-04-08 01:31:54', 'seed', 'zhangchuang', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (2, '止痛', 1, 20, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (3, '消炎', 1, 30, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (4, '抗菌', 1, 40, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (5, '抗病毒', 1, 50, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (6, '止咳', 1, 60, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (7, '化痰', 1, 70, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (8, '平喘', 1, 80, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (9, '润喉', 1, 90, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (10, '通鼻', 1, 100, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (11, '抗过敏', 1, 110, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (12, '清热解毒', 1, 120, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (13, '护胃', 1, 130, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (14, '抑酸', 1, 140, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (15, '助消化', 1, 150, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (16, '止泻', 1, 160, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (17, '通便', 1, 170, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (18, '止吐', 1, 180, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (19, '解痉', 1, 190, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (20, '健胃', 1, 200, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (21, '补气', 1, 210, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (22, '补血', 1, 220, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (23, '补钙', 1, 230, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (24, '补锌', 1, 240, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (25, '补维生素', 1, 250, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (26, '增强免疫', 1, 260, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (27, '安神', 1, 270, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (28, '助眠', 1, 280, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (29, '明目', 1, 290, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (30, '降压', 1, 300, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (31, '降脂', 1, 310, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (32, '降糖', 1, 320, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (33, '活血化瘀', 1, 330, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (34, '祛风除湿', 1, 340, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (35, '杀菌止痒', 1, 350, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (36, '促进修复', 1, 360, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (37, '驱虫', 1, 370, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (38, '止痒', 1, 380, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (39, '护肝', 1, 390, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (40, '发热', 2, 10, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (41, '头痛', 2, 20, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (42, '偏头痛', 2, 30, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (43, '咽痛', 2, 40, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (44, '咳嗽', 2, 50, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (45, '痰多', 2, 60, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (46, '气喘', 2, 70, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (47, '流鼻涕', 2, 80, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (48, '鼻塞', 2, 90, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (49, '打喷嚏', 2, 100, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (50, '过敏性鼻炎', 2, 110, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (51, '咽干咽痒', 2, 120, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (52, '胃痛', 2, 130, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (53, '胃胀', 2, 140, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (54, '反酸烧心', 2, 150, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (55, '恶心', 2, 160, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (56, '呕吐', 2, 170, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (57, '腹痛', 2, 180, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (58, '腹泻', 2, 190, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (59, '便秘', 2, 200, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (60, '消化不良', 2, 210, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (61, '晕车晕船', 2, 220, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (62, '牙痛', 2, 230, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (63, '口腔溃疡', 2, 240, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (64, '咽喉肿痛', 2, 250, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (65, '皮肤瘙痒', 2, 260, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (66, '皮疹', 2, 270, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (67, '湿疹', 2, 280, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (68, '皮炎', 2, 290, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (69, '痤疮', 2, 300, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (70, '脚气', 2, 310, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (71, '失眠', 2, 320, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (72, '焦虑烦躁', 2, 330, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (73, '眩晕', 2, 340, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (74, '眼疲劳', 2, 350, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (75, '视物模糊', 2, 360, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (76, '关节疼痛', 2, 370, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (77, '肌肉酸痛', 2, 380, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (78, '颈肩腰腿痛', 2, 390, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (79, '痛经', 2, 400, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (80, '月经不调', 2, 410, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (81, '尿频尿急', 2, 420, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (82, '高血压', 2, 430, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (83, '高血脂', 2, 440, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (84, '高血糖', 2, 450, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (85, '感冒发热', 3, 10, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (86, '咳嗽化痰', 3, 20, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (87, '鼻炎过敏', 3, 30, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (88, '咽喉口腔', 3, 40, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (89, '肠胃消化', 3, 50, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (90, '腹泻便秘', 3, 60, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (91, '皮肤护理', 3, 70, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (92, '真菌感染', 3, 80, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (93, '妇科调理', 3, 90, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (94, '男科泌尿', 3, 100, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (95, '骨科疼痛', 3, 110, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (96, '风湿关节', 3, 120, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (97, '睡眠情绪', 3, 130, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (98, '眼科护理', 3, 140, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (99, '耳鼻喉科', 3, 150, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (100, '心脑血管', 3, 160, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (101, '糖尿病调理', 3, 170, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (102, '肝胆辅助', 3, 180, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (103, '儿科常备', 3, 190, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (104, '维矿补益', 3, 200, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (105, '免疫提升', 3, 210, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (106, '驱虫止痒', 3, 220, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (107, '家庭常备', 3, 230, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (108, '儿童', 4, 10, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (109, '婴幼儿', 4, 20, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (110, '青少年', 4, 30, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (111, '成人', 4, 40, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (112, '女性', 4, 50, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (113, '男性', 4, 60, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (114, '老年人', 4, 70, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (115, '孕妇慎用', 4, 80, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (116, '哺乳期慎用', 4, 90, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (117, '司机慎用', 4, 100, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (118, '糖尿病人群', 4, 110, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (119, '高血压人群', 4, 120, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (120, '术后恢复', 4, 130, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (121, '体虚人群', 4, 140, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (122, '易过敏人群', 4, 150, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (123, '久坐人群', 4, 160, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (124, '熬夜人群', 4, 170, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (125, '上班族', 4, 180, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (126, '学生党', 4, 190, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (127, '家庭常备人群', 4, 200, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (128, '家庭常备', 5, 10, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (129, '换季常备', 5, 20, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (130, '秋冬高发', 5, 30, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (131, '春季过敏', 5, 40, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (132, '夜间急用', 5, 50, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (133, '出差旅行', 5, 60, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (134, '办公常备', 5, 70, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (135, '学校常备', 5, 80, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (136, '运动损伤', 5, 90, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (137, '经期护理', 5, 100, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (138, '餐后不适', 5, 110, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (139, '熬夜调理', 5, 120, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (140, '晕车出行', 5, 130, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (141, '免疫提升期', 5, 140, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (142, '术后恢复期', 5, 150, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (143, '高温中暑季', 5, 160, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (144, '片剂', 6, 10, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (145, '胶囊', 6, 20, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (146, '颗粒', 6, 30, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (147, '口服液', 6, 40, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (148, '糖浆', 6, 50, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (149, '滴剂', 6, 60, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (150, '喷雾', 6, 70, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (151, '贴膏', 6, 80, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (152, '软膏', 6, 90, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (153, '乳膏', 6, 100, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (154, '凝胶', 6, 110, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (155, '洗剂', 6, 120, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (156, '丸剂', 6, 130, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (157, '冲剂', 6, 140, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (158, '泡腾片', 6, 150, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (159, '含片', 6, 160, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (160, '滴眼液', 6, 170, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (161, '滴鼻液', 6, 180, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (162, '栓剂', 6, 190, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (163, '外用溶液', 6, 200, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (164, '头部', 7, 10, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (165, '咽喉', 7, 20, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (166, '鼻部', 7, 30, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (167, '口腔', 7, 40, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (168, '眼部', 7, 50, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (169, '耳部', 7, 60, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (170, '呼吸道', 7, 70, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (171, '胃部', 7, 80, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (172, '肠道', 7, 90, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (173, '肝胆', 7, 100, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (174, '泌尿系统', 7, 110, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (175, '皮肤', 7, 120, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (176, '骨关节', 7, 130, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (177, '肌肉', 7, 140, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (178, '腰背', 7, 150, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (179, '私护部位', 7, 160, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (180, 'OTC甲类', 8, 10, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (181, 'OTC乙类', 8, 20, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (182, '处方药', 8, 30, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (183, '中成药', 8, 40, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (184, '西药', 8, 50, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (185, '中西复方', 8, 60, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (186, '外用药', 8, 70, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (187, '内服药', 8, 80, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (188, '缓释控释', 8, 90, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (189, '无糖配方', 8, 100, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (190, '儿童专用', 8, 110, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (191, '医保常用', 8, 120, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (192, '保健食品', 8, 130, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (193, '医疗器械', 8, 140, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (194, '冷链储运', 8, 150, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag` (`id`, `name`, `type_id`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (195, '家中常备', 8, 160, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
COMMIT;

-- ----------------------------
-- Table structure for mall_product_tag_rel
-- ----------------------------
DROP TABLE IF EXISTS `mall_product_tag_rel`;
CREATE TABLE `mall_product_tag_rel` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `tag_id` bigint NOT NULL COMMENT '标签ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mall_product_tag_rel_product_tag` (`product_id`,`tag_id`),
  KEY `idx_mall_product_tag_rel_product_id` (`product_id`),
  KEY `idx_mall_product_tag_rel_tag_id` (`tag_id`),
  CONSTRAINT `fk_mall_product_tag_rel_tag_id` FOREIGN KEY (`tag_id`) REFERENCES `mall_product_tag` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=76 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品标签关联表';

-- ----------------------------
-- Records of mall_product_tag_rel
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for mall_product_tag_type
-- ----------------------------
DROP TABLE IF EXISTS `mall_product_tag_type`;
CREATE TABLE `mall_product_tag_type` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '标签类型ID',
  `code` varchar(64) NOT NULL COMMENT '标签类型编码',
  `name` varchar(64) NOT NULL COMMENT '标签类型名称',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序值，越小越靠前',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1-启用，0-禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0-否，1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mall_product_tag_type_code` (`code`),
  UNIQUE KEY `uk_mall_product_tag_type_name` (`name`),
  KEY `idx_mall_product_tag_type_status` (`status`),
  KEY `idx_mall_product_tag_type_sort` (`sort`,`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品标签类型表';

-- ----------------------------
-- Records of mall_product_tag_type
-- ----------------------------
BEGIN;
INSERT INTO `mall_product_tag_type` (`id`, `code`, `name`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (1, 'EFFICACY', '功效作用', 10, 1, '2026-03-21 19:56:53', '2026-04-17 01:05:01', 'seed', 'zhangchuang', 0);
INSERT INTO `mall_product_tag_type` (`id`, `code`, `name`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (2, 'SYMPTOM', '病症症状', 20, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag_type` (`id`, `code`, `name`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (3, 'DISEASE_CATEGORY', '病种分类', 30, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag_type` (`id`, `code`, `name`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (4, 'CROWD', '适用人群', 40, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag_type` (`id`, `code`, `name`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (5, 'SCENARIO', '使用场景', 50, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag_type` (`id`, `code`, `name`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (6, 'DOSAGE_FORM', '剂型规格', 60, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag_type` (`id`, `code`, `name`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (7, 'BODY_PART', '适用部位', 70, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
INSERT INTO `mall_product_tag_type` (`id`, `code`, `name`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (8, 'DRUG_FEATURE', '药品属性', 80, 1, '2026-03-21 19:56:53', '2026-03-21 19:56:53', 'seed', 'seed', 0);
COMMIT;

-- ----------------------------
-- Table structure for mall_product_unit
-- ----------------------------
DROP TABLE IF EXISTS `mall_product_unit`;
CREATE TABLE `mall_product_unit` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '单位ID',
  `name` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '单位名称',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序值，越小越靠前',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '更新人',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0-否，1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mall_product_unit_name_deleted` (`name`,`is_deleted`),
  KEY `idx_mall_product_unit_sort` (`sort`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商城商品单位字典表';

-- ----------------------------
-- Records of mall_product_unit
-- ----------------------------
BEGIN;
INSERT INTO `mall_product_unit` (`id`, `name`, `sort`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (1, '盒', 10, '2026-04-16 01:49:24', NULL, 'system', NULL, 0);
INSERT INTO `mall_product_unit` (`id`, `name`, `sort`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (2, '瓶', 20, '2026-04-16 01:49:24', NULL, 'system', NULL, 0);
INSERT INTO `mall_product_unit` (`id`, `name`, `sort`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (3, '袋', 30, '2026-04-16 01:49:24', NULL, 'system', NULL, 0);
INSERT INTO `mall_product_unit` (`id`, `name`, `sort`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (4, '支', 40, '2026-04-16 01:49:24', NULL, 'system', NULL, 0);
INSERT INTO `mall_product_unit` (`id`, `name`, `sort`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (5, '包', 50, '2026-04-16 01:49:24', NULL, 'system', NULL, 0);
INSERT INTO `mall_product_unit` (`id`, `name`, `sort`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (6, '片', 60, '2026-04-16 01:49:24', NULL, 'system', NULL, 0);
INSERT INTO `mall_product_unit` (`id`, `name`, `sort`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (7, '贴', 70, '2026-04-16 01:49:24', NULL, 'system', NULL, 0);
INSERT INTO `mall_product_unit` (`id`, `name`, `sort`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (8, '板', 80, '2026-04-16 01:49:24', NULL, 'system', NULL, 0);
INSERT INTO `mall_product_unit` (`id`, `name`, `sort`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (9, '个', 90, '2026-04-16 01:49:24', NULL, 'system', NULL, 0);
INSERT INTO `mall_product_unit` (`id`, `name`, `sort`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES (10, '件', 100, '2026-04-16 01:49:24', NULL, 'system', NULL, 0);
COMMIT;

-- ----------------------------
-- Table structure for mall_product_view_history
-- ----------------------------
DROP TABLE IF EXISTS `mall_product_view_history`;
CREATE TABLE `mall_product_view_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `view_count` int NOT NULL DEFAULT '1' COMMENT '该用户对该商品的累计浏览次数',
  `last_view_time` datetime NOT NULL COMMENT '该用户最后一次浏览该商品的时间',
  `first_view_time` datetime NOT NULL COMMENT '第一次浏览时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_product` (`user_id`,`product_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品浏览历史与浏览量统计';

-- ----------------------------
-- Records of mall_product_view_history
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for patient_profile
-- ----------------------------
DROP TABLE IF EXISTS `patient_profile`;
CREATE TABLE `patient_profile` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `name` varchar(30) NOT NULL COMMENT '就诊人姓名',
  `gender` tinyint NOT NULL COMMENT '性别：1男 2女',
  `birth_date` date NOT NULL COMMENT '出生日期',
  `allergy` varchar(255) DEFAULT NULL COMMENT '过敏史',
  `past_medical_history` varchar(500) DEFAULT NULL COMMENT '既往病史',
  `chronic_disease` varchar(500) DEFAULT NULL COMMENT '慢性病信息',
  `long_term_medications` varchar(500) DEFAULT NULL COMMENT '长期用药',
  `relationship` varchar(20) DEFAULT NULL COMMENT '与账户关系',
  `is_default` tinyint DEFAULT '0' COMMENT '是否默认就诊人：1是 0否',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='就诊人信息';

-- ----------------------------
-- Records of patient_profile
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for permission
-- ----------------------------
DROP TABLE IF EXISTS `permission`;
CREATE TABLE `permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` bigint DEFAULT '0' COMMENT '父权限ID',
  `permission_code` varchar(100) DEFAULT NULL COMMENT '权限编码',
  `permission_name` varchar(50) NOT NULL COMMENT '权限名称',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '0' COMMENT '状态（1:启用, 0:禁用）',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1010 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权限表';

-- ----------------------------
-- Records of permission
-- ----------------------------
BEGIN;
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (1, 0, 'mall:analytics', '运营分析', 10, 0, '管理端权限：运营分析', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (2, 1, 'mall:analytics:query', '运营分析查看', 10, 0, '管理端权限：运营分析查看', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (3, 0, 'mall', '商城管理', 20, 0, '管理端权限：商城管理', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (4, 3, 'mall:product', '商品列表', 10, 0, '管理端权限：商品列表', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (5, 4, 'mall:product:list', '商品列表', 10, 0, '管理端权限：商品列表', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (6, 4, 'mall:product:query', '商品详情', 20, 0, '管理端权限：商品详情', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (7, 4, 'mall:product:add', '商品新增', 30, 0, '管理端权限：商品新增', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (8, 4, 'mall:product:edit', '商品编辑', 40, 0, '管理端权限：商品编辑', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (9, 4, 'mall:product:delete', '商品删除', 50, 0, '管理端权限：商品删除', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (10, 3, 'mall:category', '商品分类', 20, 0, '管理端权限：商品分类', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (11, 10, 'mall:category:tree', '分类树', 10, 0, '管理端权限：分类树', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (12, 10, 'mall:category:option', '分类选项', 20, 0, '管理端权限：分类选项', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (13, 10, 'mall:category:query', '分类详情', 30, 0, '管理端权限：分类详情', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (14, 10, 'mall:category:add', '分类新增', 40, 0, '管理端权限：分类新增', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (15, 10, 'mall:category:edit', '分类编辑', 50, 0, '管理端权限：分类编辑', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (16, 10, 'mall:category:delete', '分类删除', 60, 0, '管理端权限：分类删除', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (17, 3, 'mall:order', '订单列表', 30, 0, '管理端权限：订单列表', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (18, 17, 'mall:order:list', '订单列表', 10, 0, '管理端权限：订单列表', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (19, 17, 'mall:order:query', '订单详情', 20, 0, '管理端权限：订单详情', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (20, 17, 'mall:order:edit', '订单编辑', 30, 0, '管理端权限：订单编辑', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (21, 17, 'mall:order:ship', '订单发货', 40, 0, '管理端权限：订单发货', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (22, 17, 'mall:order:cancel', '订单取消', 50, 0, '管理端权限：订单取消', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (23, 17, 'mall:order:refund', '订单退款', 60, 0, '管理端权限：订单退款', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (24, 17, 'mall:order:delete', '订单删除', 70, 0, '管理端权限：订单删除', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (25, 3, 'mall:after_sale', '售后管理', 40, 0, '管理端权限：售后管理', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (26, 25, 'mall:after_sale:list', '售后列表', 10, 0, '管理端权限：售后列表', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (27, 25, 'mall:after_sale:query', '售后详情', 20, 0, '管理端权限：售后详情', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (28, 25, 'mall:after_sale:audit', '售后审核', 30, 0, '管理端权限：售后审核', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (29, 25, 'mall:after_sale:refund', '售后退款', 40, 0, '管理端权限：售后退款', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (30, 25, 'mall:after_sale:exchange', '售后换货', 50, 0, '管理端权限：售后换货', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (31, 3, 'mall:product:tag', '商品标签', 50, 0, '管理端权限：商品标签', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (32, 31, 'mall:product:tag:list', '标签列表', 10, 0, '管理端权限：标签列表', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (33, 31, 'mall:product:tag:query', '标签详情', 20, 0, '管理端权限：标签详情', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (34, 31, 'mall:product:tag:add', '标签新增', 30, 0, '管理端权限：标签新增', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (35, 31, 'mall:product:tag:edit', '标签编辑', 40, 0, '管理端权限：标签编辑', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (36, 31, 'mall:product:tag:delete', '标签删除', 50, 0, '管理端权限：标签删除', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (37, 3, 'mall:coupon', '优惠券管理', 60, 0, '管理端权限：优惠券管理', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (38, 37, 'mall:coupon:template', '模板管理', 10, 0, '管理端权限：模板管理', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (39, 38, 'mall:coupon:template:list', '模板列表', 10, 0, '管理端权限：模板列表', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (40, 38, 'mall:coupon:template:query', '模板详情', 20, 0, '管理端权限：模板详情', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (41, 38, 'mall:coupon:template:add', '模板新增', 30, 0, '管理端权限：模板新增', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (42, 38, 'mall:coupon:template:edit', '模板编辑', 40, 0, '管理端权限：模板编辑', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (43, 38, 'mall:coupon:template:delete', '模板删除', 50, 0, '管理端权限：模板删除', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (44, 37, 'mall:coupon:issue', '优惠券发放', 20, 0, '管理端权限：优惠券发放', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (45, 37, 'mall:coupon:activation-batch', '激活码管理', 30, 0, '管理端权限：激活码管理', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (46, 45, 'mall:coupon:activation-batch:generate', '生成激活码', 10, 0, '管理端权限：生成激活码', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (47, 45, 'mall:coupon:activation-batch:list', '激活码批次列表', 20, 0, '管理端权限：激活码批次列表', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (48, 45, 'mall:coupon:activation-batch:query', '激活码批次详情', 30, 0, '管理端权限：激活码批次详情', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (49, 45, 'mall:coupon:activation-batch:status', '激活码状态维护', 40, 0, '管理端权限：激活码状态维护', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (50, 37, 'mall:coupon:log', '优惠券日志', 40, 0, '管理端权限：优惠券日志', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (51, 50, 'mall:coupon:log:list', '优惠券日志列表', 10, 0, '管理端权限：优惠券日志列表', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (52, 37, 'mall:coupon:activation-log', '激活码日志', 50, 0, '管理端权限：激活码日志', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (53, 52, 'mall:coupon:activation-log:list', '激活码日志列表', 10, 0, '管理端权限：激活码日志列表', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (54, 0, 'system', '系统管理', 30, 0, '管理端权限：系统管理', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (55, 54, 'system:user', '用户管理', 10, 0, '管理端权限：用户管理', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (56, 55, 'system:user:list', '用户列表', 10, 0, '管理端权限：用户列表', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (57, 55, 'system:user:query', '用户详情', 20, 0, '管理端权限：用户详情', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (58, 55, 'system:user:add', '用户新增', 30, 0, '管理端权限：用户新增', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (59, 55, 'system:user:update', '用户修改', 40, 0, '管理端权限：用户修改', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (60, 55, 'system:user:delete', '用户删除', 50, 0, '管理端权限：用户删除', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (61, 54, 'system:role', '角色管理', 20, 0, '管理端权限：角色管理', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (62, 61, 'system:role:list', '角色列表', 10, 0, '管理端权限：角色列表', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (63, 61, 'system:role:query', '角色详情', 20, 0, '管理端权限：角色详情', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (64, 61, 'system:role:add', '角色新增', 30, 0, '管理端权限：角色新增', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (65, 61, 'system:role:update', '角色修改', 40, 0, '管理端权限：角色修改', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (66, 61, 'system:role:delete', '角色删除', 50, 0, '管理端权限：角色删除', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (67, 54, 'system:permission', '权限管理', 30, 0, '管理端权限：权限管理', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (68, 67, 'system:permission:list', '权限列表', 10, 0, '管理端权限：权限列表', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (69, 67, 'system:permission:query', '权限详情', 20, 0, '管理端权限：权限详情', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (70, 67, 'system:permission:add', '权限新增', 30, 0, '管理端权限：权限新增', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (71, 67, 'system:permission:update', '权限修改', 40, 0, '管理端权限：权限修改', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (72, 67, 'system:permission:delete', '权限删除', 50, 0, '管理端权限：权限删除', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (73, 54, 'system:config', '系统配置', 40, 0, '管理端权限：系统配置', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (74, 73, 'system:security_config', '安全配置', 10, 0, '管理端权限：安全配置', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (75, 74, 'system:security_config:query', '安全配置详情', 10, 0, '管理端权限：安全配置详情', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (76, 74, 'system:security_config:update', '安全配置修改', 20, 0, '管理端权限：安全配置修改', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (77, 73, 'system:agreement_config', '软件协议配置', 20, 0, '管理端权限：软件协议配置', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (78, 77, 'system:agreement_config:query', '软件协议详情', 10, 0, '管理端权限：软件协议详情', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (79, 77, 'system:agreement_config:update', '软件协议修改', 20, 0, '管理端权限：软件协议修改', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (80, 73, 'system:es_index', '搜索索引配置', 30, 0, '管理端权限：搜索索引配置', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (81, 80, 'system:es_index:query', '搜索索引详情', 10, 0, '管理端权限：搜索索引详情', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (82, 80, 'system:es_index:rebuild', '重建搜索索引', 20, 0, '管理端权限：重建搜索索引', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (83, 0, 'system:log', '系统日志', 40, 0, '管理端权限：系统日志', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (84, 83, 'system:operation-log', '操作日志', 10, 0, '管理端权限：操作日志', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (85, 84, 'system:operation-log:list', '操作日志列表', 10, 0, '管理端权限：操作日志列表', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (86, 84, 'system:operation-log:query', '操作日志详情', 20, 0, '管理端权限：操作日志详情', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (87, 84, 'system:operation-log:delete', '操作日志删除', 30, 0, '管理端权限：操作日志删除', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (88, 83, 'system:login_log', '登录日志', 20, 0, '管理端权限：登录日志', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (89, 88, 'system:login_log:list', '登录日志列表', 10, 0, '管理端权限：登录日志列表', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (90, 88, 'system:login_log:query', '登录日志详情', 20, 0, '管理端权限：登录日志详情', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (91, 88, 'system:login_log:delete', '登录日志删除', 30, 0, '管理端权限：登录日志删除', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (92, 0, 'system:llm', '大模型管理', 50, 0, '管理端权限：大模型管理', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (93, 92, 'system:knowledge_base', '知识库', 10, 0, '管理端权限：知识库', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (94, 93, 'system:knowledge_base:list', '知识库列表', 10, 0, '管理端权限：知识库列表', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (95, 93, 'system:knowledge_base:query', '知识库详情', 20, 0, '管理端权限：知识库详情', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (96, 93, 'system:knowledge_base:add', '知识库新增', 30, 0, '管理端权限：知识库新增', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (97, 93, 'system:knowledge_base:update', '知识库修改', 40, 0, '管理端权限：知识库修改', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (98, 93, 'system:knowledge_base:delete', '知识库删除', 50, 0, '管理端权限：知识库删除', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (99, 93, 'system:kb_document', '知识库文档', 60, 0, '管理端权限：知识库文档', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (100, 99, 'system:kb_document:list', '文档列表', 10, 0, '管理端权限：文档列表', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (101, 99, 'system:kb_document:query', '文档详情', 20, 0, '管理端权限：文档详情', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (102, 99, 'system:kb_document:update', '文档修改', 30, 0, '管理端权限：文档修改', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (103, 99, 'system:kb_document:delete', '文档删除', 40, 0, '管理端权限：文档删除', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (104, 99, 'system:kb_document:import', '文档导入', 50, 0, '管理端权限：文档导入', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (105, 93, 'system:kb_document_chunk', '知识库分段', 70, 0, '管理端权限：知识库分段', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (106, 105, 'system:kb_document_chunk:list', '分段列表', 10, 0, '管理端权限：分段列表', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (107, 105, 'system:kb_document_chunk:add', '分段新增', 20, 0, '管理端权限：分段新增', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (108, 105, 'system:kb_document_chunk:update', '分段修改', 30, 0, '管理端权限：分段修改', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (109, 105, 'system:kb_document_chunk:delete', '分段删除', 40, 0, '管理端权限：分段删除', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (110, 92, 'system:llm_provider', '模型提供商', 20, 0, '管理端权限：模型提供商', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (111, 110, 'system:llm_provider:list', '提供商列表', 10, 0, '管理端权限：提供商列表', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (112, 110, 'system:llm_provider:query', '提供商详情', 20, 0, '管理端权限：提供商详情', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (113, 110, 'system:llm_provider:add', '提供商新增', 30, 0, '管理端权限：提供商新增', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (114, 110, 'system:llm_provider:update', '提供商修改', 40, 0, '管理端权限：提供商修改', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (115, 110, 'system:llm_provider:test', '提供商连通性测试', 50, 0, '管理端权限：提供商连通性测试', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (116, 110, 'system:llm_provider:delete', '提供商删除', 60, 0, '管理端权限：提供商删除', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (117, 110, 'system:llm_provider_model', '提供商模型', 70, 0, '管理端权限：提供商模型', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (118, 117, 'system:llm_provider_model:list', '模型列表', 10, 0, '管理端权限：模型列表', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (119, 117, 'system:llm_provider_model:add', '模型新增', 20, 0, '管理端权限：模型新增', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (120, 117, 'system:llm_provider_model:update', '模型修改', 30, 0, '管理端权限：模型修改', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (121, 117, 'system:llm_provider_model:delete', '模型删除', 40, 0, '管理端权限：模型删除', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (122, 92, 'system:agent_config', '系统模型配置', 30, 0, '管理端权限：系统模型配置', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (123, 122, 'system:agent_config:admin', '管理端配置', 10, 0, '管理端权限：管理端配置', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (124, 123, 'system:agent_config:admin:query', '管理端配置详情', 10, 0, '管理端权限：管理端配置详情', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (125, 123, 'system:agent_config:admin:update', '管理端配置修改', 20, 0, '管理端权限：管理端配置修改', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (126, 122, 'system:agent_config:client', '客户端配置', 20, 0, '管理端权限：客户端配置', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (127, 126, 'system:agent_config:client:query', '客户端配置详情', 10, 0, '管理端权限：客户端配置详情', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (128, 126, 'system:agent_config:client:update', '客户端配置修改', 20, 0, '管理端权限：客户端配置修改', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (129, 122, 'system:agent_config:common', '通用能力', 30, 0, '管理端权限：通用能力', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (130, 129, 'system:agent_config:common:query', '通用能力详情', 10, 0, '管理端权限：通用能力详情', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (131, 129, 'system:agent_config:common:update', '通用能力修改', 20, 0, '管理端权限：通用能力修改', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (132, 92, 'system:agent_prompt', '提示词管理', 40, 0, '管理端权限：提示词管理', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (133, 132, 'system:agent_prompt:list', '提示词列表', 10, 0, '管理端权限：提示词列表', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (134, 132, 'system:agent_prompt:query', '提示词详情', 20, 0, '管理端权限：提示词详情', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (135, 132, 'system:agent_prompt:update', '提示词修改', 30, 0, '管理端权限：提示词修改', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (136, 132, 'system:agent_prompt:rollback', '提示词回滚', 40, 0, '管理端权限：提示词回滚', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (137, 132, 'system:agent_prompt:sync', '提示词同步', 50, 0, '管理端权限：提示词同步', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (138, 132, 'system:agent_prompt:delete', '提示词删除', 60, 0, '管理端权限：提示词删除', '2026-04-25 11:46:17', '2026-04-25 11:46:17', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (1000, 0, 'system:smart_assistant', '智能助手', 60, 0, '管理端权限：智能助手入口', '2026-04-25 15:00:52', '2026-04-25 15:00:52', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (1005, 92, 'system:agent_trace', '智能体观测', 70, 0, '智能体观测菜单权限', '2026-05-02 02:26:04', '2026-05-02 02:26:04', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (1006, 1005, 'system:agent_trace:monitor', '监控面板', 10, 0, '智能体观测监控面板权限', '2026-05-02 02:26:04', '2026-05-02 02:26:04', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (1007, 1005, 'system:agent_trace:list', '智能体跟踪', 20, 0, '智能体跟踪列表权限', '2026-05-02 02:26:04', '2026-05-02 02:26:04', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (1008, 1007, 'system:agent_trace:query', '查看跟踪详情', 10, 0, '智能体跟踪详情权限', '2026-05-02 02:26:04', '2026-05-02 02:26:04', 'system', 'system');
INSERT INTO `permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `sort_order`, `status`, `remark`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (1009, 1007, 'system:agent_trace:delete', '删除跟踪记录', 20, 0, '智能体跟踪删除权限', '2026-05-02 02:26:04', '2026-05-02 02:26:04', 'system', 'system');
COMMIT;

-- ----------------------------
-- Table structure for role
-- ----------------------------
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_code` varchar(50) NOT NULL COMMENT '角色编码',
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `status` tinyint DEFAULT '1' COMMENT '状态（1:启用, 0:禁用）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';

-- ----------------------------
-- Records of role
-- ----------------------------
BEGIN;
INSERT INTO `role` (`id`, `role_code`, `role_name`, `remark`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (1, 'super_admin', '超级管理员', '系统内置超级管理员角色', 0, '2026-02-12 22:39:42', '2026-02-13 15:56:42', 'system', 'system');
INSERT INTO `role` (`id`, `role_code`, `role_name`, `remark`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (2, 'admin', '管理员', '系统管理员角色', 0, '2026-02-12 22:39:42', '2026-02-13 15:56:41', 'system', 'system');
INSERT INTO `role` (`id`, `role_code`, `role_name`, `remark`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (3, 'user', '普通用户', '系统普通用户角色', 0, '2026-02-12 22:39:42', '2026-02-13 15:56:39', 'system', 'system');
COMMIT;

-- ----------------------------
-- Table structure for role_permission
-- ----------------------------
DROP TABLE IF EXISTS `role_permission`;
CREATE TABLE `role_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `permission_id` bigint NOT NULL COMMENT '权限ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色权限中间表';

-- ----------------------------
-- Records of role_permission
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for sys_login_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID（失败时可为空）',
  `username` varchar(64) DEFAULT NULL COMMENT '登录账号',
  `login_source` varchar(32) NOT NULL COMMENT '登录来源：admin/client',
  `login_status` tinyint NOT NULL COMMENT '登录状态：0失败 1成功',
  `fail_reason` varchar(255) DEFAULT NULL COMMENT '失败原因',
  `login_type` varchar(32) DEFAULT 'password' COMMENT '登录方式',
  `ip_address` varchar(64) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` varchar(512) DEFAULT NULL COMMENT 'User-Agent',
  `device_type` varchar(64) DEFAULT NULL COMMENT '设备类型',
  `os` varchar(64) DEFAULT NULL COMMENT '操作系统',
  `browser` varchar(64) DEFAULT NULL COMMENT '浏览器',
  `login_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (`id`),
  KEY `idx_login_log_time_user_source_status` (`login_time`,`username`,`login_source`,`login_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统登录日志表';

-- ----------------------------
-- Records of sys_login_log
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for sys_operation_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `module` varchar(64) DEFAULT NULL COMMENT '业务模块',
  `action` varchar(128) DEFAULT NULL COMMENT '操作说明',
  `request_uri` varchar(255) DEFAULT NULL COMMENT '请求URI',
  `http_method` varchar(16) DEFAULT NULL COMMENT 'HTTP方法',
  `method_name` varchar(255) DEFAULT NULL COMMENT 'Controller方法名',
  `user_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `username` varchar(64) DEFAULT NULL COMMENT '操作人账号',
  `ip` varchar(64) DEFAULT NULL COMMENT '请求IP',
  `user_agent` varchar(512) DEFAULT NULL COMMENT 'User-Agent',
  `request_params` longtext COMMENT '请求参数(JSON)',
  `response_result` longtext COMMENT '返回结果(JSON)',
  `cost_time` bigint DEFAULT NULL COMMENT '耗时(ms)',
  `success` tinyint NOT NULL DEFAULT '1' COMMENT '是否成功：1成功 0失败',
  `error_msg` varchar(1000) DEFAULT NULL COMMENT '异常信息',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_operation_log_time_user_module_success` (`create_time`,`username`,`module`,`success`)
) ENGINE=InnoDB AUTO_INCREMENT=72 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统操作日志表';

-- ----------------------------
-- Records of sys_operation_log
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `nickname` varchar(256) DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(256) DEFAULT NULL COMMENT '头像',
  `email` varchar(256) DEFAULT NULL COMMENT '邮箱',
  `phone_number` varchar(256) DEFAULT NULL COMMENT '手机号',
  `gender` tinyint DEFAULT '0' COMMENT '性别',
  `birthday` date DEFAULT NULL COMMENT '生日',
  `username` varchar(256) NOT NULL COMMENT '用户名',
  `password` varchar(256) NOT NULL COMMENT '密码',
  `real_name` varchar(64) DEFAULT NULL COMMENT '真实姓名',
  `id_card` varchar(64) DEFAULT NULL COMMENT '身份证号',
  `last_login_time` datetime DEFAULT NULL COMMENT '上次登陆时间',
  `last_login_ip` varchar(64) DEFAULT NULL COMMENT '上次登陆IP',
  `status` varchar(256) NOT NULL DEFAULT '0' COMMENT '状态',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(256) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(256) DEFAULT NULL COMMENT '更新人',
  `delete_time` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of user
-- ----------------------------
BEGIN;
INSERT INTO `user` (`id`, `nickname`, `avatar`, `email`, `phone_number`, `gender`, `birthday`, `username`, `password`, `real_name`, `id_card`, `last_login_time`, `last_login_ip`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `delete_time`, `is_delete`) VALUES (1, '开源管理员', NULL, 'admin@example.com', '18888888888', 0, NULL, 'admin', '$2a$10$M/wHbN5ENWNUqAj.URs.GOQjmM6I/9pzbZL4zpUm8MQ21AI.5/lgC', '管理员', NULL, NULL, '127.0.0.1', '0', '2025-09-07 08:49:57', '2026-04-27 22:15:29', NULL, NULL, NULL, 0);
COMMIT;

-- ----------------------------
-- Table structure for user_address
-- ----------------------------
DROP TABLE IF EXISTS `user_address`;
CREATE TABLE `user_address` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '地址ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `receiver_name` varchar(50) NOT NULL COMMENT '收货人姓名',
  `receiver_phone` varchar(20) NOT NULL COMMENT '收货人手机号',
  `address` varchar(256) NOT NULL COMMENT '地址(省市区县街道等)',
  `detail_address` varchar(200) NOT NULL COMMENT '详细地址(如小区名、栋号、门牌)',
  `is_default` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否默认地址 1是 0否',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`) COMMENT '用户ID索引',
  KEY `idx_is_default` (`user_id`,`is_default`) COMMENT '默认地址索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户收货地址表';

-- ----------------------------
-- Records of user_address
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for user_coupon
-- ----------------------------
DROP TABLE IF EXISTS `user_coupon`;
CREATE TABLE `user_coupon` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户优惠券ID',
  `template_id` bigint NOT NULL COMMENT '优惠券模板ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `coupon_name_snapshot` varchar(128) NOT NULL COMMENT '优惠券名称快照',
  `threshold_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '使用门槛金额快照',
  `total_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '优惠券初始总金额',
  `available_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '当前可用金额',
  `locked_consume_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '当前锁定消耗金额',
  `continue_use_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否允许继续使用：1-允许，0-不允许',
  `stackable_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否允许叠加：1-允许，0-不允许',
  `effective_time` datetime NOT NULL COMMENT '生效时间',
  `expire_time` datetime NOT NULL COMMENT '失效时间',
  `coupon_status` varchar(32) NOT NULL COMMENT '用户优惠券状态',
  `source_type` varchar(32) NOT NULL COMMENT '来源类型',
  `source_biz_no` varchar(64) DEFAULT NULL COMMENT '来源业务号',
  `lock_order_no` varchar(64) DEFAULT NULL COMMENT '锁定订单号',
  `lock_time` datetime DEFAULT NULL COMMENT '锁定时间',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_user_coupon_status_time` (`user_id`,`coupon_status`,`effective_time`,`expire_time`),
  KEY `idx_user_coupon_lock_order` (`lock_order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户优惠券表';

-- ----------------------------
-- Records of user_coupon
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for user_role
-- ----------------------------
DROP TABLE IF EXISTS `user_role`;
CREATE TABLE `user_role` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色中间表';

-- ----------------------------
-- Records of user_role
-- ----------------------------
BEGIN;
INSERT INTO `user_role` (`user_id`, `role_id`) VALUES (6, 2);
INSERT INTO `user_role` (`user_id`, `role_id`) VALUES (1, 2);
INSERT INTO `user_role` (`user_id`, `role_id`) VALUES (6, 3);
INSERT INTO `user_role` (`user_id`, `role_id`) VALUES (2, 3);
INSERT INTO `user_role` (`user_id`, `role_id`) VALUES (1, 1);
INSERT INTO `user_role` (`user_id`, `role_id`) VALUES (7, 1);
COMMIT;

-- ----------------------------
-- Table structure for user_wallet
-- ----------------------------
DROP TABLE IF EXISTS `user_wallet`;
CREATE TABLE `user_wallet` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID，对应系统用户表ID',
  `wallet_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '钱包编号，唯一标识一个用户的钱包',
  `balance` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '可用余额',
  `frozen_balance` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '冻结金额（提现中或仲裁中）',
  `total_income` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '累计入账金额（充值、退款等）',
  `total_expend` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '累计支出金额（消费、提现等）',
  `currency` char(3) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'CNY' COMMENT '币种，默认人民币',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '状态：0正常，1冻结',
  `freeze_reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '冻结原因',
  `freeze_time` datetime DEFAULT NULL COMMENT '冻结时间',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '备注',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wallet_no` (`wallet_no`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户钱包表';

-- ----------------------------
-- Records of user_wallet
-- ----------------------------
BEGIN;
INSERT INTO `user_wallet` (`id`, `user_id`, `wallet_no`, `balance`, `frozen_balance`, `total_income`, `total_expend`, `currency`, `status`, `freeze_reason`, `freeze_time`, `version`, `is_deleted`, `remark`, `created_at`, `updated_at`) VALUES (1, 1, 'demo_wallet_0001', 0.00, 0.00, 0.00, 0.00, 'CNY', 0, NULL, NULL, 0, 0, '开源演示钱包', '2025-11-08 08:29:56', '2025-11-08 08:29:56');
COMMIT;

-- ----------------------------
-- Table structure for user_wallet_log
-- ----------------------------
DROP TABLE IF EXISTS `user_wallet_log`;
CREATE TABLE `user_wallet_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `wallet_id` bigint unsigned NOT NULL COMMENT '关联钱包ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID（冗余存储，便于查询）',
  `flow_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '流水编号，例如 WALFLOW202510310001',
  `reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '原因说明',
  `biz_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务关联单号（如订单号、提现单号等）',
  `change_type` tinyint(1) NOT NULL COMMENT '变动类型：1收入、2支出、3冻结、4解冻',
  `amount` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '变动金额',
  `before_balance` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '变动前余额',
  `after_balance` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '变动后余额',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注说明（如：支付订单#1001）',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(0未删除1已删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_no` (`flow_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_wallet_id` (`wallet_id`),
  KEY `idx_biz_type` (`reason`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='钱包流水记录表';

-- ----------------------------
-- Records of user_wallet_log
-- ----------------------------
BEGIN;
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
