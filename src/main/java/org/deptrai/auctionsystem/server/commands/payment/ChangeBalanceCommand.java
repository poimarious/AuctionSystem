package org.deptrai.auctionsystem.server.commands.payment;

import org.deptrai.auctionsystem.server.ClientHandler;
import org.deptrai.auctionsystem.server.commands.Command;
import org.deptrai.auctionsystem.server.dao.UserDAO;
import org.deptrai.auctionsystem.shared.models.users.Seller;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.shared.network.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ObjectOutputStream;

public class ChangeBalanceCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(ChangeBalanceCommand.class);

  @Override
  public void execute(ClientHandler clientHandler, Message request, ObjectOutputStream out) throws Exception {
    try {
      Object[] data = (Object[]) request.getData();
      String userId = (String) data[0];
      double amount = (Double) data[1];

      // Ổ KHÓA CHỐNG LOST UPDATE THEO ID NGƯỜI DÙNG
      synchronized (userId.intern()) {
        UserDAO userDAO = new UserDAO();
        User user = userDAO.getUserById(userId);

        if (user == null) {
          out.writeObject(new Message("FAIL", "CHANGE_BALANCE", "Tài khoản không tồn tại."));
          out.flush();
          return;
        }

        double newBalance = user.getBalance() + amount;

        // Nếu là Seller thì đây là lệnh Rút tiền (Trừ tiền)
        if (user instanceof Seller) {
          newBalance = user.getBalance() - amount;
          if (newBalance < 0) {
            out.writeObject(new Message("FAIL", "CHANGE_BALANCE", "Số tiền rút không được vượt quá số dư hiện tại!"));
            out.flush();
            return;
          }
        }

        if (userDAO.updateBalance(userId, newBalance)) {
          out.reset();
          out.writeObject(new Message("SUCCESS", "CHANGE_BALANCE", newBalance));
        } else {
          out.writeObject(new Message("FAIL", "CHANGE_BALANCE", "Lỗi CSDL khi cập nhật số dư."));
        }
        out.flush();
      }

    } catch (Exception e) {
      logger.error("Lỗi giao dịch đổi số dư: ", e);
      out.writeObject(new Message("ERROR", "CHANGE_BALANCE", "Lỗi hệ thống khi xử lý giao dịch."));
      out.flush();
    }
  }
}