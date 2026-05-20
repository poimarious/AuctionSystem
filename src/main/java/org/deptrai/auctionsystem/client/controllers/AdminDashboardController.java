package org.deptrai.auctionsystem.client.controllers;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import org.deptrai.auctionsystem.client.utils.SceneManager;
import org.deptrai.auctionsystem.client.utils.SessionManager;
import org.deptrai.auctionsystem.client.utils.SocketClient;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.users.Admin;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.shared.network.Message;

import java.util.List;
import java.util.Optional;

public class AdminDashboardController {

  // Nút Menu
  @FXML private Button btnNavAuctions;
  @FXML private Button btnNavUsers;
  @FXML private Label lblAdminInfo;

  // Panes
  @FXML private VBox paneAuctions;
  @FXML private VBox paneUsers;

  // Bảng Auctions
  @FXML private TableView<Auction> tableAuctions;
  @FXML private TableColumn<Auction, String> colAuctionId;
  @FXML private TableColumn<Auction, String> colItemName;
  @FXML private TableColumn<Auction, String> colPrice;
  @FXML private TableColumn<Auction, String> colStatus;
  @FXML private TableColumn<Auction, Void> colAuctionAction;

  // Bảng Users
  @FXML private TableView<User> tableUsers;
  @FXML private TableColumn<User, String> colUserId;
  @FXML private TableColumn<User, String> colUsername;
  @FXML private TableColumn<User, String> colRole;
  @FXML private TableColumn<User, String> colUserStatus;
  @FXML private TableColumn<User, Void> colUserAction;

  private Admin currentAdmin;

  @FXML
  public void initialize() {
    User user = SessionManager.getInstance().getCurrentUser();
    if (user instanceof Admin) {
      this.currentAdmin = (Admin) user;
      lblAdminInfo.setText("Admin: " + currentAdmin.getUsername() + " (Lv." + currentAdmin.getAdminLevel() + ")");

      // Level 1 không được quản lý User
      if (currentAdmin.getAdminLevel() < 2) {
        btnNavUsers.setVisible(false);
        btnNavUsers.setManaged(false);
      }
    } else {
      // Đá văng ra nếu cố tình vào bằng bug
      handleLogout(null);
      return;
    }

    setupAuctionTable();
    setupUserTable();

    // Mở sẵn tab Auction
    showAuctionsPane(null);
  }

  // ====================
  // ĐIỀU HƯỚNG GIAO DIỆN
  // ====================

  @FXML
  public void showAuctionsPane(ActionEvent event) {
    paneAuctions.setVisible(true);
    paneUsers.setVisible(false);
    // Đổi màu menu để biết đang ở đâu
    btnNavAuctions.setStyle("-fx-background-color: #2b244d; -fx-text-fill: #bd93f9; -fx-font-weight: bold;");
    btnNavUsers.setStyle("-fx-background-color: transparent; -fx-text-fill: #e2e8f0;");

    loadAllAuctions();
  }

  @FXML
  public void showUsersPane(ActionEvent event) {
    paneAuctions.setVisible(false);
    paneUsers.setVisible(true);

    btnNavUsers.setStyle("-fx-background-color: #2b244d; -fx-text-fill: #00f3ff; -fx-font-weight: bold;");
    btnNavAuctions.setStyle("-fx-background-color: transparent; -fx-text-fill: #e2e8f0;");

    loadAllUsers();
  }

  @FXML
  public void handleLogout(ActionEvent event) {
    new Thread(() -> {
      Message logoutReq = new Message("REQUEST", "LOGOUT", null);
      SocketClient.sendRequest(logoutReq);
    }).start();

    SessionManager.getInstance().logout();
    SceneManager.getInstance().clearHistory();
    SceneManager.getInstance().switchScene("/org/deptrai/auctionsystem/client/views/login-view.fxml", "Đăng nhập");
  }

  // =================
  // LOGIC TAB AUCTION
  // =================

