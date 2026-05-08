package org.deptrai.auctionsystem.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import org.deptrai.auctionsystem.utils.SceneManager;
import org.deptrai.auctionsystem.utils.SessionManager;

public class AdminController {

  @FXML
  public void handleLogout(ActionEvent event) {
    SessionManager.getInstance().logout();
    SceneManager.getInstance().clearHistory();
    SceneManager.getInstance().switchScene("/org.deptrai.auctionsystem.views/login-view.fxml", "Đăng nhập - Auction.UET");
  }

  @FXML
  public void handleGoBack(ActionEvent event) {
    SceneManager.getInstance().goBack();
  }

  // Các hàm xử lý Quản lý người dùng, Duyệt đấu giá... sẽ được viết tiếp tại đây
}