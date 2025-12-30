package HABMS.db;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * Minimal smoke test that exercises a few HABMSDB methods without extra test dependencies.
 * Configure JDBC connection via env: HABMS_URL, HABMS_USER, HABMS_PASS.
 */
public final class HABMSDBSmoke {
    public static void main(String[] args) throws Exception {
        String url = getenvOrDefault("HABMS_URL", "jdbc:mariadb://localhost:3306/HABMSDB?useSSL=false&allowPublicKeyRetrieval=true");
        String user = getenvOrDefault("HABMS_USER", "rjava");
        String pass = getenvOrDefault("HABMS_PASS", "rjava");

        HABMSDB db = new HABMSDB(url, user, pass);

        Account account = Account.create("SmokeUser", hexSha256("password"), "123456789012345678", "13800000000", Sex.M);
        db.InsertAccount(account);
        System.out.println("Inserted account: " + account.getAid());

        DoctorAccount doctor = DoctorAccount.create("SmokeDoctor", hexSha256("password"), false, "内科", "smoke doctor");
        db.InsertDoctorAccount(doctor);
        System.out.println("Inserted doctor: " + doctor.getDid());

        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = start.plusHours(2);
        Schedule schedule = Schedule.create(doctor.getDid(), start, end, 5);
        db.InsertSchedule(schedule);
        System.out.println("Inserted schedule: " + schedule.getSid());

        Appointment appointment = db.TryAppointment(account.getAid(), schedule.getSid());
        System.out.println("TryAppointment result: " + (appointment == null ? "null" : appointment.getApid()));

        Appointment[] byAid = db.FindAppointmentBelongAccount(account.getAid());
        System.out.println("Appointments by account count: " + byAid.length);

        Schedule[] schedulesByDoc = db.FindScheduleBelongDoctorAccount(doctor.getDid());
        System.out.println("Schedules by doctor count: " + schedulesByDoc.length);

        Appointment[] byStatus = db.FindAppointmentByStatu(AppointmentStatus.Ok);
        System.out.println("Appointments with status Ok: " + byStatus.length);

        // Cleanup
        Arrays.stream(byAid).forEach(a -> {
            try {
                db.DelAppointment(a.getApid());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        db.DelSchedule(schedule.getSid());
        db.DelDoctorAccount(doctor.getDid());
        db.DelAccount(account.getAid());
        System.out.println("Cleanup done");
    }

    private static String getenvOrDefault(String key, String def) {
        String v = System.getenv(key);
        return v != null && !v.isEmpty() ? v : def;
    }

    private static String hexSha256(String plain) throws Exception {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(plain.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
