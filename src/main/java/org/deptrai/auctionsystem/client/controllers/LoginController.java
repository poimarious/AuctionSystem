package org.deptrai.auctionsystem.client.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.deptrai.auctionsystem.client.utils.SceneManager;
import org.deptrai.auctionsystem.client.utils.SessionManager;
import org.deptrai.auctionsystem.client.utils.SocketClient;
import org.deptrai.auctionsystem.shared.models.users.Admin;
import org.deptrai.auctionsystem.shared.models.users.Bidder;
import org.deptrai.auctionsystem.shared.models.users.Seller;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.shared.network.Message;

public class LoginController {

  @FXML private TextField usernameField;

  @FXML private PasswordField passwordField;

  @FXML
  public void handleGoBack(ActionEvent event) {
    SceneManager.getInstance().navigateToHome();
  }

  @FXML
  public void handleLoginAction(ActionEvent event) {
    String inputUsername = usernameField.getText();
    String inputPassword = passwordField.getText();

    if (inputUsername.isEmpty() || inputPassword.isEmpty()) {
      showAlert(
          Alert.AlertType.WARNING,
          "Lỗi đăng nhập",
          "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!");
      return;
    }

    String[] credentials = {inputUsername, inputPassword};
    Message request = new Message("LOGIN", credentials);

    // Await response from Server after sending a request
    Message response = SocketClient.sendRequest(request);

    // KIỂM TRA MẬT KHẨU
    if (response.getStatus().equals("SUCCESS")) {
      User loggedInUser = (User) response.getData();

      // 1. DÒNG QUAN TRỌNG NHẤT: Lưu người dùng vào Session
      SessionManager.getInstance().setCurrentUser(loggedInUser);

      // 2. Xóa lịch sử
      SceneManager.getInstance().clearHistory();

      if (loggedInUser instanceof Admin) {
        showAlert(
            Alert.AlertType.INFORMATION,
            "Thành công",
            "Chào mừng Admin " + loggedInUser.getUsername());
        SceneManager.getInstance()
            .switchScene("/org/deptrai/auctionsystem/client/views/home-view.fxml", "Admin Panel");

      } else if (loggedInUser instanceof Bidder) {
        showAlert(
            Alert.AlertType.INFORMATION,
            "Thành công",
            "Chào mừng Bidder " + loggedInUser.getUsername());
        // 3. Đưa Bidder về đúng home-view
        SceneManager.getInstance()
            .switchScene(
                "/org/deptrai/auctionsystem/client/views/home-view.fxml",
                "Trang Chủ - Auction.UET");

      } else if (loggedInUser instanceof Seller) {
        showAlert(
            Alert.AlertType.INFORMATION,
            "Thành công",
            "Chào mừng Seller " + loggedInUser.getUsername());
        // Đã sửa: Cho Seller vào trang chủ ngắm đồ trước
        SceneManager.getInstance()
            .switchScene(
                "/org/deptrai/auctionsystem/client/views/home-view.fxml",
                "Trang Chủ - Auction.UET");
      }
    } else {
      // Báo lỗi nếu user không tồn tại hoặc sai mật khẩu
      String errorMsg = (String) response.getData();
      showAlert(Alert.AlertType.ERROR, "Thất bại", errorMsg);
    }
  }

  @FXML
  public void handleGoToRegister(ActionEvent event) {
    // Chuyển sang trang Register
    SceneManager.getInstance()
        .switchScene(
            "/org/deptrai/auctionsystem/client/views/register-view.fxml",
            "Hệ thống Đấu giá - Đăng ký");
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
