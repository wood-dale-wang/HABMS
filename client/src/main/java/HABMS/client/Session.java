package HABMS.client;

import HABMS.client.model.User;
import HABMS.client.model.Doctor;

public class Session {
    private static User currentUser;
    private static Doctor currentDoctor;

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
        currentDoctor = null;
    }

    public static Doctor getCurrentDoctor() {
        return currentDoctor;
    }

    public static void setCurrentDoctor(Doctor doctor) {
        currentDoctor = doctor;
        currentUser = null;
    }
    
    public static void clear() {
        currentUser = null;
        currentDoctor = null;
    }
}
