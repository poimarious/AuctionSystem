package org.deptrai.auctionsystem.client.controllers;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import org.deptrai.auctionsystem.client.utils.SceneManager;
import org.deptrai.auctionsystem.client.utils.SessionManager;
import org.deptrai.auctionsystem.client.utils.SocketClient;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.shared.network.Message;

import javafx.scene.control.TableView;
// CÁC THƯ VIỆN BỔ SUNG CHO NÚT XÓA
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.geometry.Pos;
import java.util.Optional;
import java.util.List;

public class InventoryController {

  @FXML private TableView<Auction> inventoryTable;
  @FXML private TableColumn<Auction, String> idColumn;
  @FXML private TableColumn<Auction, String> nameColumn;
  @FXML private TableColumn<Auction, String> priceColumn; // Đổi sang String để format có dấu $
  @FXML private TableColumn<Auction, String> statusColumn;
  @FXML private TableColumn<Auction, Void> actionColumn;

  @FXML
  public void handleAddNewProduct(ActionEvent event) {
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
    new Thread(() -> {
      // Gửi Message kèm theo ID của Seller
      Message request = new Message("REQUEST", "GET_SELLER_AUCTIONS", currentUser.getUserId());
      Message response = SocketClient.sendRequest(request);

      if (response != null && "SUCCESS".equals(response.getStatus())) {
        // Ép kiểu dữ liệu trả về
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
          String errorMsg = (response != null && response.getData() instanceof String)
              ? (String) response.getData()
              : "Lỗi kết nối mạng!";
          System.out.println("Lỗi tải kho hàng: " + errorMsg);
        });
      }
    }).start();
  }

  @FXML
  public void handleGoBack(ActionEvent event) {
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
        new SimpleStringProperty(String.format("$%,.2f", cellData.getValue().getCurrentPrice()))
    );

    // 4. Cột Trạng thái
    statusColumn.setCellValueFactory(cellData ->
        new SimpleStringProperty(cellData.getValue().getStatus().toString())
    );

    // BỔ SUNG: Gọi hàm thiết lập cột thao tác (chứa nút Xóa)
    setupActionColumn();

    // 5. Nạp dữ liệu từ Server sau khi đã setup xong các cột
    loadMyAuctions();
  }

  // =========================================================
  // PHẦN CODE BỔ SUNG CHO TÍNH NĂNG XÓA SẢN PHẨM
  // =========================================================

  private void setupActionColumn() {
    actionColumn.setCellFactory(param -> new TableCell<>() {
      private final Button deleteBtn = new Button("Xóa");

      {
        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        deleteBtn.setOnAction(event -> {
          Auction auction = getTableView().getItems().get(getIndex());
          handleDeleteAuction(auction);
        });
      }

      @Override
      protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
          setGraphic(null);
        } else {
          setGraphic(deleteBtn);
          setAlignment(Pos.CENTER);
        }
      }
    });
  }

  private void handleDeleteAuction(Auction auction) {
    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
    confirmAlert.setTitle("Xác nhận xóa");
    confirmAlert.setHeaderText("Xóa phiên đấu giá: " + auction.getItem().getName());
    confirmAlert.setContentText("Bạn có chắc chắn muốn xóa không? Hành động này không thể hoàn tác.");

    Optional<ButtonType> result = confirmAlert.showAndWait();
    if (result.isPresent() && result.get() == ButtonType.OK) {

      new Thread(() -> {
        Message request = new Message("REQUEST", "DELETE_AUCTION", auction.getAuctionId());
        Message response = SocketClient.sendRequest(request);

        Platform.runLater(() -> {
          if (response != null && "SUCCESS".equals(response.getStatus())) {
            inventoryTable.getItems().remove(auction);
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION, "Đã xóa sản phẩm thành công!");
            successAlert.show();
          } else {
            String errorMsg = (response != null && response.getData() instanceof String)
                ? (String) response.getData() : "Lỗi Server";
            Alert errorAlert = new Alert(Alert.AlertType.ERROR, "Không thể xóa: " + errorMsg);
            errorAlert.show();
          }
        });
      }).start();
    }
  }
}