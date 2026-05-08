package org.deptrai.auctionsystem.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.deptrai.auctionsystem.models.auction.Auction;
import org.deptrai.auctionsystem.utils.SceneManager;
import org.deptrai.auctionsystem.utils.SessionManager;

import java.time.Duration;
import java.time.LocalDateTime;

public class ItemCardController {

    @FXML private ImageView itemImageView;
    @FXML private Label nameLabel;
    @FXML private Label priceLabel;
    @FXML private Label timerLabel;
    @FXML private Button bidButton;

    private Auction auction;

    public void setData(Auction auction) {
        this.auction = auction;

        nameLabel.setText(auction.getItem().getName());
        priceLabel.setText(String.format("$%.2f", auction.getCurrentPrice()));

        Duration duration = Duration.between(LocalDateTime.now(), auction.getEndTime());
        if (duration.isNegative()) {
            timerLabel.setText("Đã kết thúc");
            timerLabel.setStyle("-fx-text-fill: red;");
            bidButton.setDisable(true);
        } else {
            long hours = duration.toHours();
            long minutes = duration.toMinutesPart();
            timerLabel.setText(String.format("Còn lại: %dh %dm", hours, minutes));
        }

        try {
            // Đảm bảo đường dẫn ảnh chính xác
            Image placeholder = new Image(getClass().getResourceAsStream("/images/placeholder.png"));
            itemImageView.setImage(placeholder);
        } catch (Exception e) {
            System.err.println("Không tìm thấy ảnh sản phẩm.");
        }
    }

    @FXML
    public void handleBidAction() {
        // Lưu phiên đấu giá vào Session để trang sau có thể truy cập
        SessionManager.getInstance().setSelectedAuction(this.auction);
        // Chuyển sang trang chi tiết
        SceneManager.getInstance().switchScene("/org.deptrai.auctionsystem.views/bidding-detail.fxml", "Chi tiết đấu giá - " + auction.getItem().getName());
    }

    @FXML
    public void handleGoBack(ActionEvent event) {
        SceneManager.getInstance().goBack();
    }
}