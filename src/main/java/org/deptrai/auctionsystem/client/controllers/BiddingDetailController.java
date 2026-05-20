package org.deptrai.auctionsystem.client.controllers;

import java.io.ByteArrayInputStream;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.deptrai.auctionsystem.client.utils.*;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.bid.Bid;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.shared.network.Message;

import java.io.File;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BiddingDetailController implements AuctionUpdateListener {

  @FXML private ImageView productImageView;
  @FXML private Label nameLabel;
  @FXML private Label descriptionLabel;
  @FXML private Label currentPriceLabel;
  @FXML private Label expiryTimerLabel;

  @FXML private TableView<Bid> bidHistoryTable;
  @FXML private TableColumn<Bid, String> timeColumn;
  @FXML private TableColumn<Bid, String> bidderColumn;
  @FXML private TableColumn<Bid, Double> amountColumn;

  @FXML private TextField bidAmountField;

  @FXML private LineChart<String, Number> bidChart;

  @FXML private RadioButton radioQuick;
  @FXML private RadioButton radioCustom;
  @FXML private ToggleGroup bidModeGroup;
  @FXML private HBox quickBidBox;
  @FXML private Label quickBidLabel;

  @FXML private TextField maxBidField;
  @FXML private TextField incrementField;
  @FXML private Button btnAutoBid;

  private double currentIntendedBid = 0.0;

  private XYChart.Series<String, Number> priceSeries;

  private Auction currentAuction;
  private Timeline countdownTimeline;

  private boolean isAutoBidActive = false;
  private double maxAutoBidLimit = 0.0;
  private double autoBidIncrement = 0.0;

  @FXML
  public void initialize() {
    // Cấu hình bảng
    amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
    timeColumn.setCellValueFactory(cellData -> {
      DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM HH:mm:ss");
      return new SimpleStringProperty(cellData.getValue().getTimestamp().format(fmt));
    });

    bidderColumn.setCellValueFactory(cellData -> {
      if (cellData.getValue().getBidder() != null) {
        return new SimpleStringProperty(cellData.getValue().getBidder().getUsername());
      }
      return new SimpleStringProperty("N/A");
    });

    // Khởi tạo đường biểu diễn giá
    priceSeries = new XYChart.Series<>();
    priceSeries.setName("Mức giá đặt");

    // Gắn đường dây này vào biểu đồ
    if (bidChart != null) {
      bidChart.getData().add(priceSeries);
    }

    // Lắng nghe sự kiện chuyển đổi Mode đặt bid
    bidModeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
      if (radioQuick.isSelected()) {
        quickBidBox.setVisible(true);
        bidAmountField.setVisible(false);
      } else {
        quickBidBox.setVisible(false);
        bidAmountField.setVisible(true);
        // Tự điền giá định đặt vào ô nhập tay cho tiện
        bidAmountField.setText(String.valueOf(currentIntendedBid));
      }
    });

    // Tự động load dữ liệu từ Session khi vừa vào trang
    String selectedId = SessionManager.getInstance().getSelectedAuctionId();

    if (selectedId != null) {
      // Bọc vào Thread phụ để không làm đơ ứng dụng khi tải

      SocketClient.runAsync(() -> {
        Message req = new Message("GET_AUCTION_BY_ID", selectedId);
        Message res = SocketClient.sendRequest(req);

        Platform.runLater(() -> {
          if (res != null && "SUCCESS".equals(res.getStatus())) {
            Auction freshAuction = (Auction) res.getData();
            setAuctionData(freshAuction);
          } else {
            // Xử lý khi mạng lỗi hoặc phiên đấu giá bị xóa
            showError("Không thể tải thông tin phiên đấu giá. Vui lòng thử lại sau!");
            handleGoBack(null); // Đẩy người dùng quay lại trang trước
          }
        });
      });
    }
  }

  public void setAuctionData(Auction auction) {
    this.currentAuction = auction;
    nameLabel.setText(auction.getItem().getName());
    descriptionLabel.setText(auction.getItem().getDescription());
    currentPriceLabel.setText(String.format("$%.2f", auction.getCurrentPrice()));
    double current = auction.getCurrentPrice();
    currentIntendedBid = current + getIncrementStep(current);
    quickBidLabel.setText(String.format("$%.2f", currentIntendedBid));

    byte[] imageBytes = auction.getItem().getImageBytes();
    if (imageBytes != null && imageBytes.length > 0) {
      try {
        Image image = new Image(new ByteArrayInputStream(imageBytes));
        productImageView.setImage(image);
      } catch (Exception e) {
        System.err.println("Lỗi giải mã hình ảnh từ mảng byte mạng: " + e.getMessage());
      }
    } else {
      System.out.println("Sản phẩm này không đi kèm dữ liệu ảnh.");
    }

    List<Bid> bids = new ArrayList<>(auction.getBids());
    bids.sort(Comparator.comparing(Bid::getTimestamp));
    bidHistoryTable.getItems().setAll(bids);

    refreshChart(bids);

    SocketClient.addListener(this); // New observer

    startTimer();

    if(AutoBidManager.getInstance().isAutoBidActive(auction.getAuctionId())) {
      var config = AutoBidManager.getInstance().getAutoBidConfig(auction.getAuctionId());
      maxBidField.setText(String.valueOf(config.maxBid));
      incrementField.setText(String.valueOf(config.increment));
      maxBidField.setDisable(true);
      incrementField.setDisable(true);
      btnAutoBid.setText("🛑 ĐANG CHẠY AUTO-BID (BẤM ĐỂ TẮT)");
      btnAutoBid.setStyle("-fx-background-color: #ff003c; -fx-text-fill: white; -fx-border-color: transparent;");
    }
  }

  private void startTimer() {
    if (countdownTimeline != null) countdownTimeline.stop();
    countdownTimeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), e -> updateCountdown()));
    countdownTimeline.setCycleCount(Timeline.INDEFINITE);
    countdownTimeline.play();
  }

  private void updateCountdown() {
    if (currentAuction == null) return;
    Duration res = Duration.between(LocalDateTime.now(), currentAuction.getEndTime());
    if (res.isNegative() || res.isZero()) {
      expiryTimerLabel.setText("00:00:00");
      bidAmountField.setDisable(true);
      countdownTimeline.stop();

      // Chỉ gửi yêu cầu kết thúc lên Server nếu phiên đấu giá thực sự ĐANG MỞ
      if (currentAuction.getStatus() == org.deptrai.auctionsystem.shared.models.auction.AuctionStatus.OPEN ||
              currentAuction.getStatus() == org.deptrai.auctionsystem.shared.models.auction.AuctionStatus.RUNNING) {

        SocketClient.runAsync(() -> {
          Message request = new Message("FINISH_AUCTION", currentAuction.getAuctionId());
          SocketClient.sendRequest(request);
        });
      }
    } else {
      expiryTimerLabel.setText(String.format("%02d:%02d:%02d",
          res.toHours(), res.toMinutesPart(), res.toSecondsPart()));
    }
  }

  // SỬA NÚT QUAY LẠI: Kiểm tra kỹ đường dẫn này!
  @FXML
  public void handleGoBack(ActionEvent event) {
    if (countdownTimeline != null) countdownTimeline.stop();
    // Removing observer
    SocketClient.removeListener(this);

    // Kiểm tra xem file của bạn là home-view.fxml hay auction-floor.fxml
    SceneManager.getInstance().switchScene(
        "/org/deptrai/auctionsystem/client/views/home-view.fxml",
        "Trang chủ"
    );
  }

  @FXML
  private void handlePlaceBid() {
    try {
      double amount;
      // KIỂM TRA XEM NGƯỜI DÙNG ĐANG DÙNG MODE NÀO
      if (radioQuick.isSelected()) {
        amount = currentIntendedBid;
      } else {
        amount = Double.parseDouble(bidAmountField.getText());

        amount = Math.round(amount * 100.0) / 100.0; // Ép làm tròn khoảng cách 0.01
      }
      if (amount < currentAuction.getCurrentPrice() + 0.01) {
        showError("Giá đặt phải lớn hơn giá hiện tại ít nhất $0.01!");
        return;
      }

      User currentUser = SessionManager.getInstance().getCurrentUser();

      Message bidReq = new Message("PLACE_BID", new Object[]{currentAuction.getAuctionId(), currentUser.getUserId(), amount});

      SocketClient.runAsync(() -> {
        Message res = SocketClient.sendRequest(bidReq);
        Platform.runLater(() -> {
          if ("SUCCESS".equals(res.getStatus())) {
            bidAmountField.clear();
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Đặt giá thành công!");
            a.show();
          } else {
            String realErrorMessage = "Không nhận được phản hồi từ Server (Mất kết nối).";

            if (res.getData() instanceof String) {
              // Nếu Server trả về lỗi dạng chuỗi String
              realErrorMessage = (String) res.getData();
            } else {
              // Nếu Server trả về object khác hoặc null
              realErrorMessage = "Server từ chối yêu cầu nhưng không rõ lý do. Trạng thái: " + res.getStatus();
            }

            showError(realErrorMessage);
          }
        });
      });

    } catch (Exception e) {
      System.out.println(e.getMessage());
      showError("Vui lòng nhập giá hợp lệ.");
    }
  }

  @FXML
  private void handleIncreaseBid() {
    // Tăng lên 1 khoảng step dựa theo giá dự định hiện tại
    currentIntendedBid += getIncrementStep(currentIntendedBid);
    quickBidLabel.setText(String.format("$%.2f", currentIntendedBid));
  }

  @FXML
  private void handleDecreaseBid() {
    double current = currentAuction.getCurrentPrice();
    double step = getIncrementStep(currentIntendedBid);

    // Chỉ cho phép giảm nếu giá sau khi giảm vẫn cao hơn giá hiện tại của phiên
    if (currentIntendedBid - step > current) {
      currentIntendedBid -= step;
    } else {
      // Ép về mức giá hợp lệ thấp nhất
      currentIntendedBid = current + getIncrementStep(current);
    }
    quickBidLabel.setText(String.format("$%.2f", currentIntendedBid));
  }

  @FXML
  public void handleActivateAutoBid(ActionEvent event) {
    String auctionId = currentAuction.getAuctionId();

    if(AutoBidManager.getInstance().isAutoBidActive(auctionId)) {
      AutoBidManager.getInstance().stopAutoBid(auctionId);
      btnAutoBid.setText("⚙️ KÍCH HOẠT AUTO-BID");
      btnAutoBid.setStyle("");
      maxBidField.setDisable(false);
      incrementField.setDisable(false);
      return;
    }

    try {
      double maxBid = Double.parseDouble(maxBidField.getText());
      double increment = Double.parseDouble(incrementField.getText());
      if(maxBid <= currentAuction.getCurrentPrice()) {
        showError("Giới hạn Max phải lớn hơn mức giá hiện tại!");
        return;
      }

      AutoBidManager.getInstance().startAutoBid(currentAuction, maxBid, increment);

      maxBidField.setDisable(true);
      incrementField.setDisable(true);
      btnAutoBid.setText("🛑 ĐANG CHẠY AUTO-BID (BẤM ĐỂ TẮT)");
      btnAutoBid.setStyle("-fx-background-color: #ff003c; -fx-text-fill: white; -fx-border-color: transparent;");
    } catch (NumberFormatException e) {
      showError("Vui lòng nhập số tiền hợp lệ!");
    }
  }

  // Hàm vẽ lại biểu đồ
  private void refreshChart(List<Bid> allBids) {
    priceSeries.getData().clear(); // Xóa khung vẽ cũ
    if (allBids == null || allBids.isEmpty()) return;

    int MAX_POINTS = 10; // Set số lượng điểm trên biểu đồ
    List<Bid> displayBids = new ArrayList<>();

    // Nếu số lượng ít thì lấy hết
    if (allBids.size() <= MAX_POINTS) {
      displayBids.addAll(allBids);
    } else {
      // Luôn lấy điểm đầu tiên được đặt
      displayBids.add(allBids.getFirst());

      // Chia đều khoảng cách để bốc mẫu các điểm ở giữa
      double step = (double) (allBids.size() - 1) / (MAX_POINTS - 1);
      for (int i = 1; i < MAX_POINTS - 1; i++) {
        int index = (int) Math.round(i * step);
        displayBids.add(allBids.get(index));
      }

      // Luôn lấy điểm mới nhất vừa được đặt
      displayBids.add(allBids.getLast());
    }

    // Vẽ danh sách đã nén lên màn hình
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm:ss");
    for (Bid b : displayBids) {
      String timeStr = b.getTimestamp().format(formatter);
      priceSeries.getData().add(new XYChart.Data<>(timeStr, b.getAmount()));
    }
  }

  private double getIncrementStep(double price) {
    if (price < 10) return 0.5;
    if (price < 100) return 1.0;
    if (price < 500) return 5.0;
    if (price < 1000) return 10.0;
    if (price < 5000) return 50.0;
    if (price < 10000) return 100.0;
    if (price < 50000) return 500.0;
    return 1000.0;
  }

  @Override
  public void onAuctionUpdated(Auction updatedAuction) {
    // Nếu màn hình này đang xem đúng cái sản phẩm vừa được ai đó đặt giá
    if (this.currentAuction.getAuctionId().equals(updatedAuction.getAuctionId())) {
      this.currentAuction = updatedAuction;

      // Tạo bản sao và ép sắp xếp theo đúng thứ tự thời gian tăng dần
      List<Bid> sortedBids = new ArrayList<>(updatedAuction.getBids());
      sortedBids.sort(Comparator.comparing(Bid::getTimestamp));

      Platform.runLater(() -> {
        // 1. Cập nhật giá tiền mới nhất
        currentPriceLabel.setText(String.format("$%.2f", updatedAuction.getCurrentPrice()));

        // 2. Nạp lại toàn bộ danh sách Bid từ Server (đảm bảo đúng thứ tự và đủ số lượng)
        bidHistoryTable.getItems().setAll(sortedBids);

        // 3. Cuộn xuống cái cuối cùng
        if (!sortedBids.isEmpty()) {
          bidHistoryTable.scrollTo(sortedBids.size() - 1);
        }

        double newPrice = updatedAuction.getCurrentPrice();
        // Nếu iá định đặt của mình đang BÉ HƠN HOẶC BẰNG giá của thằng vừa đặt
        if (currentIntendedBid <= newPrice) {
          // Tự động đẩy giá định đặt của mình lên một mức hợp lệ mới
          currentIntendedBid = newPrice + getIncrementStep(newPrice);
          quickBidLabel.setText(String.format("$%.2f", currentIntendedBid));
        }

        refreshChart(sortedBids);
      });
    }
  }

  private void showError(String msg) {
    Alert alert = new Alert(Alert.AlertType.ERROR, msg);
    alert.show();
  }
}