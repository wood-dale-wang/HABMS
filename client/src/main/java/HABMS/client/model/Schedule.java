package HABMS.client.model;

public class Schedule {
    private int sid;
    private String did;
    private String startTime;
    private String endTime;
    private int capacity;
    private int res;

    public int getSid() { return sid; }
    public void setSid(int sid) { this.sid = sid; }

    public String getDid() { return did; }
    public void setDid(String did) { this.did = did; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public int getRes() { return res; }
    public void setRes(int res) { this.res = res; }
    
    public String getTimeSlot() {
        // Simple formatting
        return startTime.replace("T", " ") + " - " + endTime.substring(11);
    }
}
