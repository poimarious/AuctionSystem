package org.deptrai.auctionsystem.client.controllers;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.deptrai.auctionsystem.client.utils.SessionManager;
import org.deptrai.auctionsystem.client.utils.SocketClient;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.auction.AuctionStatus;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.shared.network.Message;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class TransactionHistoryController {

  // Khai báo các thành phần giao diện từ FXML
  @FXML private TableView<Auction> revenueTable;
  @FXML private TableColumn<Auction, String> colDate;
  @FXML private TableColumn<Auction, String> colOrderId;
  @FXML private TableColumn<Auction, String> colProduct;
  @FXML private TableColumn<Auction, String> colBuyer;
  @FXML private TableColumn<Auction, String> colFinalPrice;
  @FXML private TableColumn<Auction, String> colStatus;

  @FXML
  public void initialize() {
    // 1. Cấu hình cách lấy dữ liệu cho từng cột
    setupTableColumns();

    // 2. Gọi Server lấy dữ liệu đổ vào bảng
    loadTransactionHistory();
  }

  private void setupTableColumns() {
    // 1. Ngày giao dịch (Lấy thời gian kết thúc của phiên đấu giá)
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    colDate.setCellValueFactory(cellData -> {
      if (cellData.getValue().getEndTime() != null) {
        return new SimpleStringProperty(cellData.getValue().getEndTime().format(formatter));
      }
      return new SimpleStringProperty("N/A");
    });

    // 2. Mã đơn hàng (Lấy trực tiếp Auction ID)
    colOrderId.setCellValueFactory(new PropertyValueFactory<>("auctionId"));

    // 3. Tên sản phẩm (Lấy từ Item)
    colProduct.setCellValueFactory(cellData -> {
      if (cellData.getValue().getItem() != null) {
        return new SimpleStringProperty(cellData.getValue().getItem().getName());
      }
      return new SimpleStringProperty("N/A");
    });

    // 4. Người mua
    colBuyer.setCellValueFactory(cellData -> {
      if (cellData.getValue().getWinner() != null) {
        return new SimpleStringProperty(cellData.getValue().getWinner().getUsername());
      }
      return new SimpleStringProperty("N/A");
    });

    // 5. Giá chốt
    colFinalPrice.setCellValueFactory(cellData ->
        new SimpleStringProperty(String.format("$%,.2f", cellData.getValue().getCurrentPrice()))
    );

    // 6. Trạng thái
    colStatus.setCellValueFactory(cellData -> new SimpleStringProperty("Hoàn tất"));
  }

  private void loadTransactionHistory() {
    User currentUser = SessionManager.getInstance().getCurrentUser();
    if (currentUser == null) return;

    SocketClient.runAsync(() -> {
      // Gửi Message yêu cầu lấy danh sách sản phẩm
      Message request = new Message("REQUEST", "GET_SELLER_AUCTIONS", currentUser.getUserId());
      Message response = SocketClient.sendRequest(request);

      if (response != null && "SUCCESS".equals(response.getStatus())) {
        List<Auction> allAuctions = (List<Auction>) response.getData();

        // LỌC DỮ LIỆU: Chỉ giữ lại những phiên đã KẾT THÚC và CÓ NGƯỜI THẮNG
        List<Auction> completedTransactions = allAuctions.stream()
            .filter(a -> a.getStatus() == AuctionStatus.FINISHED && a.getWinner() != null)
            .collect(Collectors.toList());

        // Đẩy dữ liệu vào JavaFX Thread để cập nhật giao diện
        Platform.runLater(() -> {
          revenueTable.setItems(FXCollections.observableArrayList(completedTransactions));
        });
      }
    });
  }
}
