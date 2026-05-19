package org.deptrai.auctionsystem.client.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.deptrai.auctionsystem.client.utils.SceneManager;
import org.deptrai.auctionsystem.client.utils.SessionManager;
import org.deptrai.auctionsystem.client.utils.SocketClient;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.auction.AuctionStatus;
import org.deptrai.auctionsystem.shared.models.auction.AuctionSummary;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.shared.network.Message;

import java.util.List;

public class SellerController {

  @FXML private TextField searchField;
  @FXML private Label welcomeLabel;
  @FXML private Label balanceLabel;

  @FXML private Label ongoingCountLabel;
  @FXML private Label successCountLabel;

  User currentUser;

  int ongoingCount;
  int successCount;

  private List<Auction> sellerAuctions;

  @FXML
  public void initialize() {
    System.out.println("--- Seller Center Initialized ---");


    currentUser = SessionManager.getInstance().getCurrentUser();
    if (currentUser != null && welcomeLabel != null) {
      welcomeLabel.setText("Chào mừng, " + currentUser.getUsername() + "!");
    }

    loadSellerAuctions();

    loadSellerStatistics();
  }

  private void loadSellerAuctions() {
    SocketClient.runAsync(() -> {
      Message request = new Message("GET_SELLER_AUCTIONS", currentUser.getUserId());
      Message response = SocketClient.sendRequest(request);

      if(response != null && "SUCCESS".equals(response.getStatus())) {
        sellerAuctions = (List<Auction>) response.getData();


        // Đếm các auction đang đấu giá và đã đấu giá thành công của seller
        ongoingCount = 0;
        successCount = 0;
        for (Auction auction : sellerAuctions) {
          if(auction.getStatus() == AuctionStatus.OPEN || auction.getStatus() == AuctionStatus.RUNNING) {
            this.ongoingCount++;
          } else if(auction.getStatus() == AuctionStatus.PAID) {
            this.successCount++;
          }
        }
      }
    });
  }

  private void loadSellerStatistics() {
    Platform.runLater(() -> {
      balanceLabel.setText(String.format("%.2f$", currentUser.getBalance()));
      ongoingCountLabel.setText(String.format("%d sản phẩm", ongoingCount));
      successCountLabel.setText(String.format("%d sản phẩm", successCount));
    });
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