package HABMS.client.controller;

import HABMS.client.App;
import HABMS.client.model.Doctor;
import HABMS.client.model.Request;
import HABMS.client.model.Response;
import HABMS.client.net.NetworkClient;
import HABMS.client.util.JsonUtil;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminMainController {

    @FXML private Label doctorFileLabel;
    @FXML private Label doctorImportResult;
    @FXML private Label scheduleFileLabel;
    @FXML private Label scheduleImportResult;
    @FXML private TableView<Doctor> doctorTable;
    @FXML private TableColumn<Doctor, String> colDid;
    @FXML private TableColumn<Doctor, String> colName;
    @FXML private TableColumn<Doctor, String> colDept;
    @FXML private TableColumn<Doctor, Boolean> colAdmin;
    @FXML private TableColumn<Doctor, String> colDesc;
    @FXML private Button refreshDoctorBtn;

    private final FileChooser csvChooser = new FileChooser();

    @FXML
    public void initialize() {
        csvChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV / TXT", "*.csv", "*.txt"));

        if (doctorTable != null) {
            colDid.setCellValueFactory(new PropertyValueFactory<>("did"));
            colName.setCellValueFactory(new PropertyValueFactory<>("name"));
            colDept.setCellValueFactory(new PropertyValueFactory<>("department"));
            colAdmin.setCellValueFactory(new PropertyValueFactory<>("admin"));
            colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        }
    }

    @FXML
    private void handleRefreshDoctors(ActionEvent event) {
        if (refreshDoctorBtn != null) {
            refreshDoctorBtn.setDisable(true);
        }

        Task<List<Doctor>> task = new Task<>() {
            @Override
            protected List<Doctor> call() throws Exception {
                // Step 1: fetch departments
                Request deptReq = new Request("department_list", new HashMap<>());
                Response deptResp = NetworkClient.getInstance().sendRequest(deptReq);
                if (!deptResp.isOk()) {
                    throw new IOException("科室列表获取失败: " + deptResp.getErrInfo());
                }
                List<String> departments = JsonUtil.getMapper().convertValue(
                        deptResp.getData(),
                        JsonUtil.getMapper().getTypeFactory().constructCollectionType(List.class, String.class));

                List<Doctor> all = new ArrayList<>();
                for (String dept : departments) {
                    Map<String, String> data = new HashMap<>();
                    data.put("department", dept);
                    Request req = new Request("doctor_query", data);
                    Response resp = NetworkClient.getInstance().sendRequest(req);
                    if (!resp.isOk()) {
                        throw new IOException("doctor_query 失败: " + resp.getErrInfo());
                    }
                    List<Doctor> doctors = JsonUtil.getMapper().convertValue(
                            resp.getData(),
                            JsonUtil.getMapper().getTypeFactory().constructCollectionType(List.class, Doctor.class));
                    all.addAll(doctors);
                }
                return all;
            }
        };

        task.setOnSucceeded(e -> {
            List<Doctor> list = task.getValue();
            doctorTable.setItems(FXCollections.observableArrayList(list));
            if (refreshDoctorBtn != null) {
                refreshDoctorBtn.setDisable(false);
            }
        });

        task.setOnFailed(e -> {
            doctorImportResult.setStyle("-fx-text-fill: #c0392b;");
            doctorImportResult.setText("医生列表刷新失败: " + e.getSource().getException().getMessage());
            if (refreshDoctorBtn != null) {
                refreshDoctorBtn.setDisable(false);
            }
            showError("刷新医生列表失败", e.getSource().getException().getMessage());
        });

        new Thread(task).start();
    }

    @FXML
    private void handleImportDoctors(ActionEvent event) {
        File file = csvChooser.showOpenDialog(((Node) event.getSource()).getScene().getWindow());
        if (file == null) {
            return;
        }
        doctorFileLabel.setText(file.getName());
        doctorImportResult.setText("正在导入医生账户...");

        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws Exception {
                List<Map<String, Object>> doctors = parseDoctorCsv(file);
                Map<String, Object> data = new HashMap<>();
                data.put("doctors", doctors);
                return NetworkClient.getInstance().sendRequest(new Request("admin_add_doctors", data));
            }
        };

        task.setOnSucceeded(e -> {
            Response resp = task.getValue();
            if (resp.isOk()) {
                doctorImportResult.setStyle("-fx-text-fill: #1b8a3f;");
                doctorImportResult.setText("导入成功");
            } else {
                doctorImportResult.setStyle("-fx-text-fill: #c0392b;");
                doctorImportResult.setText("导入失败: " + resp.getErrInfo());
                showError("导入医生失败", resp.getErrInfo());
            }
        });

        task.setOnFailed(e -> {
            doctorImportResult.setStyle("-fx-text-fill: #c0392b;");
            doctorImportResult.setText("导入失败: " + e.getSource().getException().getMessage());
            e.getSource().getException().printStackTrace();
            showError("导入医生失败", e.getSource().getException().getMessage());
        });

        new Thread(task).start();
    }

    @FXML
    private void handleImportSchedules(ActionEvent event) {
        File file = csvChooser.showOpenDialog(((Node) event.getSource()).getScene().getWindow());
        if (file == null) {
            return;
        }
        scheduleFileLabel.setText(file.getName());
        scheduleImportResult.setText("正在导入排班表...");

        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws Exception {
                List<Map<String, Object>> schedules = parseScheduleCsv(file);
                Map<String, Object> data = new HashMap<>();
                data.put("schedules", schedules);
                return NetworkClient.getInstance().sendRequest(new Request("admin_add_schedules", data));
            }
        };

        task.setOnSucceeded(e -> {
            Response resp = task.getValue();
            if (resp.isOk()) {
                scheduleImportResult.setStyle("-fx-text-fill: #1b8a3f;");
                scheduleImportResult.setText("导入成功");
            } else {
                scheduleImportResult.setStyle("-fx-text-fill: #c0392b;");
                scheduleImportResult.setText("导入失败: " + resp.getErrInfo());
                showError("导入排班失败", resp.getErrInfo());
            }
        });

        task.setOnFailed(e -> {
            scheduleImportResult.setStyle("-fx-text-fill: #c0392b;");
            scheduleImportResult.setText("导入失败: " + e.getSource().getException().getMessage());
            e.getSource().getException().printStackTrace();
            showError("导入排班失败", e.getSource().getException().getMessage());
        });

        new Thread(task).start();
    }

    @FXML
    private void handleLogout(ActionEvent event) throws IOException {
        App.setRoot("view/login", "飞马星球医院预约挂号系统");
    }

    private List<Map<String, Object>> parseDoctorCsv(File file) throws IOException {
        List<Map<String, Object>> doctors = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String firstLine = reader.readLine();
            if (firstLine == null) {
                throw new IOException("文件为空");
            }

            String line;
            boolean hasHeader = looksLikeDoctorHeader(firstLine);
            if (!hasHeader) {
                doctors.add(parseDoctorLine(firstLine, 1));
            }
            int lineNo = 1;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                doctors.add(parseDoctorLine(line, lineNo));
            }
        }
        return doctors;
    }

    private List<Map<String, Object>> parseScheduleCsv(File file) throws IOException {
        List<Map<String, Object>> schedules = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String firstLine = reader.readLine();
            if (firstLine == null) {
                throw new IOException("文件为空");
            }

            String line;
            boolean hasHeader = looksLikeScheduleHeader(firstLine);
            if (!hasHeader) {
                schedules.add(parseScheduleLine(firstLine, 1));
            }
            int lineNo = 1;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                schedules.add(parseScheduleLine(line, lineNo));
            }
        }
        return schedules;
    }

    private Map<String, Object> parseDoctorLine(String line, int lineNo) throws IOException {
        String[] parts = line.split(",");
        if (parts.length < 3) {
            throw new IOException("医生账户第 " + lineNo + " 行字段不足");
        }
        String name = parts[0].trim();
        String passwordPlain = parts[1].trim();
        String department = parts[2].trim();
        boolean admin = parts.length > 3 && Boolean.parseBoolean(parts[3].trim());
        String describe = parts.length > 4 ? parts[4].trim() : "";

        Map<String, Object> doctor = new HashMap<>();
        doctor.put("name", name);
        doctor.put("passwordHex", sha256(passwordPlain));
        doctor.put("department", department);
        if (admin) {
            doctor.put("admin", true);
        }
        if (!describe.isEmpty()) {
            doctor.put("describe", describe);
        }
        return doctor;
    }

    private Map<String, Object> parseScheduleLine(String line, int lineNo) throws IOException {
        String[] parts = line.split(",");
        if (parts.length < 4) {
            throw new IOException("排班第 " + lineNo + " 行字段不足");
        }
        Map<String, Object> schedule = new HashMap<>();
        schedule.put("did", parts[0].trim());
        schedule.put("startTime", parts[1].trim());
        schedule.put("endTime", parts[2].trim());
        try {
            schedule.put("capacity", Integer.parseInt(parts[3].trim()));
        } catch (NumberFormatException ex) {
            throw new IOException("排班第 " + lineNo + " 行容量非数字");
        }
        return schedule;
    }

    private boolean looksLikeDoctorHeader(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.contains("name") && lower.contains("password");
    }

    private boolean looksLikeScheduleHeader(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.contains("did") && lower.contains("start");
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("无法计算密码摘要", e);
        }
    }

    private void showError(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
