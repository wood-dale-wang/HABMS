package HABMS.client.controller;

import HABMS.client.App;
import HABMS.client.Session;
import HABMS.client.model.Doctor;
import HABMS.client.model.Response;
import HABMS.client.model.User;
import HABMS.client.service.AuthService;
import HABMS.client.service.LookupService;
import HABMS.client.util.JsonUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/**
 * 登录/注册页控制器：处理患者与医生登录、注册，以及科室列表拉取。
 */
public class LoginController {

    private final AuthService authService = new AuthService();
    private final LookupService lookupService = new LookupService();

    @FXML private TextField pidField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private ProgressIndicator loginProgress;
    @FXML private RadioButton patientRadio;
    @FXML private RadioButton doctorRadio;
    @FXML private VBox patientLoginBox;
    @FXML private VBox doctorLoginBox;
    @FXML private TextField doctorNameField;
    @FXML private ComboBox<String> doctorDeptCombo;
    
    @FXML private TextField regNameField;
    @FXML private TextField regPidField;
    @FXML private TextField regPhoneField;
    @FXML private TextField regAgeField;
    @FXML private PasswordField regPasswordField;
    @FXML private ComboBox<String> regSexCombo;
    @FXML private Label regErrorLabel;
    @FXML private Button registerButton;
    @FXML private ProgressIndicator registerProgress;

