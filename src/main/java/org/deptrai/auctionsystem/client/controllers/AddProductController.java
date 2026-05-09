package org.deptrai.auctionsystem.client.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.deptrai.auctionsystem.client.utils.SceneManager;

import java.io.File;

public class AddProductController {

  @FXML
  private TextField productNameInput;
  @FXML
  private ComboBox<String> categoryCombo;
  @FXML
  private TextField basePriceInput;
  @FXML
  private DatePicker endDatePicker;
  @FXML
  private TextArea descriptionInput;
  @FXML
  private ImageView previewImage;
  @FXML
  private Label placeholderLabel;

  private String selectedImagePath = "";

  @FXML
  public void initialize() {
    System.out.println("Đã load trang Thêm sản phẩm!");
    // Nạp danh mục vào ComboBox khi trang vừa mở
    if (categoryCombo != null) {
      categoryCombo.getItems().addAll("Điện tử", "Xe cộ", "Nghệ thuật");
    }
  }

  @FXML
  public void handleGoBack(ActionEvent event) {
    // Quay lại trang trước đó (thường là trang Kho hàng)
    SceneManager.getInstance().goBack();
  }

  @FXML
  public void handleUploadProduct(ActionEvent event) {
    // Kiểm tra sơ bộ xem đã nhập tên chưa
    String name = productNameInput.getText();

    if (name == null || name.trim().isEmpty()) {
      showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập tên sản phẩm trước khi đăng!");
      return;
    }

    // Logic giả lập đăng tải thành công
    System.out.println("Đang xử lý đăng tải sản phẩm: " + name);
    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Sản phẩm của bạn đã được gửi lên hệ thống chờ duyệt!");

    // Sau khi đăng xong thì quay lại trang quản lý
    SceneManager.getInstance().goBack();
  }

  @FXML
  public void handleChooseImage(ActionEvent event) {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Chọn hình ảnh sản phẩm");
    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

    File selectedFile = fileChooser.showOpenDialog(null);
    if (selectedFile != null) {
      selectedImagePath = selectedFile.toURI().toString();
      previewImage.setImage(new Image(selectedImagePath));
      placeholderLabel.setVisible(false);
    }
  }

  private void showAlert(Alert.AlertType type, String title, String content) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }
}