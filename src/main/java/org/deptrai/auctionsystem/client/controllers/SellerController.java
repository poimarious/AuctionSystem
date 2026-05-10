package org.deptrai.auctionsystem.client.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import org.deptrai.auctionsystem.client.utils.SceneManager;
import org.deptrai.auctionsystem.client.utils.SessionManager;

public class SellerController {

  @FXML
  public void handleGoBack(ActionEvent event) {
    SceneManager.getInstance().goBack();
  }

  @FXML
  public void handleLogout(ActionEvent event) {
    System.out.println("Đăng xuất từ Seller Center!");
    SessionManager.getInstance().logout();
    SceneManager.getInstance().clearHistory();
    SceneManager.getInstance()
        .switchScene(
            "/org/deptrai/auctionsystem/client/views/login-view.fxml", "Đăng nhập - Auction.UET");
  }

  @FXML
  public void handleAddNewProduct(ActionEvent event) {
    SceneManager.getInstance()
        .switchScene(
            "/org/deptrai/auctionsystem/client/views/add-product-view.fxml", "Đăng sản phẩm mới");
  }

  @FXML
  public void handleOpenInventory(ActionEvent event) {
    SceneManager.getInstance()
        .switchScene(
            "/org/deptrai/auctionsystem/client/views/inventory-view.fxml", "Kho hàng của tôi");
  }

  @FXML
  public void handleShowProfile(ActionEvent event) {
    SceneManager.getInstance()
        .switchScene("/org/deptrai/auctionsystem/client/views/profile-view.fxml", "Hồ sơ cá nhân");
  }

  @FXML
  public void initialize() {
    System.out.println("Đã load trang Seller Center!");
    // Sau này có thể load danh sách sản phẩm của Seller vào FlowPane tại đây
  }
}
