package org.deptrai.auctionsystem.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import org.deptrai.auctionsystem.models.auction.Auction;
import org.deptrai.auctionsystem.models.auction.AuctionStatus;
import org.deptrai.auctionsystem.models.bid.Bid;
import org.deptrai.auctionsystem.models.observer.AuctionObserver;
import org.deptrai.auctionsystem.models.users.Bidder;
import org.deptrai.auctionsystem.utils.SceneManager;
import org.deptrai.auctionsystem.utils.SessionManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BiddingDetailController implements AuctionObserver {

    @FXML private ImageView productImageView;
    @FXML private Label nameLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label expiryTimerLabel;

    @FXML private TableView<Bid> bidHistoryTable;
    @FXML private TableColumn<Bid, String> timeColumn;
    @FXML private TableColumn<Bid, String> bidderColumn;
    // Đã chuyển lại thành String để fix lỗi mất dữ liệu giá
    @FXML private TableColumn<Bid, String> amountColumn;

    @FXML private TextField bidAmountField;

    private Auction currentAuction;
    private Timeline timeline;

    @FXML
    public void initialize() {
        // Cấu hình bảng thời gian
        timeColumn.setCellValueFactory(cellData -> {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM");
            return new SimpleStringProperty(cellData.getValue().getTimestamp().format(fmt));
        });

        // Cấu hình bảng người đặt
        bidderColumn.setCellValueFactory(cellData -> {
            String name = (cellData.getValue().getBidder() != null) ? cellData.getValue().getBidder().getUsername() : "N/A";
            return new SimpleStringProperty(name);
        });

        // Cấu hình bảng giá (Ép String kèm dấu $ để KHÔNG BỊ TRỐNG)
        amountColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.format("$%.2f", cellData.getValue().getAmount()))
        );

        Auction selected = SessionManager.getInstance().getSelectedAuction();
        if (selected != null) setAuctionData(selected);
    }

    public void setAuctionData(Auction auction) {
        this.currentAuction = auction;
        nameLabel.setText(auction.getItem().getName());
        descriptionLabel.setText(auction.getItem().getDescription());
        currentPriceLabel.setText(String.format("GIÁ HIỆN TẠI: $%.2f", auction.getCurrentPrice()));

        bidHistoryTable.getItems().setAll(auction.getBids());
        this.currentAuction.attach(this);

        startSyncTimer();
    }

    private void startSyncTimer() {
        if (timeline != null) timeline.stop();
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (expiryTimerLabel == null) return;

            java.time.Duration res = java.time.Duration.between(LocalDateTime.now(), currentAuction.getEndTime());
            if (res.isNegative() || res.isZero()) {
                expiryTimerLabel.setText("ĐÃ KẾT THÚC");
                if (timeline != null) timeline.stop();
            } else {
                // Điền cả cụm chữ và giờ vào 1 nhãn duy nhất
                expiryTimerLabel.setText(String.format("HẾT HẠN TRONG: %02d:%02d:%02d", res.toHours(), res.toMinutesPart(), res.toSecondsPart()));
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    @FXML
    public void handleGoBack(ActionEvent event) {
        if (timeline != null) timeline.stop();
        SceneManager.getInstance().goBack();
    }

    @FXML
    private void handlePlaceBid() {
        try {
            double amount = Double.parseDouble(bidAmountField.getText());
            Object user = SessionManager.getInstance().getCurrentUser();
            if (user instanceof Bidder) {
                ((Bidder) user).placeBid(currentAuction, amount);
                bidAmountField.clear();
            }
        } catch (Exception e) {
            System.err.println("Lỗi đặt giá: " + e.getMessage());
        }
    }

    @Override
    public void onBidPlaced(Auction a, Bid b) {
        Platform.runLater(() -> {
            currentPriceLabel.setText(String.format("GIÁ HIỆN TẠI: $%.2f", a.getCurrentPrice()));
            if (!bidHistoryTable.getItems().contains(b)) {
                bidHistoryTable.getItems().add(b);
            }
            bidHistoryTable.scrollTo(bidHistoryTable.getItems().size() - 1);
        });
    }

    @Override public void onAuctionStatusChanged(Auction a) {}
}