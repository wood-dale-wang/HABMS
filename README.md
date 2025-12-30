# HABMS

## 结构

Maven 多模块：
- 父工程：根目录 `pom.xml`（聚合 `server` 与 `client`）
- 模块：`server`（已包含原 db 代码与后续 server 代码）、`client`（占位，待实现）

包名规划：
- 根包名：HABMS
- 数据库与服务端包：HABMS.db / HABMS.server（server 模块内）
- 客户端包：HABMS.client（client 模块内，待实现）

## 运行

要求：

- Java 17+
- Maven
- 自己装 MariaDB（建议在 WSL 内安装，省事）
- 新建数据库：HABMSDB
- 新建账户：'rjava'@'%'，密码：rjava
- 设置数据库使用账户：rjava

构建：
- 根目录打包全部：`mvn clean package`
- 仅服务端（含 db）：`mvn -pl server -am clean package`
- 仅客户端：`mvn -pl client clean package`

## 参考资料

数据库设计和接口参见数据库设计.txt
