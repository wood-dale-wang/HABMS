package HABMS.client;

import HABMS.client.model.User;
import HABMS.client.model.Doctor;

/**
 * 保存当前登录态的简单 Session 单例（患者或医生择一）。
 */
public class Session {
    private static User currentUser;
    private static Doctor currentDoctor;

    /** 获取当前登录的患者用户（医生登录时为 null）。 */
    public static User getCurrentUser() {
        return currentUser;
    }

    /** 设置患者登录态并清空医生态。 */
    public static void setCurrentUser(User user) {
        currentUser = user;
        currentDoctor = null;
    }

    /** 获取当前登录的医生（患者登录时为 null）。 */
    public static Doctor getCurrentDoctor() {
        return currentDoctor;
    }

    /** 设置医生登录态并清空患者态。 */
    public static void setCurrentDoctor(Doctor doctor) {
        currentDoctor = doctor;
        currentUser = null;
    }
    
    /** 清除两类登录态。 */
    public static void clear() {
        currentUser = null;
        currentDoctor = null;
    }
}
