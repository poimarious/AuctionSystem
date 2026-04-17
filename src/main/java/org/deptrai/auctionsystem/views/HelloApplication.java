package org.deptrai.auctionsystem.views;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Lệnh này sẽ mở màn hình Login đầu tiên
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("/login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600); // Kích thước cửa sổ
        stage.setTitle("Hệ thống Đấu giá trực tuyến");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}