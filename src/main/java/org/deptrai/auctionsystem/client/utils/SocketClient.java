package org.deptrai.auctionsystem.client.utils;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.network.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.*;

public class SocketClient {

  private static final Logger logger = LoggerFactory.getLogger(SocketClient.class);

  private static Socket socket;
  private static ObjectOutputStream out;
  private static ObjectInputStream in;

  private static final BlockingQueue<Message> responseQueue = new ArrayBlockingQueue<>(1);
  private static final List<AuctionUpdateListener> listeners = new CopyOnWriteArrayList<>();

  private static ExecutorService clientExecutor = Executors.newCachedThreadPool();

  public static void runAsync(Runnable task) {
    // [BẢO VỆ 1]: Tránh lỗi RejectedExecutionException nếu luồng lỡ bị gọi sau khi tắt mạng
    if (clientExecutor != null && !clientExecutor.isShutdown()) {
      clientExecutor.submit(task);
    } else {
      logger.warn("Thread Pool đã tắt, bỏ qua task bất đồng bộ.");
    }
  }

  public static void addListener(AuctionUpdateListener listener) {
    if (!listeners.contains(listener)) {
      listeners.add(listener);
    }
  }

  public static void removeListener(AuctionUpdateListener listener) {
    listeners.remove(listener);
  }

  public static void connect(String serverAddress, int port) {
    try {
      // [BẢO VỆ 2]: Dọn sạch rác của bài test trước, tránh nghẽn hàng đợi
      responseQueue.clear();

      socket = new Socket(serverAddress, port);
      out = new ObjectOutputStream(socket.getOutputStream()); // Output is always first
      in = new ObjectInputStream(socket.getInputStream());
      System.out.println("Đã kết nối tới Server thành công!");

      if (clientExecutor == null || clientExecutor.isShutdown() || clientExecutor.isTerminated()) {
        clientExecutor = Executors.newCachedThreadPool();
      }

      // Listening at all time
      Thread listenerThread =
          new Thread(
              () -> {
                try {
                  Message msg;
                  while ((msg = (Message) in.readObject()) != null) {
                    switch (msg.getCommand()) {
                      case "PUSH_NOTIFICATION_BELL" -> {
                        String msgText = (String) msg.getData();
                        SessionManager.getInstance().addNotification(msgText);
                      }
                      case "AUCTION_UPDATE" -> {
                        Auction updatedAuction = (Auction) msg.getData();

                        for (AuctionUpdateListener listener : listeners) {
                          // [BẢO VỆ 3]: Try-catch để không bị lỗi khi chạy Test thuần không có JavaFX UI
                          try {
                            Platform.runLater(() -> listener.onAuctionUpdated(updatedAuction));
                          } catch (IllegalStateException e) {
                            listener.onAuctionUpdated(updatedAuction);
                          }
                        }
                      }
                      case "FORCE_LOGOUT" -> {
                        String banMessage = (String) msg.getData();

                        try {
                          Platform.runLater(
                              () -> {
                                SessionManager.getInstance().logout();
                                SceneManager.getInstance().clearHistory();
                                SceneManager.getInstance().switchScene("/org/deptrai/auctionsystem/client/views/login-view.fxml", "Đăng nhập");

                                Alert alert = new Alert(AlertType.ERROR);
                                alert.setTitle("TÀI KHOẢN BỊ CẤM");
                                alert.setHeaderText("Bạn đã bị buộc đăng xuất!");
                                alert.setContentText(banMessage);
                                alert.show();
                              });
                        } catch (IllegalStateException e) {
                          SessionManager.getInstance().logout();
                        }
                      }
                      default -> {
                        // [BẢO VỆ 4]: Xóa tin nhắn cũ bị kẹt (nếu có) trước khi put cái mới để luồng không bị kẹt chết (Deadlock)
                        responseQueue.clear();
                        responseQueue.put(msg);
                      }
                    }
                  }
                } catch (Exception e) {
                  System.out.println("Luồng lắng nghe ngắt kết nối.");
                }
              });
      listenerThread.setDaemon(true); // Tự động chết khi tắt App
      listenerThread.start();

    } catch (IOException e) {
      System.out.println("Không thể kết nối mạng: " + e.getMessage());
    }
  }

  // Nhận vào 1 Message, trả về 1 Message
  public static synchronized Message sendRequest(Message request) {
    if (out == null || in == null || socket == null || socket.isClosed()) {
      return new Message("FAIL", "NETWORK", "Chưa kết nối");
    }
    try {
      out.writeObject(request);
      out.flush();
      return responseQueue.take();
    } catch (Exception e) {
      return new Message("FAIL", "NETWORK", e.getMessage());
    }
  }

  public static void disconnect() {
    try {
      if (socket != null) socket.close();
      if (clientExecutor != null && !clientExecutor.isShutdown()) {
        clientExecutor.shutdownNow(); // Ép chết ngay lập tức thay vì đợi
      }
      listeners.clear(); // Xóa sạch listener của test cũ
      responseQueue.clear(); // Dọn hàng đợi
    } catch (IOException e) {
      logger.error(e.getMessage());
    }
  }
}