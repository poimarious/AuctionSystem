package org.deptrai.auctionsystem.views;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.deptrai.auctionsystem.utils.DatabaseConnection;
import org.deptrai.auctionsystem.utils.SceneManager;

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

        SceneManager.getInstance().switchScene("/org.deptrai.auctionsystem.views/login-view.fxml", "Hệ thống Đấu giá - Đăng nhập");
    }

    public static void main(String[] args) {
        launch();
    }
}
