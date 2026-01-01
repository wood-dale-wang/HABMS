package HABMS.client.controller;

import HABMS.client.App;
import HABMS.client.Session;
import HABMS.client.model.Doctor;
import HABMS.client.model.Request;
import HABMS.client.model.Response;
import HABMS.client.model.User;
import HABMS.client.net.NetworkClient;
import HABMS.client.util.JsonUtil;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public class LoginController {

    @FXML private TextField pidField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private RadioButton patientRadio;
    @FXML private RadioButton doctorRadio;
    @FXML private VBox patientLoginBox;
    @FXML private VBox doctorLoginBox;
    @FXML private TextField doctorNameField;
    @FXML private TextField doctorDeptField;
    
    @FXML private TextField regNameField;
    @FXML private TextField regPidField;
    @FXML private TextField regPhoneField;
    @FXML private PasswordField regPasswordField;
    @FXML private ComboBox<String> regSexCombo;
    @FXML private Label regErrorLabel;

    @FXML
    public void initialize() {
        if (regSexCombo != null) {
            regSexCombo.getItems().addAll("M", "F");
            regSexCombo.getSelectionModel().selectFirst();
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

    @FXML
    private void handleLogin(ActionEvent event) {
        String password = passwordField.getText();

        if (password.isEmpty()) {
            errorLabel.setText("请输入密码");
            return;
        }

        String passwordHex = hexSha256(password);
        Map<String, String> data = new HashMap<>();
        data.put("passwordHex", passwordHex);

        String type;
        boolean doctorMode = doctorRadio != null && doctorRadio.isSelected();
        if (doctorMode) {
            String doctorName = doctorNameField != null ? doctorNameField.getText().trim() : "";
            String department = doctorDeptField != null ? doctorDeptField.getText().trim() : "";
            if (doctorName.isEmpty() || department.isEmpty()) {
                errorLabel.setText("请输入医生姓名和科室");
                return;
            }
            type = "doctor_login";
            data.put("name", doctorName);
            data.put("department", department);
        } else {
            String inputId = pidField.getText().trim();
            if (inputId.isEmpty()) {
                errorLabel.setText("请输入身份证或手机号");
                return;
            }
            if (inputId.length() == 11) {
                type = "account_login";
                data.put("phone", inputId);
            } else {
                type = "account_login";
                data.put("pid", inputId);
            }
        }

        Request req = new Request(type, data);

        // Execute network request in background
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
                    if ("doctor_login".equals(type)) {
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
            errorLabel.setText("网络错误: " + e.getSource().getException().getMessage());
            e.getSource().getException().printStackTrace();
        });

        new Thread(task).start();
    }
    
    @FXML
    private void handleRegister(ActionEvent event) {
        Map<String, String> data = new HashMap<>();
        data.put("name", regNameField.getText());
        data.put("pid", regPidField.getText());
        data.put("phone", regPhoneField.getText());
        data.put("passwordHex", hexSha256(regPasswordField.getText()));
        data.put("sex", regSexCombo.getValue());
        
        Request req = new Request("account_register", data);
        
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws Exception {
                return NetworkClient.getInstance().sendRequest(req);
            }
        };
        
        task.setOnSucceeded(e -> {
            Response resp = task.getValue();
            if (resp.isOk()) {
                regErrorLabel.setStyle("-fx-text-fill: green;");
                regErrorLabel.setText("注册成功！请切换到登录页登录。");
            } else {
                regErrorLabel.setStyle("-fx-text-fill: red;");
                regErrorLabel.setText("注册失败: " + resp.getErrInfo());
            }
        });
        
        task.setOnFailed(e -> {
            regErrorLabel.setText("网络错误");
        });
        
        new Thread(task).start();
    }
}
