package org.deptrai.auctionsystem.server.commands.auth;

import org.deptrai.auctionsystem.server.ClientHandler;
import org.deptrai.auctionsystem.server.commands.Command;
import org.deptrai.auctionsystem.server.dao.UserDAO;
import org.deptrai.auctionsystem.server.exceptions.AuthenticationException;
import org.deptrai.auctionsystem.server.exceptions.ResourceNotFoundException;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.shared.network.Message;
import org.deptrai.auctionsystem.utils.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ObjectOutputStream;

public class UpdatePasswordCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(UpdatePasswordCommand.class);

  @Override
  public void execute(ClientHandler clientHandler, Message request, ObjectOutputStream out) throws Exception {
    try {
      String[] data = (String[]) request.getData();
      String userId = data[0];
      String currentPassword = data[1];
      String newPassword = data[2];

      if (ValidationUtils.isInvalidPassword(newPassword)) {
        throw new AuthenticationException("Mật khẩu mới phải có ít nhất 8 ký tự, bao gồm chữ hoa, thường, số và ký tự đặc biệt!");
      }

      UserDAO userDAO = new UserDAO();
      User user = userDAO.getUserById(userId);

      if (user == null) {
        throw new ResourceNotFoundException("Tài khoản không tồn tại trên hệ thống!");
      }
      if (!user.getPassword().equals(currentPassword)) {
        throw new AuthenticationException("Mật khẩu hiện tại không đúng!");
      }

      if (userDAO.updatePassword(userId, newPassword)) {
        out.writeObject(new Message("SUCCESS", "UPDATE_PASSWORD", "Cập nhật mật khẩu thành công!"));
      } else {
        throw new Exception("Lỗi CSDL khi update mật khẩu.");
      }
      out.flush();

    } catch (AuthenticationException | ResourceNotFoundException e) {
      out.writeObject(new Message("FAIL", "UPDATE_PASSWORD", e.getMessage()));
      out.flush();
    } catch (Exception e) {
      logger.error("Lỗi cập nhật mật khẩu: ", e);
      out.writeObject(new Message("ERROR", "UPDATE_PASSWORD", "Định dạng dữ liệu không hợp lệ hoặc lỗi Server."));
      out.flush();
    }
  }
}