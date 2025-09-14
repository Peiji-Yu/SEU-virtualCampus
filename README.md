根据一卡通号判断用户类型：
- 学生：2开头，9位数
- 教师：1开头，9位数
- 管理员/教务：1000以内

user表：存储用户（包括学生、教师、管理员）信息

        CREATE TABLE user (
            card_number INT PRIMARY KEY,
            password VARCHAR(255) NOT NULL, -- 存储加密后的密码
            id VARCHAR(18) UNIQUE NOT NULL,
            name VARCHAR(50) NOT NULL
        );

student表：存储学生学籍信息

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

finance_card表：存储一卡通账户信息

        CREATE TABLE finance_card (
            card_number INT PRIMARY KEY,        -- 一卡通号，与user表中的card_number关联
            balance INT NOT NULL DEFAULT 0,     -- 余额（以分为单位）
            status ENUM('正常', '挂失') NOT NULL DEFAULT '正常',
            FOREIGN KEY (card_number) REFERENCES user(card_number)
        );

card_transaction表：存储一卡通交易记录

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

store_item表：存储商品信息

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

store_order表：订单主表

        CREATE TABLE store_order (
            uuid VARCHAR(36) PRIMARY KEY,       -- 订单编号
            card_number INT NOT NULL,           -- 用户一卡通号
            total_amount INT NOT NULL,          -- 订单总金额（以分为单位）
            time DATETIME NOT NULL,             -- 订单时间
            status ENUM('待支付', '已支付', '已取消') NOT NULL DEFAULT '待支付',
            remark TEXT,                        -- 订单备注
            FOREIGN KEY (card_number) REFERENCES finance_card(card_number)
        );

store_order_item表：订单商品明细表

        CREATE TABLE store_order_item (
            uuid VARCHAR(36) PRIMARY KEY,       -- 订单项ID
            order_uuid VARCHAR(36) NOT NULL,    -- 订单ID
            item_uuid VARCHAR(36) NOT NULL,     -- 商品ID
            item_price INT NOT NULL,            -- 商品单价（以分为单位）
            amount INT NOT NULL,                -- 商品数量
            FOREIGN KEY (order_uuid) REFERENCES store_order(uuid),
            FOREIGN KEY (item_uuid) REFERENCES store_item(uuid)
        );

book表：书籍信息表

        CREATE TABLE book (
            isbn VARCHAR(20) PRIMARY KEY,         -- ISBN作为主键
            name VARCHAR(255) NOT NULL,           -- 书名
            author VARCHAR(255),                  -- 作者
            publisher VARCHAR(255),               -- 出版社
            publish_date DATE,                    -- 出版日期
            description TEXT,                     -- 简介
            inventory INT NOT NULL DEFAULT 0,     -- 库存量
            category VARCHAR(50)                  -- 类别 (SCIENCE, LITERATURE...)
        );

book_item表：书籍副本表

        CREATE TABLE book_item (
            uuid CHAR(36) PRIMARY KEY,            -- 每本副本的唯一ID（UUID）
            isbn VARCHAR(20) NOT NULL,            -- 外键，对应书籍ISBN
            place VARCHAR(255),                   -- 馆藏位置
            book_status VARCHAR(20) NOT NULL,     -- 状态（INLIBRARY, LEND...）
            CONSTRAINT fk_book_item_book FOREIGN KEY (isbn) REFERENCES book(isbn)
        );

<<<<<<< HEAD
CREATE TABLE book_record (
    uuid CHAR(36) PRIMARY KEY,            -- 借阅记录UUID
    user_id INT NOT NULL,                 -- 用户一卡通号
    borrow_time DATE NOT NULL,            -- 借书时间
    due_time DATE NOT NULL,               -- 到期时间
    name VARCHAR(255) NOT NULL,           -- 书名
);

        CREATE TABLE lib_user (
            user_id INT PRIMARY KEY,              -- 用户一卡通号
            borrowed INT NOT NULL DEFAULT 0,      -- 当前已借书数量
            max_borrowed INT NOT NULL,            -- 最大可借书数量
            user_status VARCHAR(20) NOT NULL      -- 用户状态 (BORROWING, FREE, OVERDUE, TRUSTBREAK)
        );


