package org.deptrai.auctionsystem.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.deptrai.auctionsystem.models.users.Admin;
import org.deptrai.auctionsystem.models.users.Bidder;
import org.deptrai.auctionsystem.models.users.Seller;
import org.deptrai.auctionsystem.models.users.User;
import org.deptrai.auctionsystem.utils.SceneManager;

import java.util.ArrayList;
import java.util.List;

public class LoginController {

    // Khớp với fx:id trong file login-view.fxml
    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    // Giả lập Database chứa thông tin User
    private List<User> userDatabase;

    @FXML
    public void initialize() {
        // Khởi tạo dữ liệu mẫu khi màn hình vừa được load
        userDatabase = new ArrayList<>();

        // Test account
        userDatabase.add(new Bidder("Poi", "1", "poimarious@gmail.com"));
    }

    @FXML
    public void handleLoginAction(ActionEvent event) {
        String inputUsername = usernameField.getText();
        String inputPassword = passwordField.getText();

        if (inputUsername.isEmpty() || inputPassword.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi đăng nhập", "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!");
            return;
        }

        // Tìm user trong danh sách giả lập
        User loggedInUser = authenticate(inputUsername, inputPassword);

        if (loggedInUser != null) {
            if (loggedInUser instanceof Admin) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Chào mừng Admin " + loggedInUser.getUsername());
                // Chuyển sang trang Admin
                SceneManager.getInstance().switchScene("/org.deptrai.auctionsystem.views/admin.fxml", "Admin Panel");

            } else if (loggedInUser instanceof Bidder) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Chào mừng Bidder " + loggedInUser.getUsername());
                // Chuyển sang trang chủ người dùng
                SceneManager.getInstance().switchScene("/org.deptrai.auctionsystem.views/main-view.fxml", "Sàn Đấu Giá");

            } else if (loggedInUser instanceof Seller) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Chào mừng Seller " + loggedInUser.getUsername());
                // Chuyển sang trang kho hàng
                SceneManager.getInstance().switchScene("/org.deptrai.auctionsystem.views/inventory-view.fxml", "Quản lý Kho Hàng");
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Thất bại", "Sai tên đăng nhập hoặc mật khẩu.");
        }
    }

    // Hàm giả lập logic kiểm tra thông tin đăng nhập
    private User authenticate(String username, String password) {
        for (User user : userDatabase) {
            if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
                return user;
            }
        }
        return null;
    }

    // Tiện ích hiển thị thông báo popup
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}