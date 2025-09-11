CREATE TABLE user (
    card_number INT PRIMARY KEY,
    password VARCHAR(255) NOT NULL, -- 存储加密后的密码
    id VARCHAR(18) UNIQUE NOT NULL
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

-- finance_card表：存储一卡通账户信息
CREATE TABLE finance_card (
    card_number INT PRIMARY KEY,        -- 一卡通号，与user表中的card_number关联
    balance INT NOT NULL DEFAULT 0,     -- 余额（以分为单位）
    status ENUM('正常', '挂失') NOT NULL DEFAULT '正常',
    FOREIGN KEY (card_number) REFERENCES user(card_number)
);

-- card_transaction表：存储一卡通交易记录
CREATE TABLE card_transaction (
    uuid VARCHAR(36) PRIMARY KEY,       -- 交易记录ID
    card_number INT NOT NULL,           -- 一卡通号
    amount INT NOT NULL,                -- 交易金额（以分为单位，正数表示收入，负数表示支出）
    time DATETIME NOT NULL,             -- 交易时间
    type ENUM('充值', '消费', '退款') NOT NULL, -- 交易类型
    description VARCHAR(255),           -- 交易描述
    reference_id VARCHAR(36),           -- 关联的业务ID（如订单ID）
    FOREIGN KEY (card_number) REFERENCES finance_card(card_number)
);

-- store_item表：存储商品信息
CREATE TABLE store_item (
    uuid VARCHAR(36) PRIMARY KEY,      -- 商品编号
    item_name VARCHAR(100) NOT NULL,   -- 商品名称
    category VARCHAR(50) NOT NULL DEFAULT '其他'; -- 商品类别
    price INT NOT NULL,                -- 商品价格（以分为单位）
    picture_link VARCHAR(255),         -- 商品图片链接
    stock INT NOT NULL DEFAULT 0,      -- 商品库存数量
    sales_volume INT NOT NULL DEFAULT 0, -- 商品销量
    description TEXT,                  -- 商品信息描述
    barcode VARCHAR(13)                -- 商品条形码
);

-- store_order表：订单主表
CREATE TABLE store_order (
    uuid VARCHAR(36) PRIMARY KEY,       -- 订单编号
    card_number INT NOT NULL,           -- 用户一卡通号
    total_amount INT NOT NULL,          -- 订单总金额（以分为单位）
    time DATETIME NOT NULL,             -- 订单时间
    status ENUM('待支付', '已支付', '已取消') NOT NULL DEFAULT '待支付',
    remark TEXT,                        -- 订单备注
    FOREIGN KEY (card_number) REFERENCES finance_card(card_number)
);

-- store_order_item表：订单商品明细表
CREATE TABLE store_order_item (
    uuid VARCHAR(36) PRIMARY KEY,       -- 订单项ID
    order_uuid VARCHAR(36) NOT NULL,    -- 订单ID
    item_uuid VARCHAR(36) NOT NULL,     -- 商品ID
    item_price INT NOT NULL,            -- 商品单价（以分为单位）
    amount INT NOT NULL,                -- 商品数量
    FOREIGN KEY (order_uuid) REFERENCES store_order(uuid),
    FOREIGN KEY (item_uuid) REFERENCES store_item(uuid)
);
