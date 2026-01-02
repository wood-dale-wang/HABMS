package HABMS.server;

import HABMS.db.Account;
import HABMS.db.Appointment;
import HABMS.db.AppointmentStatus;
import HABMS.db.DoctorAccount;
import HABMS.db.HABMSDB;
import HABMS.db.Schedule;
import HABMS.db.Sex;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/** One Service per TCP connection. Messages are JSON per line: {"type":"...","data":{...}}. */
final class Service implements Runnable {
    private static final Logger LOG = Logger.getLogger(Service.class.getName());

    private final Socket socket;
    private final HABMSDB db;
    private final Set<String> departments;
    private final ObjectMapper mapper;

    private Account sessionAccount;
    private DoctorAccount sessionDoctor;

    Service(Socket socket, HABMSDB db, List<String> departments) {
        this.socket = Objects.requireNonNull(socket);
        this.db = Objects.requireNonNull(db);
        this.departments = new HashSet<>(departments);
        this.mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void run() {
        try (socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                Response resp = handleLine(line);
                String json = mapper.writeValueAsString(resp);
                writer.write(json);
                writer.newLine();
                writer.flush();
            }
        } catch (IOException io) {
            LOG.log(Level.FINE, "Client connection closed", io);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Service runtime error", e);
        }
    }

    private Response handleLine(String line) {
        try {
            Request req = mapper.readValue(line, Request.class);
            if (req.type == null || req.type.isBlank()) {
                return err("type missing");
            }
            JsonNode data = req.data != null ? req.data : mapper.createObjectNode();
            return switch (req.type) {
                case "account_register" -> handleAccountRegister(data);
                case "account_login" -> handleAccountLogin(data);
                case "account_logout" -> handleAccountLogout(data);
                case "account_delete" -> handleAccountDelete(data);
                case "account_update" -> handleAccountUpdate(data);
                case "department_list" -> handleDepartmentList();
                case "doctor_query" -> handleDoctorQuery(data);
                case "schedule_by_doctor" -> handleScheduleByDoctor(data);
                case "schedule_by_time" -> handleScheduleByTime(data);
                case "appointment_create" -> handleAppointmentCreate(data);
                case "appointment_cancel" -> handleAppointmentCancel(data);
                case "appointment_list" -> handleAppointmentList();
                case "doctor_login" -> handleDoctorLogin(data);
                case "doctor_logout" -> handleDoctorLogout();
                case "doctor_schedules" -> handleDoctorSchedules();
                case "doctor_appointments" -> handleDoctorAppointments();
                case "doctor_call_next" -> handleDoctorCallNext(data);
                case "admin_add_schedules" -> handleAdminAddSchedules(data);
                case "admin_update_schedule" -> handleAdminUpdateSchedule(data);
                case "admin_all_appointments" -> handleAdminAllAppointments();
                case "admin_report" -> handleAdminReport();
                case "admin_add_doctors" -> handleAdminAddDoctors(data);
                default -> err("unknown type: " + req.type);
            };
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to handle line: " + line, e);
            return err("invalid request: " + e.getMessage());
        }
    }

    private Response handleAccountRegister(JsonNode data) throws Exception {
        String name = requiredText(data, "name");
        String passwordHex = requiredText(data, "passwordHex");
        String pid = requiredText(data, "pid");
        String phone = requiredText(data, "phone");
        Sex sex = parseSex(data.path("sex"));

        if (db.FindAccount(null, pid, null) != null) {
            return err("PID already exists");
        }
        if (db.FindAccount(null, null, phone) != null) {
            return err("phone already exists");
        }

        Account account = Account.create(name, passwordHex, pid, phone, sex);
        db.InsertAccount(account);
        sessionAccount = account;
        sessionDoctor = null;
        return ok(view(account));
    }

    private Response handleAccountLogin(JsonNode data) throws Exception {
        String pid = textOrNull(data, "pid");
        String phone = textOrNull(data, "phone");
        String passwordHex = requiredText(data, "passwordHex");
        if ((pid == null || pid.isBlank()) && (phone == null || phone.isBlank())) {
            return err("pid or phone required");
        }
        Account account = db.FindAccount(null, pid, phone);
        if (account == null) {
            return err("account not found");
        }
        if (!account.getPasswordHex().equals(passwordHex)) {
            return err("password mismatch");
        }
        sessionAccount = account;
        sessionDoctor = null;
        return ok(view(account));
    }

