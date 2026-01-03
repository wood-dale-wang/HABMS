package HABMS.client.controller;

import HABMS.client.App;
import HABMS.client.Session;
import HABMS.client.model.Appointment;
import HABMS.client.model.Doctor;
import HABMS.client.model.Request;
import HABMS.client.model.Response;
import HABMS.client.model.Schedule;
import HABMS.client.net.NetworkClient;
import HABMS.client.util.JsonUtil;
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

/**
 * 医生端控制器：展示排班、候诊预约和个人资料。
 */
public class DoctorMainController {

    @FXML private Label welcomeLabel;
    @FXML private TabPane mainTabPane;

    // Schedule Tab
    @FXML private TableView<Schedule> scheduleTable;
    @FXML private TableColumn<Schedule, Integer> colSchId;
    @FXML private TableColumn<Schedule, String> colSchStart;
    @FXML private TableColumn<Schedule, String> colSchEnd;
    @FXML private TableColumn<Schedule, Integer> colSchCap;
    @FXML private TableColumn<Schedule, Integer> colSchRes;

    // Appointment Tab
    @FXML private TableView<Appointment> appointmentTable;
    @FXML private TableColumn<Appointment, String> colAppId;
    @FXML private TableColumn<Appointment, String> colAppPid;
    @FXML private TableColumn<Appointment, String> colAppTime;
    @FXML private TableColumn<Appointment, String> colAppStatus;
    @FXML private TextArea diagnosisArea;
    @FXML private ComboBox<Schedule> workScheduleCombo;

    // Removed management tab; admin功能迁移至独立页面

    // Profile Tab
    @FXML private TextField profileDid;
    @FXML private TextField profileName;
    @FXML private TextField profileDept;
    @FXML private TextArea profileDesc;
    @FXML private CheckBox profileAdmin;

    /** 初始化表格列、用户信息并加载初始数据。 */
    @FXML
    public void initialize() {
        Doctor doctor = Session.getCurrentDoctor();
        if (doctor == null) {
            try {
                App.setRoot("view/login", "登录");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        welcomeLabel.setText("欢迎, " + doctor.getName() + (doctor.isAdmin() ? " (管理员)" : ""));

        // Setup Columns
        colSchId.setCellValueFactory(new PropertyValueFactory<>("sid"));
        colSchStart.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        colSchEnd.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        colSchCap.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        colSchRes.setCellValueFactory(new PropertyValueFactory<>("res"));

        colAppId.setCellValueFactory(new PropertyValueFactory<>("apid"));
        colAppPid.setCellValueFactory(new PropertyValueFactory<>("aid")); // Assuming AID is patient ID
        colAppTime.setCellValueFactory(new PropertyValueFactory<>("startTime")); 
        // Status field is exposed as getStatus() on Appointment
        colAppStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Profile
        profileDid.setText(doctor.getDid());
        profileName.setText(doctor.getName());
        profileDept.setText(doctor.getDepartment());
        profileDesc.setText(doctor.getDescription());
        profileAdmin.setSelected(doctor.isAdmin());

        // Setup ComboBox
        workScheduleCombo.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Schedule item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getStartTime() + " - " + item.getEndTime());
                }
            }
        });
        workScheduleCombo.setButtonCell(workScheduleCombo.getCellFactory().call(null));

        // Load initial data
        handleRefreshSchedules(null);
        handleRefreshAppointments(null);
    }

    /** 退出医生登录并返回登录页。 */
    @FXML
    private void handleLogout(ActionEvent event) throws IOException {
        Session.clear();
        App.setRoot("view/login", "登录");
    }

    /** 重新加载当前医生的排班列表。 */
    @FXML
    private void handleRefreshSchedules(ActionEvent event) {
        Doctor doctor = Session.getCurrentDoctor();
        if (doctor == null) return;

        Map<String, String> data = new HashMap<>();
        data.put("did", doctor.getDid());
        Request req = new Request("schedule_by_doctor", data);
        
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws Exception {
                return NetworkClient.getInstance().sendRequest(req);
            }
        };

        task.setOnSucceeded(e -> {
            Response resp = task.getValue();
            if (resp.isOk()) {
                try {
                    List<Schedule> list = JsonUtil.getMapper().convertValue(
                        resp.getData(), 
                        JsonUtil.getMapper().getTypeFactory().constructCollectionType(List.class, Schedule.class)
                    );
                    scheduleTable.setItems(FXCollections.observableArrayList(list));
                    workScheduleCombo.setItems(FXCollections.observableArrayList(list));
                    if (!list.isEmpty()) {
                        workScheduleCombo.getSelectionModel().select(0);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        new Thread(task).start();
    }

    /** 拉取医生的预约列表，仅显示候诊中的预约。 */
    @FXML
    private void handleRefreshAppointments(ActionEvent event) {
        Request req = new Request("doctor_appointments", null);
        
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws Exception {
                return NetworkClient.getInstance().sendRequest(req);
            }
        };

        task.setOnSucceeded(e -> {
            Response resp = task.getValue();
            if (resp.isOk()) {
                try {
                    List<Appointment> list = JsonUtil.getMapper().convertValue(
                        resp.getData(), 
                        JsonUtil.getMapper().getTypeFactory().constructCollectionType(List.class, Appointment.class)
                    );
                    List<Appointment> waiting = list.stream()
                            .filter(a -> "Ok".equalsIgnoreCase(a.getStatus()))
                            .toList();
                    appointmentTable.setItems(FXCollections.observableArrayList(waiting));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                showAlert("错误", "获取预约列表失败: " + resp.getErrInfo());
            }
        });
        new Thread(task).start();
    }

    /** 按排班叫号下一位候诊患者。 */
    @FXML
    private void handleCallNext(ActionEvent event) {
        Schedule selectedSchedule = workScheduleCombo.getSelectionModel().getSelectedItem();
        if (selectedSchedule == null) {
            showAlert("提示", "请先选择当前工作的排班");
            return;
        }

        // Only proceed when there are waiting (status=Ok) appointments for this schedule
        List<Appointment> waiting = appointmentTable.getItems().stream()
                .filter(a -> a.getSid() == selectedSchedule.getSid())
                .filter(a -> "Ok".equalsIgnoreCase(a.getStatus()))
                .toList();
        
        if (waiting.isEmpty()) {
            showAlert("提示", "当前排班无候诊患者");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("sid", selectedSchedule.getSid());
        
        Request req = new Request("doctor_call_next", data);
        
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws Exception {
                return NetworkClient.getInstance().sendRequest(req);
            }
        };
        
        task.setOnSucceeded(e -> {
            Response resp = task.getValue();
            if (resp.isOk()) {
                try {
                    Appointment nextApp = JsonUtil.getMapper().convertValue(resp.getData(), Appointment.class);
                    showAlert("叫号成功", "请 " + nextApp.getAid() + " 号患者 (" + nextApp.getApid() + ") 就诊");
                    diagnosisArea.setText("正在诊疗: " + nextApp.getAid() + "\n预约号: " + nextApp.getApid());
                    handleRefreshAppointments(null); // Refresh list to show status change
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                showAlert("提示", "当前排班无更多候诊患者 (" + resp.getErrInfo() + ")");
            }
        });
        
        new Thread(task).start();
    }

    /** 完成当前诊疗占位的简单占位逻辑。 */
    @FXML
    private void handleCompleteDiagnosis(ActionEvent event) {
        diagnosisArea.clear();
        showAlert("提示", "诊疗已完成，请呼叫下一位");
    }


    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
