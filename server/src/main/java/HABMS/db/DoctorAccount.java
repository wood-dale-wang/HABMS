package HABMS.db;

import java.util.Objects;

/** 医生账户数据对象（admin==true 表示管理员）。 */
public final class DoctorAccount {
    private final String did;
    private final String name;
    private final String passwordHex;
    private final boolean admin;
    private final String department;
    private final String description;

    /** 一般初始化方法用于从数据库提取数据。 */
    public DoctorAccount(String did, String name, String passwordHex, boolean admin, String department, String description) {
        this.did = Objects.requireNonNull(did);
        this.name = Objects.requireNonNull(name);
        this.passwordHex = Objects.requireNonNull(passwordHex);
        this.admin = admin;
        this.department = Objects.requireNonNull(department);
        this.description = description;
    }

    /** 工厂方法：创建带新 DID 的医生对象。 */
    public static DoctorAccount create(String name, String passwordHex, boolean admin, String department, String Description) {
        return new DoctorAccount(IdGenerator.newDid(), name, passwordHex, admin, department, Description);
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

    public String getDescription() {
        return description;
    }
}
