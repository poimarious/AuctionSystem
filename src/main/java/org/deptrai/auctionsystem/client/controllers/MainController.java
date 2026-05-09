package org.deptrai.auctionsystem.client.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import org.deptrai.auctionsystem.client.utils.SessionManager;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.server.managers.AuctionManager;
import org.deptrai.auctionsystem.client.utils.SceneManager;

import java.io.IOException;
import java.util.List;
import org.deptrai.auctionsystem.shared.models.users.Seller;
import org.deptrai.auctionsystem.shared.models.users.User;

public class MainController {

    @FXML private HBox guestBox;
    @FXML private HBox userBox;
    @FXML private Label userNameLabel;
    @FXML private Label walletLabel;
    @FXML private MenuButton userMenu;
    @FXML private Button sellerCenterBtn;
    @FXML private FlowPane productsContainer;

    @FXML
    public void initialize() {
        User currentUser = SessionManager.getInstance().getCurrentUser();

        if (currentUser != null) {
            // Hiển thị số tiền thực tế của user thay vì 0.0
            setUpUserView(currentUser.getUsername(), currentUser.getBalance());
        } else {
            setUpGuestView();
        }

        loadFeaturedAuctions();

        // ĐĂNG KÝ NHẬN THÔNG BÁO KHI SỐ DƯ VÍ THAY ĐỔI
        SessionManager.getInstance().setBalanceListener(() -> {
            // Phải dùng Platform.runLater vì UI chỉ được cập nhật trên luồng chính (Main Thread)
            javafx.application.Platform.runLater(() -> {
                User user = SessionManager.getInstance().getCurrentUser();
                if (user != null) {
                    walletLabel.setText(String.format("Ví: $%,.2f", user.getBalance()));
                }
            });
        });
    }

    private void setUpGuestView() {
        guestBox.setVisible(true);
        guestBox.setManaged(true);

        userBox.setVisible(false);
        userBox.setManaged(false);

        if (sellerCenterBtn != null) {
            sellerCenterBtn.setVisible(false);
            sellerCenterBtn.setManaged(false);
        }
    }

    public void setUpUserView(String username, double balance) {
        guestBox.setVisible(false);
        guestBox.setManaged(false);

        userBox.setVisible(true);
        userBox.setManaged(true);

        userNameLabel.setText(username);
        walletLabel.setText(String.format("Ví: $%,.2f", balance));

        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser instanceof Seller) {
            sellerCenterBtn.setVisible(true);
            sellerCenterBtn.setManaged(true);
        } else {
            sellerCenterBtn.setVisible(false);
            sellerCenterBtn.setManaged(false);
        }
    }

    private void loadFeaturedAuctions() {
        productsContainer.getChildren().clear();

        List<Auction> allAuctions = AuctionManager.getInstance().getAllAuctions();
        int limit = Math.min(allAuctions.size(), 6);

        for (int i = 0; i < limit; i++) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/org/deptrai/auctionsystem/client/views/item-card.fxml"));
                Node itemCard = loader.load();

                ItemCardController cardController = loader.getController();
                if (cardController != null) {
                    cardController.setData(allAuctions.get(i));
                }

                productsContainer.getChildren().add(itemCard);

            } catch (IOException e) {
                System.err.println("Lỗi nạp item-card: " + e.getMessage());
            }
        }
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        SceneManager.getInstance().switchScene(
            "/org/deptrai/auctionsystem/client/views/login-view.fxml", "Đăng nhập");
    }

    @FXML
    public void handleRegister(ActionEvent event) {
        SceneManager.getInstance().switchScene(
            "/org/deptrai/auctionsystem/client/views/register-view.fxml", "Đăng ký");
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        SessionManager.getInstance().logout();
        SceneManager.getInstance().clearHistory();
        setUpGuestView();
        SceneManager.getInstance().switchScene(
            "/org/deptrai/auctionsystem/client/views/login-view.fxml", "Đăng nhập");
    }

    @FXML
    public void handleShowProfile(ActionEvent event) {
        SceneManager.getInstance().switchScene(
            "/org/deptrai/auctionsystem/client/views/profile-view.fxml", "Hồ sơ của tôi");
    }

    @FXML
    public void handleShowBidHistory(ActionEvent event) {
        SceneManager.getInstance().switchScene(
            "/org/deptrai/auctionsystem/client/views/bid-history-view.fxml", "Lịch sử đặt giá");
    }

    @FXML
    public void handleOpenAuctionFloor(ActionEvent event) {
        SceneManager.getInstance().switchScene(
            "/org/deptrai/auctionsystem/client/views/auction-floor-view.fxml", "Sàn Đấu Giá");
    }

    @FXML
    public void handleGoToSellerCenter(ActionEvent event) {
        SceneManager.getInstance().switchScene("/org/deptrai/auctionsystem/client/views/seller.fxml", "Kênh Người Bán");
    }
}