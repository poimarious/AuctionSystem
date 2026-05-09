package org.deptrai.auctionsystem.server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.deptrai.auctionsystem.server.dao.UserDAO;
import org.deptrai.auctionsystem.shared.models.users.Admin;
import org.deptrai.auctionsystem.shared.models.users.Bidder;
import org.deptrai.auctionsystem.shared.models.users.Seller;
import org.deptrai.auctionsystem.shared.models.users.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;
import org.deptrai.auctionsystem.shared.network.Message;

public class ClientHandler implements Runnable {
  private Socket socket;
  private UserDAO userDAO;
  private ObjectOutputStream out;
  private ObjectInputStream in;

  public ClientHandler(Socket socket) {
    this.socket = socket;
    this.userDAO = new UserDAO();
  }

  @Override
  public void run() {
    try {
      out = new ObjectOutputStream(socket.getOutputStream()); // Output always before
      in = new ObjectInputStream(socket.getInputStream());

      Message request;
      while ((request = (Message) in.readObject()) != null) {
        switch (request.getCommand()) {
          case "LOGIN":
            handleLogin(request);
            break;
          case "REGISTER":
            handleRegister(request);
            break;
          case "TOP_UP":
            handleTopUp(request);
            break;
          default:
            out.writeObject(new Message("FAIL", "COMMAND", "Lệnh không hợp lệ hoặc chưa được Server hỗ trợ!"));
            out.flush();
            break;
        }
      }
    } catch (Exception e) {
      System.out.println("Client ngắt kết nối.");
    } finally {
      try {
        socket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void handleLogin(Message request) {
    // Data ta quy ước Client gửi sang là mảng String[] {username, password}
    String[] credentials = (String[]) request.getData();
    String username = credentials[0];
    String password = credentials[1];

    User user = userDAO.getUserByUsername(username);

    try {
      if (user != null && user.getPassword().equals(password)) {
        out.writeObject(new Message("SUCCESS", "LOGIN", user));
      } else {
        out.writeObject(new Message("FAIL", "LOGIN", "Sai tên đăng nhập hoặc mật khẩu."));
      }
      out.flush();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void handleRegister(Message request) {
    String[] data = (String[]) request.getData();
    String username = data[0];
    String password = data[1];
    String email = data[2];
    String role = data[3];

    try {
      if (userDAO.isUsernameTaken(username)) {
        out.writeObject(new Message("FAIL", "REGISTER", "Tên đăng nhập đã tồn tại!"));
        out.flush();
        return;
      }

      User newUser; // Admin account cannot be registered
      if (role.equals("SELLER")) {
        newUser = new Seller(null, username, password, email);
      } else {
        newUser = new Bidder(null, username, password, email, new java.util.concurrent.CopyOnWriteArrayList<>());
      }

      boolean success = userDAO.insertUser(newUser, role);
      if (success) {
        out.writeObject(new Message("SUCCESS", "REGISTER", "Đăng ký thành công"));
      } else {
        out.writeObject(new Message("FAIL", "REGISTER", "Lỗi DB khi tạo tài khoản."));
      }
      out.flush();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // For top-up buttons (Dùng cho mấy nút nạp tiền ấy)
  private void handleTopUp(Message request) {
    // Dữ liệu Client gửi sang sẽ gồm: [userId, amount]
    Object[] data = (Object[]) request.getData();
    String userId = (String) data[0];
    double amount = (Double) data[1];

    User user = userDAO.getUserById(userId);
    if (user != null) {
      double newBalance = user.getBalance() + amount;
      if (userDAO.updateBalance(userId, newBalance)) {
        try {
          out.writeObject(new Message("SUCCESS", "TOP_UP", newBalance));
          out.flush();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        return;
      }
    }
    try {
      out.writeObject(new Message("FAIL", "TOP_UP", "Lỗi cập nhật số dư."));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}