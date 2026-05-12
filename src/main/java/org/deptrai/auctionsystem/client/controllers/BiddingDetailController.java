package org.deptrai.auctionsystem.client.controllers;

import java.io.File;
import java.time.LocalDateTime;
import java.time.Duration;
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
import org.deptrai.auctionsystem.client.utils.SceneManager;
import org.deptrai.auctionsystem.client.utils.SessionManager;
import org.deptrai.auctionsystem.client.utils.SocketClient;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.auction.AuctionStatus;
import org.deptrai.auctionsystem.shared.models.bid.Bid;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.shared.network.Message;
import org.deptrai.auctionsystem.shared.observer.AuctionObserver;

public class BiddingDetailController implements AuctionObserver {

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
      setAuctionData(selected);
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

    bidHistoryTable.getItems().setAll(auction.getBids());
    this.currentAuction.attach(this);
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

      Message bidReq = new Message("REQUEST", "PLACE_BID", new Object[]{currentAuction.getAuctionId(), currentUser.getUserId(), amount});
      new Thread(() -> {
        Message res = SocketClient.sendRequest(bidReq);
        Platform.runLater(() -> {
          if (res != null && "SUCCESS".equals(res.getStatus())) {
            bidAmountField.clear();
            Bid newBid = (Bid) res.getData();
            currentAuction.setCurrentPrice(newBid.getAmount());
            onBidPlaced(currentAuction, newBid);
            currentUser.setBalance(currentUser.getBalance() - amount);
          } else {
            String realErrorMessage = "Không nhận được phản hồi từ Server (Mất kết nối).";

            if (res != null) {
              if (res.getData() instanceof String) {
                // Nếu Server trả về lỗi dạng chuỗi String
                realErrorMessage = (String) res.getData();
              } else {
                // Nếu Server trả về object khác hoặc null
                realErrorMessage = "Server từ chối yêu cầu nhưng không rõ lý do. Trạng thái: " + res.getStatus();
              }
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

  @Override public void onBidPlaced(Auction a, Bid b) {
    Platform.runLater(() -> {
      currentPriceLabel.setText(String.format("$%.2f", a.getCurrentPrice()));
      bidHistoryTable.getItems().add(b);
      bidHistoryTable.scrollTo(b);
    });
  }

  @Override public void onAuctionStatusChanged(Auction a) {
    Platform.runLater(() -> {
      if (a.getStatus() == AuctionStatus.FINISHED) bidAmountField.setDisable(true);
    });
  }

  private void showError(String msg) {
    Alert alert = new Alert(Alert.AlertType.ERROR, msg);
    alert.show();
  }
}