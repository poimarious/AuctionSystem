package org.deptrai.auctionsystem.client.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import org.deptrai.auctionsystem.client.utils.SceneManager;
import org.deptrai.auctionsystem.client.utils.SessionManager;
import org.deptrai.auctionsystem.client.utils.SocketClient;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.shared.network.Message;

import java.util.List;

public class InventoryController {

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
      Message request = new Message("GET_SELLER_AUCTIONS", currentUser.getUserId());
      Message response = SocketClient.sendRequest(request);

      if ("SUCCESS".equals(response.getStatus())) {
        // Ép kiểu dữ liệu trả về
        List<Auction> myAuctions = (List<Auction>) response.getData();

        // 3. Đưa vào luồng chính JavaFX để hiển thị
        Platform.runLater(() -> {
          System.out.println("Tải thành công " + myAuctions.size() + " món hàng của tôi.");

          // TODO: Gắn dữ liệu vào giao diện của bạn tại đây
          // Ví dụ nếu dùng Bảng:
          // ObservableList<Auction> data = FXCollections.observableArrayList(myAuctions);
          // inventoryTable.setItems(data);
        });

      } else {
        Platform.runLater(() -> {
          System.out.println("Lỗi: " + response.getData());
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
    System.out.println("Đã load kho hàng của Seller!");
  }
}
