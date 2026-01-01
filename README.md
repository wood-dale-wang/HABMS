# 飞马星球医院预约挂号系统 (HABMS)

## 结构与规划

### Maven 多模块

- **父工程**：根目录 `pom.xml`（聚合 `server` 与 `client`）
- **模块**：
    - `server`：包含数据库逻辑 (`HABMS.db`) 与服务端逻辑 (`HABMS.server`)
    - `client`：包含客户端逻辑 (`HABMS.client`)，使用 JavaFX 实现

### 架构设计

- **C/S 架构**：客户端和服务端通过 TCP Socket 连接。
- **通信协议**：使用 JSON 格式传递数据（Request/Response）。
- **分层设计**：
    - 客户端：View (FXML) -> Controller -> Net -> Server
    - 服务端：Service (Thread) -> DB Interface -> MariaDB

```text
+--------+     +--------+
| server | <-> |   db   |
+--------+     +--------+
    ^
    | JSON / TCP
    v
+--------+
| client |
+--------+
```

## 项目简介

本项目是“面向对象技术（Java）大作业二”的实现，旨在为飞马星球医院开发一套预约挂号系统。系统采用 C/S 架构，基于 Java 17 开发，使用 Maven 进行项目管理。

### 主要功能

*   **患者端**：
    *   注册与登录（支持 SHA-256 密码加密）
    *   科室查询与医生查询
    *   查看医生排班信息
    *   预约挂号
    *   我的预约管理（查看、取消）
    *   个人信息修改与账号注销
*   **医生/管理端**：
    *   医生登录
    *   查看我的排班
    *   叫号诊疗（查看候诊列表、呼叫下一位）
    *   医院管理（仅管理员）：
        *   批量导入医生/排班数据（Excel）
        *   导出预约记录
        *   生成统计报告
*   **服务端**：
    *   基于 TCP Socket 的多线程网络服务
    *   JSON 数据通信协议
    *   MariaDB 数据库持久化存储
    *   科室数据管理

## 项目结构

```text
HABMS/
├── client/                 # 客户端模块 (JavaFX)
│   ├── src/main/java/      # 源代码
│   └── src/main/resources/ # FXML 视图文件
├── server/                 # 服务端模块
│   ├── src/main/java/      # 源代码 (含数据库逻辑)
│   ├── src/main/resources/ # 数据库初始化脚本
│   └── department.json     # 科室配置文件
├── pom.xml                 # 父工程 Maven 配置
├── 数据库设计.txt          # 数据库表结构设计文档
├── 网络信息格式.md         # 通信协议文档
└── README.md               # 项目说明文档
```

## 环境要求

*   **JDK**: Java 17 或更高版本
*   **Maven**: 3.6+
*   **数据库**: MariaDB (推荐 10.5+)

## 快速开始

### 1. 数据库配置

1.  安装 MariaDB 数据库。
2.  创建数据库用户 `rjava`，密码 `rjava`，并授予权限：
    ```sql
    CREATE USER 'rjava'@'%' IDENTIFIED BY 'rjava';
    GRANT ALL PRIVILEGES ON *.* TO 'rjava'@'%';
    FLUSH PRIVILEGES;
    ```
3.  运行初始化脚本：
    *   脚本路径：`server/src/main/resources/init_habms.sql`
    *   该脚本会自动创建 `HABMSDB` 数据库及相关表结构，并插入默认管理员账号。

### 2. 编译项目

在项目根目录下运行：

```bash
mvn clean package
```

### 3. 运行服务端

进入 `server` 目录并启动服务：

```bash
cd server
mvn clean compile exec:java -Dexec.mainClass=HABMS.server.ServerMain
```

*   服务端默认监听端口：`9000`
*   配置文件：`server/department.json` (定义了医院的科室列表)

### 4. 运行客户端

进入 `client` 目录并启动应用：

```bash
cd client
mvn clean javafx:run
```

*   客户端启动后，可使用注册功能创建新账号，或使用管理员账号登录（需自行在数据库查看管理员账号，默认 `00000000`）。

## 开发文档

*   **数据库设计**：详见 `数据库设计.txt`
*   **通信协议**：详见 `网络信息格式.md`
*   **服务端设计**：详见 `Server设计.md`

## 成员分工

（在此处填写小组成员及分工信息）
