package org.deptrai.auctionsystem.client.controllers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.deptrai.auctionsystem.client.utils.SceneManager;
import org.deptrai.auctionsystem.client.utils.SessionManager;
import org.deptrai.auctionsystem.client.utils.SocketClient;
import org.deptrai.auctionsystem.shared.models.bid.Bid;
import org.deptrai.auctionsystem.shared.models.users.Bidder;
import org.deptrai.auctionsystem.shared.network.Message;

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
            String userId = bidder.getUserId();

            // Chạy luồng phụ để không làm đơ giao diện khi đợi phản hồi từ Server
            new Thread(() -> {
                try {
                    // 1. Gửi yêu cầu lấy lịch sử lên Server
                    Message request = new Message("GET_BIDS_HISTORY", userId);
                    Message response = SocketClient.sendRequest(request);

                    if (response != null && "SUCCESS".equals(response.getStatus())) {
                        // 2. Nhận danh sách Bid từ Server
                        List<Bid> myBids = (List<Bid>) response.getData();

                        // 3. Cập nhật giao diện trên JavaFX Application Thread
                        Platform.runLater(() -> {
                            ObservableList<Bid> observableBids = FXCollections.observableArrayList(myBids);
                            bidHistoryTable.setItems(observableBids);

                            if (myBids.isEmpty()) {
                                System.out.println("Bạn chưa có lượt đặt giá nào.");
                            } else {
                                System.out.println("Đã cập nhật " + myBids.size() + " lượt đặt giá từ Server.");
                            }
                        });
                    } else {
                        String errorMsg = (response != null) ? (String) response.getData() : "Không có phản hồi từ Server";
                        System.err.println("Lỗi khi tải lịch sử: " + errorMsg);
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi kết nối mạng khi tải lịch sử đặt giá.");
                    e.printStackTrace();
                }
            }).start();

        } else {
            System.out.println("Người dùng hiện tại không phải là Bidder.");
        }
    }

  @FXML
  public void handleGoBack(ActionEvent event) {
    SceneManager.getInstance().goBack();
  }
}
