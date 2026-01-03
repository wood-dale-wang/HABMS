package HABMS.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * 服务器响应的通用包装，包含状态与数据节点。
 */
public class Response {
    @JsonProperty("Statu")
    private String statu;
    private JsonNode data; // Use JsonNode to handle flexible data structures

    public String getStatu() {
        return statu;
    }

    public void setStatu(String statu) {
        this.statu = statu;
    }

    public JsonNode getData() {
        return data;
    }

    public void setData(JsonNode data) {
        this.data = data;
    }
    
    public boolean isOk() {
        return "ok".equalsIgnoreCase(statu);
    }
    
    public String getErrInfo() {
        if (data != null && data.has("err_info")) {
            return data.get("err_info").asText();
        }
        return "Unknown error";
    }
}
