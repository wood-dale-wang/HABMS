package HABMS.client.controller;

import HABMS.client.App;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import java.io.IOException;

public class AdminMainController {

    @FXML
    private void handleLogout(ActionEvent event) throws IOException {
        // TODO: Send logout request to server
        App.setRoot("view/login", "飞马星球医院预约挂号系统");
    }
}
