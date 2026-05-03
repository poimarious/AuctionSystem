package org.deptrai.auctionsystem.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.deptrai.auctionsystem.models.auction.Auction;

import java.time.Duration;
import java.time.LocalDateTime;

public class ItemCardController {

    @FXML
    private ImageView itemImageView;

    @FXML
    private Label nameLabel;

    @FXML
    private Label priceLabel;

    @FXML
    private Label timerLabel;

    @FXML
    private Button bidButton;

    private Auction auction;

    // Hàm nhận dữ liệu từ MainController truyền sang
    public void setData(Auction auction) {
        this.auction = auction;

        // Cập nhật thông tin lên giao diện
        nameLabel.setText(auction.getItem().getName());
        priceLabel.setText(String.format("$%.2f", auction.getCurrentPrice()));

        // Tính thời gian còn lại đơn giản
        Duration duration = Duration.between(LocalDateTime.now(), auction.getEndTime());
        if (duration.isNegative()) {
            timerLabel.setText("Đã kết thúc");
            timerLabel.setStyle("-fx-text-fill: red;");
            bidButton.setDisable(true); // Khóa nút đặt giá nếu hết hạn
        } else {
            long hours = duration.toHours();
            long minutes = duration.toMinutesPart();
            timerLabel.setText(String.format("Còn lại: %dh %dm", hours, minutes));
        }

        // Đặt ảnh mặc định (Placeholder) - Sau này thay bằng ảnh thật
        try {
            // Đảm bảo bạn có 1 file placeholder.png trong thư mục resources/images/
            Image placeholder = new Image(getClass().getResourceAsStream("/images/placeholder.png"));
            itemImageView.setImage(placeholder);
        } catch (Exception e) {
            System.err.println("Không tìm thấy ảnh placeholder.");
        }

        // Xử lý sự kiện bấm nút "ĐẶT GIÁ NGAY"
        bidButton.setOnAction(event -> handleBidAction());
    }

    private void handleBidAction() {
        System.out.println("Mở màn hình chi tiết đấu giá cho sản phẩm: " + auction.getItem().getName());
        // TODO: Mở cửa sổ hoặc panel chi tiết sản phẩm (bidding-detail.fxml)
    }
}