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
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.GridPane;
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

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class AdminMainController {

    @FXML private Label doctorFileLabel;
    @FXML private Label doctorImportResult;
    @FXML private Label doctorListResult;
    @FXML private Label scheduleFileLabel;
    @FXML private Label scheduleImportResult;
    @FXML private TableView<Doctor> doctorTable;
    @FXML private TableColumn<Doctor, String> colDid;
    @FXML private TableColumn<Doctor, String> colName;
    @FXML private TableColumn<Doctor, String> colDept;
    @FXML private TableColumn<Doctor, Boolean> colAdmin;
    @FXML private TableColumn<Doctor, String> colDesc;
    @FXML private Button refreshDoctorBtn;

    @FXML private Label scheduleListResult;
    @FXML private Button refreshScheduleBtn;
    @FXML private TableView<HABMS.client.model.Schedule> scheduleTable;
    @FXML private TableColumn<HABMS.client.model.Schedule, Integer> colSid;
    @FXML private TableColumn<HABMS.client.model.Schedule, String> colSDid;
    @FXML private TableColumn<HABMS.client.model.Schedule, String> colSStart;
    @FXML private TableColumn<HABMS.client.model.Schedule, String> colSEnd;
    @FXML private TableColumn<HABMS.client.model.Schedule, Integer> colSCap;
    @FXML private TableColumn<HABMS.client.model.Schedule, Integer> colSRes;

    private final FileChooser csvChooser = new FileChooser();
    private final DataFormatter dataFormatter = new DataFormatter();

    @FXML
    public void initialize() {
        csvChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV / TXT", "*.csv", "*.txt"));
        csvChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel (xls/xlsx)", "*.xls", "*.xlsx"));

        if (doctorTable != null) {
            colDid.setCellValueFactory(new PropertyValueFactory<>("did"));
            colName.setCellValueFactory(new PropertyValueFactory<>("name"));
            colDept.setCellValueFactory(new PropertyValueFactory<>("department"));
            colAdmin.setCellValueFactory(new PropertyValueFactory<>("admin"));
            colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        }

        if (scheduleTable != null) {
            colSid.setCellValueFactory(new PropertyValueFactory<>("sid"));
            colSDid.setCellValueFactory(new PropertyValueFactory<>("did"));
            colSStart.setCellValueFactory(new PropertyValueFactory<>("startTime"));
            colSEnd.setCellValueFactory(new PropertyValueFactory<>("endTime"));
            colSCap.setCellValueFactory(new PropertyValueFactory<>("capacity"));
            colSRes.setCellValueFactory(new PropertyValueFactory<>("res"));
        }
    }

    @FXML
    private void handleRefreshDoctors(ActionEvent event) {
        if (refreshDoctorBtn != null) {
            refreshDoctorBtn.setDisable(true);
        }

        if (doctorListResult != null) {
            doctorListResult.setText("");
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
            if (doctorListResult != null) {
                doctorListResult.setText("医生列表刷新失败: " + e.getSource().getException().getMessage());
            }
            if (refreshDoctorBtn != null) {
                refreshDoctorBtn.setDisable(false);
            }
            showError("刷新医生列表失败", e.getSource().getException().getMessage());
        });

        new Thread(task).start();
    }

    @FXML
    private void handleRefreshSchedules(ActionEvent event) {
        if (refreshScheduleBtn != null) {
            refreshScheduleBtn.setDisable(true);
        }
        if (scheduleListResult != null) {
            scheduleListResult.setText("");
        }

        Task<List<HABMS.client.model.Schedule>> task = new Task<>() {
            @Override
            protected List<HABMS.client.model.Schedule> call() throws Exception {
                Request req = new Request("admin_report", new HashMap<>());
                Response resp = NetworkClient.getInstance().sendRequest(req);
                if (!resp.isOk()) {
                    throw new IOException("获取排班列表失败: " + resp.getErrInfo());
                }
                com.fasterxml.jackson.databind.JsonNode data = resp.getData();
                com.fasterxml.jackson.databind.JsonNode schedulesObj = data.get("schedules");
                return JsonUtil.getMapper().convertValue(
                        schedulesObj,
                        JsonUtil.getMapper().getTypeFactory().constructCollectionType(List.class, HABMS.client.model.Schedule.class));
            }
        };

        task.setOnSucceeded(e -> {
            List<HABMS.client.model.Schedule> list = task.getValue();
            scheduleTable.setItems(FXCollections.observableArrayList(list));
            if (refreshScheduleBtn != null) {
                refreshScheduleBtn.setDisable(false);
            }
        });

        task.setOnFailed(e -> {
            if (scheduleListResult != null) {
                scheduleListResult.setText("排班列表刷新失败: " + e.getSource().getException().getMessage());
            }
            if (refreshScheduleBtn != null) {
                refreshScheduleBtn.setDisable(false);
            }
            showError("刷新排班列表失败", e.getSource().getException().getMessage());
        });

        new Thread(task).start();
    }

    @FXML
    private void handleDeleteDoctor(ActionEvent event) {
        Doctor selected = doctorTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("未选择医生", "请先在列表中选择要删除的医生");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText("删除医生: " + selected.getName());
        alert.setContentText("确定要删除该医生吗？此操作不可恢复。");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Response> task = new Task<>() {
                    @Override
                    protected Response call() throws Exception {
                        Map<String, Object> data = new HashMap<>();
                        data.put("did", selected.getDid());
                        return NetworkClient.getInstance().sendRequest(new Request("admin_delete_doctor", data));
                    }
                };

                task.setOnSucceeded(e -> {
                    Response resp = task.getValue();
                    if (resp.isOk()) {
                        handleRefreshDoctors(null);
                    } else {
                        showError("删除失败", resp.getErrInfo());
                    }
                });

                task.setOnFailed(e -> {
                    showError("删除失败", e.getSource().getException().getMessage());
                });

                new Thread(task).start();
            }
        });
    }

    @FXML
    private void handleDeleteSchedule(ActionEvent event) {
        HABMS.client.model.Schedule selected = scheduleTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("未选择排班", "请先在列表中选择要删除的排班");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText("删除排班 SID: " + selected.getSid());
        alert.setContentText("确定要删除该排班吗？此操作不可恢复。");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Response> task = new Task<>() {
                    @Override
                    protected Response call() throws Exception {
                        Map<String, Object> data = new HashMap<>();
                        data.put("sid", selected.getSid());
                        return NetworkClient.getInstance().sendRequest(new Request("admin_delete_schedule", data));
                    }
                };

                task.setOnSucceeded(e -> {
                    Response resp = task.getValue();
                    if (resp.isOk()) {
                        handleRefreshSchedules(null);
                    } else {
                        showError("删除失败", resp.getErrInfo());
                    }
                });

                task.setOnFailed(e -> {
                    showError("删除失败", e.getSource().getException().getMessage());
                });

                new Thread(task).start();
            }
        });
    }

    @FXML
    private void handleEditSchedule(ActionEvent event) {
        HABMS.client.model.Schedule selected = scheduleTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("未选择排班", "请先在列表中选择要修改的排班");
            return;
        }

        Dialog<Map<String, Object>> dialog = new Dialog<>();
        dialog.setTitle("修改排班信息");
        dialog.setHeaderText("修改排班 SID: " + selected.getSid());

        ButtonType okButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField did = new TextField(selected.getDid());
        did.setPromptText("医生ID");
        
        TextField startTime = new TextField(selected.getStartTime());
        startTime.setPromptText("2026-01-01T09:00:00");
        
        TextField endTime = new TextField(selected.getEndTime());
        endTime.setPromptText("2026-01-01T12:00:00");
        
        TextField capacity = new TextField(String.valueOf(selected.getCapacity()));
        capacity.setPromptText("20");

        grid.add(new Label("医生ID:"), 0, 0);
        grid.add(did, 1, 0);
        grid.add(new Label("开始时间:"), 0, 1);
        grid.add(startTime, 1, 1);
        grid.add(new Label("结束时间:"), 0, 2);
        grid.add(endTime, 1, 2);
        grid.add(new Label("容量:"), 0, 3);
        grid.add(capacity, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                Map<String, Object> schedule = new HashMap<>();
                schedule.put("sid", selected.getSid());
                schedule.put("did", did.getText().trim());
                schedule.put("startTime", startTime.getText().trim());
                schedule.put("endTime", endTime.getText().trim());
                try {
                    schedule.put("capacity", Integer.parseInt(capacity.getText().trim()));
                } catch (NumberFormatException e) {
                    return null;
                }
                return schedule;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(schedule -> {
            if (schedule.get("capacity") == null) {
                showError("输入错误", "容量必须为数字");
                return;
            }
            
            Task<Response> task = new Task<>() {
                @Override
                protected Response call() throws Exception {
                    return NetworkClient.getInstance().sendRequest(new Request("admin_update_schedule", schedule));
                }
            };

            task.setOnSucceeded(e -> {
                Response resp = task.getValue();
                if (resp.isOk()) {
                    handleRefreshSchedules(null);
                } else {
                    showError("修改排班失败", resp.getErrInfo());
                }
            });
            
            task.setOnFailed(e -> {
                showError("修改排班失败", e.getSource().getException().getMessage());
            });
            
            new Thread(task).start();
        });
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
                List<Map<String, Object>> doctors = parseDoctorFile(file);
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
    private void handleManualAddDoctor(ActionEvent event) {
        showDoctorInputDialog(null);
    }

    @FXML
    private void handleEditDoctor(ActionEvent event) {
        Doctor selectedDoctor = doctorTable.getSelectionModel().getSelectedItem();
        if (selectedDoctor == null) {
            showError("未选择医生", "请先在列表中选择要修改的医生");
            return;
        }
        showDoctorInputDialog(selectedDoctor);
    }

    private void showDoctorInputDialog(Doctor existing) {
        boolean isEdit = (existing != null);
        Dialog<Map<String, Object>> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "修改医生信息" : "手动添加医生");
        dialog.setHeaderText(isEdit ? "修改医生: " + existing.getName() : "请输入医生信息");

        ButtonType okButtonType = new ButtonType(isEdit ? "保存" : "添加", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField did = new TextField(isEdit ? existing.getDid() : "");
        did.setPromptText("医生ID (可选)");
        if (isEdit) {
            did.setEditable(false);
            did.setDisable(true);
        }

        TextField name = new TextField(isEdit ? existing.getName() : "");
        name.setPromptText("姓名");
        
        PasswordField password = new PasswordField();
        password.setPromptText(isEdit ? "留空则不修改密码" : "密码");
        
        TextField department = new TextField(isEdit ? existing.getDepartment() : "");
        department.setPromptText("科室");
        
        TextArea description = new TextArea(isEdit ? existing.getDescription() : "");
        description.setPromptText("描述");
        description.setPrefRowCount(3);
        
        CheckBox isAdmin = new CheckBox("管理员");
        if (isEdit) isAdmin.setSelected(existing.isAdmin());

        grid.add(new Label("ID:"), 0, 0);
        grid.add(did, 1, 0);
        grid.add(new Label("姓名:"), 0, 1);
        grid.add(name, 1, 1);
        grid.add(new Label("密码:"), 0, 2);
        grid.add(password, 1, 2);
        grid.add(new Label("科室:"), 0, 3);
        grid.add(department, 1, 3);
        grid.add(new Label("描述:"), 0, 4);
        grid.add(description, 1, 4);
        grid.add(isAdmin, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                Map<String, Object> doctor = new HashMap<>();
                doctor.put("did", did.getText().trim());
                doctor.put("name", name.getText().trim());
                String pwd = password.getText().trim();
                if (!pwd.isEmpty()) {
                    doctor.put("passwordHex", sha256(pwd));
                } else {
                    doctor.put("passwordHex", "");
                }
                doctor.put("department", department.getText().trim());
                doctor.put("admin", isAdmin.isSelected());
                doctor.put("description", description.getText().trim());
                return doctor;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(doctor -> {
            if (((String) doctor.get("name")).isEmpty() || ((String) doctor.get("department")).isEmpty()) {
                showError("输入错误", "姓名和科室不能为空");
                return;
            }
            
            Task<Response> task = new Task<>() {
                @Override
                protected Response call() throws Exception {
                    List<Map<String, Object>> doctors = new ArrayList<>();
                    doctors.add(doctor);
                    Map<String, Object> data = new HashMap<>();
                    data.put("doctors", doctors);
                    return NetworkClient.getInstance().sendRequest(new Request("admin_add_doctors", data));
                }
            };

            task.setOnSucceeded(e -> {
                Response resp = task.getValue();
                if (resp.isOk()) {
                    doctorImportResult.setStyle("-fx-text-fill: #1b8a3f;");
                    doctorImportResult.setText(isEdit ? "修改成功" : "添加成功");
                    if (isEdit) handleRefreshDoctors(null);
                } else {
                    doctorImportResult.setStyle("-fx-text-fill: #c0392b;");
                    doctorImportResult.setText((isEdit ? "修改失败: " : "添加失败: ") + resp.getErrInfo());
                    showError(isEdit ? "修改医生失败" : "添加医生失败", resp.getErrInfo());
                }
            });
            
            task.setOnFailed(e -> {
                doctorImportResult.setStyle("-fx-text-fill: #c0392b;");
                doctorImportResult.setText("操作失败: " + e.getSource().getException().getMessage());
                showError("操作失败", e.getSource().getException().getMessage());
            });
            
            new Thread(task).start();
        });
    }

    @FXML
    private void handleManualAddSchedule(ActionEvent event) {
        Dialog<Map<String, Object>> dialog = new Dialog<>();
        dialog.setTitle("手动添加排班");
        dialog.setHeaderText("请输入排班信息");

        ButtonType loginButtonType = new ButtonType("添加", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField did = new TextField();
        did.setPromptText("医生ID");
        TextField name = new TextField();
        name.setPromptText("医生姓名");
        TextField department = new TextField();
        department.setPromptText("科室");
        TextField startTime = new TextField();
        startTime.setPromptText("2026-01-01T09:00:00");
        TextField endTime = new TextField();
        endTime.setPromptText("2026-01-01T12:00:00");
        TextField capacity = new TextField();
        capacity.setPromptText("20");

        grid.add(new Label("医生ID:"), 0, 0);
        grid.add(did, 1, 0);
        grid.add(new Label("医生姓名:"), 0, 1);
        grid.add(name, 1, 1);
        grid.add(new Label("科室:"), 0, 2);
        grid.add(department, 1, 2);
        grid.add(new Label("开始时间:"), 0, 3);
        grid.add(startTime, 1, 3);
        grid.add(new Label("结束时间:"), 0, 4);
        grid.add(endTime, 1, 4);
        grid.add(new Label("容量:"), 0, 5);
        grid.add(capacity, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                Map<String, Object> schedule = new HashMap<>();
                schedule.put("did", did.getText().trim());
                schedule.put("name", name.getText().trim());
                schedule.put("department", department.getText().trim());
                schedule.put("startTime", startTime.getText().trim());
                schedule.put("endTime", endTime.getText().trim());
                try {
                    schedule.put("capacity", Integer.parseInt(capacity.getText().trim()));
                } catch (NumberFormatException e) {
                    return null; // Validation fail
                }
                return schedule;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(schedule -> {
            if (schedule.get("capacity") == null) {
                showError("输入错误", "容量必须为数字");
                return;
            }
            
            Task<Response> task = new Task<>() {
                @Override
                protected Response call() throws Exception {
                    List<Map<String, Object>> schedules = new ArrayList<>();
                    schedules.add(schedule);
                    Map<String, Object> data = new HashMap<>();
                    data.put("schedules", schedules);
                    return NetworkClient.getInstance().sendRequest(new Request("admin_add_schedules", data));
                }
            };

            task.setOnSucceeded(e -> {
                Response resp = task.getValue();
                if (resp.isOk()) {
                    scheduleImportResult.setStyle("-fx-text-fill: #1b8a3f;");
                    scheduleImportResult.setText("添加成功");
                } else {
                    scheduleImportResult.setStyle("-fx-text-fill: #c0392b;");
                    scheduleImportResult.setText("添加失败: " + resp.getErrInfo());
                    showError("添加排班失败", resp.getErrInfo());
                }
            });
            
            task.setOnFailed(e -> {
                scheduleImportResult.setStyle("-fx-text-fill: #c0392b;");
                scheduleImportResult.setText("添加失败: " + e.getSource().getException().getMessage());
                showError("添加排班失败", e.getSource().getException().getMessage());
            });
            
            new Thread(task).start();
        });
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
                List<Map<String, Object>> schedules = parseScheduleFile(file);
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

    private List<Map<String, Object>> parseDoctorFile(File file) throws IOException {
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".csv") || name.endsWith(".txt")) {
            return parseDoctorCsv(file);
        }
        if (name.endsWith(".xls") || name.endsWith(".xlsx")) {
            return parseDoctorExcel(file);
        }
        throw new IOException("仅支持 csv/txt/xls/xlsx 文件");
    }

    private List<Map<String, Object>> parseScheduleFile(File file) throws IOException {
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".csv") || name.endsWith(".txt")) {
            return parseScheduleCsv(file);
        }
        if (name.endsWith(".xls") || name.endsWith(".xlsx")) {
            return parseScheduleExcel(file);
        }
        throw new IOException("仅支持 csv/txt/xls/xlsx 文件");
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

    private List<Map<String, Object>> parseDoctorExcel(File file) throws IOException {
        List<Map<String, Object>> doctors = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file); Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new IOException("文件为空");
            }

            var rows = sheet.rowIterator();
            Row first = null;
            int lineNo = 0;
            while (rows.hasNext()) {
                Row row = rows.next();
                lineNo = row.getRowNum() + 1;
                if (isRowEmpty(row)) {
                    continue;
                }
                first = row;
                break;
            }
            if (first == null) {
                throw new IOException("文件为空");
            }

            boolean hasHeader = looksLikeDoctorHeader(rowToHeaderString(first));
            if (!hasHeader) {
                doctors.add(parseDoctorRow(first, lineNo));
            }

            while (rows.hasNext()) {
                Row row = rows.next();
                lineNo = row.getRowNum() + 1;
                if (isRowEmpty(row)) {
                    continue;
                }
                doctors.add(parseDoctorRow(row, lineNo));
            }
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException("读取 Excel 失败: " + ex.getMessage(), ex);
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

    private List<Map<String, Object>> parseScheduleExcel(File file) throws IOException {
        List<Map<String, Object>> schedules = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file); Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new IOException("文件为空");
            }

            var rows = sheet.rowIterator();
            Row first = null;
            int lineNo = 0;
            while (rows.hasNext()) {
                Row row = rows.next();
                lineNo = row.getRowNum() + 1;
                if (isRowEmpty(row)) {
                    continue;
                }
                first = row;
                break;
            }
            if (first == null) {
                throw new IOException("文件为空");
            }

            boolean hasHeader = looksLikeScheduleHeader(rowToHeaderString(first));
            if (!hasHeader) {
                schedules.add(parseScheduleRow(first, lineNo));
            }

            while (rows.hasNext()) {
                Row row = rows.next();
                lineNo = row.getRowNum() + 1;
                if (isRowEmpty(row)) {
                    continue;
                }
                schedules.add(parseScheduleRow(row, lineNo));
            }
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException("读取 Excel 失败: " + ex.getMessage(), ex);
        }
        return schedules;
    }

    private Map<String, Object> parseDoctorLine(String line, int lineNo) throws IOException {
        String[] parts = line.split(",");
        if (parts.length < 4) {
            throw new IOException("医生账户第 " + lineNo + " 行字段不足");
        }
        String did = parts[0].trim();
        String name = parts[1].trim();
        String passwordPlain = parts[2].trim();
        String department = parts[3].trim();
        boolean admin = parts.length > 4 && Boolean.parseBoolean(parts[4].trim());
        String description = parts.length > 5 ? parts[5].trim() : "";

        Map<String, Object> doctor = new HashMap<>();
        doctor.put("did", did);
        doctor.put("name", name);
        doctor.put("passwordHex", sha256(passwordPlain));
        doctor.put("department", department);
        if (admin) {
            doctor.put("admin", true);
        }
        if (!description.isEmpty()) {
            doctor.put("description", description);
        }
        return doctor;
    }

    private Map<String, Object> parseDoctorRow(Row row, int lineNo) throws IOException {
        String did = getCell(row, 0);
        String name = getCell(row, 1);
        String passwordPlain = getCell(row, 2);
        String department = getCell(row, 3);
        String adminStr = getCell(row, 4);
        String description = getCell(row, 5);

        if (did.isEmpty() || name.isEmpty() || passwordPlain.isEmpty() || department.isEmpty()) {
            throw new IOException("医生账户第 " + lineNo + " 行字段不足");
        }

        Map<String, Object> doctor = new HashMap<>();
        doctor.put("did", did);
        doctor.put("name", name);
        doctor.put("passwordHex", sha256(passwordPlain));
        doctor.put("department", department);
        if (!adminStr.isEmpty() && Boolean.parseBoolean(adminStr)) {
            doctor.put("admin", true);
        }
        if (!description.isEmpty()) {
            doctor.put("description", description);
        }
        return doctor;
    }

    private Map<String, Object> parseScheduleLine(String line, int lineNo) throws IOException {
        String[] parts = line.split(",");
        if (parts.length < 6) {
            throw new IOException("排班第 " + lineNo + " 行字段不足（需 did, name, department, startTime, endTime, capacity）");
        }
        Map<String, Object> schedule = new HashMap<>();
        schedule.put("did", parts[0].trim());
        schedule.put("name", parts[1].trim());
        schedule.put("department", parts[2].trim());
        schedule.put("startTime", parts[3].trim());
        schedule.put("endTime", parts[4].trim());
        try {
            schedule.put("capacity", Integer.parseInt(parts[5].trim()));
        } catch (NumberFormatException ex) {
            throw new IOException("排班第 " + lineNo + " 行容量非数字");
        }
        return schedule;
    }

    private Map<String, Object> parseScheduleRow(Row row, int lineNo) throws IOException {
        String did = getCell(row, 0);
        String name = getCell(row, 1);
        String department = getCell(row, 2);
        String startTime = getCell(row, 3);
        String endTime = getCell(row, 4);
        String capacityStr = getCell(row, 5);

        if (did.isEmpty() || name.isEmpty() || department.isEmpty() || startTime.isEmpty() || endTime.isEmpty() || capacityStr.isEmpty()) {
            throw new IOException("排班第 " + lineNo + " 行字段不足（需 did, name, department, startTime, endTime, capacity）");
        }

        Map<String, Object> schedule = new HashMap<>();
        schedule.put("did", did);
        schedule.put("name", name);
        schedule.put("department", department);
        schedule.put("startTime", startTime);
        schedule.put("endTime", endTime);
        try {
            schedule.put("capacity", Integer.parseInt(capacityStr));
        } catch (NumberFormatException ex) {
            throw new IOException("排班第 " + lineNo + " 行容量非数字");
        }
        return schedule;
    }

    private boolean isRowEmpty(Row row) {
        if (row == null || row.getLastCellNum() <= 0) {
            return true;
        }
        int last = row.getLastCellNum();
        for (int i = 0; i < last; i++) {
            String value = getCell(row, i).trim();
            if (!value.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String rowToHeaderString(Row row) {
        StringBuilder sb = new StringBuilder();
        int last = row.getLastCellNum();
        for (int i = 0; i < last; i++) {
            String value = getCell(row, i).trim();
            if (value.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(value);
        }
        return sb.toString();
    }

    private String getCell(Row row, int index) {
        return dataFormatter.formatCellValue(row.getCell(index));
    }

    private boolean looksLikeDoctorHeader(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.contains("did") && lower.contains("name") && lower.contains("password");
    }

    private boolean looksLikeScheduleHeader(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.contains("did") && lower.contains("name") && lower.contains("department");
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
