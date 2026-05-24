package org.deptrai.auctionsystem.client.utils;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.deptrai.auctionsystem.shared.models.users.User;

public class SessionManager {
  private static SessionManager instance;

  // Lưu trữ người dùng đang đăng nhập hệ thống
  private User currentUser;


  // Biến lắng nghe sự thay đổi số dư ví
  private Runnable balanceListener;

  private String selectedAuctionId;

  private final ObservableList<String> notifications = FXCollections.observableArrayList();

  private SessionManager() {}

  public static SessionManager getInstance() {
    if (instance == null) {
      instance = new SessionManager();
    }
    return instance;
  }

  public User getCurrentUser() {
    return currentUser;
  }
  public String getSelectedAuctionId() {return selectedAuctionId;}

  // --- QUẢN LÝ NGƯỜI DÙNG ---
  public void setCurrentUser(User user) {
    this.currentUser = user;
  }
  public void setSelectedAuctionId(String selectAuctionId) {this.selectedAuctionId = selectAuctionId;}

  public void logout() {
    this.currentUser = null;
    this.balanceListener = null; // Xóa chuông khi đăng xuất
  }

  // --- HỆ THỐNG LẮNG NGHE SỐ DƯ (MỚI THÊM) ---
  public void setBalanceListener(Runnable listener) {
    this.balanceListener = listener;
  }

  public void notifyBalanceChanged() {
    if (balanceListener != null) {
      balanceListener.run();
    }
  }

  public ObservableList<String> getNotifications() {
    if(notifications.size() > 10) {
      notifications.removeLast();
    }
    return notifications;
  }

  public void addNotification(String msg) {
    Platform.runLater(() -> notifications.addFirst(msg));
  }

}
