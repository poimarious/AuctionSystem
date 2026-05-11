package org.deptrai.auctionsystem.client.controllers;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.deptrai.auctionsystem.client.utils.SceneManager;
import org.deptrai.auctionsystem.client.utils.SessionManager;
import org.deptrai.auctionsystem.client.utils.SocketClient;
import org.deptrai.auctionsystem.shared.models.items.*;
import org.deptrai.auctionsystem.shared.models.users.Seller;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.shared.network.Message;

public class AddProductController {

  private final Map<String, ItemFactory> factoryRegistry = new HashMap<>();
  @FXML private TextField productNameInput;
  @FXML private ComboBox<String> categoryCombo;
  @FXML private TextField basePriceInput;
  @FXML private DatePicker endDatePicker;
  @FXML private TextArea descriptionInput;
  @FXML private ImageView previewImage;
  @FXML private Label placeholderLabel;
  private String selectedImagePath = "";

  @FXML
  public void initialize() {
    factoryRegistry.put("Nghệ thuật", new ArtFactory());
    factoryRegistry.put("Điện tử", new ElectronicsFactory());
    factoryRegistry.put("Xe cộ", new VehicleFactory());
    System.out.println("Đã load trang Thêm sản phẩm!");

    // Nạp danh mục vào ComboBox khi trang vừa mở
    if (categoryCombo != null) {
      categoryCombo.getItems().addAll("Điện tử", "Xe cộ", "Nghệ thuật");
    }

    // check không cho chọn ngày trong quá khứ
    if (endDatePicker != null) {
      endDatePicker.setDayCellFactory(
          picker ->
              new DateCell() {
                public void updateItem(LocalDate date, boolean empty) {
                  super.updateItem(date, empty);
                  setDisable(empty || !date.isAfter(LocalDate.now()));
                }
              });
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
    String category = categoryCombo.getValue();
    String priceStr = basePriceInput.getText();
    String description = descriptionInput.getText();
    LocalDate endDate = endDatePicker.getValue();

    if (name == null
        || name.trim().isEmpty()
        || category == null
        || priceStr == null
        || priceStr.trim().isEmpty()
        || description == null
        || description.trim().isEmpty()
        || endDate == null) {
      showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập đầy đủ dữ liệu!");
      return;
    }

    double basePrice;
    try {
      basePrice = Double.parseDouble(priceStr);
      if (basePrice <= 0) {
        showAlert(Alert.AlertType.WARNING, "Lỗi giá trị", "Giá khởi điểm phải lớn hơn 0!");
        return;
      }
    } catch (NumberFormatException e) {
      showAlert(Alert.AlertType.ERROR, "Lỗi định dạng", "Giá phải là một con số!");
      return;
    }

    User currentUser = SessionManager.getInstance().getCurrentUser();

    if (!(currentUser instanceof Seller currentSeller)) {
      showAlert(
          Alert.AlertType.ERROR, "Từ chối truy cập", "Chỉ có Người bán mới được đăng sản phẩm!");
      return;
    }

    ItemFactory selectedFactory = factoryRegistry.get(category);
    if (selectedFactory == null) {
      showAlert(
          Alert.AlertType.ERROR, "Lỗi hệ thống", "Không tìm thấy Factory xử lý cho danh mục này!");
      return;
    }

    Item newItem = selectedFactory.createItem(name, description, basePrice, currentSeller);

    if (newItem != null) {
      byte[] imageBytes = null;
      String fileName = "default.png"; // Tên mặc định nếu không có ảnh

      // CHUYỂN HÓA FILE ẢNH THÀNH MẢNG BYTE
      if (selectedImagePath != null && !selectedImagePath.isEmpty()) {
        try {
          // Dùng URI để đọc chính xác đường dẫn dạng file:///C:/...
          java.nio.file.Path path = Paths.get(java.net.URI.create(selectedImagePath));
          imageBytes = Files.readAllBytes(path);
          fileName = path.getFileName().toString(); // Lấy tên file gốc
        } catch (Exception e) {
          System.err.println("Lỗi đọc file ảnh để gửi đi: " + e.getMessage());
        }
      }

      LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

      // NÂNG CẤP PAYLOAD: Gửi kèm cả mảng Byte và Tên file ảnh
      Object[] payload = new Object[] {newItem, endDateTime, imageBytes, fileName};
      Message request = new Message("REQUEST", "CREATE_AUCTION", payload);

      System.out.println("Đang gửi yêu cầu và Tải ảnh lên Server...");

      // Gọi SocketClient gửi đi
      Message response = SocketClient.sendRequest(request);

      if (response != null && "SUCCESS".equals(response.getStatus())) {
        showAlert(
            Alert.AlertType.INFORMATION,
            "Thành công",
            "Sản phẩm và Hình ảnh đã được lưu trữ an toàn trên Server!");
        SceneManager.getInstance().goBack();
      } else {
        String errorMsg =
            (response != null && response.getData() instanceof String)
                ? (String) response.getData()
                : "Lỗi đường truyền mạng!";
        showAlert(Alert.AlertType.ERROR, "Đăng tải thất bại", errorMsg);
      }
    }
  }

  @FXML
  public void handleChooseImage(ActionEvent event) {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Chọn hình ảnh sản phẩm");
    fileChooser
        .getExtensionFilters()
        .add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

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
