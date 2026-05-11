package org.deptrai.auctionsystem.client.controllers;

import java.time.LocalDateTime;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import org.deptrai.auctionsystem.client.utils.SceneManager;
import org.deptrai.auctionsystem.client.utils.SessionManager;
import org.deptrai.auctionsystem.shared.models.auction.Auction;

public class ItemCardController {

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


    // Quay lại dùng ảnh mặc định như cũ để không bị lỗi đỏ
    try {
      Image placeholder = new Image(getClass().getResourceAsStream("/org/deptrai/auctionsystem/client/images/placeholder.png"));
      itemImageView.setImage(placeholder);
    } catch (Exception e) {
      System.err.println("Không tìm thấy ảnh placeholder.");
    }

    startCountdown();
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

    // 1. Lưu auction vào Session để trang sau có cái mà hiển thị
    SessionManager.getInstance().setSelectedAuction(this.auction);

    // 2. Chuyển sang đúng file FXML bạn vừa gửi
    SceneManager.getInstance().switchScene(
        "/org/deptrai/auctionsystem/client/views/bidding-detail.fxml",
        "Chi tiết đấu giá - " + auction.getItem().getName());
  }

  @FXML
  public void handleGoBack(ActionEvent event) {
    if (timeline != null) timeline.stop();
    // Quay về trang chủ
    SceneManager.getInstance().switchScene(
        "/org/deptrai/auctionsystem/client/views/home-view.fxml",
        "Trang chủ - Auction.UET"
    );
  }
}
