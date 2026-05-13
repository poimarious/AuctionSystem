package org.deptrai.auctionsystem.client.controllers;

import java.io.File;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.deptrai.auctionsystem.client.utils.AuctionUpdateListener;
import org.deptrai.auctionsystem.client.utils.SceneManager;
import org.deptrai.auctionsystem.client.utils.SessionManager;
import org.deptrai.auctionsystem.client.utils.SocketClient;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.auction.AuctionStatus;
import org.deptrai.auctionsystem.shared.models.bid.Bid;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.shared.network.Message;

public class BiddingDetailController implements AuctionUpdateListener {

  @FXML private ImageView productImageView;
  @FXML private Label nameLabel;
  @FXML private Label descriptionLabel;
  @FXML private Label currentPriceLabel;
  @FXML private Label expiryTimerLabel;

  @FXML private TableView<Bid> bidHistoryTable;
  @FXML private TableColumn<Bid, String> timeColumn;
  @FXML private TableColumn<Bid, String> bidderColumn;
  @FXML private TableColumn<Bid, Double> amountColumn;

  @FXML private TextField bidAmountField;

  private Auction currentAuction;
  private Timeline countdownTimeline;

  @FXML
  public void initialize() {
    // Cấu hình bảng
    amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
    timeColumn.setCellValueFactory(cellData ->
        new SimpleStringProperty(cellData.getValue().getTimestamp().toString().substring(11, 19)));

    bidderColumn.setCellValueFactory(cellData -> {
      if (cellData.getValue().getBidder() != null) {
        return new SimpleStringProperty(cellData.getValue().getBidder().getUsername());
      }
      return new SimpleStringProperty("N/A");
    });

    // Tự động load dữ liệu từ Session khi vừa vào trang
    Auction selected = SessionManager.getInstance().getSelectedAuction();
    if (selected != null) {
      Message req = new Message("GET_AUCTION_BY_ID", selected.getAuctionId());

      Message res = SocketClient.sendRequest(req);

      if (res.getStatus().equals("SUCCESS")) {
        // Lấy được hàng nóng hổi từ DB Server
        Auction freshAuction = (Auction) res.getData();
        setAuctionData(freshAuction);
      } else {
        // Dự phòng rủi ro: Nếu mạng lag thì dùng tạm dữ liệu cũ
        setAuctionData(selected);
      }
    }
  }

  public void setAuctionData(Auction auction) {
    this.currentAuction = auction;
    nameLabel.setText(auction.getItem().getName());
    descriptionLabel.setText(auction.getItem().getDescription());
    currentPriceLabel.setText(String.format("$%.2f", auction.getCurrentPrice()));

    String imagePath = auction.getItem().getImageUrl();
    if(imagePath != null && !imagePath.isEmpty()) {
      try {
        File imgfile = new File(imagePath);
        if(imgfile.exists()) {
          Image image = new Image(imgfile.toURI().toString());
          productImageView.setImage(image);
        }
      } catch (Exception e) {
        System.err.println("Không thể load ảnh thật từ đường dẫn: " + imagePath);
      }
    }
    List<Bid> bids = new ArrayList<>(auction.getBids());
    bids.sort(Comparator.comparing(Bid::getTimestamp));
    bidHistoryTable.getItems().setAll(bids);

    SocketClient.addListener(this); // New observer

    startTimer();
  }

  private void startTimer() {
    if (countdownTimeline != null) countdownTimeline.stop();
    countdownTimeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), e -> updateCountdown()));
    countdownTimeline.setCycleCount(Timeline.INDEFINITE);
    countdownTimeline.play();
  }

  private void updateCountdown() {
    if (currentAuction == null) return;
    Duration res = Duration.between(LocalDateTime.now(), currentAuction.getEndTime());
    if (res.isNegative() || res.isZero()) {
      expiryTimerLabel.setText("00:00:00");
      bidAmountField.setDisable(true);
      countdownTimeline.stop();
    } else {
      expiryTimerLabel.setText(String.format("%02d:%02d:%02d",
          res.toHours(), res.toMinutesPart(), res.toSecondsPart()));
    }
  }

  // SỬA NÚT QUAY LẠI: Kiểm tra kỹ đường dẫn này!
  @FXML
  public void handleGoBack(ActionEvent event) {
    if (countdownTimeline != null) countdownTimeline.stop();
    // Removing observer
    SocketClient.removeListener(this);

    // Kiểm tra xem file của bạn là home-view.fxml hay auction-floor.fxml
    SceneManager.getInstance().switchScene(
        "/org/deptrai/auctionsystem/client/views/home-view.fxml",
        "Trang chủ - Auction.UET"
    );
  }

  @FXML
  private void handlePlaceBid() {
    try {
      double amount = Double.parseDouble(bidAmountField.getText());
      User currentUser = SessionManager.getInstance().getCurrentUser();

      Message bidReq = new Message("PLACE_BID", new Object[]{currentAuction.getAuctionId(), currentUser.getUserId(), amount});
      new Thread(() -> {
        Message res = SocketClient.sendRequest(bidReq);
        Platform.runLater(() -> {
          if ("SUCCESS".equals(res.getStatus())) {
            bidAmountField.clear();
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Đặt giá thành công!");
            a.show();
          } else {
            String realErrorMessage = "Không nhận được phản hồi từ Server (Mất kết nối).";

            if (res.getData() instanceof String) {
              // Nếu Server trả về lỗi dạng chuỗi String
              realErrorMessage = (String) res.getData();
            } else {
              // Nếu Server trả về object khác hoặc null
              realErrorMessage = "Server từ chối yêu cầu nhưng không rõ lý do. Trạng thái: " + res.getStatus();
            }

            showError(realErrorMessage);
          }
        });
      }).start();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      showError("Vui lòng nhập giá hợp lệ.");
    }
  }

  @Override
  public void onAuctionUpdated(Auction updatedAuction) {
    // Nếu màn hình này đang xem đúng cái sản phẩm vừa được ai đó đặt giá
    if (this.currentAuction.getAuctionId().equals(updatedAuction.getAuctionId())) {
      this.currentAuction = updatedAuction;

      Platform.runLater(() -> {
        // 1. Cập nhật giá tiền mới nhất
        currentPriceLabel.setText(String.format("$%.2f", updatedAuction.getCurrentPrice()));

        // 2. Nạp lại toàn bộ danh sách Bid từ Server (đảm bảo đúng thứ tự và đủ số lượng)
        bidHistoryTable.getItems().setAll(updatedAuction.getBids());

        // 3. Cuộn xuống cái cuối cùng
        if (!updatedAuction.getBids().isEmpty()) {
          bidHistoryTable.scrollTo(updatedAuction.getBids().size() - 1);
        }
      });
    }
  }

  private void showError(String msg) {
    Alert alert = new Alert(Alert.AlertType.ERROR, msg);
    alert.show();
  }
}