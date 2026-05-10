package org.deptrai.auctionsystem.client.controllers;

import java.io.IOException;
import java.util.List;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import org.deptrai.auctionsystem.client.utils.SceneManager;
import org.deptrai.auctionsystem.server.managers.AuctionManager;
import org.deptrai.auctionsystem.shared.models.auction.Auction;

public class AuctionFloorController {

  @FXML private FlowPane productsContainer;

  @FXML private TextField searchField;

  @FXML private ComboBox<String> categoryCombo;

  @FXML private ComboBox<String> statusCombo;

  @FXML
  public void handleGoBack(javafx.event.ActionEvent event) {
    SceneManager.getInstance().goBack();
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

    // 2. Load danh sách sản phẩm lên sàn
    loadAuctions();
  }

  private void loadAuctions() {
    productsContainer.getChildren().clear();
    List<Auction> allAuctions = AuctionManager.getInstance().getAllAuctions();

    for (Auction auction : allAuctions) {
      try {
        // Nạp file giao diện của thẻ sản phẩm
        FXMLLoader loader =
            new FXMLLoader(
                getClass().getResource("/org/deptrai/auctionsystem/client/views/item-card.fxml"));
        Node itemCard = loader.load();

        // Lấy controller của card để truyền dữ liệu
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
