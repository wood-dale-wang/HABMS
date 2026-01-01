# HABMS.client

属于client部分，子项目在./client文件夹中

## App类

JavaFX 应用的启动入口。

职责：
- 启动 JavaFX 应用
- 初始化主舞台 (Stage)
- 提供静态方法 `setRoot` 用于在 Controller 中切换场景 (Scene)

## Controller层

负责处理界面交互逻辑。

### LoginController
- 处理用户登录和注册。
- **登录**：根据输入长度判断是患者(PID/Phone)还是医生(DID)，对密码进行 SHA-256 加密后发送请求。
- **注册**：收集用户注册信息，发送注册请求。

### PatientMainController
- **预约挂号 Tab**：
    - 加载科室列表 (`department_list`)
    - 联动查询医生列表 (`doctor_query`)
    - 联动查询排班信息 (`schedule_by_doctor`)
    - 发起预约请求 (`appointment_create`)
- **我的预约 Tab**：
    - 查询当前用户的预约记录 (`appointment_list`)
    - 取消预约 (`appointment_cancel`)
- **个人中心 Tab**：
    - 显示和修改个人信息 (`account_update`)
    - 注销账号 (`account_delete`)
    - 退出登录 (`account_logout`)

### DoctorMainController
- **我的排班 Tab**：
    - 查询当前医生的排班信息 (`schedule_by_doctor`)
- **叫号诊疗 Tab**：
    - 选择当前工作的排班
    - 刷新候诊列表 (`doctor_appointments`)
    - 呼叫下一位患者 (`doctor_call_next`)
    - 完成诊疗
- **医院管理 Tab** (仅管理员可见)：
    - 批量导入数据 (Excel)
    - 导出预约记录
    - 生成统计报告
- **个人信息 Tab**：
    - 显示医生个人信息

## Model层

### 数据对象
- `User`: 患者信息
- `Doctor`: 医生信息
- `Schedule`: 排班信息
- `Appointment`: 预约记录

### 通信对象
- `Request`: 发送给服务器的请求对象 (type, data)
- `Response`: 服务器返回的响应对象 (Statu, data)

## Net层 (NetworkClient)

- 单例模式 (`getInstance`)
- 维护与服务器的 TCP `Socket` 连接
- 提供 `sendRequest(Request)` 方法：
    - 将 Request 对象序列化为 JSON 字符串发送
    - 阻塞等待服务器响应
    - 将接收到的 JSON 字符串反序列化为 Response 对象返回

## Session类

- 静态存储当前登录的 `User` 对象，用于在不同 Controller 之间共享用户信息。

## View层 (FXML)

- `login.fxml`: 登录与注册界面
- `patient_main.fxml`: 患者主界面 (TabPane 布局)
- `admin_main.fxml`: (预留) 医生/管理员主界面
