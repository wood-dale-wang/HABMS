package HABMS.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 主数据库逻辑对象：封装 JDBC CRUD 与核心业务事务。 */
public class HABMSDB {
    private final String url;
    private final String user;
    private final String password;

    public HABMSDB(String url, String user, String password) {
        this.url = Objects.requireNonNull(url);
        this.user = Objects.requireNonNull(user);
        this.password = Objects.requireNonNull(password);
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    // insert
    /** 新增患者账户。 */
    public void InsertAccount(Account account) throws SQLException {
        String sql = "INSERT INTO Account(AID,Name,Password,PID,Phone,Sex) VALUES (?,?,?,?,?,?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, account.getAid());
            ps.setString(2, account.getName());
            ps.setString(3, account.getPasswordHex());
            ps.setString(4, account.getPid());
            ps.setString(5, account.getPhone());
            ps.setString(6, account.getSex().name());
            ps.executeUpdate();
        }
    }

    /** 新增医生账户。 */
    public void InsertDoctorAccount(DoctorAccount doctor) throws SQLException {
        String sql = "INSERT INTO Doctor(DID,Name,Password,Admin,Department,Description) VALUES (?,?,?,?,?,?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, doctor.getDid());
            ps.setString(2, doctor.getName());
            ps.setString(3, doctor.getPasswordHex());
            ps.setBoolean(4, doctor.isAdmin());
            ps.setString(5, doctor.getDepartment());
            ps.setString(6, doctor.getDescription());
            ps.executeUpdate();
        }
    }

    /** 覆盖更新医生信息。 */
    public void UpdateDoctorAccount(DoctorAccount doctor) throws SQLException {
        String sql = "UPDATE Doctor SET Name=?, Password=?, Admin=?, Department=?, Description=? WHERE DID=?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, doctor.getName());
            ps.setString(2, doctor.getPasswordHex());
            ps.setBoolean(3, doctor.isAdmin());
            ps.setString(4, doctor.getDepartment());
            ps.setString(5, doctor.getDescription());
            ps.setString(6, doctor.getDid());
            ps.executeUpdate();
        }
    }

    /** 覆盖更新排班信息。 */
    public void UpdateSchedule(Schedule schedule) throws SQLException {
        String sql = "UPDATE Schedule SET DID=?, STime=?, ETime=?, Capacity=?, Res=? WHERE SID=?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schedule.getDid());
            ps.setTimestamp(2, Timestamp.valueOf(schedule.getStartTime()));
            ps.setTimestamp(3, Timestamp.valueOf(schedule.getEndTime()));
            ps.setInt(4, schedule.getCapacity());
            ps.setInt(5, schedule.getRes());
            ps.setInt(6, schedule.getSid());
            ps.executeUpdate();
        }
    }

    /** 插入预约记录（无并发控制，供管理用）。 */
    public void InsertAppointment(Appointment appointment) throws SQLException {
        String sql = "INSERT INTO Appointment(APID,AID,DID,SID,Statu) VALUES (?,?,?,?,?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, appointment.getApid());
            ps.setString(2, appointment.getAid());
            ps.setString(3, appointment.getDid());
            ps.setInt(4, appointment.getSid());
            ps.setString(5, appointment.getStatus().name());
            ps.executeUpdate();
        }
    }

    /** 插入单条排班。 */
    public void InsertSchedule(Schedule schedule) throws SQLException {
        String sql = "INSERT INTO Schedule(SID,DID,STime,ETime,Capacity,Res) VALUES (?,?,?,?,?,?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, schedule.getSid());
            ps.setString(2, schedule.getDid());
            ps.setTimestamp(3, Timestamp.valueOf(schedule.getStartTime()));
            ps.setTimestamp(4, Timestamp.valueOf(schedule.getEndTime()));
            ps.setInt(5, schedule.getCapacity());
            ps.setInt(6, schedule.getRes());
            ps.executeUpdate();
        }
    }

    /** 批量插入排班列表。 */
    public void InsertSchedules(List<Schedule> schedules) throws SQLException {
        String sql = "INSERT INTO Schedule(SID,DID,STime,ETime,Capacity,Res) VALUES (?,?,?,?,?,?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Schedule s : schedules) {
                ps.setInt(1, s.getSid());
                ps.setString(2, s.getDid());
                ps.setTimestamp(3, Timestamp.valueOf(s.getStartTime()));
                ps.setTimestamp(4, Timestamp.valueOf(s.getEndTime()));
                ps.setInt(5, s.getCapacity());
                ps.setInt(6, s.getRes());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // delete
    /** 删除患者账号。 */
    public void DelAccount(String aid) throws SQLException {
        executeDelete("DELETE FROM Account WHERE AID=?", aid);
    }

    /** 删除医生账号。 */
    public void DelDoctorAccount(String did) throws SQLException {
        executeDelete("DELETE FROM Doctor WHERE DID=?", did);
    }

    /** 删除预约记录。 */
    public void DelAppointment(String apid) throws SQLException {
        executeDelete("DELETE FROM Appointment WHERE APID=?", apid);
    }

    /** 删除排班。 */
    public void DelSchedule(int sid) throws SQLException {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM Schedule WHERE SID=?")) {
            ps.setInt(1, sid);
            ps.executeUpdate();
        }
    }

    private void executeDelete(String sql, String key) throws SQLException {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.executeUpdate();
        }
    }

    // find
    /** 按 AID/PID/Phone 任一匹配查询账户（限制 1 条）。 */
    public Account FindAccount(String aid, String pid, String phone) throws SQLException {
        String sql = "SELECT * FROM Account WHERE AID=? OR PID=? OR Phone=? LIMIT 1";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, aid);
            ps.setString(2, pid);
            ps.setString(3, phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapAccount(rs);
                }
                return null;
            }
        }
    }

    /** 按 DID 或姓名查询医生（限制 1 条）。 */
    public DoctorAccount FindDoctorAccount(String did, String name) throws SQLException {
        String sql = "SELECT * FROM Doctor WHERE DID=? OR Name=? LIMIT 1";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, did);
            ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapDoctor(rs);
                }
                return null;
            }
        }
    }

    /** 按科室查找医生列表。 */
    public DoctorAccount[] FindDoctorAccounts(String department) throws SQLException {
        return findDoctorsWithWhere("Department=?", department);
    }

    /** 按姓名模糊/精确查询医生列表。 */
    public DoctorAccount[] FindDoctorAccountsByName(String name) throws SQLException {
        return findDoctorsWithWhere("Name=?", name);
    }

    private DoctorAccount[] findDoctorsWithWhere(String whereClause, String value) throws SQLException {
        String sql = "SELECT * FROM Doctor WHERE " + whereClause;
        List<DoctorAccount> list = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapDoctor(rs));
                }
            }
        }
        return list.toArray(new DoctorAccount[0]);
    }

    /** 查单个预约，附带医生与排班时间。 */
    public Appointment FindAppointment(String apid) throws SQLException {
        String sql = "SELECT a.*, s.STime, s.ETime, d.Name as DocName, d.Department as DocDept FROM Appointment a JOIN Schedule s ON a.SID=s.SID JOIN Doctor d ON a.DID=d.DID WHERE a.APID=?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, apid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapAppointment(rs);
                }
                return null;
            }
        }
    }

    /** 查询患者的全部预约。 */
    public Appointment[] FindAppointmentBelongAccount(String aid) throws SQLException {
        return findAppointmentsWithWhere("a.AID=?", aid);
    }

    /** 查询医生的全部预约。 */
    public Appointment[] FindAppointmentBelongDoctorAccount(String did) throws SQLException {
        return findAppointmentsWithWhere("a.DID=?", did);
    }

    /** 查询某排班下的全部预约。 */
    public Appointment[] FindAppointmentBelongSchedule(int sid) throws SQLException {
        return findAppointmentsWithWhere("a.SID=?", sid);
    }

    /** 按状态查询预约。 */
    public Appointment[] FindAppointmentByStatu(AppointmentStatus statu) throws SQLException {
        return findAppointmentsWithWhere("a.Statu=?", statu.name());
    }

    private Appointment[] findAppointmentsWithWhere(String whereClause, Object value) throws SQLException {
        String sql = "SELECT a.*, s.STime, s.ETime, d.Name as DocName, d.Department as DocDept FROM Appointment a JOIN Schedule s ON a.SID=s.SID JOIN Doctor d ON a.DID=d.DID WHERE " + whereClause;
        List<Appointment> list = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (value instanceof Integer) {
                ps.setInt(1, (Integer) value);
            } else {
                ps.setString(1, value.toString());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAppointment(rs));
                }
            }
        }
        return list.toArray(new Appointment[0]);
    }

    /** 按 SID 查排班。 */
    public Schedule FindSchedule(int sid) throws SQLException {
        String sql = "SELECT * FROM Schedule WHERE SID=?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapSchedule(rs);
                }
                return null;
            }
        }
    }

    /** 查某医生全部排班。 */
    public Schedule[] FindScheduleBelongDoctorAccount(String did) throws SQLException {
        String sql = "SELECT * FROM Schedule WHERE DID=?";
        List<Schedule> list = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, did);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapSchedule(rs));
                }
            }
        }
        return list.toArray(new Schedule[0]);
    }

    /** 查覆盖指定时间的排班列表。 */
    public Schedule[] FindScheduleByTime(LocalDateTime time) throws SQLException {
        String sql = "SELECT * FROM Schedule WHERE STime<=? AND ETime>=?";
        List<Schedule> list = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            Timestamp ts = Timestamp.valueOf(time);
            ps.setTimestamp(1, ts);
            ps.setTimestamp(2, ts);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapSchedule(rs));
                }
            }
        }
        return list.toArray(new Schedule[0]);
    }

    /** 按时间与科室过滤排班列表。 */
    public Schedule[] FindScheduleByTimeInDepartment(LocalDateTime time, String department) throws SQLException {
        String sql = "SELECT s.* FROM Schedule s JOIN Doctor d ON s.DID=d.DID WHERE s.STime<=? AND s.ETime>=? AND d.Department=?";
        List<Schedule> list = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            Timestamp ts = Timestamp.valueOf(time);
            ps.setTimestamp(1, ts);
            ps.setTimestamp(2, ts);
            ps.setString(3, department);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapSchedule(rs));
                }
            }
        }
        return list.toArray(new Schedule[0]);
    }

    // change
    /** 修改患者姓名/密码/电话。 */
    public void ChangeAccountInfo(Account account) throws SQLException {
        String sql = "UPDATE Account SET Name=?, Password=?, Phone=? WHERE AID=?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, account.getName());
            ps.setString(2, account.getPasswordHex());
            ps.setString(3, account.getPhone());
            ps.setString(4, account.getAid());
            ps.executeUpdate();
        }
    }

    /** 修改医生资料。 */
    public void ChangeDoctorAccountInfo(DoctorAccount doctor) throws SQLException {
        String sql = "UPDATE Doctor SET Name=?, Password=?, Admin=?, Department=?, Description=? WHERE DID=?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, doctor.getName());
            ps.setString(2, doctor.getPasswordHex());
            ps.setBoolean(3, doctor.isAdmin());
            ps.setString(4, doctor.getDepartment());
            ps.setString(5, doctor.getDescription());
            ps.setString(6, doctor.getDid());
            ps.executeUpdate();
        }
    }

    /**
     * 更新预约状态，若从 Ok -> Abandon 则返还号源；使用事务保证一致性。
     */
    public void ChangeAppointmentStatu(String apid, AppointmentStatus statu) throws SQLException {
        String selectSql = "SELECT Statu, SID FROM Appointment WHERE APID=? FOR UPDATE";
        String updateAppointmentSql = "UPDATE Appointment SET Statu=? WHERE APID=?";
        String restoreScheduleResSql = "UPDATE Schedule SET Res=Res+1 WHERE SID=?";

        try (Connection conn = getConnection()) {
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            AppointmentStatus oldStatus;
            int sid;
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setString(1, apid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        conn.setAutoCommit(oldAutoCommit);
                        return;
                    }
                    oldStatus = AppointmentStatus.valueOf(rs.getString("Statu"));
                    sid = rs.getInt("SID");
                }
            }

            // Only restore capacity when changing from Ok to Abandon
            if (oldStatus == AppointmentStatus.Ok && statu == AppointmentStatus.Abandon) {
                try (PreparedStatement ps = conn.prepareStatement(restoreScheduleResSql)) {
                    ps.setInt(1, sid);
                    ps.executeUpdate();
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(updateAppointmentSql)) {
                ps.setString(1, statu.name());
                ps.setString(2, apid);
                ps.executeUpdate();
            }

            conn.commit();
            conn.setAutoCommit(oldAutoCommit);
        }
    }

    /** 修改排班容量数据，输入的是增量。 */
    public void ChangeScheduleCapacity(int sid, int delta) throws SQLException {
        String sql = "UPDATE Schedule SET Capacity=Capacity+?, Res=Res+? WHERE SID=?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setInt(2, delta);
            ps.setInt(3, sid);
            ps.executeUpdate();
        }
    }

    /** 尝试预定操作，原子扣减号源并插入预约，失败回滚。 */
    public Appointment TryAppointment(String aid, int sid) throws SQLException {
        String updateSql = "UPDATE Schedule SET Res=Res-1 WHERE SID=? AND Res>0";
        String selectSchedule = "SELECT DID, STime, ETime FROM Schedule WHERE SID=?";
        String insertAppointment = "INSERT INTO Appointment(APID,AID,DID,SID,Statu) VALUES (?,?,?,?,?)";

        try (Connection conn = getConnection()) {
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);  // 切换为手动事务控制模式
                                        // 此时必须显示调用Connection.commit()才会影响数据库
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setInt(1, sid);
                int updated = ps.executeUpdate();
                if (updated != 1) { // 失败返回
                    conn.rollback();
                    conn.setAutoCommit(oldAutoCommit);
                    return null;
                }
            }

            String did;
            LocalDateTime sTime;
            LocalDateTime eTime;
            try (PreparedStatement ps = conn.prepareStatement(selectSchedule)) {
                ps.setInt(1, sid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        conn.setAutoCommit(oldAutoCommit);
                        return null;
                    }
                    did = rs.getString("DID");
                    sTime = rs.getTimestamp("STime").toLocalDateTime();
                    eTime = rs.getTimestamp("ETime").toLocalDateTime();
                }
            }

            Appointment appointment = Appointment.create(aid, did, sid, AppointmentStatus.Ok, sTime, eTime);
            try (PreparedStatement ps = conn.prepareStatement(insertAppointment)) {
                ps.setString(1, appointment.getApid());
                ps.setString(2, appointment.getAid());
                ps.setString(3, appointment.getDid());
                ps.setInt(4, appointment.getSid());
                ps.setString(5, appointment.getStatus().name());
                ps.executeUpdate();
            }

            conn.commit();
            conn.setAutoCommit(oldAutoCommit);
            return appointment;
        }
    }

    // mappers
    private Account mapAccount(ResultSet rs) throws SQLException {
        return new Account(
                rs.getString("AID"),
                rs.getString("Name"),
                rs.getString("Password"),
                rs.getString("PID"),
                rs.getString("Phone"),
                Sex.valueOf(rs.getString("Sex"))
        );
    }

    private DoctorAccount mapDoctor(ResultSet rs) throws SQLException {
        return new DoctorAccount(
                rs.getString("DID"),
                rs.getString("Name"),
                rs.getString("Password"),
                rs.getBoolean("Admin"),
                rs.getString("Department"),
                rs.getString("Description")
        );
    }

    private Schedule mapSchedule(ResultSet rs) throws SQLException {
        Timestamp st = rs.getTimestamp("STime");
        Timestamp et = rs.getTimestamp("ETime");
        return new Schedule(
                rs.getInt("SID"),
                rs.getString("DID"),
                st.toLocalDateTime(),
                et.toLocalDateTime(),
                rs.getInt("Capacity"),
                rs.getInt("Res")
        );
    }

    private Appointment mapAppointment(ResultSet rs) throws SQLException {
        Timestamp st = rs.getTimestamp("STime");
        Timestamp et = rs.getTimestamp("ETime");
        LocalDateTime sTime = st != null ? st.toLocalDateTime() : null;
        LocalDateTime eTime = et != null ? et.toLocalDateTime() : null;
        
        String docName = null;
        String docDept = null;
        try {
            docName = rs.getString("DocName");
            docDept = rs.getString("DocDept");
        } catch (SQLException e) {
            // Ignore if columns not present (e.g. if query didn't join)
        }

        return new Appointment(
                rs.getInt("SerialNumber"),
                rs.getString("APID"),
                rs.getString("AID"),
                rs.getString("DID"),
                docName,
                docDept,
                rs.getInt("SID"),
                AppointmentStatus.valueOf(rs.getString("Statu")),
                sTime,
                eTime
        );
    }
}
