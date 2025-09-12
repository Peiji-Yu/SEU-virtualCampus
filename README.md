CREATE TABLE user (
    card_number INT PRIMARY KEY,
    password VARCHAR(255) NOT NULL, -- 存储加密后的密码
    id VARCHAR(18) NOT NULL
);

CREATE TABLE student (
    identity VARCHAR(18) UNIQUE NOT NULL,      -- 身份证号，唯一，非空
    card_number INT PRIMARY KEY,       -- 一卡通号，主键
    student_number VARCHAR(8) UNIQUE,      -- 学号，唯一
    major VARCHAR(50),                     -- 专业名称
    school VARCHAR(50),                    -- 学院名称
    status ENUM('ENROLLED', 'GRADUATED', 'DROPPED', 'SUSPENDED') NOT NULL, -- 学籍状态
    enrollment DATE,                       -- 入学日期
    birth DATE,                            -- 出生日期
    birth_place VARCHAR(50),              -- 籍贯
    political_stat ENUM('PARTY_MEMBER', 'LEAGUE_MEMBER', 'MASSES', 'OTHER'), -- 政治面貌
    gender ENUM('MALE', 'FEMALE') NOT NULL,-- 性别
    name VARCHAR(50) NOT NULL             -- 姓名
);
