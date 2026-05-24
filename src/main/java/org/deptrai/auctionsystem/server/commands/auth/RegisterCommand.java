package org.deptrai.auctionsystem.server.commands.auth;

import java.util.concurrent.CopyOnWriteArrayList;
import org.deptrai.auctionsystem.server.ClientHandler;
import org.deptrai.auctionsystem.server.commands.Command;
import org.deptrai.auctionsystem.server.dao.UserDAO;
import org.deptrai.auctionsystem.server.exceptions.AuthenticationException;
import org.deptrai.auctionsystem.shared.models.users.Bidder;
import org.deptrai.auctionsystem.shared.models.users.Seller;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.shared.network.Message;
import org.deptrai.auctionsystem.utils.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ObjectOutputStream;

public class RegisterCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(RegisterCommand.class);

  @Override
  public void execute(ClientHandler clientHandler, Message request, ObjectOutputStream out) throws Exception {
    String[] data = (String[]) request.getData();
    String username = data[0];
    String password = data[1];
    String email = data[2];
    String role = data[3];

    try {
      UserDAO userDAO = new UserDAO();

      // Lưới bắt lỗi xác thực
      if (ValidationUtils.isInvalidPassword(password)) {
        throw new AuthenticationException("Mật khẩu phải chứa chữ thường, in hoa, số và ký tự đặc biệt!");
      }
      if (userDAO.isUsernameTaken(username)) {
        throw new AuthenticationException("Tên đăng nhập đã tồn tại!");
      }
      if (userDAO.isEmailTaken(email)) {
        throw new AuthenticationException("Email này đã được sử dụng cho một tài khoản khác!");
      }

      // Khởi tạo User
      User newUser;
      if (role.equals("SELLER")) {
        newUser = new Seller(null, username, password, email);
      } else {
        newUser = new Bidder(null, username, password, email, new CopyOnWriteArrayList<>());
      }

      if (userDAO.insertUser(newUser, role)) {
        clientHandler.sendMessage(new Message("SUCCESS", "REGISTER", "Đăng ký thành công"));
      } else {
        throw new Exception("Không thể insertUser xuống CSDL.");
      }

    } catch (AuthenticationException e) {
      clientHandler.sendMessage(new Message("FAIL", "REGISTER", e.getMessage()));
    } catch (Exception e) {
      logger.error("Lỗi đăng ký: ", e);
      clientHandler.sendMessage(new Message("ERROR", "REGISTER", "Lỗi DB khi tạo tài khoản."));
    }
  }
}