package HABMS.db;

import java.util.Objects;

/** 患者账户数据对象 */
public final class Account {
    private final String aid;
    private final String name;
    private final String passwordHex;
    private final String pid;
    private final String phone;
    private final Sex sex;

    /** 一般初始化方法用于从数据库提取数据 */
    public Account(String aid, String name, String passwordHex, String pid, String phone, Sex sex) {
        this.aid = Objects.requireNonNull(aid);
        this.name = Objects.requireNonNull(name);
        this.passwordHex = Objects.requireNonNull(passwordHex);
        this.pid = Objects.requireNonNull(pid);
        this.phone = Objects.requireNonNull(phone);
        this.sex = Objects.requireNonNull(sex);
    }

    /** create工厂方法用于创建带新AID的对象 */
    public static Account create(String name, String passwordHex, String pid, String phone, Sex sex) {
        return new Account(IdGenerator.newAid(), name, passwordHex, pid, phone, sex);
    }

    public String getAid() {
        return aid;
    }

    public String getName() {
        return name;
    }

    public String getPasswordHex() {
        return passwordHex;
    }

    public String getPid() {
        return pid;
    }

    public String getPhone() {
        return phone;
    }

    public Sex getSex() {
        return sex;
    }
}
