package org.deptrai.auctionsystem.client.controllers;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
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
import org.deptrai.auctionsystem.shared.models.auction.AuctionStatus;
import org.deptrai.auctionsystem.shared.models.auction.AuctionSummary;
import org.deptrai.auctionsystem.shared.models.users.Seller;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.shared.network.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MainController {

  private static final Logger logger = LoggerFactory.getLogger(MainController.class);

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
  private Button toggleViewBtn;
  @FXML
  private Label mainTitleLabel;
  @FXML
  private Label subTitleLabel;
  private List<AuctionSummary> allAuctions = new ArrayList<>();
  private boolean isShowingPending = false;
  private final List<AuctionSummary> runningAuctions = new ArrayList<>();
  private final List<AuctionSummary> pendingAuctions = new ArrayList<>();


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

    if (notificationBell != null) {
      notificationBell.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

      var notifs = SessionManager.getInstance().getNotifications();
      notifs.addListener((ListChangeListener<String>) _ -> Platform.runLater(this::updateNotification));
      updateNotification();

      // Fix JavaFX nếu mở lần đầu, khi đó javaFX chưa biết kích thước của cái box nên nó sẽ show ra hẳn ngoài app
      // phương pháp: tự động mở lần đầu tiên và đóng ngay lập tức.
      Platform.runLater(() -> {
        notificationBell.show();
        notificationBell.hide();
      });
    }

    if (searchField != null) {
      searchField.textProperty().addListener((_, _, newValue) -> {
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
    if (toggleViewBtn != null) {
      toggleViewBtn.setVisible(false);
      toggleViewBtn.setManaged(false);
    }
  }

  public void setUpUserView(String username, double balance) {
    guestBox.setVisible(false);
    guestBox.setManaged(false);

    userBox.setVisible(true);
    userBox.setManaged(true);

    userNameLabel.setText(username);
    walletLabel.setText(String.format("Ví: $%,.2f", balance));
    if (toggleViewBtn != null) {
      toggleViewBtn.setVisible(true);
      toggleViewBtn.setManaged(true);
    }

    User currentUser = SessionManager.getInstance().getCurrentUser();

    if (currentUser != null) {
      new Thread(() -> {
        // Hỏi Server: "Tôi vừa online, có thông báo nào tích lũy lúc tôi offline không?"
        Message req = new Message("GET_NOTIFICATIONS", currentUser.getUserId());
        Message res = SocketClient.sendRequest(req);

        if ("SUCCESS".equals(res.getStatus())) {
          @SuppressWarnings("unchecked")
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
    if (currentUser instanceof User user) {
      userId = user.getUserId();
    }

    Message request = new Message("GET_ALL_AUCTIONS", userId);
    if (currentUser instanceof Seller) {
      request = new Message("GET_SELLER_AUCTIONS", userId);
    }
    Message response = SocketClient.sendRequest(request);

    if (response.getStatus().equals("SUCCESS")) {
      if (currentUser instanceof Seller) {
        @SuppressWarnings("unchecked")
        List<Auction> sellerAuctions = (List<Auction>) response.getData();

        for (Auction auction : sellerAuctions) {
          AuctionSummary auctionSummary = new AuctionSummary(
                  auction.getAuctionId(),
                  auction.getItem().getName(),
                  auction.getItem().getDescription(),
                  auction.getItem().getCategory(),
                  auction.getCurrentPrice(),
                  auction.getStatus(),
                  auction.getEndTime(),
                  auction.getItem().getImageUrl(),
                  auction.getItem().getImageBytes()
          );
          allAuctions.add(auctionSummary);
        }
      } else {
        @SuppressWarnings("unchecked")
        List<AuctionSummary> auctions = (List<AuctionSummary>) response.getData();
        allAuctions = auctions;
      }
      // Dọn sạch 2 kho chứa trước khi chia bài
      runningAuctions.clear();
      pendingAuctions.clear();

      for (AuctionSummary auc : allAuctions) {
        if (auc.getStatus() != null) {
          AuctionStatus status = auc.getStatus();

          if (status == AuctionStatus.RUNNING || status == AuctionStatus.OPEN) {
            runningAuctions.add(auc);
          } else if (status == AuctionStatus.FINISHED) {
            pendingAuctions.add(auc);
          }
        }
      }

      // Mặc định ép hiển thị kho Đang bán
      isShowingPending = false;
      displayAuctions(runningAuctions);
      boolean isSeller = currentUser instanceof Seller;
      String defaultBtnText = isSeller ? "📦 Đồ đã hết thời gian" : "💳 Cần thanh toán";

      // Đồng bộ lại Text và Màu nút
      Platform.runLater(() -> {
        if (toggleViewBtn != null) {
          toggleViewBtn.setText(defaultBtnText);
          toggleViewBtn.getStyleClass().removeAll("btn-primary", "btn-accent");
          toggleViewBtn.getStyleClass().add("btn-accent");
        }
        if (mainTitleLabel != null) mainTitleLabel.setText("Sản phẩm đang đấu giá");
        if (subTitleLabel != null) subTitleLabel.setText("(sản phẩm mới hôm nay)");
      });

      Platform.runLater(() -> {
        if (mainScrollPane != null) {
          mainScrollPane.setVvalue(0.0);
          mainScrollPane.requestFocus();
        }
      });
    } else {
      logger.error("Không thể lấy danh sách đấu giá từ server");
    }
  }

  private void displayAuctions(List<AuctionSummary> auctionsToDisplay) {
    javafx.application.Platform.runLater(() -> {

      for (Node node : productsContainer.getChildren()) {
        if (node.getUserData() instanceof ItemCardController oldController) {
          SocketClient.removeListener(oldController);
        }
      }

      productsContainer.getChildren().clear();

      int limit = Math.min(auctionsToDisplay.size(), 6);

      for (int i = 0; i < limit; i++) {
        if (auctionsToDisplay.get(i).getStatus() == AuctionStatus.PAID) continue;
        try {
          FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/deptrai/auctionsystem/client/views/item-card.fxml"));
          Node itemCard = loader.load();
          ItemCardController cardController = loader.getController();
          if (cardController != null) {
            cardController.setData(auctionsToDisplay.get(i));
          }
          productsContainer.getChildren().add(itemCard);
        } catch (IOException e) {
          logger.error("Lỗi nạp item-card: ", e);
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

      for (String text : notifs) {
        Label lb = new Label(text);
        lb.setWrapText(true);
        lb.setPrefWidth(280);
        lb.setStyle("-fx-font-size: 13px;");
        listView.getItems().add(lb);
      }

      listView.getSelectionModel().selectionModeProperty().addListener((_, _, newVal) -> {
        if (newVal != null) {
          javafx.application.Platform.runLater(() -> listView.getSelectionModel().clearSelection());
        }
      });
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
  public void handleLogin() {
    SceneManager.getInstance()
            .switchScene("/org/deptrai/auctionsystem/client/views/login-view.fxml", "Đăng nhập");
  }

  @FXML
  public void handleRegister() {
    SceneManager.getInstance()
            .switchScene("/org/deptrai/auctionsystem/client/views/register-view.fxml", "Đăng ký");
  }

  @FXML
  public void handleLogout() {
    new Thread(() -> {
      Message logoutReq = new Message("REQUEST", "LOGOUT", null);
      SocketClient.sendRequest(logoutReq);
    }).start();

    SessionManager.getInstance().logout();
    SceneManager.getInstance().clearHistory();
    setUpGuestView();
    SceneManager.getInstance()
            .switchScene("/org/deptrai/auctionsystem/client/views/login-view.fxml", "Đăng nhập");
  }

  @FXML
  public void handleShowProfile() {
    SceneManager.getInstance()
            .switchScene("/org/deptrai/auctionsystem/client/views/profile-view.fxml", "Hồ sơ của tôi");
  }

  @FXML
  public void handleShowBidHistory() {
    SceneManager.getInstance()
            .switchScene(
                    "/org/deptrai/auctionsystem/client/views/bid-history-view.fxml", "Lịch sử đặt giá");
  }

  @FXML
  public void handleOpenAuctionFloor() {
    SceneManager.getInstance()
            .switchScene(
                    "/org/deptrai/auctionsystem/client/views/auction-floor-view.fxml", "Sàn Đấu Giá");
  }
  @FXML
  public void handleToggleView() {
    if (productsContainer == null) return;

    // Kiểm tra xem người dùng hiện tại có phải là Seller không
    boolean isSeller = SessionManager.getInstance().getCurrentUser() instanceof Seller;

    if (isShowingPending) {
      // TRỞ LẠI SÀN ĐẤU GIÁ
      displayAuctions(runningAuctions);

      if (toggleViewBtn != null) {
        // Nếu là Seller thì hiện nút "Đồ hết thời gian", nếu là người mua thì hiện "Cần thanh toán"
        toggleViewBtn.setText(isSeller ? "📦 Đồ đã hết thời gian" : "💳 Cần thanh toán");
        toggleViewBtn.getStyleClass().removeAll("btn-primary", "btn-accent");
        toggleViewBtn.getStyleClass().add("btn-accent"); // Nút màu hồng
      }

      if (mainTitleLabel != null) mainTitleLabel.setText("Sản phẩm đang đấu giá");
      if (subTitleLabel != null) subTitleLabel.setText("(sản phẩm mới hôm nay)");

      isShowingPending = false;
    } else {
      // XEM TRANG ĐỒ ĐÃ KẾT THÚC / CẦN THANH TOÁN
      displayAuctions(pendingAuctions);

      if (toggleViewBtn != null) {
        toggleViewBtn.setText("⬅ Trở lại Sàn đấu giá");
        toggleViewBtn.getStyleClass().removeAll("btn-primary", "btn-accent");
        toggleViewBtn.getStyleClass().add("btn-primary"); // Nút màu tím
      }

      if (mainTitleLabel != null) {
        // ĐỔI TIÊU ĐỀ TRANG THEO ROLE
        mainTitleLabel.setText(isSeller ? "Danh sách đồ hết thời gian" : "Danh sách cần thanh toán");
      }
      if (subTitleLabel != null) {
        // Đổi luôn cả dòng chú thích nhỏ ở dưới cho mượt
        subTitleLabel.setText(isSeller ? "(Các phiên đấu giá của bạn đã khép lại)" : "(Sản phẩm bạn đã thắng đấu giá)");
      }

      isShowingPending = true;
    }
  }

  @FXML
  public void handleGoToSellerCenter() {
    SceneManager.getInstance()
            .switchScene("/org/deptrai/auctionsystem/client/views/seller.fxml", "Kênh Người Bán");
  }
  // Hàm xử lý sự kiện khi ấn nút "Làm mới"
  @FXML
  public void handleReload() {
    logger.info("Đang tải lại danh sách đấu giá mới nhất...");

    // Xóa chữ trong ô tìm kiếm (nếu đang có) để hiển thị lại toàn bộ danh sách
    if (searchField != null) {
      searchField.clear();
    }

    // Gọi lại hàm lấy dữ liệu (hàm này đã có sẵn logic sắp xếp ID mới nhất lên đầu)
    loadFeaturedAuctions();
  }
}