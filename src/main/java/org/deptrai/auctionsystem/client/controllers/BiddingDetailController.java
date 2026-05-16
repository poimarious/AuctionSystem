package org.deptrai.auctionsystem.client.controllers;

import java.io.File;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
import org.deptrai.auctionsystem.client.utils.AuctionUpdateListener;
import org.deptrai.auctionsystem.client.utils.SceneManager;
import org.deptrai.auctionsystem.client.utils.SessionManager;
import org.deptrai.auctionsystem.client.utils.SocketClient;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.auction.AuctionStatus;
import org.deptrai.auctionsystem.shared.models.bid.Bid;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.shared.network.Message;

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
  private XYChart.Series<String, Number> priceSeries;

  private Auction currentAuction;
  private Timeline countdownTimeline;

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

    // Tự động load dữ liệu từ Session khi vừa vào trang
    String selectedId = SessionManager.getInstance().getSelectedAuctionId();

    if (selectedId != null) {
      // Bọc vào Thread phụ để không làm đơ ứng dụng khi tải
      new Thread(() -> {
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
      }).start();
    }
  }

  public void setAuctionData(Auction auction) {
    this.currentAuction = auction;
    nameLabel.setText(auction.getItem().getName());
    descriptionLabel.setText(auction.getItem().getDescription());
    currentPriceLabel.setText(String.format("$%.2f", auction.getCurrentPrice()));

    String imagePath = auction.getItem().getImageUrl();
    if(imagePath != null && !imagePath.isEmpty()) {
      try {
        File imgfile = new File(imagePath);
        if(imgfile.exists()) {
          Image image = new Image(imgfile.toURI().toString());
          productImageView.setImage(image);
        }
      } catch (Exception e) {
        System.err.println("Không thể load ảnh thật từ đường dẫn: " + imagePath);
      }
    }
    List<Bid> bids = new ArrayList<>(auction.getBids());
    bids.sort(Comparator.comparing(Bid::getTimestamp));
    bidHistoryTable.getItems().setAll(bids);

    refreshChart(bids);

    SocketClient.addListener(this); // New observer

    startTimer();
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

        new Thread(() -> {
          Message request = new Message("FINISH_AUCTION", currentAuction.getAuctionId());
          SocketClient.sendRequest(request);
        }).start();
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
        "Trang chủ - Auction.UET"
    );
  }

  @FXML
  private void handlePlaceBid() {
    try {
      double amount = Double.parseDouble(bidAmountField.getText());
      User currentUser = SessionManager.getInstance().getCurrentUser();

      Message bidReq = new Message("PLACE_BID", new Object[]{currentAuction.getAuctionId(), currentUser.getUserId(), amount});
      new Thread(() -> {
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
      }).start();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      showError("Vui lòng nhập giá hợp lệ.");
    }
  }

  // Hàm vẽ lại biểu đồ với thuật toán Downsampling (Tối đa 15 điểm)
  private void refreshChart(List<Bid> allBids) {
    priceSeries.getData().clear(); // Xóa khung vẽ cũ
    if (allBids == null || allBids.isEmpty()) return;

    int MAX_POINTS = 10;
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

        refreshChart(sortedBids);
      });
    }
  }

  private void showError(String msg) {
    Alert alert = new Alert(Alert.AlertType.ERROR, msg);
    alert.show();
  }
}