package HABMS.client.controller;

import HABMS.client.App;
import HABMS.client.Session;
import HABMS.client.model.*;
import HABMS.client.net.NetworkClient;
import HABMS.client.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 患者端控制器：负责挂号、个人预约管理与资料维护。
 */
public class PatientMainController {

    // Booking Tab
    @FXML private ListView<String> departmentList;
    @FXML private TextField searchDoctorField;
    @FXML private TableView<Doctor> doctorTable;
    @FXML private TableColumn<Doctor, String> docNameCol;
    @FXML private TableColumn<Doctor, String> docDeptCol;
    @FXML private TableColumn<Doctor, String> docDescCol;
    
    @FXML private TableView<Schedule> scheduleTable;
    @FXML private TableColumn<Schedule, String> schDateCol; // Actually TimeSlot
    @FXML private TableColumn<Schedule, String> schTimeCol;
    @FXML private TableColumn<Schedule, Integer> schCapCol;
    @FXML private TableColumn<Schedule, Integer> schResCol;
    @FXML private Label bookingStatusLabel;

    // My Appointments Tab
    @FXML private TableView<Appointment> appointmentTable;
    @FXML private TableColumn<Appointment, String> apptIdCol;
    @FXML private TableColumn<Appointment, String> apptAidCol;
    @FXML private TableColumn<Appointment, String> apptDocCol;
    @FXML private TableColumn<Appointment, String> apptDocNameCol;
    @FXML private TableColumn<Appointment, String> apptDeptCol;
    @FXML private TableColumn<Appointment, String> apptTimeCol;
    @FXML private TableColumn<Appointment, String> apptStatusCol;
    @FXML private Label apptStatusLabel;

    // Profile Tab
    @FXML private TextField profileNameField;
    @FXML private TextField profilePidField;
    @FXML private TextField profilePhoneField;
    @FXML private ComboBox<String> profileSexCombo;
    @FXML private Label profileStatusLabel;

    /** 初始化各个 tab 的表格/控件并加载数据。 */
    @FXML
    public void initialize() {
        setupBookingTab();
        setupAppointmentsTab();
        setupProfileTab();
    }

