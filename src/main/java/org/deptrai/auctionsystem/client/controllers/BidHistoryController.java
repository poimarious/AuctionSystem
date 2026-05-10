package org.deptrai.auctionsystem.client.controllers;

import java.time.Duration;
import java.time.LocalDateTime;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.deptrai.auctionsystem.client.utils.SceneManager;
import org.deptrai.auctionsystem.client.utils.SessionManager;
import org.deptrai.auctionsystem.shared.models.bid.Bid;
import org.deptrai.auctionsystem.shared.models.users.Bidder;

public class BidHistoryController {

  @FXML private TableView<Bid> bidHistoryTable;
  @FXML private TableColumn<Bid, String> productNameColumn;
  @FXML private TableColumn<Bid, String> myBidPriceColumn;
  @FXML private TableColumn<Bid, String> currentPriceColumn;
  @FXML private TableColumn<Bid, String> timeRemainingColumn;
  @FXML private TableColumn<Bid, String> statusColumn;
  @FXML private TableColumn<Bid, String> imageColumn;

  @FXML
  public void initialize() {
    System.out.println("Đã load trang Lịch sử đặt giá!");

    // 1. Cột Tên sản phẩm
    productNameColumn.setCellValueFactory(
        cellData -> {
          if (cellData.getValue().getAuction() != null) {
            return new SimpleStringProperty(cellData.getValue().getAuction().getItem().getName());
          }
          return new SimpleStringProperty("N/A");
        });

    // 2. Cột Giá bạn đặt
    myBidPriceColumn.setCellValueFactory(
        cellData ->
            new SimpleStringProperty(String.format("$%.2f", cellData.getValue().getAmount())));

    // 3. Cột Giá hiện tại của phiên đấu giá
    currentPriceColumn.setCellValueFactory(
        cellData -> {
          if (cellData.getValue().getAuction() != null) {
            return new SimpleStringProperty(
                String.format("$%.2f", cellData.getValue().getAuction().getCurrentPrice()));
          }
          return new SimpleStringProperty("$0.00");
        });

    // 4. Cột Thời gian còn lại (Tính toán tự động)
    timeRemainingColumn.setCellValueFactory(
        cellData -> {
          if (cellData.getValue().getAuction() != null) {
            LocalDateTime endTime = cellData.getValue().getAuction().getEndTime();
            Duration remaining = Duration.between(LocalDateTime.now(), endTime);

            if (remaining.isNegative() || remaining.isZero()) {
              return new SimpleStringProperty("Đã kết thúc");
            }
            return new SimpleStringProperty(
                String.format(
                    "%02d:%02d:%02d",
                    remaining.toHours(), remaining.toMinutesPart(), remaining.toSecondsPart()));
          }
          return new SimpleStringProperty("--:--:--");
        });

    // 5. Cột Trạng thái
    statusColumn.setCellValueFactory(
        cellData -> {
          if (cellData.getValue().getAuction() != null) {
            // Bạn có thể tùy chỉnh hiển thị dựa trên Enum AuctionStatus
            return new SimpleStringProperty(
                cellData.getValue().getAuction().getStatus().toString());
          }
          return new SimpleStringProperty("N/A");
        });

    // Kéo dữ liệu đổ vào bảng
    loadBidHistory();
  }

  private void loadBidHistory() {
    Object currentUser = SessionManager.getInstance().getCurrentUser();

    if (currentUser instanceof Bidder bidder) {

      // Đã đổi thành bidder.getBidHistory() cho khớp 100% với code của bạn
      if (bidder.getBidHistory() != null) {
        ObservableList<Bid> myBids = FXCollections.observableArrayList(bidder.getBidHistory());
        bidHistoryTable.setItems(myBids);

        System.out.println(
            "Đã tải thành công "
                + myBids.size()
                + " lượt đặt giá cho User: "
                + bidder.getUsername());
      } else {
        System.out.println("User này chưa có lịch sử đặt giá nào.");
      }
    } else {
      System.out.println("Người dùng hiện tại không phải là Bidder.");
    }
  }

  @FXML
  public void handleGoBack(ActionEvent event) {
    SceneManager.getInstance().goBack();
  }
}
