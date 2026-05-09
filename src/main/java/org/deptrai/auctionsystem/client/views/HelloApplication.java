package org.deptrai.auctionsystem.client.views;

import javafx.application.Application;
import javafx.stage.Stage;
import org.deptrai.auctionsystem.server.utils.DatabaseConnection;
import org.deptrai.auctionsystem.server.utils.SceneManager;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {


//        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("/org.deptrai.auctionsystem.views/login-view.fxml"));
//        Scene scene = new Scene(fxmlLoader.load(), 800, 600); // Kích thước cửa sổ
//        stage.setTitle("Hệ thống Đấu giá trực tuyến");
//        stage.setScene(scene);
//        stage.show();

        DatabaseConnection.initializeDatabase();

        SceneManager.getInstance().setPrimaryStage(stage);

        SceneManager.getInstance().switchScene("/org.deptrai.auctionsystem.views/home-view.fxml", "Hệ thống Đấu giá - Đăng nhập");
    }

    public static void main(String[] args) {
        launch();
    }
}