    @FXML
    public void initialize() {
        if (regSexCombo != null) {
            regSexCombo.getItems().addAll("M", "F");
            regSexCombo.getSelectionModel().selectFirst();
        }

        if (doctorDeptCombo != null) {
            doctorDeptCombo.setItems(FXCollections.observableArrayList(Arrays.asList(
                    "内科", "外科", "儿科", "妇产科", "口腔科", "眼科", "耳鼻喉科", "皮肤科", "急诊科", "体检中心", "管理"
            )));
            if (doctorDeptCombo.getValue() == null && !doctorDeptCombo.getItems().isEmpty()) {
                doctorDeptCombo.getSelectionModel().selectFirst();
            }
            refreshDepartmentsSilently();
        }

        if (patientRadio != null && doctorRadio != null) {
            ToggleGroup loginGroup = new ToggleGroup();
            patientRadio.setToggleGroup(loginGroup);
            doctorRadio.setToggleGroup(loginGroup);
            patientRadio.setSelected(true);
            loginGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> updateLoginMode());
        }
        updateLoginMode();
    }

    private String getDoctorDepartment() {
        if (doctorDeptCombo == null) {
            return "";
        }

        String editorText = null;
        if (doctorDeptCombo.isEditable() && doctorDeptCombo.getEditor() != null) {
            editorText = doctorDeptCombo.getEditor().getText();
        }
        if (editorText != null && !editorText.trim().isEmpty()) {
            return editorText.trim();
        }

        String value = doctorDeptCombo.getValue();
        return value == null ? "" : value.trim();
    }

    /** 拉取科室列表用于医生登录页，失败时静默忽略。 */
    private void refreshDepartmentsSilently() {
        if (doctorDeptCombo == null) {
            return;
        }

        String current = getDoctorDepartment();

        Task<Response> task = lookupService.departmentList();
        task.setOnSucceeded(e -> {
            Response resp = task.getValue();
            if (!resp.isOk()) {
                return;
            }
            List<String> departments = JsonUtil.getMapper().convertValue(
                    resp.getData(),
                    JsonUtil.getMapper().getTypeFactory().constructCollectionType(List.class, String.class));

            List<String> merged = new ArrayList<>(departments);
            if (!merged.contains("管理")) {
                merged.add("管理");
            }

            Platform.runLater(() -> {
                doctorDeptCombo.getItems().setAll(merged);
                if (current != null && !current.isBlank()) {
                    if (doctorDeptCombo.getItems().contains(current)) {
                        doctorDeptCombo.getSelectionModel().select(current);
                    } else if (doctorDeptCombo.isEditable() && doctorDeptCombo.getEditor() != null) {
                        doctorDeptCombo.getEditor().setText(current);
                    }
                } else if (!doctorDeptCombo.getItems().isEmpty()) {
                    doctorDeptCombo.getSelectionModel().selectFirst();
                }
            });
        });
        task.setOnFailed(e -> {
            // ignore: server may reject department_list before login
        });

        lookupService.run(task);
    }

    /** 对密码做 SHA-256 单向散列以避免明文传输。 */
    private String hexSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** 根据选中角色切换医生/患者登录框显示。 */
    private void updateLoginMode() {
        boolean doctorMode = doctorRadio != null && doctorRadio.isSelected();
        if (patientLoginBox != null) {
            patientLoginBox.setVisible(!doctorMode);
            patientLoginBox.setManaged(!doctorMode);
        }
        if (doctorLoginBox != null) {
            doctorLoginBox.setVisible(doctorMode);
            doctorLoginBox.setManaged(doctorMode);
        }
        if (errorLabel != null) {
            errorLabel.setText("");
        }
    }

    /** 切换登录按钮与输入框的加载状态。 */
    private void setLoginLoading(boolean loading) {
        if (loginButton != null) {
            loginButton.setDisable(loading);
            loginButton.setOpacity(loading ? 0.75 : 1.0);
        }
        if (patientRadio != null) {
            patientRadio.setDisable(loading);
        }
        if (doctorRadio != null) {
            doctorRadio.setDisable(loading);
        }
        if (pidField != null) {
            pidField.setDisable(loading);
        }
        if (doctorNameField != null) {
            doctorNameField.setDisable(loading);
        }
        if (doctorDeptCombo != null) {
            doctorDeptCombo.setDisable(loading);
        }
        if (passwordField != null) {
            passwordField.setDisable(loading);
        }
        if (loginProgress != null) {
            loginProgress.setVisible(loading);
            loginProgress.setManaged(loading);
        }
    }

    /** 切换注册按钮与输入框的加载状态。 */
    private void setRegisterLoading(boolean loading) {
        if (registerButton != null) {
            registerButton.setDisable(loading);
            registerButton.setOpacity(loading ? 0.75 : 1.0);
        }
        if (regNameField != null) {
            regNameField.setDisable(loading);
        }
        if (regPidField != null) {
            regPidField.setDisable(loading);
        }
        if (regPhoneField != null) {
            regPhoneField.setDisable(loading);
        }
        if (regAgeField != null) {
            regAgeField.setDisable(loading);
        }
        if (regPasswordField != null) {
            regPasswordField.setDisable(loading);
        }
        if (regSexCombo != null) {
            regSexCombo.setDisable(loading);
        }
        if (registerProgress != null) {
            registerProgress.setVisible(loading);
            registerProgress.setManaged(loading);
        }
    }

    /** 在注册区域显示成功/失败状态文案。 */
    private void setRegisterStatus(boolean ok, String text) {
        if (regErrorLabel == null) {
            return;
        }
        regErrorLabel.setText(text);
        regErrorLabel.getStyleClass().remove("error-label");
        regErrorLabel.getStyleClass().remove("success-label");
        regErrorLabel.getStyleClass().add(ok ? "success-label" : "error-label");
    }

    /** 登录患者或医生，成功后跳转对应主界面。 */
    @FXML
    private void handleLogin(ActionEvent event) {
        setLoginLoading(false);
        if (errorLabel != null) {
            errorLabel.setText("");
        }
        String password = passwordField.getText();

        if (password.isEmpty()) {
            errorLabel.setText("请输入密码");
            return;
        }

        String passwordHex = hexSha256(password);

        boolean doctorMode = doctorRadio != null && doctorRadio.isSelected();
        final boolean isDoctorLogin = doctorMode;

        Task<Response> task;
        if (doctorMode) {
            String doctorName = doctorNameField != null ? doctorNameField.getText().trim() : "";
            String department = getDoctorDepartment();
            if (doctorName.isEmpty() || department.isEmpty()) {
                errorLabel.setText("请输入医生姓名和科室");
                return;
            }
            task = authService.loginDoctor(doctorName, department, passwordHex);
        } else {
            String inputId = pidField != null ? pidField.getText().trim() : "";
            if (inputId.isEmpty()) {
                errorLabel.setText("请输入身份证或手机号");
                return;
            }
            task = authService.loginAccount(inputId, passwordHex);
        }

        setLoginLoading(true);

        task.setOnSucceeded(e -> {
            setLoginLoading(false);
            Response resp = task.getValue();
            if (resp.isOk()) {
                try {
                    if (isDoctorLogin) {
                        // Doctor/Admin Login
                        Doctor doctor = JsonUtil.getMapper().convertValue(resp.getData(), Doctor.class);
                        System.out.println("Doctor login success: " + doctor.getName());
                        Session.setCurrentDoctor(doctor);
                            if (doctor.isAdmin()) {
                                App.setRoot("view/admin_main", "飞马医院 - 管理端");
                            } else {
                                App.setRoot("view/doctor_main", "飞马医院 - 医生端");
                            }
                    } else {
                        // Patient Login
                        User user = JsonUtil.getMapper().convertValue(resp.getData(), User.class);
                        System.out.println("Login success: " + user.getName());
                        Session.setCurrentUser(user);
                        App.setRoot("view/patient_main", "飞马医院 - 患者端");
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } else {
                errorLabel.setText("登录失败: " + resp.getErrInfo());
            }
        });

        task.setOnFailed(e -> {
            setLoginLoading(false);
            errorLabel.setText("网络错误: " + e.getSource().getException().getMessage());
            e.getSource().getException().printStackTrace();
        });

        authService.run(task);
    }
    
    /** 提交患者注册信息并提示结果。 */
    @FXML
    private void handleRegister(ActionEvent event) {
        setRegisterLoading(false);
        if (regErrorLabel != null) {
            regErrorLabel.setText("");
            regErrorLabel.getStyleClass().remove("error-label");
            regErrorLabel.getStyleClass().remove("success-label");
        }

        String name = regNameField != null ? regNameField.getText() : "";
        String pid = regPidField != null ? regPidField.getText() : "";
        String phone = regPhoneField != null ? regPhoneField.getText() : "";
        String ageText = regAgeField != null ? regAgeField.getText() : "";
        String passwordHex = hexSha256(regPasswordField != null ? regPasswordField.getText() : "");
        String sex = regSexCombo != null ? regSexCombo.getValue() : null;

        if (ageText == null || ageText.trim().isEmpty()) {
            setRegisterStatus(false, "请输入年龄");
            return;
        }
        int age;
        try {
            age = Integer.parseInt(ageText.trim());
        } catch (NumberFormatException ex) {
            setRegisterStatus(false, "请输入有效的数字年龄");
            return;
        }
        if (age <= 10) {
            setRegisterStatus(false, "年龄需大于10岁");
            return;
        }

        setRegisterLoading(true);

        Task<Response> task = authService.registerAccount(name, pid, phone, passwordHex, sex);

        task.setOnSucceeded(e -> {
            setRegisterLoading(false);
            Response resp = task.getValue();
            if (resp.isOk()) {
                setRegisterStatus(true, "注册成功！请切换到登录页登录。");
            } else {
                setRegisterStatus(false, "注册失败: " + resp.getErrInfo());
            }
        });

        task.setOnFailed(e -> {
            setRegisterLoading(false);
            setRegisterStatus(false, "网络错误");
        });

        authService.run(task);
    }
}
