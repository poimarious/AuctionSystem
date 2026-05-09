package org.deptrai.auctionsystem.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.deptrai.auctionsystem.client.utils.SocketClient;
import org.deptrai.auctionsystem.client.utils.SceneManager;

import java.io.IOException;

public class ClientApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        System.out.println("=== [CLIENT] ĐANG KHỞI CHẠY GIAO DIỆN ===");

        // 1. Kết nối tới Server Socket trước khi hiện màn hình
        SocketClient.connect("localhost", 5000);

        // 2. Nạp file FXML từ thư mục resources mới
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/org/deptrai/auctionsystem/client/views/home-view.fxml"));

        Scene scene = new Scene(fxmlLoader.load());

        // 3. Khởi tạo SceneManager để quản lý việc chuyển cảnh sau này
        SceneManager.getInstance().setPrimaryStage(stage);

        stage.setTitle("Hệ thống Đấu giá trực tuyến - UET");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        // Ngắt kết nối mạng an toàn khi đóng cửa sổ App
        System.out.println(">> Đang đóng kết nối Client...");
        SocketClient.disconnect();
    }
}