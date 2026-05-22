package org.deptrai.auctionsystem.client.controllers;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

public class AddProductController {

  private static final Logger logger = LoggerFactory.getLogger(AddProductController.class);

  private final Map<String, ItemFactory> factoryRegistry = new HashMap<>();
  @FXML private TextField productNameInput;
  @FXML private ComboBox<String> categoryCombo;
  @FXML private TextField basePriceInput;
  @FXML private DatePicker endDatePicker;
  @FXML private TextArea descriptionInput;
  @FXML private ImageView previewImage;
  @FXML private Label placeholderLabel;
  @FXML private ComboBox<String> hourCombo;
  @FXML private ComboBox<String> minuteCombo;
  private String selectedImagePath = "";

  @FXML
  public void initialize() {
    factoryRegistry.put("Nghệ thuật", new ArtFactory());
    factoryRegistry.put("Điện tử", new ElectronicsFactory());
    factoryRegistry.put("Xe cộ", new VehicleFactory());
    logger.info("Đã load trang Thêm sản phẩm!");

    // Nạp danh mục vào ComboBox khi trang vừa mở
    if (categoryCombo != null) {
      categoryCombo.getItems().addAll("Điện tử", "Xe cộ", "Nghệ thuật");
    }

    // check không cho chọn ngày trong quá khứ
    if (endDatePicker != null) {
      endDatePicker.setDayCellFactory(
          ignored ->
              new DateCell() {
                public void updateItem(LocalDate date, boolean empty) {
                  super.updateItem(date, empty);
                  setDisable(empty || date.isBefore(LocalDate.now()) || date.isAfter(LocalDate.now().plusYears(1)));
                }
              });
      // Nạp dữ liệu cho ô chọn Giờ (00 - 23)
      if (hourCombo != null) {
        for (int i = 0; i < 24; i++) {
          hourCombo.getItems().add(String.format("%02d", i));
        }
        hourCombo.getSelectionModel().select("23"); // Mặc định là 23 giờ
      }

      // Nạp dữ liệu cho ô chọn Phút (00 - 59)
      if (minuteCombo != null) {
        for (int i = 0; i < 60; i++) {
          minuteCombo.getItems().add(String.format("%02d", i));
        }
        minuteCombo.getSelectionModel().select("59"); // Mặc định là 59 phút
      }
    }
  }

  @FXML
  public void handleGoBack() {
    // Quay lại trang trước đó (thường là trang Kho hàng)
    SceneManager.getInstance().goBack();
  }

  @FXML
  public void handleUploadProduct() {
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
      basePrice = Math.round(basePrice * 100.0) / 100.0; // Ép làm tròn khoảng cách 0.01
      if (basePrice <= 0) {
        showAlert(Alert.AlertType.WARNING, "Lỗi giá trị", "Giá khởi điểm phải từ $0.01 trở lên!");
        return;
      }
    } catch (NumberFormatException e) {
      showAlert(Alert.AlertType.ERROR, "Lỗi định dạng", "Giá phải là một con số hợp lệ!");
      return;
    }

    String hourStr = hourCombo.getValue();
    String minuteStr = minuteCombo.getValue();

    if (hourStr == null || minuteStr == null) {
      showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng chọn đầy đủ Giờ và Phút!");
      return;
    }

    // Chuyển đổi thành LocalTime
    int hour = Integer.parseInt(hourStr);
    int minute = Integer.parseInt(minuteStr);
    LocalTime time = LocalTime.of(hour, minute);

    // Gộp LocalDate và LocalTime thành LocalDateTime chuẩn xác đến từng phút
    LocalDateTime endDateTime = endDate.atTime(time);

    // Kiểm tra thời gian kết thúc phải lớn hơn thời điểm hiện tại
    if(endDateTime.isBefore(LocalDateTime.now())) {
      showAlert(Alert.AlertType.WARNING, "Lỗi thời gian", "Không thể chọn thời gian kết thúc trong quá khứ!");
      return ;
    } else if(endDateTime.isAfter(LocalDateTime.now().plusYears(1))) {
      showAlert(Alert.AlertType.WARNING, "Lỗi thời gian", "Thời gian đấu giá tối đa là 1 năm!");
      return ;
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
          logger.error(e.getMessage());
        }
      }

      Object[] payload = new Object[] {newItem, endDateTime, imageBytes, fileName};
      Message request = new Message("REQUEST", "CREATE_AUCTION", payload);

      logger.info("Đang gửi yêu cầu và Tải ảnh lên Server...");

      // Gọi SocketClient gửi đi
      Message response = SocketClient.sendRequest(request);

      if ("SUCCESS".equals(response.getStatus())) {
        showAlert(
            Alert.AlertType.INFORMATION,
            "Thành công",
            "Sản phẩm và Hình ảnh đã được lưu trữ an toàn trên Server!");
        SceneManager.getInstance().goBack();
      } else {
        String errorMsg =
            (response.getData() instanceof String)
                ? (String) response.getData()
                : "Lỗi đường truyền mạng!";
        showAlert(Alert.AlertType.ERROR, "Đăng tải thất bại", errorMsg);
      }
    }
  }

  @FXML
  public void handleChooseImage() {
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
