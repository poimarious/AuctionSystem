package org.deptrai.auctionsystem.server.commands.admin;

import org.deptrai.auctionsystem.server.ClientHandler;
import org.deptrai.auctionsystem.server.ServerMain;
import org.deptrai.auctionsystem.server.commands.Command;
import org.deptrai.auctionsystem.server.dao.UserDAO;
import org.deptrai.auctionsystem.server.exceptions.AuthenticationException;
import org.deptrai.auctionsystem.shared.models.users.Admin;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.shared.network.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ObjectOutputStream;

public class BanUserCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(BanUserCommand.class);

  @Override
  public void execute(ClientHandler clientHandler, Message request, ObjectOutputStream out) throws Exception {
    try {
      Object[] data = (Object[]) request.getData();
      User requester = (User) data[0];
      String targetUserId = (String) data[1];
      String banReason = (String) data[2];

      if (!(requester instanceof Admin) || ((Admin) requester).getAdminLevel() < 2) {
        throw new AuthenticationException("Bạn không có quyền Ban người dùng!");
      }

      if (new UserDAO().banUser(targetUserId, banReason)) {
        // Đá người dùng văng khỏi mạng nếu đang online
        for (ClientHandler client : ServerMain.activeClients) {
          if (client.getAuthenticatedUser() != null && client.getAuthenticatedUser().getUserId().equals(targetUserId)) {
            client.sendMessage(new Message("UPDATE", "FORCE_LOGOUT", "Tài khoản của bạn vừa bị cấm bởi Admin!\nLý do: " + banReason));
            client.setAuthenticatedUser(null);
            break;
          }
        }
        out.reset();
        out.writeObject(new Message("SUCCESS", "BAN_USER", "Đã ban người dùng thành công!"));
      } else {
        out.writeObject(new Message("FAIL", "BAN_USER", "Lỗi CSDL không thể cập nhật trạng thái Ban."));
      }
      out.flush();

    } catch (AuthenticationException e) {
      out.writeObject(new Message("FAIL", "BAN_USER", e.getMessage()));
      out.flush();
    } catch (Exception e) {
      logger.error("Lỗi khi Ban user: ", e);
      out.writeObject(new Message("ERROR", "BAN_USER", "Lỗi hệ thống Server."));
      out.flush();
    }
  }
}