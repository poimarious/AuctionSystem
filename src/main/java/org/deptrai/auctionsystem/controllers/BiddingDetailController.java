package org.deptrai.auctionsystem.controllers;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import org.deptrai.auctionsystem.models.auction.Auction;
import org.deptrai.auctionsystem.models.auction.AuctionStatus;
import org.deptrai.auctionsystem.models.bid.Bid;
import org.deptrai.auctionsystem.models.observer.AuctionObserver;
import org.deptrai.auctionsystem.models.users.Bidder;
import org.deptrai.auctionsystem.utils.SceneManager;
import org.deptrai.auctionsystem.utils.SessionManager;

import java.time.LocalDateTime;

public class BiddingDetailController implements AuctionObserver {

    @FXML private ImageView productImageView;
    @FXML private Label nameLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label currentPriceLabel;

    @FXML private TableView<Bid> bidHistoryTable;
    @FXML private TableColumn<Bid, String> timeColumn;
    @FXML private TableColumn<Bid, String> bidderColumn;
    @FXML private TableColumn<Bid, Double> amountColumn;

    @FXML private TextField bidAmountField;
    @FXML private TextField maxBidField;
    @FXML private TextField incrementField;

    private Auction currentAuction;

    @FXML
    public void initialize() {
        // Cấu hình bảng
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        bidderColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getBidder() != null) {
                return new SimpleStringProperty(cellData.getValue().getBidder().getUsername());
            }
            return new SimpleStringProperty("N/A");
        });

        // Tự động load dữ liệu nếu có sản phẩm đang được chọn trong Session
        Auction selected = SessionManager.getInstance().getSelectedAuction();
        if (selected != null) {
            setAuctionData(selected);
        }
    }

    public void setAuctionData(Auction auction) {
        this.currentAuction = auction;
        nameLabel.setText(auction.getItem().getName());
        descriptionLabel.setText(auction.getItem().getDescription());
        currentPriceLabel.setText(String.format("GIÁ HIỆN TẠI: $%.2f", auction.getCurrentPrice()));

        bidHistoryTable.getItems().setAll(auction.getBids());
        this.currentAuction.attach(this);
    }

    @FXML
    public void handleGoBack(ActionEvent event) {
        SceneManager.getInstance().goBack();
    }

    @FXML
    private void handlePlaceBid() {
        try {
            double amount = Double.parseDouble(bidAmountField.getText());
            Bidder currentUser = (Bidder) SessionManager.getInstance().getCurrentUser();

            if (currentUser != null) {
                currentUser.placeBid(currentAuction, amount);
                bidAmountField.clear();
            } else {
                showErrorAlert("Bạn cần đăng nhập để đặt giá!");
            }
        } catch (NumberFormatException e) {
            showErrorAlert("Vui lòng nhập số tiền hợp lệ!");
        } catch (Exception e) {
            showErrorAlert(e.getMessage());
        }
    }

    @Override
    public void onBidPlaced(Auction a, Bid b) {
        Platform.runLater(() -> {
            currentPriceLabel.setText(String.format("GIÁ HIỆN TẠI: $%.2f", a.getCurrentPrice()));
            bidHistoryTable.getItems().add(b);
            bidHistoryTable.scrollTo(b);
        });
    }

    @Override
    public void onAuctionStatusChanged(Auction a) {
        Platform.runLater(() -> {
            if (a.getStatus() == AuctionStatus.FINISHED || a.getStatus() == AuctionStatus.CANCELED) {
                bidAmountField.setDisable(true);
            }
        });
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.show();
    }
}