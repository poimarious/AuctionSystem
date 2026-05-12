package org.deptrai.auctionsystem.client.controllers;

import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextInputDialog;
import org.deptrai.auctionsystem.client.utils.SceneManager;
import org.deptrai.auctionsystem.client.utils.SessionManager;
import org.deptrai.auctionsystem.client.utils.SocketClient;
import org.deptrai.auctionsystem.shared.models.users.Admin;
import org.deptrai.auctionsystem.shared.models.users.Seller;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.shared.network.Message;

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
      profileEmailLabel.setText(
          currentUser.getEmail() != null ? currentUser.getEmail() : "Chưa cập nhật");

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

    result.ifPresent(
        amountStr -> {
          try {
            double amount = Double.parseDouble(amountStr);

            if (amount <= 0) {
              showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Số tiền nạp phải lớn hơn 0!");
              return;
            }

            Object[] topUpData = {currentUser.getUserId(), amount};
            Message request = new Message("TOP_UP", topUpData);

            // Awaiting response from Server after sending a request
            Message response = SocketClient.sendRequest(request);

            if (response.getStatus().equals("SUCCESS")) {
              double newBalance = (Double) response.getData(); // Server trả về số dư mới

              // Cập nhật lại Session trên RAM của Client
              currentUser.setBalance(newBalance);
              SessionManager.getInstance()
                  .notifyBalanceChanged(); // Báo cho các giao diện tự update số
              balanceLabel.setText(String.format("$%.2f", newBalance));

              showAlert(
                  Alert.AlertType.INFORMATION,
                  "Thành công",
                  "Đã nạp thành công $" + amount + " vào ví!");
            } else {
              showAlert(Alert.AlertType.ERROR, "Lỗi Server", (String) response.getData());
            }
          } catch (NumberFormatException e) {
            showAlert(
                Alert.AlertType.ERROR,
                "Lỗi nhập liệu",
                "Vui lòng chỉ nhập số (Ví dụ: 100 hoặc 50.5)");
          }
        });
  }

  @FXML
  public void handleUpdatePassword(ActionEvent event) {
    String currentPass = currentPassField.getText();
    String newPass = newPassField.getText();
    String confirmPass = confirmNewPassField.getText();

    if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
      showAlert(
          Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập đầy đủ các trường mật khẩu!");
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
    // SỬA: Chuyển thẳng về trang chủ thay vì goBack() để tránh lỗi lịch sử trống
    SceneManager.getInstance()
        .switchScene(
            "/org/deptrai/auctionsystem/client/views/home-view.fxml", "Trang chủ - Auction.UET");}
  @FXML
  public void handleLogout(ActionEvent event) {
    SessionManager.getInstance().logout();
    SceneManager.getInstance().clearHistory();
    SceneManager.getInstance()
        .switchScene(
            "/org/deptrai/auctionsystem/client/views/home-view.fxml", "Trang chủ - Auction.UET");
  }

  private void showAlert(Alert.AlertType alertType, String title, String message) {
    Alert alert = new Alert(alertType);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }
}
