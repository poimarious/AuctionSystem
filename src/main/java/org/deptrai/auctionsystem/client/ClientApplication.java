package org.deptrai.auctionsystem.client;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.deptrai.auctionsystem.client.utils.AutoBidManager;
import org.deptrai.auctionsystem.client.utils.SceneManager;
import org.deptrai.auctionsystem.client.utils.SocketClient;

public class ClientApplication extends Application {

  @Override
  public void start(Stage stage) throws IOException {
    System.out.println("=== [CLIENT] ĐANG KHỞI CHẠY GIAO DIỆN ===");

    // 1. Kết nối tới Server Socket trước khi hiện màn hình
    SocketClient.connect("26.74.145.232", 5000);
    AutoBidManager.getInstance();

    // 2. Nạp file FXML từ thư mục resources mới
    FXMLLoader fxmlLoader =
        new FXMLLoader(
            getClass().getResource("/org/deptrai/auctionsystem/client/views/home-view.fxml"));

    Scene scene = new Scene(fxmlLoader.load());

    // 3. Khởi tạo SceneManager để quản lý việc chuyển cảnh sau này
    SceneManager.getInstance().setPrimaryStage(stage);

    try {
      Image appIcon = new Image(getClass().getResourceAsStream("/org/deptrai/auctionsystem/client/views/images/logo.png"));
      stage.getIcons().add(appIcon);
    } catch (Exception e) {
      System.err.println("Không thể load logo ứng dụng: " + e.getMessage());
    }


    stage.setTitle("Hệ thống Đấu giá trực tuyến");
    stage.setScene(scene);
    stage.setWidth(1500);
    stage.setHeight(800);
    stage.show();
  }

  @Override
  public void stop() {
    // Ngắt kết nối mạng an toàn khi đóng cửa sổ App
    System.out.println(">> Đang đóng kết nối Client...");
    SocketClient.disconnect();
  }
}
