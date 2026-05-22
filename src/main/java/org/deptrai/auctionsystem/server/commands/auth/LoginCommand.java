package org.deptrai.auctionsystem.server.commands.auth;

import org.deptrai.auctionsystem.server.ClientHandler;
import org.deptrai.auctionsystem.server.commands.Command;
import org.deptrai.auctionsystem.server.dao.UserDAO;
import org.deptrai.auctionsystem.server.exceptions.AuthenticationException;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.shared.network.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ObjectOutputStream;

public class LoginCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(LoginCommand.class);

  @Override
  public void execute(ClientHandler clientHandler, Message request, ObjectOutputStream out) throws Exception {
    String[] credentials = (String[]) request.getData();
    String username = credentials[0];
    String password = credentials[1];

    try {
      UserDAO userDAO = new UserDAO();
      User user = userDAO.getUserByUsername(username);

      // Ném lỗi nếu sai thông tin
      if (user == null || !user.getPassword().equals(password)) {
        throw new AuthenticationException("Sai tên đăng nhập hoặc mật khẩu.");
      }

      // Ném lỗi nếu bị khóa
      if (user.isBanned()) {
        throw new AuthenticationException("Tài khoản của bạn đã bị cấm!\nLý do: " + user.getBanReason());
      }

      // Đăng nhập thành công
      clientHandler.setAuthenticatedUser(user);
      out.writeObject(new Message("SUCCESS", "LOGIN", user));
      out.flush();

    } catch (AuthenticationException e) {
      out.writeObject(new Message("FAIL", "LOGIN", e.getMessage()));
      out.flush();
    } catch (Exception e) {
      logger.error("Lỗi hệ thống khi đăng nhập: ", e);
      out.writeObject(new Message("ERROR", "LOGIN", "Lỗi Server nội bộ."));
      out.flush();
    }
  }
}