package org.deptrai.auctionsystem.server.commands.admin;

import org.deptrai.auctionsystem.server.ClientHandler;
import org.deptrai.auctionsystem.server.commands.Command;
import org.deptrai.auctionsystem.server.dao.UserDAO;
import org.deptrai.auctionsystem.shared.models.users.Admin;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.shared.network.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ObjectOutputStream;

public class UnbanUserCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(UnbanUserCommand.class);

  @Override
  public void execute(ClientHandler clientHandler, Message request, ObjectOutputStream out) throws Exception {
    try {
      Object[] data = (Object[]) request.getData();
      User requester = (User) data[0];
      String targetUserId = (String) data[1];

      if (!(requester instanceof Admin) || ((Admin) requester).getAdminLevel() < 2) {
        out.writeObject(new Message("FAIL", "UNBAN_USER", "Bạn không có quyền Gỡ cấm người dùng!"));
        out.flush();
        return;
      }

      if (new UserDAO().unbanUser(targetUserId)) {
        out.writeObject(new Message("SUCCESS", "UNBAN_USER", "Đã gỡ cấm thành công!"));
      } else {
        out.writeObject(new Message("FAIL", "UNBAN_USER", "Lỗi CSDL khi gỡ cấm."));
      }
      out.flush();

    } catch (Exception e) {
      logger.error("Lỗi gỡ ban: ", e);
    }
  }
}