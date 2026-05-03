package org.deptrai.auctionsystem.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneManager {
    // Thể hiện duy nhất của class (Singleton pattern)
    private static SceneManager instance;

    // Cửa sổ chính của ứng dụng
    private Stage primaryStage;

    // Private constructor để ngăn tạo instance mới từ bên ngoài
    private SceneManager() {}

    // Lấy instance duy nhất
    public static SceneManager getInstance() {
        if (instance == null) {
            instance = new SceneManager();
        }
        return instance;
    }

    // Thiết lập Stage chính (chỉ gọi 1 lần ở class Main/HelloApplication)
    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    // Hàm chuyển đổi màn hình
    public void switchScene(String fxmlPath, String title) {
        if (primaryStage == null) {
            System.err.println("Lỗi: Chưa khởi tạo Primary Stage cho SceneManager!");
            return;
        }

        try {
            // Lưu ý: Đường dẫn fxmlPath phải bắt đầu bằng "/" nếu tính từ thư mục resources
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            primaryStage.setTitle(title);
            primaryStage.setScene(scene);

            // Căn giữa cửa sổ sau khi đổi scene
            primaryStage.centerOnScreen();
            primaryStage.show();

        } catch (IOException e) {
            System.err.println("Không thể tải giao diện: " + fxmlPath);
            e.printStackTrace();
        }
    }
}