    private Response handleAccountLogout(JsonNode data) {
        if (sessionAccount == null) {
            return err("not logged in");
        }
        String aid = textOrNull(data, "aid");
        if (aid != null && !aid.equals(sessionAccount.getAid())) {
            return err("aid mismatch");
        }
        sessionAccount = null;
        return ok(Map.of());
    }

    private Response handleAccountDelete(JsonNode data) throws Exception {
        if (sessionAccount == null) {
            return err("not logged in");
        }
        String aid = requiredText(data, "aid");
        if (!aid.equals(sessionAccount.getAid())) {
            return err("aid mismatch");
        }
        db.DelAccount(aid);
        sessionAccount = null;
        return ok(Map.of());
    }

    private Response handleAccountUpdate(JsonNode data) throws Exception {
        if (sessionAccount == null) {
            return err("not logged in");
        }
        String aid = requiredText(data, "aid");
        if (!aid.equals(sessionAccount.getAid())) {
            return err("aid mismatch");
        }
        String name = requiredText(data, "name");
        String passwordHex = requiredText(data, "passwordHex");
        String pid = requiredText(data, "pid");
        String phone = requiredText(data, "phone");
        Sex sex = parseSex(data.path("sex"));

        if (!pid.equals(sessionAccount.getPid()) || !phone.equals(sessionAccount.getPhone())) {
            return err("immutable field changed (pid/phone)");
        }

        Account updated = new Account(aid, name, passwordHex, pid, phone, sex);
        db.ChangeAccountInfo(updated);
        sessionAccount = updated;
        return ok(view(updated));
    }

    private Response handleDepartmentList() {
        // if (!isLoggedIn()) {
        //     return err("not logged in");
        // }
        return ok(new ArrayList<>(departments));
    }

    private Response handleDoctorQuery(JsonNode data) throws Exception {
        if (!isLoggedIn()) {
            return err("not logged in");
        }
        String did = textOrNull(data, "did");
        String name = textOrNull(data, "name");
        String department = textOrNull(data, "department");

        List<DoctorAccount> found = new ArrayList<>();
        if (did != null || name != null) {
            DoctorAccount doctor = db.FindDoctorAccount(did, name);
            if (doctor != null) {
                found.add(doctor);
            }
        } else if (department != null) {
            if (!departments.isEmpty() && !departments.contains(department)) {
                return err("department not exists");
            }
            found.addAll(Arrays.asList(db.FindDoctorAccounts(department)));
        } else {
            return err("department/name/did required");
        }

        return ok(found.stream().map(this::view).toList());
    }

    private Response handleScheduleByDoctor(JsonNode data) throws Exception {
        if (!isLoggedIn()) {
            return err("not logged in");
        }
        String did = requiredText(data, "did");
        Schedule[] schedules = db.FindScheduleBelongDoctorAccount(did);
        return ok(Arrays.stream(schedules).map(this::view).toList());
    }

    private Response handleScheduleByTime(JsonNode data) throws Exception {
        if (!isLoggedIn()) {
            return err("not logged in");
        }
        String timeStr = requiredText(data, "time");
        String department = requiredText(data, "department");
        if (!departments.isEmpty() && !departments.contains(department)) {
            return err("department not exists");
        }
        LocalDateTime time = LocalDateTime.parse(timeStr);
        Schedule[] schedules = db.FindScheduleByTimeInDepartment(time, department);
        return ok(Arrays.stream(schedules).map(this::view).toList());
    }

    private Response handleAppointmentCreate(JsonNode data) throws Exception {
        if (sessionAccount == null) {
            return err("not logged in");
        }
        String did = requiredText(data, "did");
        int sid = requiredInt(data, "sid");

        Schedule schedule = db.FindSchedule(sid);
        if (schedule == null) {
            return err("sid not exists");
        }
        if (!schedule.getDid().equals(did)) {
            return err("did and sid not match");
        }

        Appointment appointment = db.TryAppointment(sessionAccount.getAid(), sid);
        if (appointment == null) {
            return err("capacity is zero");
        }
        return ok(view(appointment));
    }

