package org.deptrai.auctionsystem.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import org.deptrai.auctionsystem.models.users.Admin;
import org.deptrai.auctionsystem.models.users.Seller;
import org.deptrai.auctionsystem.models.users.User;
import org.deptrai.auctionsystem.utils.SceneManager;
import org.deptrai.auctionsystem.utils.SessionManager;

public class ProfileController {

    // --- CÁC THÀNH PHẦN GIAO DIỆN ---
    @FXML
    private Label profileNameLabel;

    @FXML
    private Label profileRoleLabel;

    @FXML
    private Label profileEmailLabel;

    @FXML
    private Label balanceLabel;

    @FXML
    private PasswordField currentPassField;

    @FXML
    private PasswordField newPassField;

    @FXML
    private PasswordField confirmNewPassField;

    @FXML
    public void initialize() {
        // Nạp thông tin người dùng đang đăng nhập
        loadUserData();
    }

    private void loadUserData() {
        User currentUser = SessionManager.getInstance().getCurrentUser();

        if (currentUser != null) {
            profileNameLabel.setText(currentUser.getUsername());
            profileEmailLabel.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "Chưa cập nhật");

            // Xác định chức vụ
            if (currentUser instanceof Admin) {
                profileRoleLabel.setText("Quản trị viên hệ thống (Admin)");
            } else if (currentUser instanceof Seller) {
                profileRoleLabel.setText("Người bán hàng (Seller)");
            } else {
                profileRoleLabel.setText("Người mua hàng (Bidder)");
            }

            // Tạm thời gán số dư ví là 0 (Sau này bạn có thể cập nhật lấy từ Database)
            balanceLabel.setText("$0.00");
        } else {
            profileNameLabel.setText("Khách");
            profileRoleLabel.setText("Chưa đăng nhập");
        }
    }

    // --- CÁC HÀM XỬ LÝ SỰ KIỆN ---

    @FXML
    public void handleUpdatePassword(ActionEvent event) {
        String currentPass = currentPassField.getText();
        String newPass = newPassField.getText();
        String confirmPass = confirmNewPassField.getText();

        if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập đầy đủ các trường mật khẩu!");
            return;
        }

        User currentUser = SessionManager.getInstance().getCurrentUser();

        // Kiểm tra mật khẩu cũ có đúng không
        if (!currentUser.getPassword().equals(currentPass)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Mật khẩu hiện tại không chính xác!");
            return;
        }

        // Kiểm tra mật khẩu mới có khớp không
        if (!newPass.equals(confirmPass)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Xác nhận mật khẩu mới không khớp!");
            return;
        }

        // CHÚ Ý: Ở đây bạn sẽ gọi UserDAO để lưu mật khẩu mới vào Database

        // Tạm thời hiển thị thông báo thành công
        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Mật khẩu của bạn đã được cập nhật!");

        // Xóa trắng các ô nhập liệu
        currentPassField.clear();
        newPassField.clear();
        confirmNewPassField.clear();
    }

    @FXML
    public void handleGoBack(ActionEvent event) {
        SceneManager.getInstance().goBack();
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        System.out.println("Đăng xuất từ trang Profile!");
        SessionManager.getInstance().logout();
        SceneManager.getInstance().clearHistory();
        SceneManager.getInstance().switchScene("/org.deptrai.auctionsystem.views/login-view.fxml", "Đăng nhập - Auction.UET");
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}