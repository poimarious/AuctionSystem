package org.deptrai.auctionsystem.client.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextInputDialog;
import org.deptrai.auctionsystem.shared.models.users.Admin;
import org.deptrai.auctionsystem.shared.models.users.Seller;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.server.utils.SceneManager;
import org.deptrai.auctionsystem.server.utils.SessionManager;

import java.util.Optional;

public class ProfileController {

    @FXML private Label profileNameLabel;
    @FXML private Label profileRoleLabel;
    @FXML private Label profileEmailLabel;
    @FXML private Label balanceLabel;

    @FXML private PasswordField currentPassField;
    @FXML private PasswordField newPassField;
    @FXML private PasswordField confirmNewPassField;

    @FXML
    public void initialize() {
        loadUserData();
    }

    private void loadUserData() {
        User currentUser = SessionManager.getInstance().getCurrentUser();

        if (currentUser != null) {
            profileNameLabel.setText(currentUser.getUsername());
            profileEmailLabel.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "Chưa cập nhật");

            if (currentUser instanceof Admin) {
                profileRoleLabel.setText("Quản trị viên hệ thống (Admin)");
            } else if (currentUser instanceof Seller) {
                profileRoleLabel.setText("Người bán hàng (Seller)");
            } else {
                profileRoleLabel.setText("Người mua hàng (Bidder)");
            }

            balanceLabel.setText(String.format("$%.2f", currentUser.getBalance()));
        } else {
            profileNameLabel.setText("Khách");
            profileRoleLabel.setText("Chưa đăng nhập");
            balanceLabel.setText("$0.00");
        }
    }

    @FXML
    public void handleTopUpWallet(ActionEvent event) {
        User currentUser = SessionManager.getInstance().getCurrentUser();

        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi xác thực", "Bạn cần đăng nhập để nạp tiền!");
            return;
        }

        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("Nạp tiền vào ví");
        dialog.setHeaderText("Số dư hiện tại: " + String.format("$%.2f", currentUser.getBalance()));
        dialog.setContentText("Vui lòng nhập số tiền muốn nạp ($):");

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(amountStr -> {
            try {
                double amount = Double.parseDouble(amountStr);

                if (amount <= 0) {
                    showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Số tiền nạp phải lớn hơn 0!");
                    return;
                }

                // Cộng tiền vào user
                currentUser.addBalance(amount);

                // GỬI THÔNG BÁO CHO TRANG CHỦ CẬP NHẬT LẠI TIỀN
                SessionManager.getInstance().notifyBalanceChanged();

                // Cập nhật nhãn tại trang Profile
                balanceLabel.setText(String.format("$%.2f", currentUser.getBalance()));

                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã nạp thành công $" + amount + " vào ví!");

            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Vui lòng chỉ nhập số (Ví dụ: 100 hoặc 50.5)");
            }
        });
    }

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

        if (!currentUser.getPassword().equals(currentPass)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Mật khẩu hiện tại không chính xác!");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Xác nhận mật khẩu mới không khớp!");
            return;
        }

        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Mật khẩu của bạn đã được cập nhật!");

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