    private Response handleAppointmentCancel(JsonNode data) throws Exception {
        if (sessionAccount == null) {
            return err("not logged in");
        }
        String apid = requiredText(data, "apid");
        Appointment ap = db.FindAppointment(apid);
        if (ap == null) {
            return err("apid not exists");
        }
        if (!sessionAccount.getAid().equals(ap.getAid())) {
            return err("aid and apid not match");
        }
        db.ChangeAppointmentStatu(apid, AppointmentStatus.Abandon);
        Appointment updated = db.FindAppointment(apid);
        return ok(view(updated));
    }

    private Response handleAppointmentList() throws Exception {
        if (sessionAccount == null) {
            return err("not logged in");
        }
        Appointment[] appointments = db.FindAppointmentBelongAccount(sessionAccount.getAid());
        return ok(Arrays.stream(appointments).map(this::view).toList());
    }

    private Response handleDoctorLogin(JsonNode data) throws Exception {
        String name = requiredText(data, "name");
        String department = requiredText(data, "department");
        String passwordHex = requiredText(data, "passwordHex");
        DoctorAccount doctor = findDoctorByNameAndDepartment(name, department);
        if (doctor == null) {
            return err("doctor not found");
        }
        if (!doctor.getPasswordHex().equals(passwordHex)) {
            return err("password mismatch");
        }
        sessionDoctor = doctor;
        sessionAccount = null;
        return ok(view(doctor));
    }

    private Response handleDoctorLogout() {
        if (sessionDoctor == null) {
            return err("not logged in");
        }
        sessionDoctor = null;
        return ok(Map.of());
    }

    private Response handleDoctorSchedules() throws Exception {
        if (sessionDoctor == null) {
            return err("not logged in");
        }
        Schedule[] schedules = db.FindScheduleBelongDoctorAccount(sessionDoctor.getDid());
        return ok(Arrays.stream(schedules).map(this::view).toList());
    }

    private Response handleDoctorAppointments() throws Exception {
        if (sessionDoctor == null) {
            return err("not logged in");
        }
        Appointment[] appointments = db.FindAppointmentBelongDoctorAccount(sessionDoctor.getDid());
        return ok(Arrays.stream(appointments).map(this::view).toList());
    }

    private Response handleDoctorCallNext(JsonNode data) throws Exception {
        if (sessionDoctor == null) {
            return err("not logged in");
        }
        int sid = requiredInt(data, "sid");
        int currentSerial = optionalInt(data, "serialNumber", -1);
        Appointment[] appointments = db.FindAppointmentBelongSchedule(sid);
        Appointment next = Arrays.stream(appointments)
                .filter(a -> a.getDid().equals(sessionDoctor.getDid()))
                .filter(a -> a.getSerialNumber() > currentSerial)
                .filter(a -> a.getStatus() == AppointmentStatus.Ok)
                .min(Comparator.comparingInt(Appointment::getSerialNumber))
                .orElse(null);
        if (next == null) {
            return err("no next appointment");
        }
        db.ChangeAppointmentStatu(next.getApid(), AppointmentStatus.Done);
        Appointment updated = db.FindAppointment(next.getApid());
        return ok(view(updated));
    }

    private Response handleAdminAddSchedules(JsonNode data) throws Exception {
        if (!isAdmin()) {
            return err("not admin");
        }
        JsonNode schedulesNode = data.path("schedules");
        if (!schedulesNode.isArray()) {
            return err("schedules array required");
        }
        List<Schedule> newSchedules = new ArrayList<>();
        for (JsonNode node : schedulesNode) {
            String name = requiredText(node, "name");
            String department = requiredText(node, "department");
            String did = textOrNull(node, "did");
            
            DoctorAccount doctor;
            if (did != null && !did.isBlank()) {
                doctor = db.FindDoctorAccount(did, "");
            } else {
                doctor = findDoctorByNameAndDepartment(name, department);
            }

            if (doctor == null) {
                return err("doctor not exists");
            }
            LocalDateTime start = LocalDateTime.parse(requiredText(node, "startTime"));
            LocalDateTime end = LocalDateTime.parse(requiredText(node, "endTime"));
            int capacity = requiredInt(node, "capacity");
            newSchedules.add(Schedule.create(doctor.getDid(), start, end, capacity));
        }

        // simple overlap check per doctor
        for (Schedule s : newSchedules) {
            Schedule[] existing = db.FindScheduleBelongDoctorAccount(s.getDid());
            for (Schedule e : existing) {
                if (overlap(s, e)) {
                    return err("schedule overlap for did " + s.getDid());
                }
            }
            for (Schedule other : newSchedules) {
                if (s == other) {
                    continue;
                }
                if (s.getDid().equals(other.getDid()) && overlap(s, other)) {
                    return err("new schedule overlap for did " + s.getDid());
                }
            }
        }

        db.InsertSchedules(newSchedules);
        return ok(newSchedules.stream().map(this::view).toList());
    }

