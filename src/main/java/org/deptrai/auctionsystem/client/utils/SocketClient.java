package org.deptrai.auctionsystem.client.utils;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.network.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.*;

public class SocketClient {
  private static Socket socket;
  private static ObjectOutputStream out;
  private static ObjectInputStream in;

  private static final BlockingQueue<Message> responseQueue = new ArrayBlockingQueue<>(1);
  private static final List<AuctionUpdateListener> listeners = new CopyOnWriteArrayList<>();

  private static ExecutorService clientExecutor = Executors.newCachedThreadPool();

  public static void runAsync(Runnable task) {
    clientExecutor.submit(task);
  }

  public static void addListener(AuctionUpdateListener listener) {
    listeners.add(listener);
  }

  public static void removeListener(AuctionUpdateListener listener) {
    listeners.remove(listener);
  }

  public static void connect(String serverAddress, int port) {
    try {
      socket = new Socket(serverAddress, port);
      out = new ObjectOutputStream(socket.getOutputStream()); // Output is always first to avoid deadlock
      in = new ObjectInputStream(socket.getInputStream());
      System.out.println("Đã kết nối tới Server thành công!");

      if(clientExecutor == null || clientExecutor.isShutdown() || clientExecutor.isTerminated()) {
        clientExecutor = Executors.newCachedThreadPool();
      }

      // Listening at all time
      Thread listenerThread =
          new Thread(
              () -> {
                try {
                  Message msg;
                  while ((msg = (Message) in.readObject()) != null) {
                    if (msg.getCommand().equals("PUSH_NOTIFICATION_BELL")) {
                      String msgText = (String) msg.getData();
                      SessionManager.getInstance().addNotification(msgText);
                    } else if (msg.getCommand().equals("AUCTION_UPDATE")) {
                      // Nhận được Broadcast -> Báo cho giao diện cập nhật ngay lập tức
                      Auction updatedAuction = (Auction) msg.getData();

                      for (AuctionUpdateListener listener : listeners) {
                        Platform.runLater(() -> listener.onAuctionUpdated(updatedAuction));
                      }
                    } else if (msg.getCommand().equals("FORCE_LOGOUT")) {
                      String banMessage = (String) msg.getData();

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
                    } else {
                      // Tin nhắn trả lời bình thường -> Nhét vào hàng đợi cho hàm sendRequest lấy
                      responseQueue.put(msg);
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
    if (out == null || in == null) return new Message("FAIL", "NETWORK", "Chưa kết nối");
    try {
      out.writeObject(request);
      out.flush();
      // Đứng im tại đây chờ listenerThread ném kết quả vào hàng đợi (Thay vì tự đọc)
      return responseQueue.take();
    } catch (Exception e) {
      return new Message("FAIL", "NETWORK", e.getMessage());
    }
  }

  public static void disconnect() {
    try {
      if (socket != null) socket.close();
      clientExecutor.shutdown();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
