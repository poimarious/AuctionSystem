package org.deptrai.auctionsystem.client.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import org.deptrai.auctionsystem.client.utils.SceneManager;
import org.deptrai.auctionsystem.client.utils.SessionManager;
import org.deptrai.auctionsystem.client.utils.SocketClient;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.shared.network.Message;

import java.io.IOException;
import java.util.List;

public class SellerController {

  @FXML private FlowPane productsContainer;
  @FXML private TextField searchField;
  @FXML private Label welcomeLabel;

  @FXML
  public void initialize() {
    System.out.println("--- Seller Center Initialized ---");

    // 1. Hiển thị thông tin cá nhân
    User currentUser = SessionManager.getInstance().getCurrentUser();
    if (currentUser != null && welcomeLabel != null) {
      welcomeLabel.setText("Chào mừng, " + currentUser.getUsername() + "!");
    }

    // 2. Tự động nạp danh sách sản phẩm từ Server
    loadSellerProducts();
  }

  /**
   * Gửi yêu cầu lấy danh sách đấu giá của riêng người bán này
   */
  private void loadSellerProducts() {
    User currentUser = SessionManager.getInstance().getCurrentUser();
    if (currentUser == null) return;

    // Tạo yêu cầu với Command mà Server đã hỗ trợ (GET_SELLER_AUCTIONS)
    Message request = new Message("REQUEST", "GET_SELLER_AUCTIONS", currentUser.getUserId());

    // Chạy trong Thread riêng để không làm đơ giao diện khi chờ mạng
    new Thread(() -> {
      try {
        Message response = SocketClient.sendRequest(request);

        if (response != null && "SUCCESS".equals(response.getStatus())) {
          List<Auction> sellerAuctions = (List<Auction>) response.getData();

          // Quay lại luồng UI chính để cập nhật giao diện
          Platform.runLater(() -> displayAuctions(sellerAuctions));
        } else {
          Platform.runLater(() -> System.err.println("Lỗi: Không thể lấy dữ liệu từ Server"));
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }).start();
  }

  /**
   * Hàm quan trọng: Biến List dữ liệu thành các thẻ ItemCard FXML
   */
  private void displayAuctions(List<Auction> auctions) {
    if (productsContainer == null) return;

    productsContainer.getChildren().clear(); // Xóa các thẻ cũ (nếu có)

    for (Auction auction : auctions) {
      try {
        // Nạp file FXML của thẻ sản phẩm
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/deptrai/auctionsystem/client/views/item-card.fxml"));
        Parent card = loader.load();

        // Lấy controller của thẻ và đổ dữ liệu vào
        ItemCardController cardController = loader.getController();
        cardController.setData(auction);

        // Thêm thẻ vào FlowPane
        productsContainer.getChildren().add(card);

      } catch (IOException e) {
        System.err.println("Lỗi nạp ItemCard: " + e.getMessage());
      }
    }
  }

  // --- CÁC HÀM ĐIỀU HƯỚNG ---

  @FXML
  public void handleGoBack(ActionEvent event) {
    SceneManager.getInstance().goBack();
  }

  @FXML
  public void handleLogout(ActionEvent event) {
    SessionManager.getInstance().logout();
    SceneManager.getInstance().clearHistory();
    SceneManager.getInstance().switchScene("/org/deptrai/auctionsystem/client/views/login-view.fxml", "Đăng nhập");
  }

  @FXML
  public void handleAddNewProduct(ActionEvent event) {
    SceneManager.getInstance().switchScene("/org/deptrai/auctionsystem/client/views/add-product-view.fxml", "Đăng sản phẩm mới");
  }

  @FXML
  public void handleOpenInventory(ActionEvent event) {
    SceneManager.getInstance().switchScene("/org/deptrai/auctionsystem/client/views/inventory-view.fxml", "Kho hàng của tôi");
  }

  @FXML
  public void handleShowProfile(ActionEvent event) {
    SceneManager.getInstance().switchScene("/org/deptrai/auctionsystem/client/views/profile-view.fxml", "Hồ sơ cá nhân");
  }
}