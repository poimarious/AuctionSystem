package org.deptrai.auctionsystem.client.controllers;

import java.nio.file.Files;
import java.time.LocalDateTime;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import org.deptrai.auctionsystem.client.utils.AuctionUpdateListener;
import org.deptrai.auctionsystem.client.utils.SceneManager;
import org.deptrai.auctionsystem.client.utils.SessionManager;
import org.deptrai.auctionsystem.client.utils.SocketClient;
import org.deptrai.auctionsystem.shared.models.auction.Auction;

import java.io.File;

public class ItemCardController implements AuctionUpdateListener {

  @FXML private ImageView itemImageView;
  @FXML private Label nameLabel;
  @FXML private Label priceLabel;
  @FXML private Label timerLabel;
  @FXML private Button bidButton;

  private Auction auction;
  private Timeline timeline;

  public void setData(Auction auction) {
    this.auction = auction;
    nameLabel.setText(auction.getItem().getName());
    priceLabel.setText(String.format("$%.2f", auction.getCurrentPrice()));

    String imagePath = auction.getItem().getImageUrl();

    if(imagePath != null && !imagePath.isEmpty()) {
      try {
        File imgFile = new File(imagePath);
        if(imgFile.exists()) {
          Image realImage = new Image(imgFile.toURI().toString());
          itemImageView.setImage(realImage);
        }
      } catch (Exception e) {
        System.err.println("Không thể load ảnh thật từ đường dẫn: " + imagePath);
      }
    }

    startCountdown();
    SocketClient.addListener(this);
  }

  private void startCountdown() {
    if (timeline != null) timeline.stop();
    timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateTimer()));
    timeline.setCycleCount(Timeline.INDEFINITE);
    timeline.play();
    updateTimer();
  }

  private void updateTimer() {
    if (auction == null || timerLabel == null) return;
    java.time.Duration remaining =
        java.time.Duration.between(LocalDateTime.now(), auction.getEndTime());

    if (remaining.isNegative() || remaining.isZero()) {
      timerLabel.setText("00:00:00");
      timerLabel.setStyle("-fx-text-fill: red;");
      if (bidButton != null) bidButton.setDisable(true);
      if (timeline != null) timeline.stop();
    } else {
      timerLabel.setText(
          String.format(
              "%02d:%02d:%02d",
              remaining.toHours(), remaining.toMinutesPart(), remaining.toSecondsPart()));
    }
  }

  /**
   * HÀM QUAN TRỌNG: Chuyển sang trang bidding-detail
   */
  @FXML
  public void handleBidAction() {
    if (this.auction == null) return;

    if (timeline != null) timeline.stop();

    SocketClient.removeListener(this);

    // 1. Lưu auction vào Session để trang sau có cái mà hiển thị
    SessionManager.getInstance().setSelectedAuction(this.auction);

    // 2. Chuyển sang đúng file FXML bạn vừa gửi
    SceneManager.getInstance().switchScene(
        "/org/deptrai/auctionsystem/client/views/bidding-detail.fxml",
        "Chi tiết đấu giá - " + auction.getItem().getName());
  }

  @Override
  public void onAuctionUpdated(Auction updatedAuction) {
    // Kiểm tra xem tin nhắn đổi giá có phải dành cho món hàng của cái thẻ này không
    if (this.auction.getAuctionId().equals(updatedAuction.getAuctionId())) {
      // Update this card's RAM
      this.auction = updatedAuction;

      // Nhảy số tiền trên giao diện
      Platform.runLater(() -> {
        priceLabel.setText(String.format("$%.2f", updatedAuction.getCurrentPrice()));
      });
    }
  }
}
