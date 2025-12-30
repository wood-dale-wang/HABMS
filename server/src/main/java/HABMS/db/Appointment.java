package HABMS.db;

import java.time.LocalDateTime;
import java.util.Objects;

/** 预定订单数据对象 */
public final class Appointment {
    private final String apid;
    private final String aid;
    private final String did;
    private final int sid;
    private final AppointmentStatus status;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;

    /** 一般初始化方法用于从数据库提取数据 */
    public Appointment(String apid, String aid, String did, int sid, AppointmentStatus status,
                       LocalDateTime startTime, LocalDateTime endTime) {
        this.apid = Objects.requireNonNull(apid);
        this.aid = Objects.requireNonNull(aid);
        this.did = Objects.requireNonNull(did);
        this.sid = sid;
        this.status = Objects.requireNonNull(status);
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /** create工厂方法用于创建带新APID的对象 */
    public static Appointment create(String aid, String did, int sid, AppointmentStatus status,
                                     LocalDateTime startTime, LocalDateTime endTime) {
        return new Appointment(IdGenerator.newApid(), aid, did, sid, status, startTime, endTime);
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