    private Response handleAdminUpdateSchedule(JsonNode data) throws Exception {
        if (!isAdmin()) {
            return err("not admin");
        }
        int sid = requiredInt(data, "sid");
        Schedule existing = db.FindSchedule(sid);
        if (existing == null) {
            return err("sid not exists");
        }
        String did = textOrNull(data, "did");
        String start = textOrNull(data, "startTime");
        String end = textOrNull(data, "endTime");
        if (did != null && !did.equals(existing.getDid())) {
            return err("immutable did changed");
        }
        if (start != null && !LocalDateTime.parse(start).equals(existing.getStartTime())) {
            return err("immutable startTime changed");
        }
        if (end != null && !LocalDateTime.parse(end).equals(existing.getEndTime())) {
            return err("immutable endTime changed");
        }
        int newCapacity = requiredInt(data, "capacity");
        int delta = newCapacity - existing.getCapacity();
        db.ChangeScheduleCapacity(sid, delta);
        Schedule updated = new Schedule(existing.getSid(), existing.getDid(), existing.getStartTime(), existing.getEndTime(), existing.getCapacity() + delta, existing.getRes() + delta);
        return ok(view(updated));
    }

    private Response handleAdminAllAppointments() throws Exception {
        if (!isAdmin()) {
            return err("not admin");
        }
        Map<String, Appointment> map = new LinkedHashMap<>();
        for (AppointmentStatus st : AppointmentStatus.values()) {
            Appointment[] items = db.FindAppointmentByStatu(st);
            for (Appointment a : items) {
                map.put(a.getApid(), a);
            }
        }
        return ok(map.values().stream().map(this::view).toList());
    }

    private Response handleAdminReport() throws Exception {
        if (!isAdmin()) {
            return err("not admin");
        }
        Map<String, Object> report = new LinkedHashMap<>();

        List<DoctorAccount> doctors = new ArrayList<>();
        for (String dep : departments) {
            doctors.addAll(Arrays.asList(db.FindDoctorAccounts(dep)));
        }
        report.put("doctors", doctors.stream().map(this::view).toList());

        List<Schedule> schedules = new ArrayList<>();
        for (DoctorAccount d : doctors) {
            schedules.addAll(Arrays.asList(db.FindScheduleBelongDoctorAccount(d.getDid())));
        }
        report.put("schedules", schedules.stream().map(this::view).toList());

        Map<String, Appointment> apMap = new LinkedHashMap<>();
        for (AppointmentStatus st : AppointmentStatus.values()) {
            for (Appointment a : db.FindAppointmentByStatu(st)) {
                apMap.put(a.getApid(), a);
            }
        }
        report.put("appointments", apMap.values().stream().map(this::view).toList());

        return ok(report);
    }

    private Response handleAdminAddDoctors(JsonNode data) throws Exception {
        if (!isAdmin()) {
            return err("not admin");
        }
        JsonNode doctorsNode = data.path("doctors");
        if (!doctorsNode.isArray()) {
            return err("doctors array required");
        }

        List<DoctorAccount> resultList = new ArrayList<>();

        for (JsonNode node : doctorsNode) {
            String name = requiredText(node, "name");
            String passwordHex = textOrNull(node, "passwordHex");
            boolean admin = optionalBool(node, "admin", false);
            String department = requiredText(node, "department");
            String description = textOrNull(node, "description");
            String did = textOrNull(node, "did");

            if (!departments.isEmpty() && !departments.contains(department)) {
                return err("department not exists");
            }

            if (did != null && !did.isBlank()) {
                DoctorAccount existing = db.FindDoctorAccount(did, "");
                if (existing != null && existing.getDid().equals(did)) {
                    // Update existing
                    String newPass = (passwordHex != null && !passwordHex.isEmpty()) ? passwordHex : existing.getPasswordHex();
                    DoctorAccount updated = new DoctorAccount(did, name, newPass, admin, department, description == null ? "" : description);
                    db.UpdateDoctorAccount(updated);
                    resultList.add(updated);
                } else {
                    // Insert new with specific ID
                    if (passwordHex == null || passwordHex.isEmpty()) {
                        return err("password required for new doctor");
                    }
                    DoctorAccount newDoc = new DoctorAccount(did, name, passwordHex, admin, department, description == null ? "" : description);
                    db.InsertDoctorAccount(newDoc);
                    resultList.add(newDoc);
                }
            } else {
                // Create new with auto-generated ID
                if (passwordHex == null || passwordHex.isEmpty()) {
                    return err("password required for new doctor");
                }
                DoctorAccount newDoc = DoctorAccount.create(name, passwordHex, admin, department, description == null ? "" : description);
                db.InsertDoctorAccount(newDoc);
                resultList.add(newDoc);
            }
        }
        return ok(resultList.stream().map(this::view).toList());
    }