    /** 预约挂号页表格绑定与联动。 */
    private void setupBookingTab() {
        // Setup Doctor Table
        docNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        docDeptCol.setCellValueFactory(new PropertyValueFactory<>("department"));
        docDescCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        // Setup Schedule Table
        schDateCol.setCellValueFactory(new PropertyValueFactory<>("startTime")); // Raw start time
        schTimeCol.setCellValueFactory(new PropertyValueFactory<>("timeSlot"));
        schCapCol.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        schResCol.setCellValueFactory(new PropertyValueFactory<>("res"));

        // Load Departments
        loadDepartments();

        // Listeners
        departmentList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadDoctors(newVal);
            }
        });

        doctorTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadSchedules(newVal.getDid());
            }
        });
    }

    /** 初始化我的预约页表格。 */
    private void setupAppointmentsTab() {
        apptIdCol.setCellValueFactory(new PropertyValueFactory<>("apid"));
        apptAidCol.setCellValueFactory(new PropertyValueFactory<>("aid"));
        apptDocCol.setCellValueFactory(new PropertyValueFactory<>("did"));
        apptDocNameCol.setCellValueFactory(new PropertyValueFactory<>("doctorName"));
        apptDeptCol.setCellValueFactory(new PropertyValueFactory<>("department"));
        apptTimeCol.setCellValueFactory(new PropertyValueFactory<>("timeSlot"));
        apptStatusCol.setCellValueFactory(new PropertyValueFactory<>("statusDisplay"));
        
        handleRefreshAppointments(null);
    }

    /** 初始化个人资料页字段。 */
    private void setupProfileTab() {
        User user = Session.getCurrentUser();
        if (user != null) {
            profileNameField.setText(user.getName());
            profilePidField.setText(user.getPid());
            profilePhoneField.setText(user.getPhone());
            profileSexCombo.getItems().addAll("M", "F");
            profileSexCombo.setValue(user.getSex());
        }
    }

    // --- Network Tasks ---

    /** 拉取科室列表填充左侧列表。 */
    private void loadDepartments() {
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws Exception {
                return NetworkClient.getInstance().sendRequest(new Request("department_list", null));
            }
        };
        task.setOnSucceeded(e -> {
            Response resp = task.getValue();
            if (resp.isOk()) {
                List<String> depts = JsonUtil.getMapper().convertValue(resp.getData(), new TypeReference<List<String>>() {});
                departmentList.setItems(FXCollections.observableArrayList(depts));
            }
        });
        new Thread(task).start();
    }

    /** 按科室加载医生列表。 */
    private void loadDoctors(String department) {
        Map<String, String> data = new HashMap<>();
        data.put("department", department);
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws Exception {
                return NetworkClient.getInstance().sendRequest(new Request("doctor_query", data));
            }
        };
        task.setOnSucceeded(e -> {
            Response resp = task.getValue();
            if (resp.isOk()) {
                List<Doctor> docs = JsonUtil.getMapper().convertValue(resp.getData(), new TypeReference<List<Doctor>>() {});
                doctorTable.setItems(FXCollections.observableArrayList(docs));
            }
        });
        new Thread(task).start();
    }

    /** 按医生加载排班列表。 */
    private void loadSchedules(String did) {
        Map<String, String> data = new HashMap<>();
        data.put("did", did);
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws Exception {
                return NetworkClient.getInstance().sendRequest(new Request("schedule_by_doctor", data));
            }
        };
        task.setOnSucceeded(e -> {
            Response resp = task.getValue();
            if (resp.isOk()) {
                List<Schedule> schedules = JsonUtil.getMapper().convertValue(resp.getData(), new TypeReference<List<Schedule>>() {});
                scheduleTable.setItems(FXCollections.observableArrayList(schedules));
            }
        });
        new Thread(task).start();
    }

    /** 手动刷新当前医生排班。 */
    @FXML
    private void handleRefreshSchedules(ActionEvent event) {
        Doctor selectedDoc = doctorTable.getSelectionModel().getSelectedItem();
        if (selectedDoc != null) {
            loadSchedules(selectedDoc.getDid());
        }
    }

    /** 通过姓名查询医生列表。 */
    @FXML
    private void handleSearchDoctor(ActionEvent event) {
        String name = searchDoctorField.getText().trim();
        if (name.isEmpty()) {
            // If empty, maybe reload based on selected department or clear?
            // For now, let's just return or show all if department selected.
            String selectedDept = departmentList.getSelectionModel().getSelectedItem();
            if (selectedDept != null) {
                loadDoctors(selectedDept);
            }
            return;
        }

        Map<String, String> data = new HashMap<>();
        data.put("name", name);
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws Exception {
                return NetworkClient.getInstance().sendRequest(new Request("doctor_query", data));
            }
        };
        task.setOnSucceeded(e -> {
            Response resp = task.getValue();
            if (resp.isOk()) {
                List<Doctor> docs = JsonUtil.getMapper().convertValue(resp.getData(), new TypeReference<List<Doctor>>() {});
                doctorTable.setItems(FXCollections.observableArrayList(docs));
            } else {
                 // Handle error or empty
                 doctorTable.setItems(FXCollections.observableArrayList());
            }
        });
        new Thread(task).start();
    }

    /** 提交预约请求。 */
    @FXML
    private void handleBookAppointment(ActionEvent event) {
        Schedule selectedSchedule = scheduleTable.getSelectionModel().getSelectedItem();
        if (selectedSchedule == null) {
            bookingStatusLabel.setText("请先选择排班");
            return;
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("sid", selectedSchedule.getSid());
        data.put("did", selectedSchedule.getDid());
        
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws Exception {
                return NetworkClient.getInstance().sendRequest(new Request("appointment_create", data));
            }
        };
        task.setOnSucceeded(e -> {
            Response resp = task.getValue();
            if (resp.isOk()) {
                bookingStatusLabel.setStyle("-fx-text-fill: green;");
                bookingStatusLabel.setText("预约成功！");
                handleRefreshSchedules(null); // Refresh capacity
            } else {
                bookingStatusLabel.setStyle("-fx-text-fill: red;");
                bookingStatusLabel.setText("预约失败: " + resp.getErrInfo());
            }
        });
        new Thread(task).start();
    }

    /** 刷新“我的预约”列表。 */
    @FXML
    private void handleRefreshAppointments(ActionEvent event) {
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws Exception {
                return NetworkClient.getInstance().sendRequest(new Request("appointment_list", null));
            }
        };
        task.setOnSucceeded(e -> {
            Response resp = task.getValue();
            if (resp.isOk()) {
                List<Appointment> appts = JsonUtil.getMapper().convertValue(resp.getData(), new TypeReference<List<Appointment>>() {});
                appointmentTable.setItems(FXCollections.observableArrayList(appts));
            }
        });
        new Thread(task).start();
    }

    /** 取消选中的预约。 */
    @FXML
    private void handleCancelAppointment(ActionEvent event) {
        Appointment selected = appointmentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            apptStatusLabel.setText("请选择要取消的预约");
            return;
        }
        
        Map<String, String> data = new HashMap<>();
        data.put("apid", selected.getApid());
        
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws Exception {
                return NetworkClient.getInstance().sendRequest(new Request("appointment_cancel", data));
            }
        };
        task.setOnSucceeded(e -> {
            Response resp = task.getValue();
            if (resp.isOk()) {
                apptStatusLabel.setStyle("-fx-text-fill: green;");
                apptStatusLabel.setText("取消成功");
                handleRefreshAppointments(null);
            } else {
                apptStatusLabel.setStyle("-fx-text-fill: red;");
                apptStatusLabel.setText("取消失败: " + resp.getErrInfo());
            }
        });
        new Thread(task).start();
    }

    /** 更新个人资料并同步 Session。 */
    @FXML
    private void handleUpdateProfile(ActionEvent event) {
        User user = Session.getCurrentUser();
        if (user == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("aid", user.getAid());
        data.put("pid", user.getPid());
        data.put("name", profileNameField.getText());
        data.put("phone", profilePhoneField.getText());
        data.put("sex", profileSexCombo.getValue());
        data.put("passwordHex", user.getPasswordHex());
        
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws Exception {
                return NetworkClient.getInstance().sendRequest(new Request("account_update", data));
            }
        };
        task.setOnSucceeded(e -> {
            Response resp = task.getValue();
            if (resp.isOk()) {
                profileStatusLabel.setStyle("-fx-text-fill: green;");
                profileStatusLabel.setText("修改成功");
                // Update Session
                User u = Session.getCurrentUser();
                u.setName(profileNameField.getText());
                u.setPhone(profilePhoneField.getText());
                u.setSex(profileSexCombo.getValue());
            } else {
                profileStatusLabel.setStyle("-fx-text-fill: red;");
                profileStatusLabel.setText("修改失败: " + resp.getErrInfo());
            }
        });
        new Thread(task).start();
    }

    /** 注销账号并返回登录页。 */
    @FXML
    private void handleDeleteAccount(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认注销");
        alert.setHeaderText("您确定要注销账号吗？");
        alert.setContentText("此操作不可恢复！");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Task<Response> task = new Task<>() {
                @Override
                protected Response call() throws Exception {
                    Map<String, Object> data = new HashMap<>();
                    data.put("aid", Session.getCurrentUser().getAid());
                    return NetworkClient.getInstance().sendRequest(new Request("account_delete", data));
                }
            };
            task.setOnSucceeded(e -> {
                Response resp = task.getValue();
                if (resp.isOk()) {
                    try {
                        handleLogout(null);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                } else {
                    profileStatusLabel.setText("注销失败: " + resp.getErrInfo());
                }
            });
            new Thread(task).start();
        }
    }

    /** 退出登录并回到登录页。 */
    @FXML
    private void handleLogout(ActionEvent event) throws IOException {
        NetworkClient.getInstance().sendRequest(new Request("account_logout", null));
        Session.clear();
        App.setRoot("view/login", "飞马星球医院预约挂号系统");
    }
}
