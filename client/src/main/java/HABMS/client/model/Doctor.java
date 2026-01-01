package HABMS.client.model;

public class Doctor {
    private String did;
    private String name;
    private String passwordHex;
    private boolean admin;
    private String department;
    private String description;

    // Getters and Setters
    public String getDid() { return did; }
    public void setDid(String did) { this.did = did; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPasswordHex() { return passwordHex; }
    public void setPasswordHex(String passwordHex) { this.passwordHex = passwordHex; }

    public boolean isAdmin() { return admin; }
    public void setAdmin(boolean admin) { this.admin = admin; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
