package HABMS.db;

import java.time.LocalDateTime;
import java.util.Objects;

/** 排班数据对象 */
public final class Schedule {
    private final int sid;
    private final String did;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final int capacity;
    private final int res;

    /** 一般初始化方法用于从数据库提取数据 */
    public Schedule(int sid, String did, LocalDateTime startTime, LocalDateTime endTime, int capacity, int res) {
        this.sid = sid;
        this.did = Objects.requireNonNull(did);
        this.startTime = Objects.requireNonNull(startTime);
        this.endTime = Objects.requireNonNull(endTime);
        this.capacity = capacity;
        this.res = res;
    }

    /** create工厂方法用于创建带新SID的对象 */
    public static Schedule create(String did, LocalDateTime startTime, LocalDateTime endTime, int capacity) {
        return new Schedule(IdGenerator.newSid(), did, startTime, endTime, capacity, capacity);
    }

    public int getSid() {
        return sid;
    }

    public String getDid() {
        return did;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getRes() {
        return res;
    }
}
