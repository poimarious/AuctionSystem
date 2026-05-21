package org.deptrai.auctionsystem.client.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import org.deptrai.auctionsystem.client.utils.SceneManager;
import org.deptrai.auctionsystem.client.utils.SearchEngine;
import org.deptrai.auctionsystem.client.utils.SessionManager;
import org.deptrai.auctionsystem.client.utils.SocketClient;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.auction.AuctionStatus;
import org.deptrai.auctionsystem.shared.models.auction.AuctionSummary;
import org.deptrai.auctionsystem.shared.models.users.Bidder;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.shared.network.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionFloorController {

  private static final Logger logger = LoggerFactory.getLogger(AuctionFloorController.class);

  @FXML
  private FlowPane productsContainer;
  @FXML
  private TextField searchField;
  @FXML
  private ComboBox<String> categoryCombo;
  @FXML
  private ComboBox<String> statusCombo;
  @FXML
  private ScrollPane floorScrollPane;

  private final int PAGE_SIZE = 12;
  private int currentIndex = 0;
  private List<AuctionSummary> currentFilteredList = new ArrayList<>();

  private List<AuctionSummary> allAuctionsList = new ArrayList<>();

  /**
   * 1. SỬA NÚT QUAY LẠI: Ép về thẳng Home View
   */
  @FXML
  public void handleGoBack(ActionEvent event) {SceneManager.getInstance().goBack();}

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
      statusCombo.setOnAction(event -> {
        filterAuctions();
      });
    }

    if (searchField != null) {
      searchField.textProperty().addListener((observable, oldValue, newValue) -> {
        filterAuctions();
      });
    }

    if(floorScrollPane != null) {
      floorScrollPane.vvalueProperty().addListener((observable, oldValue, newValue) -> {
        if(newValue.doubleValue() >= 0.9) {
          loadMoreAuctions();
        }
      });
    } else {
      System.err.println("CẢNH BÁO: floorScrollPane bị NULL! Hãy kiểm tra lại SceneBuilder.");
    }

    // 2. SỬA CÁCH LOAD DỮ LIỆU: Lấy từ Server thay vì AuctionManager
    loadAuctionsFromServer();
  }

  private void loadAuctionsFromServer() {
    // Gửi yêu cầu qua Socket
    Object currentUser = SessionManager.getInstance().getCurrentUser();
    String userId = null;
    if (currentUser instanceof User user) {
      userId = user.getUserId();
    }
    Message request = new Message("GET_ALL_AUCTIONS", userId);

    SocketClient.runAsync(() -> {
      Message response = SocketClient.sendRequest(request);
      if ("SUCCESS".equals(response.getStatus())) {
        List<AuctionSummary> allAuctions = (List<AuctionSummary>) response.getData();

        allAuctionsList = allAuctions;

        // Cập nhật giao diện an toàn trên luồng UI
        Platform.runLater(() -> filterAuctions());
      }
    });
  }
  // Hàm lọc dữ liệu nội bộ trên RAM
  private void filterAuctions() {
    String selectedCategory = categoryCombo.getValue();
    String selectedStatus = statusCombo.getValue();
    String keyword = searchField.getText();

    //Tìm các auctions theo keyword trước
    List<AuctionSummary> tempResult = SearchEngine.searchAuctions(allAuctionsList, keyword);

    // lọc theo category và status
    List<AuctionSummary> finalResults = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();


    for (AuctionSummary auction : tempResult) {
      // Kiểm tra xem sản phẩm có khớp danh mục không
      boolean matchCategory = (selectedCategory == null || selectedCategory.equals("Tất cả")) ||
              (auction.getCategory() != null &&
                      auction.getCategory().equalsIgnoreCase(selectedCategory));

      boolean matchStatus = true;

      if (selectedStatus != null && !selectedStatus.equals("Tất cả")) {
        boolean isActive =  (auction.getStatus() == AuctionStatus.OPEN ||
                auction.getStatus() == AuctionStatus.RUNNING) &&
                auction.getEndTime().isAfter(now);

        if(selectedStatus.equals("Đang diễn ra")) {
          matchStatus = isActive;
        } else if(selectedStatus.equals("Sắp kết thúc")) {
          if(isActive) {
            long hourLeft = Duration.between(now, auction.getEndTime()).toHours();

            // một auction còn thời gian dưới 24 tiếng được gọi là sắp kết thúc
            matchStatus = (0 <= hourLeft && hourLeft <= 24);
          } else {
            matchStatus = false;
          }
        }
      }

      // Nếu qua được màng lọc danh mục thì thêm vào danh sách cuối cùng
      if (matchCategory && matchStatus) {
        finalResults.add(auction);
      }
    }

    this.currentFilteredList = finalResults;
    this.currentIndex = 0;

    for(Node node : productsContainer.getChildren()) {
      if(node.getUserData() instanceof ItemCardController oldController) {
        SocketClient.removeListener(oldController);
      }
    }

    productsContainer.getChildren().clear();

    if(floorScrollPane != null ) {
      floorScrollPane.setVvalue(0.0);
    }
    loadMoreAuctions();
  }

  private void loadMoreAuctions() {
    // Nếu đã load hết danh sách thì không làm gì cả
    if (currentIndex >= currentFilteredList.size()) {
      return;
    }

    // Tính toán điểm dừng cho lô tiếp theo
    int endIndex = Math.min(currentIndex + PAGE_SIZE, currentFilteredList.size());
    List<AuctionSummary> batch = currentFilteredList.subList(currentIndex, endIndex);

    // Bọc trong runLater để đảm bảo FXML được vẽ trên luồng chính của JavaFX
    Platform.runLater(() -> {
      for (AuctionSummary auction : batch) {
        try {
          FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/deptrai/auctionsystem/client/views/item-card.fxml"));
          Node itemCard = loader.load();

          ItemCardController cardController = loader.getController();
          if (cardController != null) {
            cardController.setData(auction);
            // LƯU LẠI CONTROLLER VÀO UI ĐỂ DỌN RÁC (Tránh Memory Leak)
            itemCard.setUserData(cardController);
          }

          productsContainer.getChildren().add(itemCard);
        } catch (IOException e) {
          logger.error("Lỗi nạp item-card: " + e.getMessage());
        }
      }
      // Cập nhật lại chỉ số cho lần cuộn tiếp theo
      currentIndex = endIndex;
    });
  }
}