  private void setupAuctionTable() {
    colAuctionId.setCellValueFactory(new PropertyValueFactory<>("auctionId"));
    colItemName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getItem() != null ? data.getValue().getItem().getName() : "N/A"));
    colPrice.setCellValueFactory(data -> new SimpleStringProperty(String.format("$%,.2f", data.getValue().getCurrentPrice())));
    colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().toString()));

    // Nút Xóa
    colAuctionAction.setCellFactory(param -> new TableCell<>() {
      private final Button btn = new Button("XÓA");
      {
        btn.setStyle("-fx-background-color: #ff003c; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btn.setOnAction(event -> {
          Auction auction = getTableView().getItems().get(getIndex());
          handleDeleteAuction(auction);
        });
      }

      @Override
      protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) setGraphic(null);
        else { setGraphic(btn); setAlignment(Pos.CENTER); }
      }
    });
  }

  @FXML
  public void loadAllAuctions() {
    new Thread(() -> {
      try {
        Message request = new Message("REQUEST", "GET_ALL_AUCTIONS_ADMIN", null);
        Message response = SocketClient.sendRequest(request);

        if (response.getStatus().equals("SUCCESS")) {
          List<Auction> auctions = (List<Auction>) response.getData();
          Platform.runLater(() -> {
            tableAuctions.getItems().setAll(auctions);
            System.out.println(">> Đã tải " + auctions.size() + " phiên đấu giá vào Admin Panel");
          });
        } else {
          System.err.println(">> Server từ chối tải danh sách Auction!");
        }
      } catch (Exception e) {
        System.err.println(">> Lỗi khi nạp bảng Auction: ");
        e.printStackTrace();
      }
    }).start();
  }

  private void handleDeleteAuction(Auction auction) {
    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Hủy vĩnh viễn phiên đấu giá này?", ButtonType.YES, ButtonType.NO);
    confirm.setTitle("Xác nhận Admin");
    confirm.showAndWait().ifPresent(response -> {
      if (response == ButtonType.YES) {
        new Thread(() -> {
          Message req = new Message("REQUEST", "DELETE_AUCTION", auction.getAuctionId());
          Message res = SocketClient.sendRequest(req);
          Platform.runLater(() -> {
            if (res.getStatus().equals("SUCCESS")) loadAllAuctions(); // Load lại bảng
            else showAlert(Alert.AlertType.ERROR, "Lỗi", (String) res.getData());
          });
        }).start();
      }
    });
  }

  // =============================
  // LOGIC TAB USERS (Chỉ Level 2)
  // =============================

  private void setupUserTable() {
    colUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));
    colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
    colRole.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getClass().getSimpleName().toUpperCase()));
    colUserStatus.setCellValueFactory(data -> {
      boolean isBanned = data.getValue().isBanned();
      return new SimpleStringProperty(isBanned ? ("BỊ CẤM: " + data.getValue().getBanReason()) : "Đang hoạt động");
    });

    // Nút BAN/BỎ BAN
    colUserAction.setCellFactory(param -> new TableCell<>() {
      private final Button btn = new Button();
      {
        btn.setOnAction(event -> {
          User user = getTableView().getItems().get(getIndex());
          if (user.isBanned()) {
            handleUnbanUser(user);
          } else {
            handleBanUser(user);
          }
        });
      }

      @Override
      protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
          setGraphic(null);
        } else {
          User user = getTableView().getItems().get(getIndex());
          // Không cho phép Admin tự Ban mình hoặc Ban Admin khác
          if (user.getUserId().equals(currentAdmin.getUserId()) || user instanceof Admin) {
            setGraphic(null);
            return;
          }

          if (user.isBanned()) {
            btn.setText("GỠ CẤM");
            btn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
          } else {
            btn.setText("CẤM (BAN)");
            btn.setStyle("-fx-background-color: #ff003c; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
          }
          setGraphic(btn);
          setAlignment(Pos.CENTER);
        }
      }
    });
  }

  @FXML
  public void loadAllUsers() {
    new Thread(() -> {
      try{
        Message request = new Message("GET_ALL_USERS", null);
        Message response = SocketClient.sendRequest(request);

        if (response.getStatus().equals("SUCCESS")) {
          List<User> users = (List<User>) response.getData();
          Platform.runLater(() -> {
            tableUsers.getItems().setAll(users);
            System.out.println(">> Đã tải " + users.size() + " người dùng vào Admin Panel");
          });
        } else{
          System.err.println(">> Server từ chối tải danh sách User");
        }
      } catch(Exception e) {
        System.err.println(">> Lỗi khi nạp bảng User: ");
        e.printStackTrace();
      }
    }).start();

  }

  private void handleBanUser(User user) {
    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle("Cấm Người Dùng");
    dialog.setHeaderText("Ban người dùng: " + user.getUsername());
    dialog.setContentText("Nhập lý do Ban:");

    Optional<String> result = dialog.showAndWait();
    result.ifPresent(reason -> {
      if (reason.trim().isEmpty()) {
        showAlert(Alert.AlertType.WARNING, "Lỗi", "Vui lòng nhập lý do cấm!");
        return;
      }

      new Thread(() -> {
        // Đóng gói data gửi lên Server: [Admin gửi, ID kẻ bị Ban, Lý do]
        Object[] payload = new Object[]{currentAdmin, user.getUserId(), reason};
        Message req = new Message("BAN_USER", payload);
        Message res = SocketClient.sendRequest(req);

        Platform.runLater(() -> {
          if (res.getStatus().equals("SUCCESS")) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã Ban và đá người dùng ra khỏi hệ thống!");
            loadAllUsers(); // Cập nhật lại bảng
          } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", (String) res.getData());
          }
        });
      }).start();
    });
  }

  private void handleUnbanUser(User user) {
    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
        "Bạn có chắc chắn muốn gỡ cấm cho tài khoản: " + user.getUsername() + "?",
        ButtonType.YES, ButtonType.NO);
    confirm.setTitle("Xác nhận Gỡ Cấm");

    confirm.showAndWait().ifPresent(response -> {
      if (response == ButtonType.YES) {
        new Thread(() -> {
          Object[] payload = new Object[]{currentAdmin, user.getUserId()};
          Message req = new Message("UNBAN_USER", payload);
          Message res = SocketClient.sendRequest(req);

          Platform.runLater(() -> {
            if (res.getStatus().equals("SUCCESS")) {
              showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã khôi phục quyền truy cập cho người dùng!");
              loadAllUsers();
            } else {
              showAlert(Alert.AlertType.ERROR, "Lỗi", (String) res.getData());
            }
          });
        }).start();
      }
    });
  }

  private void showAlert(Alert.AlertType type, String title, String content) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }
}