package org.deptrai.auctionsystem;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import org.deptrai.auctionsystem.client.utils.SocketClient;
import org.deptrai.auctionsystem.server.ClientHandler;
import org.deptrai.auctionsystem.server.utils.DatabaseConnection;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.shared.network.Message;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthenticationTest {

  private static ServerSocket serverSocket;
  private static Thread serverThread;

  // Dùng chung một thông tin tài khoản cho chuỗi Test liên hoàn
  private static final String TEST_USERNAME = "testUser_" + System.currentTimeMillis();
  private static final String TEST_EMAIL = "test_" + System.currentTimeMillis() + "@gmail.com";
  private static final String VALID_PASSWORD = "StrongPassword123!"; // Khớp với ValidationUtils
  private static final String INVALID_PASSWORD = "weak";

  @BeforeAll
  static void setUp() throws Exception {
    // 1. Khởi tạo Database (tạo bảng nếu chưa có)
    DatabaseConnection.initializeDatabase();

    // 2. Chạy Server trên một port riêng biệt (ví dụ 5005) để không đụng port chính 5000
    serverSocket = new ServerSocket(5005);
    serverThread = new Thread(() -> {
      try {
        while (!serverSocket.isClosed()) {
          Socket clientSocket = serverSocket.accept();
          ClientHandler handler = new ClientHandler(clientSocket);
          // Cho ClientHandler chạy trên 1 luồng riêng để lắng nghe Message
          new Thread(handler).start();
        }
      } catch (IOException e) {
        // Exception bị ném ra khi serverSocket.close() được gọi ở AfterAll, có thể bỏ qua
      }
    });
    serverThread.start();

    // 3. Client kết nối tới Server test
    SocketClient.connect("localhost", 5005);
  }

  @AfterAll
  static void tearDown() throws Exception {
    // Đóng kết nối mạng sau khi test xong
    SocketClient.disconnect();
    if (serverSocket != null && !serverSocket.isClosed()) {
      serverSocket.close();
    }
    if (serverThread != null) {
      serverThread.interrupt();
    }
  }

  // ==========================================
  // TESTS CHO METHOD: handleRegister
  // ==========================================

  @Test
  @Order(1)
  void testRegister_InvalidPassword_ShouldFail() {
    // Chuẩn bị dữ liệu với mật khẩu yếu
    String[] registerData = {TEST_USERNAME, INVALID_PASSWORD, TEST_EMAIL, "BIDDER"};
    Message request = new Message("REGISTER", registerData);

    Message response = SocketClient.sendRequest(request);

    assertEquals("FAIL", response.getStatus(), "Đăng ký phải thất bại do mật khẩu yếu");
    assertTrue(((String) response.getData()).contains("Mật khẩu bao gồm"), "Nên trả về câu cảnh báo mật khẩu");
  }

  @Test
  @Order(2)
  void testRegister_ValidData_ShouldSucceed() {
    // Chuẩn bị dữ liệu hợp lệ
    String[] registerData = {TEST_USERNAME, VALID_PASSWORD, TEST_EMAIL, "BIDDER"};
    Message request = new Message("REGISTER", registerData);

    Message response = SocketClient.sendRequest(request);

    assertEquals("SUCCESS", response.getStatus(), "Đăng ký tài khoản mới phải thành công");
    assertEquals("Đăng ký thành công", response.getData());
  }

  @Test
  @Order(3)
  void testRegister_DuplicateUsername_ShouldFail() {
    // Cố tình đăng ký lại chính Username vừa thành công ở Test 2
    String[] registerData = {TEST_USERNAME, VALID_PASSWORD, "anotheremail@gmail.com", "BIDDER"};
    Message request = new Message("REGISTER", registerData);

    Message response = SocketClient.sendRequest(request);

    assertEquals("FAIL", response.getStatus(), "Phải thất bại vì Username đã tồn tại");
    assertEquals("Tên đăng nhập đã tồn tại!", response.getData());
  }

  // ==========================================
  // TESTS CHO METHOD: handleLogin
  // ==========================================

  @Test
  @Order(4)
  void testLogin_WrongPassword_ShouldFail() {
    // Đăng nhập với username đúng nhưng sai mật khẩu
    String[] loginData = {TEST_USERNAME, "WrongPass123!"};
    Message request = new Message("LOGIN", loginData);

    Message response = SocketClient.sendRequest(request);

    assertEquals("FAIL", response.getStatus(), "Đăng nhập phải thất bại");
    assertEquals("Sai tên đăng nhập hoặc mật khẩu.", response.getData());
  }

  @Test
  @Order(5)
  void testLogin_WrongUsername_ShouldFail() {
    String[] loginData = {"UserKhongTonTai", VALID_PASSWORD};
    Message request = new Message("LOGIN", loginData);

    Message response = SocketClient.sendRequest(request);

    assertEquals("FAIL", response.getStatus(), "Đăng nhập phải thất bại do User không tồn tại");
  }

  @Test
  @Order(6)
  void testLogin_ValidCredentials_ShouldSucceed() {
    // Đăng nhập với tài khoản hợp lệ (vừa đăng ký ở Test 2)
    String[] loginData = {TEST_USERNAME, VALID_PASSWORD};
    Message request = new Message("LOGIN", loginData);

    Message response = SocketClient.sendRequest(request);

    assertEquals("SUCCESS", response.getStatus(), "Đăng nhập với mật khẩu đúng phải thành công");
    assertNotNull(response.getData(), "Dữ liệu trả về không được null");
    assertTrue(response.getData() instanceof User, "Dữ liệu trả về phải là một Object của User");

    User loggedInUser = (User) response.getData();
    assertEquals(TEST_USERNAME, loggedInUser.getUsername(), "Username nhận được phải khớp với DB");
  }
}
