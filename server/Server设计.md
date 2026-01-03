# HABMS.server

属于server部分，子项目在./server文件夹中

## Server类

包含main方法，是后端的启动入口。

职责：启动、初始化、监听Socket接入并新建Service实例处理业务

附加初始化任务：读取配置文件"department.json"，初始化全局department列表

## Service类

职责：对于每个Socket一个实例，处理业务逻辑

实例应该存储当前连接对应的Account或DoctorAccount数据

实现网络接口功能（见`网络信息格式.md`）
其中：department查询和存在判断通过全局department列表

数据对象与数据库接口见`数据对象与数据库接口.md`

处理流程：接收->校验（数据完整性检验、登录校验、权限校验）->处理->返回

## 附加任务

注释清晰，需要详细文档
