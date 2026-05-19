package org.deptrai.auctionsystem.client.controllers;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import org.deptrai.auctionsystem.client.utils.SceneManager;
import org.deptrai.auctionsystem.client.utils.SearchEngine;
import org.deptrai.auctionsystem.client.utils.SessionManager;
import org.deptrai.auctionsystem.client.utils.SocketClient;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.auction.AuctionSummary;
import org.deptrai.auctionsystem.shared.models.users.Bidder;
import org.deptrai.auctionsystem.shared.models.users.Seller;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.shared.network.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MainController {

  @FXML
  private HBox guestBox;
  @FXML
  private HBox userBox;
  @FXML
  private Label userNameLabel;
  @FXML
  private Label walletLabel;
  @FXML
  private Button sellerCenterBtn;
  @FXML
  private FlowPane productsContainer;
  @FXML
  private ScrollPane mainScrollPane;
  @FXML
  private MenuButton notificationBell;
  @FXML
  private TextField searchField;

  @FXML
  private ScrollPane floorScrollPane;

  private final int PAGE_SIZE = 12;
  private int currentIndex = 0;
  private List <AuctionSummary> currentFilteredList = new ArrayList<>();
  private List<AuctionSummary> allAuctions = new ArrayList<>();


  @FXML
  public void initialize() {
    User currentUser = SessionManager.getInstance().getCurrentUser();

    if (currentUser != null) {
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

    if(notificationBell != null) {
      notificationBell.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

      var notifs = SessionManager.getInstance().getNotifications();
      notifs.addListener((ListChangeListener<String>) change -> {
        Platform.runLater(this::updateNotification);
      });
      updateNotification();

      // Fix JavaFX nếu mở lần đầu, khi đó javaFX chưa biết kích thước của cái box nên nó sẽ show ra hẳn ngoài app
      // phương pháp: tự động mở lần đầu tiên và đóng ngay lập tức.
      Platform.runLater(() -> {
        notificationBell.show();
        notificationBell.hide();
      });
    }

    if(searchField != null) {
      searchField.textProperty().addListener((observable, oldValue, newValue) -> {
        List<AuctionSummary> searchResult = SearchEngine.searchAuctions(allAuctions, newValue);

        displayAuctions(searchResult);
      });
    }
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

    if (currentUser != null) {
      new Thread(() -> {
        // Hỏi Server: "Tôi vừa online, có thông báo nào tích lũy lúc tôi offline không?"
        Message req = new Message("GET_NOTIFICATIONS", currentUser.getUserId());
        Message res = SocketClient.sendRequest(req);

        if (res != null && "SUCCESS".equals(res.getStatus())) {
          List<String> offlineNotifs = (List<String>) res.getData();
          Platform.runLater(() -> {
            // Đổ toàn bộ thông báo tích lũy vào chuông thông báo trên RAM Client
            for (String msg : offlineNotifs) {
              SessionManager.getInstance().addNotification(msg);
            }
          });
        }
      }).start();
    }

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

    Object currentUser = SessionManager.getInstance().getCurrentUser();
    String userId = null;
    if (currentUser instanceof Bidder bidder) {
      userId = bidder.getUserId();
    }
    Message request = new Message("GET_ALL_AUCTIONS", userId);
    Message response = SocketClient.sendRequest(request);

    if (response.getStatus().equals("SUCCESS")) {
      allAuctions = (List<AuctionSummary>) response.getData();

      displayAuctions(allAuctions);

      javafx.application.Platform.runLater(() -> {
        // Bọc thêm 1 lớp runLater để đảm bảo 100% giao diện đã nạp xong hết các nút bấm
        javafx.application.Platform.runLater(() -> {
          if (mainScrollPane != null) {
            mainScrollPane.setVvalue(0.0); // Kéo thanh cuộn lên đỉnh
            mainScrollPane.requestFocus(); // Đòi lại focus cho ScrollPane, không cho các nút ở dưới giành giật nữa
          }
        });
      });
    } else {
      System.err.println("Không thể lấy danh sách đấu giá từ server");
    }

  }

  private void displayAuctions(List<AuctionSummary> auctionsToDisplay) {
    javafx.application.Platform.runLater(() -> {

      for(Node node : productsContainer.getChildren()) {
        if(node.getUserData() instanceof ItemCardController oldController) {
          SocketClient.removeListener(oldController);
        }
      }

      productsContainer.getChildren().clear();

      int limit = Math.min(auctionsToDisplay.size(), 6);

      for (int i = 0; i < limit; i++) {
        try {
          FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/deptrai/auctionsystem/client/views/item-card.fxml"));
          Node itemCard = loader.load();
          ItemCardController cardController = loader.getController();
          if (cardController != null) {
            cardController.setData(auctionsToDisplay.get(i));
          }
          productsContainer.getChildren().add(itemCard);
        } catch (IOException e) {
          System.err.println("Lỗi nạp item-card: " + e.getMessage());
        }
      }
    });
  }

  private void updateNotification() {
    notificationBell.getItems().clear();
    var notifs = SessionManager.getInstance().getNotifications();

    if (notifs.isEmpty()) {
      notificationBell.setText("\uD83D\uDD14 (0)");
      notificationBell.getItems().add(new MenuItem("Không có thông báo mới"));
    } else {
      notificationBell.setText("\uD83D\uDD14(" + notifs.size() + ")");

      ListView<Label> listView = new ListView<>();
      //listView.getItems().addAll(notifs);
      listView.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);

      for(String text : notifs) {
        Label lb = new Label(text);
        lb.setWrapText(true);
        lb.setPrefWidth(280);
        lb.setStyle("-fx-font-size: 13px;");
        listView.getItems().add(lb);
      }

      listView.getSelectionModel().selectionModeProperty().addListener((obs, oldVal, newVal) -> {
        if(newVal != null) {
          javafx.application.Platform.runLater(() -> listView.getSelectionModel().clearSelection());
        }
      });
      listView.setStyle("-fx-background-insets: 0;");
      listView.setFocusTraversable(false);

      listView.setPrefWidth(300);
      int height = Math.min(notifs.size() * 50 + 10, 400);
      listView.setPrefHeight(height);

      CustomMenuItem customItem = new CustomMenuItem(listView, false);

      customItem.getStyleClass().clear();
      notificationBell.getItems().add(customItem);
    }
  }

  @FXML
  public void handleLogin(ActionEvent event) {
    SceneManager.getInstance()
            .switchScene("/org/deptrai/auctionsystem/client/views/login-view.fxml", "Đăng nhập");
  }

  @FXML
  public void handleRegister(ActionEvent event) {
    SceneManager.getInstance()
            .switchScene("/org/deptrai/auctionsystem/client/views/register-view.fxml", "Đăng ký");
  }

  @FXML
  public void handleLogout(ActionEvent event) {
    SessionManager.getInstance().logout();
    SceneManager.getInstance().clearHistory();
    setUpGuestView();
    SceneManager.getInstance()
            .switchScene("/org/deptrai/auctionsystem/client/views/login-view.fxml", "Đăng nhập");
  }

  @FXML
  public void handleShowProfile(ActionEvent event) {
    SceneManager.getInstance()
            .switchScene("/org/deptrai/auctionsystem/client/views/profile-view.fxml", "Hồ sơ của tôi");
  }

  @FXML
  public void handleShowBidHistory(ActionEvent event) {
    SceneManager.getInstance()
            .switchScene(
                    "/org/deptrai/auctionsystem/client/views/bid-history-view.fxml", "Lịch sử đặt giá");
  }

  @FXML
  public void handleOpenAuctionFloor(ActionEvent event) {
    SceneManager.getInstance()
            .switchScene(
                    "/org/deptrai/auctionsystem/client/views/auction-floor-view.fxml", "Sàn Đấu Giá");
  }

  @FXML
  public void handleGoToSellerCenter(ActionEvent event) {
    SceneManager.getInstance()
            .switchScene("/org/deptrai/auctionsystem/client/views/seller.fxml", "Kênh Người Bán");
  }
  // Hàm xử lý sự kiện khi ấn nút "Làm mới"
  @FXML
  public void handleReload(ActionEvent event) {
    System.out.println("Đang tải lại danh sách đấu giá mới nhất...");

    // Xóa chữ trong ô tìm kiếm (nếu đang có) để hiển thị lại toàn bộ danh sách
    if (searchField != null) {
      searchField.clear();
    }

    // Gọi lại hàm lấy dữ liệu (hàm này đã có sẵn logic sắp xếp ID mới nhất lên đầu)
    loadFeaturedAuctions();
  }


}
