package org.deptrai.auctionsystem.client.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import org.deptrai.auctionsystem.client.utils.SceneManager;

public class InventoryController {

  @FXML
  public void handleAddNewProduct(ActionEvent event) {
    // Chuyển sang trang Đăng sản phẩm mới
    SceneManager.getInstance().switchScene("/org.deptrai.auctionsystem.views/add-product-view.fxml", "Đăng sản phẩm đấu giá mới");
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