    private boolean isLoggedIn() {
        return sessionAccount != null || sessionDoctor != null;
    }

    private boolean isAdmin() {
        return sessionDoctor != null && sessionDoctor.isAdmin();
    }

    private DoctorAccount findDoctorByNameAndDepartment(String name, String department) throws Exception {
        DoctorAccount[] doctors = db.FindDoctorAccounts(department);
        for (DoctorAccount doctor : doctors) {
            if (doctor.getName().equals(name)) {
                return doctor;
            }
        }
        return null;
    }

    private Sex parseSex(JsonNode node) {
        if (node == null || node.isMissingNode()) {
            return Sex.M;
        }
        return Sex.valueOf(node.asText());
    }

    private String requiredText(JsonNode node, String field) {
        JsonNode child = node.path(field);
        if (!child.isTextual()) {
            throw new IllegalArgumentException(field + " required");
        }
        String value = child.asText();
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " required");
        }
        return value;
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode child = node.path(field);
        return child.isTextual() ? child.asText() : null;
    }

    private int requiredInt(JsonNode node, String field) {
        JsonNode child = node.path(field);
        if (!child.canConvertToInt()) {
            throw new IllegalArgumentException(field + " required");
        }
        return child.asInt();
    }

    private int optionalInt(JsonNode node, String field, int def) {
        JsonNode child = node.path(field);
        return child.canConvertToInt() ? child.asInt() : def;
    }

    private boolean optionalBool(JsonNode node, String field, boolean def) {
        JsonNode child = node.path(field);
        return child.isBoolean() ? child.asBoolean() : def;
    }

    private Response ok(Object data) {
        return new Response("ok", data);
    }

    private Response err(String info) {
        Map<String, Object> data = new HashMap<>();
        data.put("err_info", info);
        return new Response("err", data);
    }

    private Map<String, Object> view(Account account) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("aid", account.getAid());
        map.put("name", account.getName());
        map.put("pid", account.getPid());
        map.put("phone", account.getPhone());
        map.put("sex", account.getSex().name());
        return map;
    }

    private Map<String, Object> view(DoctorAccount doctor) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("did", doctor.getDid());
        map.put("name", doctor.getName());
        map.put("admin", doctor.isAdmin());
        map.put("department", doctor.getDepartment());
        map.put("description", doctor.getDescription());
        return map;
    }

    private Map<String, Object> view(Schedule schedule) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("sid", schedule.getSid());
        map.put("did", schedule.getDid());
        map.put("startTime", schedule.getStartTime());
        map.put("endTime", schedule.getEndTime());
        map.put("capacity", schedule.getCapacity());
        map.put("res", schedule.getRes());
        return map;
    }

    private Map<String, Object> view(Appointment appointment) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("serialNumber", appointment.getSerialNumber());
        map.put("apid", appointment.getApid());
        map.put("aid", appointment.getAid());
        map.put("did", appointment.getDid());
        map.put("sid", appointment.getSid());
        map.put("status", appointment.getStatus().name());
        map.put("startTime", appointment.getStartTime());
        map.put("endTime", appointment.getEndTime());
        return map;
    }

    private boolean overlap(Schedule a, Schedule b) {
        return !a.getEndTime().isBefore(b.getStartTime()) && !a.getStartTime().isAfter(b.getEndTime());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class Request {
        public String type;
        public JsonNode data;
    }

    private static final class Response {
        @JsonProperty("Statu")
        public final String statu;
        public final Object data;

        Response(String statu, Object data) {
            this.statu = statu;
            this.data = data == null ? Map.of() : data;
        }
    }
}
