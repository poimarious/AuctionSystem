package org.deptrai.auctionsystem.views;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class LoginController {

    @FXML
    private void handleLogin(ActionEvent event) {
        try {

            Parent root = FXMLLoader.load(getClass().getResource("/main-view.fxml"));

            // 2. Lấy thông tin "Sân khấu" (Stage) hiện tại từ cái nút vừa bấm
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // . Tạo "Phân cảnh" (Scene) mới với giao diện đã tải
            Scene scene = new Scene(root);

            // 4. Đặt cảnh mới lên sân khấu và hiển thị
            stage.setScene(scene);
            stage.centerOnScreen(); // Căn giữa màn hình cho đẹp
            stage.show();

            System.out.println("Đăng nhập thành công! Đang chuyển trang...");

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Lỗi: Không tìm thấy file giao diện màn hình chính!");
        }
    }
}