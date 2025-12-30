package HABMS.db;

import java.util.Objects;

/** 医生状态对象，注意只有admin==true的才认为是管理员 */
public final class DoctorAccount {
    private final String did;
    private final String name;
    private final String passwordHex;
    private final boolean admin;
    private final String department;
    private final String describe;

    /** 一般初始化方法用于从数据库提取数据 */
    public DoctorAccount(String did, String name, String passwordHex, boolean admin, String department, String describe) {
        this.did = Objects.requireNonNull(did);
        this.name = Objects.requireNonNull(name);
        this.passwordHex = Objects.requireNonNull(passwordHex);
        this.admin = admin;
        this.department = Objects.requireNonNull(department);
        this.describe = describe;
    }

    /** create工厂方法用于创建带新DID的对象 */
    public static DoctorAccount create(String name, String passwordHex, boolean admin, String department, String describe) {
        return new DoctorAccount(IdGenerator.newDid(), name, passwordHex, admin, department, describe);
    }

    public String getDid() {
        return did;
    }

    public String getName() {
        return name;
    }

    public String getPasswordHex() {
        return passwordHex;
    }

    public boolean isAdmin() {
        return admin;
    }

    public String getDepartment() {
        return department;
    }

    public String getDescribe() {
        return describe;
    }
}
