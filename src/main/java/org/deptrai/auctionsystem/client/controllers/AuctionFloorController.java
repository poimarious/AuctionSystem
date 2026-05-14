package org.deptrai.auctionsystem.client.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import org.deptrai.auctionsystem.client.utils.SceneManager;
import org.deptrai.auctionsystem.client.utils.SocketClient;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.network.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AuctionFloorController {

  @FXML private FlowPane productsContainer;
  @FXML private TextField searchField;
  @FXML private ComboBox<String> categoryCombo;
  @FXML private ComboBox<String> statusCombo;
  private List<Auction> allAuctionsList = new ArrayList<>();

  /**
   * 1. SỬA NÚT QUAY LẠI: Ép về thẳng Home View
   */
  @FXML
  public void handleGoBack(ActionEvent event) {
    SceneManager.getInstance().switchScene(
        "/org/deptrai/auctionsystem/client/views/home-view.fxml",
        "Trang chủ - Auction.UET"
    );
  }

  @FXML
  public void initialize() {
    // Khởi tạo các ComboBox (Giữ nguyên logic của bạn)
    if (categoryCombo != null) {
      categoryCombo.getItems().addAll("Tất cả", "Art", "Electronics", "Vehicle");
      categoryCombo.getSelectionModel().selectFirst();
      categoryCombo.setOnAction(event -> filterAuctions());
    }
    if (statusCombo != null) {
      statusCombo.getItems().addAll("Tất cả", "Đang diễn ra", "Sắp kết thúc");
      statusCombo.getSelectionModel().selectFirst();
    }

    // 2. SỬA CÁCH LOAD DỮ LIỆU: Lấy từ Server thay vì AuctionManager
    loadAuctionsFromServer();
  }

  private void loadAuctionsFromServer() {
    // Gửi yêu cầu qua Socket
    Message request = new Message("REQUEST", "GET_ALL_AUCTIONS", null);

    new Thread(() -> {
      Message response = SocketClient.sendRequest(request);
      if ("SUCCESS".equals(response.getStatus())) {
        List<Auction> allAuctions = (List<Auction>) response.getData();
        //1
        allAuctionsList = allAuctions;

        // Cập nhật giao diện an toàn trên luồng UI
        Platform.runLater(() -> displayAuctions(allAuctions));
      }
    }).start();
  }
  // Hàm lọc dữ liệu nội bộ trên RAM
  private void filterAuctions() {
    String selectedCategory = categoryCombo.getValue();

    // Nếu chọn "Tất cả"  hiển thị lại toàn bộ kho gốc
    if (selectedCategory == null || selectedCategory.equals("Tất cả")) {
      displayAuctions(allAuctionsList);
      return;
    }

    // chọn loại
    List<Auction> filteredList = new ArrayList<>();

    // Lục lọi trong kho gốc
    for (Auction auction : allAuctionsList) {
      if (auction.getItem() != null && auction.getItem().getCategory() != null) {
        // Khớp thì nhặt bỏ vào giỏ
        if (auction.getItem().getCategory().equalsIgnoreCase(selectedCategory)) {
          filteredList.add(auction);
        }
      }
    }

    // Vẽ lại màn hình với danh sách đã lọc
    displayAuctions(filteredList);
  }

  private void displayAuctions(List<Auction> auctions) {
    productsContainer.getChildren().clear();
    for (Auction auction : auctions) {
      try {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/org/deptrai/auctionsystem/client/views/item-card.fxml"));
        Node itemCard = loader.load();

        ItemCardController cardController = loader.getController();
        if (cardController != null) {
          cardController.setData(auction);
        }

        productsContainer.getChildren().add(itemCard);
      } catch (IOException e) {
        System.err.println("Lỗi nạp item-card trên sàn: " + e.getMessage());
      }
    }
  }
}