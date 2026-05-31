package org.deptrai.auctionsystem;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import org.deptrai.auctionsystem.client.utils.SocketClient;
import org.deptrai.auctionsystem.server.ClientHandler;
import org.deptrai.auctionsystem.server.dao.NotificationDAO;
import org.deptrai.auctionsystem.server.dao.UserDAO;
import org.deptrai.auctionsystem.server.utils.DatabaseConnection;
import org.deptrai.auctionsystem.shared.models.users.Bidder;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.shared.network.Message;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NotificationSystemTest {

  private static ServerSocket serverSocket;
  private static Thread serverThread;
  private static User testUser;
  private static NotificationDAO notiDAO;

  @BeforeAll
  static void setUp() throws Exception {
    DatabaseConnection.initializeDatabase();
    UserDAO userDAO = new UserDAO();
    notiDAO = new NotificationDAO();

    // 1. Khởi chạy Server ảo trên cổng 5010 (Để không đụng chạm các bài test khác)
    serverSocket = new ServerSocket(0);
    int testPort = serverSocket.getLocalPort();
    serverThread = new Thread(() -> {
      try {
        while (!serverSocket.isClosed()) {
          Socket clientSocket = serverSocket.accept();
          ClientHandler handler = new ClientHandler(clientSocket);
          new Thread(handler).start();
        }
      } catch (IOException e) {}
    });
    serverThread.start();

    // 2. Kết nối Client
    SocketClient.connect("localhost", testPort);

    // 3. Tạo một người dùng ảo để nhận thông báo
    String username = "noti_user_" + System.currentTimeMillis();
    testUser = new Bidder(null, username, "Test1234!", username + "@gmail.com", new java.util.concurrent.CopyOnWriteArrayList<>());
    userDAO.insertUser(testUser, "BIDDER");
    testUser = userDAO.getUserByUsername(username);
  }

  @AfterAll
  static void tearDown() throws Exception {
    SocketClient.disconnect();
    if (serverSocket != null) serverSocket.close();
    if (serverThread != null) serverThread.interrupt();
  }

  // ==========================================
  // BÀI TEST 1: ĐỌC VÀ XÓA THÔNG BÁO (READ AND DESTROY)
  // ==========================================
  @Test
  @Order(1)
  void testGetNotifications_WithExistingNotifs_ShouldReturnAndClear() {
    // 1. SETUP: Bơm 2 thông báo ảo vào DB cho user này
    notiDAO.insertNotification(testUser.getUserId(), "Thông báo số 1: Bạn đã thắng!");
    notiDAO.insertNotification(testUser.getUserId(), "Thông báo số 2: Đồ của bạn đã được thanh toán.");

    // 2. ACTION: Gửi request lấy thông báo lên Server
    Message request = new Message("GET_NOTIFICATIONS", testUser.getUserId());
    Message response = SocketClient.sendRequest(request);

    // 3. ASSERT: Kiểm tra response trả về xem có đủ 2 thông báo không
    assertEquals("SUCCESS", response.getStatus());

    @SuppressWarnings("unchecked")
    List<String> notifs = (List<String>) response.getData();

    assertNotNull(notifs);
    assertEquals(2, notifs.size(), "Phải lấy được đúng 2 thông báo vừa tạo");
    assertTrue(notifs.contains("Thông báo số 1: Bạn đã thắng!"));
    assertTrue(notifs.contains("Thông báo số 2: Đồ của bạn đã được thanh toán."));

    // 4. KIỂM TRA QUAN TRỌNG NHẤT: Đảm bảo DB đã tự động quét sạch thông báo
    List<String> remainingNotifs = notiDAO.getUnreadNotifications(testUser.getUserId());
    assertEquals(0, remainingNotifs.size(), "LỖI: Thông báo dưới DB chưa bị xóa sạch sau khi đọc!");

    System.out.println(">> [NOTIFICATION TEST 1] Lấy thông báo thành công và DB đã được dọn dẹp!");
  }

  // ==========================================
  // BÀI TEST 2: LẤY THÔNG BÁO KHI TRỐNG (EMPTY STATE)
  // ==========================================
  @Test
  @Order(2)
  void testGetNotifications_WithEmptyNotifs_ShouldReturnEmptyList() {
    // LƯU Ý: Ở Test 1, hệ thống đã quét sạch DB rồi, nên lúc này hộp thư phải đang rỗng

    Message request = new Message("GET_NOTIFICATIONS", testUser.getUserId());
    Message response = SocketClient.sendRequest(request);

    assertEquals("SUCCESS", response.getStatus());

    @SuppressWarnings("unchecked")
    List<String> notifs = (List<String>) response.getData();

    assertNotNull(notifs);
    assertEquals(0, notifs.size(), "Danh sách thông báo trả về phải rỗng do không có thông báo mới");

    System.out.println(">> [NOTIFICATION TEST 2] Trả về danh sách rỗng chính xác khi không có thông báo!");
  }
}