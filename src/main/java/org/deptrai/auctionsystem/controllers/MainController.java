package org.deptrai.auctionsystem.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.deptrai.auctionsystem.models.auction.Auction;
import org.deptrai.auctionsystem.models.auction.AuctionManager;
import org.deptrai.auctionsystem.models.items.ArtFactory;
import org.deptrai.auctionsystem.models.items.ElectronicsFactory;
import org.deptrai.auctionsystem.models.items.Item;
import org.deptrai.auctionsystem.models.items.ItemFactory;
import org.deptrai.auctionsystem.models.users.Seller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public class MainController {

    @FXML
    private FlowPane productsContainer;

    @FXML
    public void initialize() {
        // 1. Tạo Mock Data (Dữ liệu giả)
        createMockData();

        // 2. Lấy dữ liệu từ Manager và in ra giao diện
        loadAuctionsToGrid();
    }

    @FXML
    public void handleAuctionFloor(javafx.event.ActionEvent event) {
        System.out.println("Đã bấm nút chuyển sang Sàn Đấu Giá");
        // Tương lai có thể dùng SceneManager đổi nội dung ở giữa tại đây
    }

    @FXML
    public void handleInventory(javafx.event.ActionEvent event) {
        System.out.println("Đã bấm nút chuyển sang Quản lý Kho");
        // Tương lai gọi SceneManager nạp inventory-view.fxml
    }

    // Các hàm cho Header (nếu main-view.fxml của bạn có chứa Header từ source 6)
    @FXML
    public void handleLogin(javafx.event.ActionEvent event) {
        System.out.println("Đã bấm Đăng nhập");
    }

    @FXML
    public void handleLogout(javafx.event.ActionEvent event) {
        System.out.println("Đã bấm Đăng xuất");
    }

    @FXML
    public void handleShowProfile(javafx.event.ActionEvent event) {
        System.out.println("Đã bấm Xem Hồ sơ");
    }

    @FXML
    public void handleShowBidHistory(javafx.event.ActionEvent event) {
        System.out.println("Đã bấm Xem Lịch sử đặt giá");
    }

    private void loadAuctionsToGrid() {
        List<Auction> allAuctions = AuctionManager.getInstance().getAllAuctions();

        for (Auction auction : allAuctions) {
            try {
                // Tải file FXML của một thẻ sản phẩm
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/org.deptrai.auctionsystem.views/item-card.fxml"));
                VBox itemCard = loader.load();

                // Lấy Controller của thẻ đó và truyền dữ liệu vào
                ItemCardController cardController = loader.getController();
                cardController.setData(auction);

                // Thêm thẻ vào FlowPane
                productsContainer.getChildren().add(itemCard);

            } catch (IOException e) {
                System.err.println("Lỗi khi nạp item-card.fxml: " + e.getMessage());
            }
        }
    }

    // Giả lập tạo các phiên đấu giá mẫu để test giao diện
    private void createMockData() {
        // Chỉ tạo giả nếu danh sách đang trống để tránh bị lặp khi chuyển đi chuyển lại giữa các Scene
        if (AuctionManager.getInstance().getAllAuctions().isEmpty()) {
            Seller mockSeller = new Seller("seller1", "pass123", "seller@uet.edu.vn");

            ItemFactory artFactory = new ArtFactory();
            ItemFactory elecFactory = new ElectronicsFactory();

            Item item1 = artFactory.createItem("Tranh Mona Lisa (Bản rep 1:1)", "Tranh sơn dầu tái bản chất lượng cao", 5000.0, mockSeller);
            Item item2 = elecFactory.createItem("MacBook Pro M3 Max", "Máy tính xách tay cấu hình cao cấp nhất, bảo hành 12 tháng", 2500.0, mockSeller);
            Item item3 = elecFactory.createItem("PlayStation 5 Pro", "Máy chơi game thế hệ mới nguyên seal", 800.0, mockSeller);

            // Thời gian kết thúc sau 2 giờ, 1 ngày, và thời gian ở quá khứ (để test hết hạn)
            AuctionManager.getInstance().createAuction(item1, LocalDateTime.now().plusHours(2));
            AuctionManager.getInstance().createAuction(item2, LocalDateTime.now().plusDays(1));
            AuctionManager.getInstance().createAuction(item3, LocalDateTime.now().minusHours(1));
        }
    }
}