package HABMS.db;

import java.time.LocalDateTime;
import java.util.Objects;

/** 预约订单数据对象。 */
public final class Appointment {
    private final int serialNumber;
    private final String apid;
    private final String aid;
    private final String did;
    private final String doctorName;
    private final String department;
    private final int sid;
    private final AppointmentStatus status;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;

    /** 一般初始化方法用于从数据库提取数据。 */
    public Appointment(int serialNumber, String apid, String aid, String did, String doctorName, String department, int sid, AppointmentStatus status,
                       LocalDateTime startTime, LocalDateTime endTime) {
        this.serialNumber = serialNumber;
        this.apid = Objects.requireNonNull(apid);
        this.aid = Objects.requireNonNull(aid);
        this.did = Objects.requireNonNull(did);
        this.doctorName = doctorName;
        this.department = department;
        this.sid = sid;
        this.status = Objects.requireNonNull(status);
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /** 工厂方法：创建带新 APID 的预约对象。 */
    public static Appointment create(String aid, String did, int sid, AppointmentStatus status,
                                     LocalDateTime startTime, LocalDateTime endTime) {
        return new Appointment(0, IdGenerator.newApid(), aid, did, null, null, sid, status, startTime, endTime);
    }

    public int getSerialNumber() {
        return serialNumber;
    }

    public String getApid() {
        return apid;
    }

    public String getAid() {
        return aid;
    }

    public String getDid() {
        return did;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public String getDepartment() {
        return department;
    }

    public int getSid() {
        return sid;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }
}
