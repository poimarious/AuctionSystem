//package org.deptrai.auctionsystem.views;
//
//import javafx.fxml.FXML;
//import javafx.fxml.FXMLLoader;
//import javafx.scene.layout.StackPane;
//import javafx.scene.Parent;
//import java.io.IOException;
//
//public class MainController {
//
//    @FXML
//    private StackPane contentArea; // Đây là cái ô trống CENTER chúng ta tạo lúc đầu
//
//    // Hàm này để đổi nội dung ở giữa màn hình mà không làm mất Menu bên trái
//    @FXML
//    private void showBiddingFloor() {
//        loadView("/bidding-detail.fxml");
//    }
//
//    private void loadView(String fxmlFile) {
//        try {
//            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
//            contentArea.getChildren().removeAll();
//            contentArea.getChildren().setAll(root);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//}
package org.deptrai.auctionsystem.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.deptrai.auctionsystem.models.auction.Auction;

import java.io.IOException;
import java.net.URL;

public class MainController {

    // --- 1. CHUYỂN SANG SÀN ĐẤU GIÁ --
    @FXML
    private void handleAuctionFloor(ActionEvent event) {
        // Chuyển sang màn hình bidding-detail.fxml (Trang bạn vừa làm xong)
        changeScene(event, "/org.deptrai.auctionsystem.views/bidding-detail.fxml", "Sàn Đấu Giá");
    }

    // --- 2. CHUYỂN SANG QUẢN LÝ KHO ---
    @FXML
    private void handleInventory(ActionEvent event) {
        // Chuyển sang màn hình quản lý kho
        changeScene(event, "/inventory-view.fxml", "Quản Lý Kho");
    }

    // --- 3. CHUYỂN SANG CHI TIẾT (CÓ TRUYỀN DỮ LIỆU) ---
    /**
     * Hàm này dùng khi bạn chọn một món hàng cụ thể và muốn xem chi tiết.
     * Nó sẽ truyền đối tượng Auction sang cho BiddingDetailController.
     */
    public void goToAuctionDetail(ActionEvent event, Auction selectedAuction) {
        try {
            URL url = getClass().getResource("/org.deptrai.auctionsystem.views/bidding-detail.fxml");
            if (url == null) {
                throw new IOException("Không tìm thấy file: /bidding-detail.fxml");
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            // Lấy controller của trang đích để đổ dữ liệu vào
            BiddingDetailController detailController = loader.getController();
            detailController.setAuctionData(selectedAuction);

            // Thực hiện thay đổi màn hình
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Chi tiết đấu giá: " + selectedAuction.getItem().getName());
            stage.show();

        } catch (IOException e) {
            showError("Lỗi hệ thống", "Không thể mở trang chi tiết: " + e.getMessage());
        }
    }

    /**
     * HÀM DÙNG CHUNG: Để chuyển các trang đơn giản (không cần truyền dữ liệu)
     */
    private void changeScene(ActionEvent event, String fxmlPath, String title) {
        try {
            URL url = getClass().getResource(fxmlPath);
            if (url == null) {
                throw new IOException("Đường dẫn file không tồn tại: " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            // Lấy Stage (Cửa sổ) hiện tại từ sự kiện click
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.centerOnScreen(); // Đưa cửa sổ ra giữa màn hình
            stage.show();

            System.out.println("Chuyển trang thành công: " + title);

        } catch (IOException e) {
            showError("Lỗi chuyển trang", "Chi tiết: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Hàm hiển thị thông báo lỗi nhanh
     */
    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Thông báo");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
