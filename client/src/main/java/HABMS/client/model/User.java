package HABMS.client.model;

/**
 * 患者账户信息 DTO。
 */
public class User {
    private String aid;
    private String name;
    private String passwordHex;
    private String pid;
    private String phone;
    private String sex; // M or F

    // Getters and Setters
    public String getAid() { return aid; }
    public void setAid(String aid) { this.aid = aid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPasswordHex() { return passwordHex; }
    public void setPasswordHex(String passwordHex) { this.passwordHex = passwordHex; }

    public String getPid() { return pid; }
    public void setPid(String pid) { this.pid = pid; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }
}
