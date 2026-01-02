package HABMS.client.model;

public class Appointment {
    private String serialNumber;
    private String apid;
    private String aid;
    private String did;
    private int sid;
    private String status;
    private String startTime;
    private String endTime;
    
    // Extra fields for display
    private String doctorName;
    private String department;

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getApid() { return apid; }
    public void setApid(String apid) { this.apid = apid; }

    public String getAid() { return aid; }
    public void setAid(String aid) { this.aid = aid; }

    public String getDid() { return did; }
    public void setDid(String did) { this.did = did; }

    public int getSid() { return sid; }
    public void setSid(int sid) { this.sid = sid; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public String getTimeSlot() {
        if (startTime == null) return "";
        // Format: yyyy-MM-dd HH:mm - HH:mm
        String start = startTime.replace("T", " ");
        if (start.length() > 16) start = start.substring(0, 16);
        
        String end = "";
        if (endTime != null && endTime.length() > 16) {
            end = endTime.substring(11, 16);
        }
        return start + " - " + end;
    }

    public String getStatusDisplay() {
        if (status == null) return "";
        switch (status) {
            case "Ok": return "已预约";
            case "Abandon": return "已取消";
            case "Done": return "已完成";
            default: return status;
        }
    }
}
