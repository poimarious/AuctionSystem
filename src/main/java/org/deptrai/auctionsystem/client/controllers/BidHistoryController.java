package org.deptrai.auctionsystem.client.controllers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList; // BỔ SUNG IMPORT
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BidHistoryController {
  private static final Logger logger = LoggerFactory.getLogger(BidHistoryController.class);
  @FXML private TableView<Bid> bidHistoryTable;
  @FXML private TableColumn<Bid, String> productNameColumn;
  @FXML private TableColumn<Bid, String> myBidPriceColumn;
  @FXML private TableColumn<Bid, String> currentPriceColumn;
  @FXML private TableColumn<Bid, String> timeRemainingColumn;
  @FXML private TableColumn<Bid, String> statusColumn;
  @FXML private TableColumn<Bid, String> imageColumn;

  // --- Kho chứa gốc lưu TOÀN BỘ lịch sử trên RAM ---
  private List<Bid> allBidsList = new ArrayList<>();

  @FXML
  public void initialize() {
    logger.info("Đã load trang Lịch sử đặt giá!");

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

      SocketClient.runAsync(() -> {
        try {
          Message request = new Message("GET_BIDS_HISTORY", userId);
          Message response = SocketClient.sendRequest(request);

          if (response != null && "SUCCESS".equals(response.getStatus())) {
            List<Bid> myBids = (List<Bid>) response.getData();

            Platform.runLater(() -> {

              // SỬA CHỖ NÀY: Cất nguyên bản toàn bộ danh sách vào kho gốc
              allBidsList = myBids;

              filterBids("ALL"); // Mặc định hiển thị tất cả

              if (myBids.isEmpty()) {
                logger.info("Bạn chưa có lượt đặt giá nào.");
              } else {
                logger.info("Đã tải " + myBids.size() + " lượt đặt giá từ Server.");
              }
            });
          } else {
            String errorMsg = (response != null) ? (String) response.getData() : "Không có phản hồi từ Server";
            logger.error("Lỗi khi tải lịch sử: " + errorMsg);
          }
        } catch (Exception e) {
          logger.error("Lỗi kết nối mạng khi tải lịch sử đặt giá.");
          e.printStackTrace();
        }
      });
      new Thread(() -> {

      }).start();

    } else {
      logger.info("Người dùng hiện tại không phải là Bidder.");
    }
  }

  // --- SỬA CHỖ NÀY: Mang logic Gom nhóm xuống hàm Lọc ---
  private void filterBids(String filterType) {
    if (allBidsList == null) return;

    // Danh sách trung gian để chuẩn bị xét duyệt
    List<Bid> listToProcess = allBidsList;

    // NẾU KHÔNG PHẢI "ALL" THÌ MỚI GOM NHÓM (Để loại bỏ giá cũ, lấy giá cao nhất)
    if (!"ALL".equals(filterType)) {
      java.util.Map<String, Bid> highestBidsMap = new java.util.HashMap<>();
      for (Bid bid : allBidsList) {
        if (bid.getAuction() != null && bid.getAuction().getItem() != null) {
          String productName = bid.getAuction().getItem().getName();
          if (!highestBidsMap.containsKey(productName) || bid.getAmount() > highestBidsMap.get(productName).getAmount()) {
            highestBidsMap.put(productName, bid);
          }
        }
      }
      listToProcess = new java.util.ArrayList<>(highestBidsMap.values());
    }

    List<Bid> filteredList = new ArrayList<>();

    for (Bid bid : listToProcess) {
      if (bid.getAuction() == null) continue;

      String status = bid.getAuction().getStatus().toString();

      switch (filterType) {
        case "ALL":
          filteredList.add(bid);
          break;
        case "RUNNING":
          if ("RUNNING".equalsIgnoreCase(status)) {
            filteredList.add(bid);
          }
          break;
        case "FINISHED":
          if ("FINISHED".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status)) {
            filteredList.add(bid);
          }
          break;
        case "WON":
          if (("FINISHED".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status))
              && bid.getAmount() == bid.getAuction().getCurrentPrice()) {
            filteredList.add(bid);
          }
          break;
      }
    }

    ObservableList<Bid> observableBids = FXCollections.observableArrayList(filteredList);
    bidHistoryTable.setItems(observableBids);
  }

  // --- 4 Hàm dành cho 4 cái nút trên FXML ---
  @FXML
  public void handleShowAll(ActionEvent event) {
    filterBids("ALL");
  }

  @FXML
  public void handleShowRunning(ActionEvent event) {
    filterBids("RUNNING");
  }

  @FXML
  public void handleShowFinished(ActionEvent event) {
    filterBids("FINISHED");
  }

  @FXML
  public void handleShowWon(ActionEvent event) {
    filterBids("WON");
  }

  @FXML
  public void handleGoBack(ActionEvent event) {
    SceneManager.getInstance().goBack();
  }
}