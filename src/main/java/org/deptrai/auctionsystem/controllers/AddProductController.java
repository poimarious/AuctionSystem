package org.deptrai.auctionsystem.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import org.deptrai.auctionsystem.utils.SceneManager;

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

  @FXML
  public void initialize() {
    System.out.println("Đã load trang Thêm sản phẩm!");
    // Nạp danh mục vào ComboBox khi trang vừa mở
    if (categoryCombo != null) {
      categoryCombo.getItems().addAll("Điện tử", "Xe cộ", "Nghệ thuật", "Thời trang", "Khác");
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
    // Sau này bạn có thể dùng FileChooser tại đây
    System.out.println("Mở trình chọn ảnh...");
  }

  private void showAlert(Alert.AlertType type, String title, String content) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }
}