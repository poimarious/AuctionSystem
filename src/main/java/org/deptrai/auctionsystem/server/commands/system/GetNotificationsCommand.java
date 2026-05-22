package org.deptrai.auctionsystem.server.commands.system;

import org.deptrai.auctionsystem.server.ClientHandler;
import org.deptrai.auctionsystem.server.commands.Command;
import org.deptrai.auctionsystem.server.dao.NotificationDAO;
import org.deptrai.auctionsystem.shared.network.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ObjectOutputStream;
import java.util.List;

public class GetNotificationsCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(GetNotificationsCommand.class);

  @Override
  public void execute(ClientHandler clientHandler, Message request, ObjectOutputStream out) throws Exception {
    try {
      String userId = (String) request.getData();
      NotificationDAO notiDAO = new NotificationDAO();

      List<String> unreadNotifs = notiDAO.getUnreadNotifications(userId);
      notiDAO.deleteNotificationsByUserId(userId); // Xóa sau khi đọc

      out.reset();
      out.writeObject(new Message("SUCCESS", "GET_NOTIFICATIONS", unreadNotifs));
      out.flush();
    } catch (Exception e) {
      logger.error("Lỗi tải thông báo: ", e);
    }
  }
}