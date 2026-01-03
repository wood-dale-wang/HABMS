package HABMS.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX 启动类：创建主舞台并加载首个登录界面。
 */
public class App extends Application {

    private static Scene scene;
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        scene = new Scene(loadFXML("view/login"), 800, 600);
        scene.getStylesheets().add(App.class.getResource("app.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("飞马星球医院预约挂号系统");
        stage.show();
    }

    /** 切换到指定 FXML 视图，不修改窗口标题。 */
    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    /** 切换到指定 FXML 视图，同时更新窗口标题。 */
    public static void setRoot(String fxml, String title) throws IOException {
        primaryStage.setTitle(title);
        scene.setRoot(loadFXML(fxml));
    }

    /** 按名称加载位于 resources/HABMS/client 下的 FXML。 */
    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}
