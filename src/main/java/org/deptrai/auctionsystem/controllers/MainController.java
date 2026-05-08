package org.deptrai.auctionsystem.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import org.deptrai.auctionsystem.models.auction.Auction;
import org.deptrai.auctionsystem.models.auction.AuctionManager;
import org.deptrai.auctionsystem.utils.SceneManager;

import java.io.IOException;
import java.util.List;

public class MainController {

    // --- CÁC THÀNH PHẦN GIAO DIỆN ---
    @FXML
    private HBox guestBox;

    @FXML
    private HBox userBox;

    @FXML
    private Label userNameLabel;

    @FXML
    private Label walletLabel;

    @FXML
    private MenuButton userMenu;

    @FXML
    private FlowPane productsContainer;

    @FXML
    public void initialize() {
        // Kiểm tra xem có ai đang đăng nhập không
        org.deptrai.auctionsystem.models.users.User currentUser = org.deptrai.auctionsystem.utils.SessionManager.getInstance().getCurrentUser();

        if (currentUser != null) {
            // Nếu có người đăng nhập, hiển thị giao diện User
            setUpUserView(currentUser.getUsername(), 0.0);
        } else {
            // Nếu không, hiển thị giao diện Khách
            setUpGuestView();
        }

        // Load tối đa 6 sản phẩm nổi bật lên trang chủ
        loadFeaturedAuctions();
    }

    // --- LOGIC GIAO DIỆN NGƯỜI DÙNG ---

    private void setUpGuestView() {
        guestBox.setVisible(true);
        guestBox.setManaged(true);

        userBox.setVisible(false);
        userBox.setManaged(false);
    }

    public void setUpUserView(String username, double balance) {
        guestBox.setVisible(false);
        guestBox.setManaged(false);

        userBox.setVisible(true);
        userBox.setManaged(true);

        userNameLabel.setText(username);
        walletLabel.setText(String.format("Ví: $%,.2f", balance));
    }

    private void loadFeaturedAuctions() {
        productsContainer.getChildren().clear();

        List<Auction> allAuctions = AuctionManager.getInstance().getAllAuctions();
        int limit = Math.min(allAuctions.size(), 6);

        for (int i = 0; i < limit; i++) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/org.deptrai.auctionsystem.views/item-card.fxml"));
                Node itemCard = loader.load();

                ItemCardController cardController = loader.getController();
                if (cardController != null) {
                    cardController.setData(allAuctions.get(i));
                }

                productsContainer.getChildren().add(itemCard);

            } catch (IOException e) {
                System.err.println("Lỗi nạp item-card trên trang chủ: " + e.getMessage());
            }
        }
    }

    // --- CÁC HÀM ĐIỀU HƯỚNG ---

    @FXML
    public void handleLogin(ActionEvent event) {
        SceneManager.getInstance().switchScene("/org.deptrai.auctionsystem.views/login-view.fxml", "Hệ thống Đấu giá - Đăng nhập");
    }

    @FXML
    public void handleRegister(ActionEvent event) {
        SceneManager.getInstance().switchScene("/org.deptrai.auctionsystem.views/register-view.fxml", "Hệ thống Đấu giá - Đăng ký");
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        System.out.println("Đăng xuất thành công!");

        org.deptrai.auctionsystem.utils.SessionManager.getInstance().logout();

        SceneManager.getInstance().clearHistory();
        setUpGuestView();

        SceneManager.getInstance().switchScene("/org.deptrai.auctionsystem.views/login-view.fxml", "Đăng nhập - Auction.UET");
    }

    @FXML
    public void handleShowProfile(ActionEvent event) {
        // Dẫn đến trang cấu hình tài khoản cá nhân
        SceneManager.getInstance().switchScene("/org.deptrai.auctionsystem.views/profile-view.fxml", "Hồ sơ của tôi");
    }

    @FXML
    public void handleShowBidHistory(ActionEvent event) {
        // Dẫn đến trang xem các phiên đã từng đặt giá
        SceneManager.getInstance().switchScene("/org.deptrai.auctionsystem.views/bid-history-view.fxml", "Lịch sử đặt giá");
    }

    @FXML
    public void handleLearnMore(ActionEvent event) {
        // Nút "Tìm hiểu thêm" sẽ dẫn người dùng thẳng vào Sàn Đấu Giá để xem toàn bộ sản phẩm
        SceneManager.getInstance().switchScene("/org.deptrai.auctionsystem.views/auction-floor-view.fxml", "Sàn Đấu Giá - Auction.UET");
    }
}