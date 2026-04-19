package org.deptrai.auctionsystem.views;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;

public class BiddingDetailController {

    // 1. Kết nối với các linh kiện bên Trái
    @FXML private ImageView productImageView;
    @FXML private Label nameLabel;
    @FXML private Label descriptionLabel;

    // 2. Kết nối với các linh kiện bên Phải
    @FXML private TableView<?> bidHistoryTable;
    @FXML private TextField bidAmountField;
    @FXML private TextField maxBidField;
    @FXML private TextField incrementField;

    // 3. Hành động khi ấn nút Đặt giá
    @FXML
    private void handlePlaceBid() {
        String amount = bidAmountField.getText();
        if (amount.isEmpty()) {
            System.out.println("Vui lòng nhập số tiền!");
        }
        else {
            System.out.println("Đã ghi nhận mức giá mới: " + amount);
            // Sau này logic xử lý tiền tệ sẽ nằm ở đây
        }
    }
}