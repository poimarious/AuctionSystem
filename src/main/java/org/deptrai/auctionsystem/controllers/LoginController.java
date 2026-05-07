package org.deptrai.auctionsystem.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.deptrai.auctionsystem.dao.UserDAO;
import org.deptrai.auctionsystem.models.users.Admin;
import org.deptrai.auctionsystem.models.users.Bidder;
import org.deptrai.auctionsystem.models.users.Seller;
import org.deptrai.auctionsystem.models.users.User;
import org.deptrai.auctionsystem.utils.SceneManager;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    public void handleLoginAction(ActionEvent event) {
        String inputUsername = usernameField.getText();
        String inputPassword = passwordField.getText();

        if (inputUsername.isEmpty() || inputPassword.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi đăng nhập", "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!");
            return;
        }

        // TÌM USER TỪ DATABASE THẬT THÔNG QUA UserDAO
        UserDAO userDAO = new UserDAO();
        User loggedInUser = userDAO.getUserByUsername(inputUsername);

        // KIỂM TRA MẬT KHẨU
        if (loggedInUser != null && loggedInUser.getPassword().equals(inputPassword)) {
            if (loggedInUser instanceof Admin) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Chào mừng Admin " + loggedInUser.getUsername());
                SceneManager.getInstance().switchScene("/org.deptrai.auctionsystem.views/admin.fxml", "Admin Panel");

            } else if (loggedInUser instanceof Bidder) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Chào mừng Bidder " + loggedInUser.getUsername());
                SceneManager.getInstance().switchScene("/org.deptrai.auctionsystem.views/main-view.fxml", "Sàn Đấu Giá");

            } else if (loggedInUser instanceof Seller) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Chào mừng Seller " + loggedInUser.getUsername());
                SceneManager.getInstance().switchScene("/org.deptrai.auctionsystem.views/inventory-view.fxml", "Quản lý Kho Hàng");
            }
        } else {
            // Báo lỗi nếu user không tồn tại hoặc sai mật khẩu
            showAlert(Alert.AlertType.ERROR, "Thất bại", "Sai tên đăng nhập hoặc mật khẩu.");
        }
    }

    @FXML
    public void handleGoToRegister(ActionEvent event) {
        // Chuyển sang trang Register
        SceneManager.getInstance().switchScene("/org.deptrai.auctionsystem.views/register-view.fxml", "Hệ thống Đấu giá - Đăng ký");
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