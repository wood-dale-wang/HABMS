# HABMS Server TCP 接口（Service）

Service 以每行一个 JSON 消息进行通信。客户端发送形如：

```json
{"type":"...","data":{}}
```

服务端返回：

- 成功：`{"Statu":"ok","data":{...}}`
- 失败：`{"Statu":"err","data":{"err_info":"..."}}`

通用规则：

- 字段缺失、解析异常会返回 `err`。
- 登录态：同一 TCP 连接内缓存一个 Account 或 DoctorAccount 会话；`account_login` 和 `doctor_login` 会互斥切换。
- 时间字段使用 ISO-8601 文本（`LocalDateTime.parse` 可解析），例如 `2026-01-01T12:00:00`。
- 服务器会移除所有 passwordHex 字段，仅回传非敏感字段。

## 用户相关

### account_register

- data：`name,passwordHex,pid,phone,sex`
- 返回：Account（含 `aid`，不含 passwordHex）
- 失败：PID/phone 已存在

### account_login

- data：`(pid 或 phone), passwordHex`
- 返回：Account（不含 passwordHex）
- 失败：未找到、密码错误

### account_logout

- data：可选 `aid`（如提供需匹配当前会话）
- 返回：空对象
- 失败：未登录、aid 不匹配

### account_delete

- data：`aid`
- 返回：空对象
- 失败：未登录、aid 不匹配

### account_update

- data：完整 Account（需包含 aid、pid、phone、name、passwordHex、sex）
- 返回：更新后的 Account
- 失败：未登录、aid 不匹配、pid/phone 修改被拒绝

### appointment_list

- data：空
- 返回：当前账户的 Appointment 列表
- 失败：未登录

### appointment_create

- data：`did,sid`
- 返回：新 Appointment（已扣减容量）
- 失败：未登录、sid 不存在、did/sid 不匹配、容量为 0

### appointment_cancel

- data：`apid`
- 返回：更新后的 Appointment（状态已变更）
- 失败：未登录、apid 不存在、aid 与 apid 不匹配

## 公共查询

### department_list

- data：空
- 返回：科室字符串列表
- 失败：未登录

### doctor_query

- data（三选一）：`did` / `name` / `department`
- 返回：DoctorAccount 列表（无 passwordHex）
- 失败：未登录、缺少识别字段、科室不存在

### schedule_by_doctor

- data：`did`
- 返回：Schedule 列表
- 失败：未登录

### schedule_by_time

- data：`time, department`
- 返回：Schedule 列表（指定时间、科室覆盖的排班）
- 失败：未登录、科室不存在

## 医生端

### doctor_login

- data：`did,passwordHex`
- 返回：DoctorAccount（无 passwordHex）
- 失败：未找到、密码错误

### doctor_logout

- data：空
- 返回：空对象
- 失败：未登录

### doctor_schedules

- data：空
- 返回：当前医生的 Schedule 列表
- 失败：未登录

### doctor_appointments

- data：空
- 返回：预约当前医生的 Appointment 列表
- 失败：未登录

### doctor_call_next

- data：`sid, serialNumber`（serialNumber 为当前已叫号，返回下一个）
- 返回：下一个 Appointment（状态已更新为 Done）
- 失败：未登录、未找到下一个

## 管理员端（需 doctor.admin=true）

### admin_add_doctors

- data：`doctors` 数组，每项含 `name,passwordHex,department`，可选 `admin,describe`
- 逻辑：为每项生成 DID，插入 Doctor 表
- 返回：新医生列表（含 DID，描述字段为 description）
- 失败：未登录、非 admin、科室不存在、数据缺失

### admin_add_schedules

- data：`schedules` 数组，每项 `did,startTime,endTime,capacity`
- 逻辑：检查与已存在及新建排班的时间重叠（同 did），通过后批量插入
- 返回：新 Schedule 列表
- 失败：未登录、非 admin、排班时间重叠、字段缺失

### admin_update_schedule

- data：`sid` + 更新后的 `capacity`（did/startTime/endTime 视为不可变，仅可相等）
- 逻辑：按 delta 调整 capacity/res
- 返回：更新后的 Schedule 视图
- 失败：未登录、非 admin、sid 不存在、不可变字段被修改

### admin_all_appointments

- data：空
- 返回：所有 Appointment 列表（按 apid 去重聚合）
- 失败：未登录、非 admin

### admin_report

- data：空
- 返回：聚合报告 `{doctors:[], schedules:[], appointments:[]}`
- 失败：未登录、非 admin

## 返回数据字段视图

- Account：`aid,name,pid,phone,sex`
- Doctor：`did,name,admin,department,description`
- Schedule：`sid,did,startTime,endTime,capacity,res`
- Appointment：`serialNumber,apid,aid,did,sid,status,startTime,endTime`
