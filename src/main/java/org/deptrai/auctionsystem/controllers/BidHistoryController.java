package org.deptrai.auctionsystem.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import org.deptrai.auctionsystem.utils.SceneManager;

public class BidHistoryController {

  @FXML
  public void handleGoBack(ActionEvent event) {
    SceneManager.getInstance().goBack();
  }

  @FXML
  public void initialize() {
    System.out.println("Đã load trang Lịch sử đặt giá!");
    // Sau này sẽ gọi BidDAO để load dữ liệu vào TableView tại đây
  }
}