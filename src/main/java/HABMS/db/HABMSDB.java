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

    public void InsertDoctorAccount(DoctorAccount doctor) throws SQLException {
        String sql = "INSERT INTO Doctor(DID,Name,Password,Admin,Department,Describe) VALUES (?,?,?,?,?,?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, doctor.getDid());
            ps.setString(2, doctor.getName());
            ps.setString(3, doctor.getPasswordHex());
            ps.setBoolean(4, doctor.isAdmin());
            ps.setString(5, doctor.getDepartment());
            ps.setString(6, doctor.getDescribe());
            ps.executeUpdate();
        }
    }

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
    public void DelAccount(String aid) throws SQLException {
        executeDelete("DELETE FROM Account WHERE AID=?", aid);
    }

    public void DelDoctorAccount(String did) throws SQLException {
        executeDelete("DELETE FROM Doctor WHERE DID=?", did);
    }

    public void DelAppointment(String apid) throws SQLException {
        executeDelete("DELETE FROM Appointment WHERE APID=?", apid);
    }

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

    public DoctorAccount[] FindDoctorAccounts(String department) throws SQLException {
        String sql = "SELECT * FROM Doctor WHERE Department=?";
        List<DoctorAccount> list = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, department);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapDoctor(rs));
                }
            }
        }
        return list.toArray(new DoctorAccount[0]);
    }

    public Appointment FindAppointment(String apid) throws SQLException {
        String sql = "SELECT a.*, s.STime, s.ETime FROM Appointment a JOIN Schedule s ON a.SID=s.SID WHERE a.APID=?";
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

    public Appointment[] FindAppointmentBelongAccount(String aid) throws SQLException {
        return findAppointmentsWithWhere("a.AID=?", aid);
    }

    public Appointment[] FindAppointmentBelongDoctorAccount(String did) throws SQLException {
        return findAppointmentsWithWhere("a.DID=?", did);
    }

    public Appointment[] FindAppointmentBelongSchedule(int sid) throws SQLException {
        return findAppointmentsWithWhere("a.SID=?", sid);
    }

    public Appointment[] FindAppointmentByStatu(AppointmentStatus statu) throws SQLException {
        return findAppointmentsWithWhere("a.Statu=?", statu.name());
    }

    private Appointment[] findAppointmentsWithWhere(String whereClause, Object value) throws SQLException {
        String sql = "SELECT a.*, s.STime, s.ETime FROM Appointment a JOIN Schedule s ON a.SID=s.SID WHERE " + whereClause;
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

    // change
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

    public void ChangeDoctorAccountInfo(DoctorAccount doctor) throws SQLException {
        String sql = "UPDATE Doctor SET Name=?, Password=?, Admin=?, Department=?, Describe=? WHERE DID=?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, doctor.getName());
            ps.setString(2, doctor.getPasswordHex());
            ps.setBoolean(3, doctor.isAdmin());
            ps.setString(4, doctor.getDepartment());
            ps.setString(5, doctor.getDescribe());
            ps.setString(6, doctor.getDid());
            ps.executeUpdate();
        }
    }

    public void ChangeAppointmentStatu(String apid, AppointmentStatus statu) throws SQLException {
        String sql = "UPDATE Appointment SET Statu=? WHERE APID=?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, statu.name());
            ps.setString(2, apid);
            ps.executeUpdate();
        }
    }

    public void ChangeScheduleCapacity(int sid, int delta) throws SQLException {
        String sql = "UPDATE Schedule SET Capacity=Capacity+?, Res=Res+? WHERE SID=?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setInt(2, delta);
            ps.setInt(3, sid);
            ps.executeUpdate();
        }
    }

    public Appointment TryAppointment(String aid, int sid) throws SQLException {
        String updateSql = "UPDATE Schedule SET Res=Res-1 WHERE SID=? AND Res>0";
        String selectSchedule = "SELECT DID, STime, ETime FROM Schedule WHERE SID=?";
        String insertAppointment = "INSERT INTO Appointment(APID,AID,DID,SID,Statu) VALUES (?,?,?,?,?)";

        try (Connection conn = getConnection()) {
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setInt(1, sid);
                int updated = ps.executeUpdate();
                if (updated != 1) {
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
                rs.getString("Describe")
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
        return new Appointment(
                rs.getString("APID"),
                rs.getString("AID"),
                rs.getString("DID"),
                rs.getInt("SID"),
                AppointmentStatus.valueOf(rs.getString("Statu")),
                sTime,
                eTime
        );
    }
}