=======
book_record表：借阅记录表

        CREATE TABLE book_record (
            uuid CHAR(36) PRIMARY KEY,            -- 借阅记录UUID
            user_id INT NOT NULL,                 -- 用户一卡通号
            borrow_time DATE NOT NULL,            -- 借书时间
            due_time DATE NOT NULL,               -- 到期时间
            name VARCHAR(255) NOT NULL            -- 书名
        );
>>>>>>> 4754b8772aa31ff689fccd1fb7c685437413d647

students表: 学生表

        CREATE TABLE students (
            card_number INT(9) UNSIGNED ZEROFILL PRIMARY KEY,   --一卡通号
            student_number INT(8) UNSIGNED ZEROFILL UNIQUE NOT NULL,  --学号
            name VARCHAR(50) NOT NULL,     --姓名
            major VARCHAR(100) NOT NULL,    --专业
            school VARCHAR(100) NOT NULL,    --学院
            status ENUM('在校', '休学', '退学', '毕业') DEFAULT '在校',  --学籍状态
            -- 添加检查约束确保卡号和学号长度正确
    CONSTRAINT chk_card_number_length CHECK (LENGTH(card_number) = 9),
    CONSTRAINT chk_student_number_length CHECK (LENGTH(student_number) = 8)
    );

courses表: 课程表

        CREATE TABLE courses (
            course_id VARCHAR(20) PRIMARY KEY,   --课程编号
            course_name VARCHAR(100) NOT NULL,   --课程名
            school VARCHAR(100) NOT NULL,    --开设学院
            credit FLOAT NOT NULL,   --学分
        );

teachers表: 教师表

        CREATE TABLE teachers (
            teacher_id INT PRIMARY KEY,  --教师id
            name VARCHAR(100) NOT NULL,  --教师姓名
            school VARCHAR(100) NOT NULL,  --所属学院
            title VARCHAR(50),   --职称
        );

teaching_classes表: 教学班表

        CREATE TABLE teaching_classes (
            uuid VARCHAR(36) PRIMARY KEY,  --标识符
            course_id VARCHAR(20) NOT NULL,   --课程id
            teacher_name VARCHAR(100) NOT NULL, -- 教师姓名
            schedule JSON,  --上课信息
            place VARCHAR(100),  --上课地点
            capacity INT NOT NULL DEFAULT 0, --课容量
            selected_count INT NOT NULL DEFAULT 0,  --已选人数
            FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE
        );

student_teaching_class表: 选课关系表

        CREATE TABLE student_teaching_class (
            id INT AUTO_INCREMENT PRIMARY KEY,  
            student_card_number INT(9) UNSIGNED ZEROFILL NOT NULL,  --学生一卡通号
            teaching_class_uuid VARCHAR(36) NOT NULL,   --教学班uuid
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            UNIQUE KEY unique_student_class (student_card_number, teaching_class_uuid),
            FOREIGN KEY (student_card_number) REFERENCES students(card_number) ON DELETE CASCADE,
            FOREIGN KEY (teaching_class_uuid) REFERENCES teaching_classes(uuid) ON DELETE CASCADE
        );

teaching_class_students图: 教学班学生列表视图

        CREATE VIEW teaching_class_students AS
        SELECT 
            stc.teaching_class_uuid,
            s.card_number,
            s.student_number,
            s.name,
            s.major,
            s.school,
            s.status
        FROM student_teaching_class stc
        JOIN students s ON stc.student_card_number = s.card_number;

student_selected_courses图：学生已选课程视图

        CREATE VIEW student_selected_courses AS
        SELECT 
            stc.student_card_number,
            tc.uuid as teaching_class_uuid,
            tc.course_id,
            c.course_name,
            c.school as course_school,
            c.credit,
            tc.teacher_name,
            tc.schedule,
            tc.place,
            tc.capacity,
            tc.selected_count
        FROM student_teaching_class stc
        JOIN teaching_classes tc ON stc.teaching_class_uuid = tc.uuid
        JOIN courses c ON tc.course_id = c.course_id;
<<<<<<< HEAD


=======
>>>>>>> 4754b8772aa31ff689fccd1fb7c685437413d647
