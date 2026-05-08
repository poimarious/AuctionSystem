package org.deptrai.auctionsystem.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.deptrai.auctionsystem.dao.UserDAO;
import org.deptrai.auctionsystem.models.users.Bidder;
import org.deptrai.auctionsystem.models.users.Seller;
import org.deptrai.auctionsystem.models.users.User;
import org.deptrai.auctionsystem.utils.SceneManager;

public class RegisterController {

    @FXML
    private TextField fullNameField; // Đã thêm trường này để khớp với FXML

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private ToggleGroup roleGroup;

    // Đã xóa roleComboBox và hàm initialize() đi vì giao diện mới dùng RadioButton

    @FXML
    public void handleRegisterAction(ActionEvent event) {
        String fullName = fullNameField.getText(); // Lấy thêm fullName
        String username = usernameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // 1. Kiểm tra không được để trống (Cập nhật check thêm fullName)
        if (fullName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng điền đầy đủ tất cả các trường!");
            return;
        }

        // 2. Kiểm tra mật khẩu xác nhận
        if (!password.equals(confirmPassword)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi mật khẩu", "Mật khẩu xác nhận không khớp. Vui lòng nhập lại!");
            return;
        }

        // 3. Khởi tạo đối tượng User phù hợp
        User newUser;
        String role = "BIDDER";

        // Cấp phát một ID duy nhất cho User mới
        String newUserId = "USER_" + System.currentTimeMillis();

        // Xử lý lấy Role từ ToggleGroup (RadioButton)
        RadioButton selectedRole = (RadioButton) roleGroup.getSelectedToggle();
        if (selectedRole != null && "Seller".equals(selectedRole.getUserData())) {
            newUser = new Seller(username, password, email);
            role = "SELLER";
        } else {
            newUser = new Bidder(username, password, email);
        }
        newUser.setUserId(newUserId);

        // 4. Lưu User vào Database bằng UserDAO
        UserDAO userDAO = new UserDAO();
        boolean isSuccess = userDAO.insertUser(newUser, role);

        if (isSuccess) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng ký tài khoản thành công! Vui lòng đăng nhập.");
            // Chuyển hướng về trang Đăng nhập
            SceneManager.getInstance().switchScene("/org.deptrai.auctionsystem.views/login-view.fxml", "Hệ thống Đấu giá - Đăng nhập");
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Không thể tạo tài khoản. Có thể tên đăng nhập đã tồn tại.");
        }
    }

    @FXML
    public void handleBackToLogin(ActionEvent event) {
        // Dùng cho nút ⬅ quay lại trang đăng nhập
        SceneManager.getInstance().switchScene("/org.deptrai.auctionsystem.views/login-view.fxml", "Hệ thống Đấu giá - Đăng nhập");
    }

    // Hàm tiện ích hiển thị thông báo
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}