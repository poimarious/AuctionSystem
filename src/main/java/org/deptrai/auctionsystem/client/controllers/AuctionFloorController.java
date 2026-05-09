package org.deptrai.auctionsystem.client.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.server.managers.AuctionManager;
import org.deptrai.auctionsystem.shared.models.items.ArtFactory;
import org.deptrai.auctionsystem.shared.models.items.ElectronicsFactory;
import org.deptrai.auctionsystem.shared.models.items.Item;
import org.deptrai.auctionsystem.shared.models.items.ItemFactory;
import org.deptrai.auctionsystem.shared.models.items.VehicleFactory;
import org.deptrai.auctionsystem.shared.models.users.Seller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public class AuctionFloorController {

    @FXML
    private FlowPane productsContainer;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> categoryCombo;

    @FXML
    private ComboBox<String> statusCombo;

    @FXML
    public void handleGoBack(javafx.event.ActionEvent event) {
        org.deptrai.auctionsystem.server.utils.SceneManager.getInstance().goBack();
    }

    @FXML
    public void initialize() {
        // 1. Khởi tạo các lựa chọn cho ComboBox bộ lọc
        if (categoryCombo != null) {
            categoryCombo.getItems().addAll("Tất cả", "Art", "Electronics", "Vehicle");
            categoryCombo.getSelectionModel().selectFirst();
        }
        if (statusCombo != null) {
            statusCombo.getItems().addAll("Tất cả", "Đang diễn ra", "Sắp kết thúc");
            statusCombo.getSelectionModel().selectFirst();
        }

        // 2. Tạo dữ liệu mẫu (Mock Data) để test giao diện
        createMockData();

        // 3. Load danh sách sản phẩm lên sàn
        loadAuctions();
    }

    private void createMockData() {
        // Chỉ tạo dữ liệu nếu danh sách đang trống để tránh bị lặp (nhân bản) khi chuyển đi chuyển lại giữa các màn hình
        if (AuctionManager.getInstance().getAllAuctions().isEmpty()) {

            // Tạo một Seller ảo
            Seller mockSeller = new Seller("SELLER_MOCK_01", "Nguyễn Văn Bán", "pass123", "seller@uet.edu.vn");

            // Sử dụng Factory Pattern để tạo Item
            ItemFactory artFactory = new ArtFactory();
            ItemFactory elecFactory = new ElectronicsFactory();
            ItemFactory vehicleFactory = new VehicleFactory();

            // Tạo 3 sản phẩm mẫu
            Item art = artFactory.createItem("Tranh Đêm Đầy Sao (Bản sao 1:1)", "Tranh sơn dầu chất lượng cao, phục chế hoàn hảo.", 1500.0, mockSeller);
            Item laptop = elecFactory.createItem("MacBook Pro M3 Max", "Máy tính xách tay cấu hình cao cấp nhất, RAM 128GB.", 4200.0, mockSeller);
            Item car = vehicleFactory.createItem("Porsche 911 GT3 RS", "Siêu xe thể thao đời 2024, mới đi được 5000 dặm.", 250000.0, mockSeller);

            // Đưa sản phẩm lên sàn đấu giá với các mốc thời gian kết thúc khác nhau
            AuctionManager.getInstance().createAuction(art, LocalDateTime.now().plusDays(2));
            AuctionManager.getInstance().createAuction(laptop, LocalDateTime.now().plusHours(5));
            AuctionManager.getInstance().createAuction(car, LocalDateTime.now().plusDays(7));

            // Chuyển trạng thái của tất cả các phiên đấu giá mẫu sang RUNNING
            for(Auction a : AuctionManager.getInstance().getAllAuctions()) {
                a.startAuction();
            }
        }
    }

    private void loadAuctions() {
        productsContainer.getChildren().clear();
        List<Auction> allAuctions = AuctionManager.getInstance().getAllAuctions();

        for (Auction auction : allAuctions) {
            try {
                // Nạp file giao diện của thẻ sản phẩm
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/org.deptrai.auctionsystem.views/item-card.fxml"));
                Node itemCard = loader.load();

                // Lấy controller của card để truyền dữ liệu (ItemCardController đã được viết ở các bước trước)
                ItemCardController cardController = loader.getController();
                if (cardController != null) {
                    cardController.setData(auction);
                }

                // Nhét thẻ sản phẩm vào FlowPane
                productsContainer.getChildren().add(itemCard);

            } catch (IOException e) {
                System.err.println("Lỗi nạp item-card trên sàn: " + e.getMessage());
            }
        }
    }
}