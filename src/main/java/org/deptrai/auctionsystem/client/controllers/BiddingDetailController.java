package org.deptrai.auctionsystem.client.controllers;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.auction.AuctionStatus;
import org.deptrai.auctionsystem.shared.models.bid.Bid;
import org.deptrai.auctionsystem.shared.observer.AuctionObserver;
import org.deptrai.auctionsystem.shared.models.users.Bidder;

import java.time.LocalDateTime;


public class BiddingDetailController implements AuctionObserver {

    // --- 1. KẾT NỐI LINH KIỆN FXML ---
    @FXML
    private ImageView productImageView;
    @FXML
    private Label nameLabel;
    @FXML
    private Label descriptionLabel;
    @FXML
    private Label currentPriceLabel;

    @FXML
    private TableView<Bid> bidHistoryTable;
    @FXML
    private TableColumn<Bid, String> timeColumn;
    @FXML
    private TableColumn<Bid, String> bidderColumn;
    @FXML
    private TableColumn<Bid, Double> amountColumn;

    @FXML
    private TextField bidAmountField;
    @FXML
    private TextField maxBidField;
    @FXML
    private TextField incrementField;

    // Đối tượng phiên đấu giá hiện tại đang quản lý
    private Auction currentAuction;

    // --- 2. HÀM KHỞI TẠO (CHẠY TỰ ĐỘNG) ---
    @FXML
    public void initialize() {
        // Cấu hình kết nối dữ liệu cho các cột của bảng
        // "amount" và "timestamp" phải khớp với tên thuộc tính trong lớp Bid.java
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        // Cấu hình cột người đặt (Lấy username từ đối tượng Bidder bên trong Bid)
        bidderColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getBidder() != null) {
                return new SimpleStringProperty(cellData.getValue().getBidder().getUsername());
            }
            return new SimpleStringProperty("N/A");
        });

        System.out.println("GUI: Đã khởi tạo cấu hình bảng lịch sử đấu giá.");
    }

    // --- 3. THIẾT LẬP DỮ LIỆU BAN ĐẦU ---
    public void setAuctionData(Auction auction) {
        this.currentAuction = auction;

        // Hiển thị thông tin sản phẩm
        if (auction.getItem() != null) {
            nameLabel.setText(auction.getItem().getName());
            descriptionLabel.setText(auction.getItem().getDescription());
        }

        // Hiển thị giá hiện tại ban đầu
        currentPriceLabel.setText(String.format("$%.2f", auction.getCurrentPrice()));

        // Đổ dữ liệu lịch sử giá đã có vào bảng
        bidHistoryTable.getItems().setAll(auction.getBids());

        // QUAN TRỌNG: Đăng ký Controller này làm "Người theo dõi" phiên đấu giá
        this.currentAuction.attach(this);
        System.out.println("GUI: Đã kết nối và đăng ký theo dõi phiên: " + auction.getItem().getName());
    }

    // --- 4. XỬ LÝ SỰ KIỆN ĐẶT GIÁ ---
    @FXML
    private void handlePlaceBid() {
        try {
            double amount = Double.parseDouble(bidAmountField.getText());

            // Ở phiên bản hoàn thiện, bạn nên lấy Bidder từ Session đăng nhập
            Bidder currentBidder = null;

            Bid newBid = new Bid(currentBidder, currentAuction, amount, LocalDateTime.now());
            currentAuction.placeBid(newBid);

            // Xóa nội dung ô nhập sau khi đặt thành công
            bidAmountField.clear();

        } catch (NumberFormatException e) {
            showErrorAlert("Vui lòng nhập số tiền hợp lệ!");
        } catch (Exception e) {
            showErrorAlert(e.getMessage());
        }
    }

    // --- 5. CẬP NHẬT GIAO DIỆN THỜI GIAN THỰC (OBSERVER METHODS) ---
    @Override
    public void onBidPlaced(Auction a, Bid b) {
        // Sử dụng Platform.runLater để cập nhật giao diện từ luồng phụ an toàn
        Platform.runLater(() -> {
            // Cập nhật nhãn giá tiền
            currentPriceLabel.setText(String.format("$%.2f", a.getCurrentPrice()));

            // Thêm dòng mới vào bảng và tự động cuộn xuống
            bidHistoryTable.getItems().add(b);
            bidHistoryTable.scrollTo(b);

            System.out.println("GUI: Đã nhận thông báo và cập nhật giá mới: $" + b.getAmount());
        });
    }

    @Override
    public void onAuctionStatusChanged(Auction a) {
        Platform.runLater(() -> {
            // Khóa nút đặt giá nếu phiên đã kết thúc hoặc bị hủy
            if (a.getStatus() == AuctionStatus.FINISHED || a.getStatus() == AuctionStatus.CANCELED) {
                bidAmountField.setDisable(true);

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Thông báo");
                alert.setHeaderText(null);
                alert.setContentText("Phiên đấu giá '" + a.getItem().getName() + "' đã kết thúc!");
                alert.showAndWait();
            }
        });
    }

    // --- HÀM TIỆN ÍCH ---
    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
