package org.deptrai.auctionsystem.client.controllers;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.deptrai.auctionsystem.client.utils.SceneManager;
import org.deptrai.auctionsystem.client.utils.SessionManager;
import org.deptrai.auctionsystem.client.utils.SocketClient;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.shared.network.Message;

import java.util.List;

// CÁC THƯ VIỆN BỔ SUNG CHO NÚT XÓA

public class InventoryController {

  @FXML private TableView<Auction> inventoryTable;
  @FXML private TableColumn<Auction, String> idColumn;
  @FXML private TableColumn<Auction, String> nameColumn;
  @FXML private TableColumn<Auction, String> priceColumn; // Đổi sang String để format có dấu $
  @FXML private TableColumn<Auction, String> statusColumn;
//  @FXML private TableColumn<Auction, String> actionColumn;
@FXML private TableColumn<Auction, String> finalPriceColumn;

  @FXML
  public void handleAddNewProduct() {
    // Chuyển sang trang Đăng sản phẩm mới
    SceneManager.getInstance()
        .switchScene(
            "/org/deptrai/auctionsystem/client/views/add-product-view.fxml",
            "Đăng sản phẩm đấu giá mới");
  }


  private void loadMyAuctions() {
    // 1. Lấy user hiện tại đang đăng nhập
    User currentUser = SessionManager.getInstance().getCurrentUser();

    if (currentUser == null) return;

    // 2. Chạy luồng phụ để hỏi Server

    SocketClient.runAsync(() -> {
      // Gửi Message kèm theo ID của Seller
      Message request = new Message("REQUEST", "GET_SELLER_AUCTIONS", currentUser.getUserId());
      Message response = SocketClient.sendRequest(request);

      if ("SUCCESS".equals(response.getStatus())) {
        // Ép kiểu dữ liệu trả về
        @SuppressWarnings("unchecked")
        List<Auction> myAuctions = (List<Auction>) response.getData();
        // 3. Đưa vào luồng chính JavaFX để hiển thị
        Platform.runLater(() -> {
          // ĐỔ DỮ LIỆU VÀO BẢNG Ở ĐÂY
          if (inventoryTable != null) {
            inventoryTable.getItems().setAll(myAuctions);
            System.out.println("Đã tải thành công " + myAuctions.size() + " sản phẩm vào kho.");
          }
        });

      } else {
        Platform.runLater(() -> {
          String errorMsg = response.getData() instanceof String
                  ? (String) response.getData()
                  : "Lỗi kết nối mạng!";
          System.out.println("Lỗi tải kho hàng: " + errorMsg);
        });
      }
    });
  }

  @FXML
  public void handleGoBack() {
    SceneManager.getInstance().goBack();
  }

  @FXML
  public void initialize() {
    // 1. Cột ID: Lấy trực tiếp auctionId
    idColumn.setCellValueFactory(new PropertyValueFactory<>("auctionId"));

    // 2. Cột Tên sản phẩm: Lấy gián tiếp qua Item (Rất quan trọng!)
    nameColumn.setCellValueFactory(cellData -> {
      if (cellData.getValue().getItem() != null) {
        return new SimpleStringProperty(cellData.getValue().getItem().getName());
      }
      return new SimpleStringProperty("N/A");
    });

    // 3. Cột Giá: Ép kiểu sang chuỗi và format thêm dấu $
    priceColumn.setCellValueFactory(cellData ->
        new SimpleStringProperty(String.format("$%,.2f", cellData.getValue().getItem().getStartingPrice()))
    );
    // Cột Giá cuối cùng: Lấy giá đấu cao nhất hiện tại (currentPrice)
    finalPriceColumn.setCellValueFactory(cellData ->
        new SimpleStringProperty(String.format("$%,.2f", cellData.getValue().getCurrentPrice()))
    );

    // 4. Cột Trạng thái
    statusColumn.setCellValueFactory(cellData ->
        new SimpleStringProperty(cellData.getValue().getStatus().toString())
    );

    // 5. Nạp dữ liệu từ Server sau khi đã setup xong các cột
    loadMyAuctions();
  }
}