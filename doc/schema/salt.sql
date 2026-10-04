-- salt 食盐专营管理与食盐质量安全监管 -- schema (chen-022)
-- 列名与基线实体契约（@TableName/@TableField）逐列对齐，改列必须同步实体。
-- 库：chen_022

CREATE TABLE IF NOT EXISTS t_salt_ent (
  id bigint NOT NULL COMMENT '主键',
  site_no varchar(64) DEFAULT NULL COMMENT '食盐定点企业代号',
  site_name varchar(128) DEFAULT NULL COMMENT '企业全称',
  site_type varchar(32) DEFAULT NULL COMMENT '企业类别',
  road_name varchar(128) DEFAULT NULL COMMENT '属地那一路(省—市—县)',
  contact_person varchar(64) DEFAULT NULL COMMENT '联络人（空着几季没人补，条目当场判分不出）',
  status int DEFAULT NULL COMMENT '名录情形 0在册 1已摘牌',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='食盐定点企业总名录';

CREATE TABLE IF NOT EXISTS t_salt_iod_line (
  id bigint NOT NULL COMMENT '主键',
  rule_code varchar(64) DEFAULT NULL COMMENT '碘含量分档判定线代号',
  rule_name varchar(128) DEFAULT NULL COMMENT '线名',
  th1_max decimal(12,3) DEFAULT NULL COMMENT '起算数值(毫克每千克)',
  th2_max decimal(12,3) DEFAULT NULL COMMENT '够线数值(毫克每千克)',
  th3_max decimal(12,3) DEFAULT NULL COMMENT '封顶数值(毫克每千克)',
  eff_start datetime DEFAULT NULL COMMENT '立线之日',
  eff_end datetime DEFAULT NULL COMMENT '让位之日(不含)',
  priority int DEFAULT NULL COMMENT '让位顺位(数值大的先说话)',
  status int DEFAULT NULL COMMENT '线的情形 0在场 1已停用',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='碘含量分档判定线';

CREATE TABLE IF NOT EXISTS t_salt_link_page (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '企业品种挂接代号',
  node_no int DEFAULT NULL COMMENT '适用区域层级序',
  site_id int DEFAULT NULL COMMENT '所属定点企业',
  site_no varchar(64) DEFAULT NULL COMMENT '所属企业代号',
  should_count int DEFAULT NULL COMMENT '应挂品种数',
  done_count int DEFAULT NULL COMMENT '已挂品种数',
  lack_count int DEFAULT NULL COMMENT '欠挂品种数',
  content varchar(255) DEFAULT NULL COMMENT '随页交来的标签要件',
  status int DEFAULT NULL COMMENT '挂接进展 0待挂 1已挂讫 2压页',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业品种挂接簿页';

CREATE TABLE IF NOT EXISTS t_salt_record_flow (
  id bigint NOT NULL COMMENT '主键',
  biz_no varchar(64) DEFAULT NULL COMMENT '跨省经营备案单代号',
  stage int DEFAULT NULL COMMENT '当前步次 0..3（递单受理/材料核验/生效确认/缴回封卷）',
  status int DEFAULT NULL COMMENT '备案单走到的那一步 0未起 1在办 2已封卷',
  content varchar(255) DEFAULT NULL COMMENT '一段一记',
  last_action varchar(64) DEFAULT NULL COMMENT '最近一次挪步动作',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='跨省经营备案单';

CREATE TABLE IF NOT EXISTS t_salt_renew_bill (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '延续换证核签单',
  node_no int DEFAULT NULL COMMENT '当前所在档口 0..2（县初核/市复审/省核准）',
  sign_mode int DEFAULT NULL COMMENT '同档算齐办法 0一名即可 1两名须同判',
  need_count int DEFAULT NULL COMMENT '本档应落人数',
  sign_count int DEFAULT NULL COMMENT '本档已落印数',
  status int DEFAULT NULL COMMENT '核签情形 0在核 1已办结 2已挪回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='延续换证核签单';

CREATE TABLE IF NOT EXISTS t_salt_spot_row (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) DEFAULT NULL COMMENT '监督抽检结果报送册码',
  row_no int DEFAULT NULL COMMENT '原报行次',
  item_code varchar(64) DEFAULT NULL COMMENT '被对上的定点企业代号',
  qty decimal(12,2) DEFAULT NULL COMMENT '本行不合格件数',
  hours_num decimal(12,2) DEFAULT NULL COMMENT '本行送检件数',
  status int DEFAULT NULL COMMENT '行落地情形 0未勾 1已收下 2已退回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='抽检结果报送核收行';

CREATE TABLE IF NOT EXISTS t_salt_year_task (
  id bigint NOT NULL COMMENT '主键',
  item_no varchar(64) DEFAULT NULL COMMENT '年检催办条目代号',
  ent_id bigint DEFAULT NULL COMMENT '拴着哪家企业(t_salt_ent.id)',
  biz_type int DEFAULT NULL COMMENT '事由 0年检催办 1换证提醒',
  due_at datetime DEFAULT NULL COMMENT '应开口那一日的止点时刻',
  amount decimal(12,2) DEFAULT NULL COMMENT '最可提前几个工作日开口（挑单与回写同一把尺数）',
  script_code varchar(64) DEFAULT NULL COMMENT '照哪一份话本去发(t_salt_script.script_code)',
  status int DEFAULT NULL COMMENT '条目情形 0待派 1已派出 2分不出',
  assigned_to bigint DEFAULT NULL COMMENT '派给了哪位执法员(t_salt_officer.id)',
  dispatched_at datetime DEFAULT NULL COMMENT '派出那一刻落下的时刻（与情形、执法员同一笔落）',
  fail_reason varchar(255) DEFAULT NULL COMMENT '分不出的原委（摘牌/联络人空/接满定额），日后按原委挑批',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='年检到期催办单';

-- 初始档案数据（状态约定：id=1 启用 / id=2 停用）
-- t_salt_ent 预置两条种子名录：id=0 在册、id=1 已摘牌；企业代号与类别按各表既有排法给值。
INSERT IGNORE INTO t_salt_ent (id, site_no, site_name, site_type, road_name, contact_person, status, del_flag, create_by, create_time)
VALUES (0, 'JY00', '盐泽制盐一厂（制盐企业，在册）', '制盐企业', '盐岭省—盐泽市—临卤县', '卤大有', 0, 0, 'seed', NOW()),
       (1, 'JY01', '青卤批发部（转行后摘牌）', '省内批发企业', '盐岭省—盐泽市—青卤县', NULL, 1, 0, 'seed', NOW());

-- 话本：按事由与企业类别对号，条目只存代号
CREATE TABLE IF NOT EXISTS t_salt_script (
  id bigint NOT NULL COMMENT '主键',
  script_code varchar(64) DEFAULT NULL COMMENT '话本代号（条目拴这一格）',
  script_name varchar(128) DEFAULT NULL COMMENT '话本名',
  biz_type int DEFAULT NULL COMMENT '事由 0年检催办 1换证提醒',
  site_type varchar(32) DEFAULT NULL COMMENT '对哪一类企业（空=各类通用）',
  body_text varchar(1000) DEFAULT NULL COMMENT '话本正文',
  status int DEFAULT NULL COMMENT '话本情形 0启用 1停用',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='催办换证话本';

-- 执法员：休办日、接得动的案件类别、当日定额都在这张表上
CREATE TABLE IF NOT EXISTS t_salt_officer (
  id bigint NOT NULL COMMENT '主键',
  officer_no varchar(64) DEFAULT NULL COMMENT '执法员代号',
  officer_name varchar(64) DEFAULT NULL COMMENT '执法员姓名',
  accept_types varchar(255) DEFAULT NULL COMMENT '接得动的企业类别，逗号隔开（空=各类都接）',
  daily_quota int DEFAULT NULL COMMENT '当日接活定额（空=不限）',
  leave_days varchar(500) DEFAULT NULL COMMENT '当日休办的日子，yyyy-MM-dd 逗号隔开（个人休办，与全省日历无关）',
  status int DEFAULT NULL COMMENT '在岗情形 0在岗 1停岗',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='盐政执法员名册';

-- 一把尺：非工作日与集中办案窗口，挑单/回写/屏幕都只认这一处
-- day_kind 0国家放假 1省里通令不办公 2全省集中办案窗口；
-- 落在 0/1 的日子数工作日时不数；落在 2 的日子窗口冻结，已到档也不许开口，等下一轮。
-- 未登记的周一至周五按工作日计（跨夜那一段本就不数，只按整日）。
CREATE TABLE IF NOT EXISTS t_salt_day (
  id bigint NOT NULL COMMENT '主键',
  day_date date NOT NULL COMMENT '哪一日',
  day_kind int NOT NULL COMMENT '日档 0国家放假 1省令停办 2集中办案窗口',
  day_name varchar(128) DEFAULT NULL COMMENT '名目（如国庆、全省集中办案）',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_salt_day (day_date, day_kind)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='非工作日与办案窗口日历';

-- 话本种子：年检/换证各一份通用
INSERT IGNORE INTO t_salt_script (id, script_code, script_name, biz_type, site_type, body_text, status, del_flag, create_by, create_time)
VALUES (0, 'SCRIPT_YEAR_COMMON', '年检催办通用话本', 0, NULL, '贵单位食盐定点许可证年检已到期，请于五个工作日内办理。', 0, 0, 'seed', NOW()),
       (1, 'SCRIPT_RENEW_COMMON', '换证提醒通用话本', 1, NULL, '贵单位食盐定点许可证有效期将满，请及时申请延续换证。', 0, 0, 'seed', NOW());

-- 执法员种子：两位在岗，第三位定额为1，专门压"接满定额改判分不出"的情形
INSERT IGNORE INTO t_salt_officer (id, officer_no, officer_name, accept_types, daily_quota, status, del_flag, create_by, create_time)
VALUES (0, 'ZF00', '严盐政', NULL, 10, 0, 0, 'seed', NOW()),
       (1, 'ZF01', '管咸事', '制盐企业', 10, 0, 0, 'seed', NOW()),
       (2, 'ZF02', '定额一', NULL, 1, 0, 0, 'seed', NOW());

-- 存量库升级（已是旧结构的库执行；新库上面 CREATE 即新结构，勿重复执行）：
-- ALTER TABLE t_salt_ent ADD COLUMN contact_person varchar(64) DEFAULT NULL COMMENT '联络人' AFTER road_name;
-- ALTER TABLE t_salt_year_task
--   ADD COLUMN ent_id bigint DEFAULT NULL COMMENT '拴着哪家企业' AFTER item_no,
--   ADD COLUMN biz_type int DEFAULT NULL COMMENT '事由 0年检催办 1换证提醒' AFTER ent_id,
--   ADD COLUMN script_code varchar(64) DEFAULT NULL COMMENT '照哪一份话本去发' AFTER amount,
--   ADD COLUMN assigned_to bigint DEFAULT NULL COMMENT '派给了哪位执法员' AFTER status,
--   ADD COLUMN dispatched_at datetime DEFAULT NULL COMMENT '派出那一刻' AFTER assigned_to,
--   ADD COLUMN fail_reason varchar(255) DEFAULT NULL COMMENT '分不出原委' AFTER dispatched_at,
--   DROP COLUMN content;

