package org.deptrai.auctionsystem;

import org.deptrai.auctionsystem.server.ClientHandler;
import org.deptrai.auctionsystem.server.ServerMain;
import org.junit.jupiter.api.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.deptrai.auctionsystem.shared.network.Message;
import static org.junit.jupiter.api.Assertions.*;

public class ServerMainTest {

  private ServerSocket dummyServerSocket;
  private List<Socket> clientSockets;
  private List<ObjectInputStream> clientInputs;

  @BeforeEach
  void setUp() throws Exception {
    clientSockets = new ArrayList<>();
    clientInputs = new ArrayList<>();

    // 1. Mở một cổng phụ (5010) để test, tránh đụng cổng 5000 của Server thật
    dummyServerSocket = new ServerSocket(5010);

    // Xóa sạch danh sách client cũ của hệ thống (nếu có)
    ServerMain.activeClients.clear();

    // 2. Giả lập 2 Client kết nối vào hệ thống
    for (int i = 0; i < 2; i++) {

      // Mảng tạm để hứng dữ liệu từ Thread của Client
      Socket[] tempSocket = new Socket[1];
      ObjectInputStream[] tempIn = new ObjectInputStream[1];

      // Chạy Client trên một luồng riêng để đi qua quá trình Handshake với Server
      Thread clientThread = new Thread(() -> {
        try {
          tempSocket[0] = new Socket("localhost", 5010);
          // Bắt buộc mở luồng Output trước
          ObjectOutputStream out = new ObjectOutputStream(tempSocket[0].getOutputStream());
          out.flush();

          // LƯU Ý: Lệnh này sẽ bị BLOCK cho đến khi Server mở luồng Output tương ứng
          tempIn[0] = new ObjectInputStream(tempSocket[0].getInputStream());
        } catch (Exception e) {
          e.printStackTrace();
        }
      });
      clientThread.start();

      // Server chấp nhận kết nối
      Socket serverSideSocket = dummyServerSocket.accept();

      // SỬA LỖI DEADLOCK Ở ĐÂY:
      // Bắt buộc phải khởi tạo và Start ClientHandler NGAY LẬP TỨC để Server tạo ObjectOutputStream
      // Nhờ đó, Client ở trên mới nhận được Header và thoát khỏi trạng thái BLOCK
      ClientHandler handler = new ClientHandler(serverSideSocket);
      ServerMain.activeClients.add(handler);
      new Thread(handler).start();

      // Sau khi Server đã "mở cửa", lúc này ta join() chờ Client chạy xong là an toàn 100%
      clientThread.join();

      // Lưu lại thông tin Client để test
      clientSockets.add(tempSocket[0]);
      clientInputs.add(tempIn[0]);
    }

    // Đợi một chút xíu để các Handler chuẩn bị sẵn sàng
    Thread.sleep(300);
  }

  @AfterEach
  void tearDown() throws Exception {
    ServerMain.activeClients.clear();
    for (Socket s : clientSockets) {
      if (s != null && !s.isClosed()) s.close();
    }
    if (dummyServerSocket != null && !dummyServerSocket.isClosed()) {
      dummyServerSocket.close();
    }
  }

  // ==========================================
  // TEST 1: TÍNH NĂNG GỬI THÔNG BÁO CHUNG
  // ==========================================
  @Test
  @DisplayName("Test Broadcast - Gửi tin nhắn đồng thời cho tất cả Client")
  void testBroadcast_ShouldSendToAllActiveClients() throws Exception {
    // 1. Tạo một Message thông báo
    Message broadcastMsg = new Message("SUCCESS", "AUCTION_UPDATE", "Có giá mới!");

    // 2. Gọi hàm broadcast của ServerMain
    ServerMain.broadcast(broadcastMsg);

    // 3. Kiểm tra xem CẢ 2 Client có nhận được chính xác tin nhắn này không
    for (int i = 0; i < clientInputs.size(); i++) {
      ObjectInputStream in = clientInputs.get(i);

      // Đọc tin nhắn truyền tới qua mạng
      Message received = (Message) in.readObject();

      assertNotNull(received, "Client thứ " + (i+1) + " phải nhận được tin nhắn");
      assertEquals("AUCTION_UPDATE", received.getCommand());
      assertEquals("Có giá mới!", received.getData(), "Nội dung tin nhắn phải khớp");
    }
  }

  // ==========================================
  // TEST 2: QUẢN LÝ DANH SÁCH ONLINE
  // ==========================================
  @Test
  @DisplayName("Test Active Clients - Tự động xóa Client khi ngắt kết nối")
  void testActiveClients_RemovalWhenClientDisconnects() throws Exception {
    assertEquals(2, ServerMain.activeClients.size(), "Ban đầu phải có đúng 2 client online");

    // 1. Giả lập Client 0 bị mất mạng đột ngột (Rút cáp)
    clientSockets.get(0).close();

    // 2. Cố tình gửi broadcast
    Message testMsg = new Message("INFO", "PING", "Ping check mạng");
    ServerMain.broadcast(testMsg);

    // Đợi 200 mili-giây để Exception trong ClientHandler kịp kích hoạt hàm remove()
    Thread.sleep(200);

    // 3. Kiểm tra lại danh sách
    assertEquals(1, ServerMain.activeClients.size(), "Danh sách activeClients phải tự động giảm xuống còn 1");
  }
}