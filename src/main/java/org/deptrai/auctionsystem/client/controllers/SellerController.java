package org.deptrai.auctionsystem.client.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import org.deptrai.auctionsystem.client.utils.SceneManager;
import org.deptrai.auctionsystem.client.utils.SessionManager;
import org.deptrai.auctionsystem.client.utils.SocketClient;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.auction.AuctionStatus;
import org.deptrai.auctionsystem.shared.models.bid.Bid;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.shared.network.Message;

import java.time.DayOfWeek;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class SellerController {
  @FXML
  private Label welcomeLabel;
  @FXML
  private Label balanceLabel;

  @FXML
  private Label ongoingCountLabel;
  @FXML
  private Label successCountLabel;

  @FXML
  private LineChart<String, Number> revenueChart;

  User currentUser;

  int ongoingCount;
  int successCount;
  double totalRevenue;

  private List<Auction> sellerAuctions;

  @FXML
  public void initialize() {
    currentUser = SessionManager.getInstance().getCurrentUser();
    if (currentUser != null && welcomeLabel != null) {
      welcomeLabel.setText("Chào mừng, " + currentUser.getUsername() + "!");
    }

    loadSellerData();
  }

  private void loadSellerData() {
    SocketClient.runAsync(() -> {
      Message request = new Message("GET_SELLER_AUCTIONS", currentUser.getUserId());
      Message response = SocketClient.sendRequest(request);

      if ("SUCCESS".equals(response.getStatus())) {
        @SuppressWarnings("unchecked")
        List<Auction> auctions = (List<Auction>) response.getData();
        sellerAuctions = auctions;


        // Đếm các auction đang đấu giá và đã đấu giá thành công của seller
        ongoingCount = 0;
        successCount = 0;
        totalRevenue = 0;
        for (Auction auction : sellerAuctions) {
          if (auction.getStatus() == AuctionStatus.OPEN || auction.getStatus() == AuctionStatus.RUNNING) {
            this.ongoingCount++;
          } else if (auction.getStatus() == AuctionStatus.PAID) {
            this.successCount++;
            this.totalRevenue += auction.getCurrentPrice();
          }
        }

        Map<DayOfWeek, Integer> biddingPerDay = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
          biddingPerDay.put(day, 0);
        }

        for (Auction auction : sellerAuctions) {
          if (auction.getStatus() != AuctionStatus.PAID) continue;
          for (Bid bid : auction.getBids()) {
            if (bid.getTimestamp() != null) {
              DayOfWeek day = bid.getTimestamp().getDayOfWeek();
              biddingPerDay.put(day, biddingPerDay.get(day) + 1);
            }
          }
        }

        Platform.runLater(() -> {
          balanceLabel.setText(String.format("%.2f$", totalRevenue));
          ongoingCountLabel.setText(String.format("%d sản phẩm", ongoingCount));
          successCountLabel.setText(String.format("%d sản phẩm", successCount));

          if (revenueChart != null) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Lượt đặt giá");
            String[] days = {"Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "CN"};

            int id = 0;
            for (DayOfWeek day : DayOfWeek.values()) {
              series.getData().add(new XYChart.Data<>(days[id], biddingPerDay.get(day)));
              id++;
            }


            revenueChart.setAnimated(false);
            revenueChart.getXAxis().setAnimated(false);
            revenueChart.getData().clear();
            revenueChart.getData().add(series);
          }
        });
      }
    });
  }

  // --- CÁC HÀM ĐIỀU HƯỚNG ---

  @FXML
  public void handleGoBack() {
    SceneManager.getInstance().goBack();
  }

  @FXML
  public void handleLogout() {
    SessionManager.getInstance().logout();
    SceneManager.getInstance().clearHistory();
    SceneManager.getInstance().switchScene("/org/deptrai/auctionsystem/client/views/login-view.fxml", "Đăng nhập");
  }

  @FXML
  public void handleAddNewProduct() {
    SceneManager.getInstance().switchScene("/org/deptrai/auctionsystem/client/views/add-product-view.fxml", "Đăng sản phẩm mới");
  }

  @FXML
  public void handleOpenInventory() {
    SceneManager.getInstance().switchScene("/org/deptrai/auctionsystem/client/views/inventory-view.fxml", "Kho hàng của tôi");
  }

  @FXML
  public void handleShowRevenue() {
    // Chuyển hướng sang file tkdt.fxml (Lịch sử giao dịch)
    // Lưu ý: Hãy kiểm tra lại đường dẫn thư mục xem tkdt.fxml nằm chính xác ở đâu nhé
    SceneManager.getInstance().switchScene(
            "/org/deptrai/auctionsystem/client/views/tkdt.fxml",
            "Thống kê doanh thu"
    );
  }
}