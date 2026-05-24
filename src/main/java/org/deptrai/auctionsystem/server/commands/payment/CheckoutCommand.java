package org.deptrai.auctionsystem.server.commands.payment;

import org.deptrai.auctionsystem.server.ClientHandler;
import org.deptrai.auctionsystem.server.ServerMain;
import org.deptrai.auctionsystem.server.commands.Command;
import org.deptrai.auctionsystem.server.dao.AuctionDAO;
import org.deptrai.auctionsystem.server.dao.NotificationDAO;
import org.deptrai.auctionsystem.server.dao.UserDAO;
import org.deptrai.auctionsystem.server.exceptions.InsufficientBalanceException;
import org.deptrai.auctionsystem.server.managers.AuctionManager;
import org.deptrai.auctionsystem.server.utils.ServerThreadPool;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.auction.AuctionStatus;
import org.deptrai.auctionsystem.shared.models.users.Bidder;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.shared.network.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ObjectOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class CheckoutCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(CheckoutCommand.class);

  @Override
  public void execute(ClientHandler clientHandler, Message request, ObjectOutputStream out) throws Exception {
    try {
      // SỬA ĐOẠN NÀY: Bóc tách mảng Object[] giống hệt PlaceBidCommand
      Object[] data = (Object[]) request.getData();
      String auctionId = (String) data[0];
      String requesterId = (String) data[1];
      Auction auction = AuctionManager.getInstance().getAuctionById(auctionId);

      if (auction == null) {
        clientHandler.sendMessage(new Message("FAIL", "CHECKOUT", "Phiên đấu giá không tồn tại."));
        return;
      }

      // ================= LỚP KHÓA 1: CHỐNG RACE CONDITION (DOUBLE-SPENDING) =================
      synchronized (auction) {
        if (auction.getStatus() != AuctionStatus.FINISHED) {
          clientHandler.sendMessage(new Message("FAIL", "CHECKOUT", "Phiên đấu giá không hợp lệ hoặc đã được thanh toán."));
          return;
        }

        Bidder winner = auction.getWinner();
        if (winner == null) {
          clientHandler.sendMessage(new Message("FAIL", "CHECKOUT", "Không có người chiến thắng để thanh toán."));
          return;
        }



        // Kiểm tra xem người gửi có phải là người chiến thắng không?
        if (!requesterId.equals(winner.getUserId())) {
          clientHandler.sendMessage(new Message("FAIL", "CHECKOUT", "Xác thực thất bại: Bạn không phải là người chiến thắng của phiên đấu giá này!"));
          return;
        }
        // =============================================================

        String buyerId = winner.getUserId();
        String sellerId = auction.getItem().getSeller().getUserId();
        double finalPrice = auction.getCurrentPrice();
        // ... (Đoạn code trừ tiền bên dưới giữ nguyên) ...

        UserDAO userDAO = new UserDAO();

        // Sắp xếp ID để tránh Deadlock
        String firstLock = buyerId.compareTo(sellerId) < 0 ? buyerId : sellerId;
        String secondLock = buyerId.compareTo(sellerId) < 0 ? sellerId : buyerId;

        synchronized (firstLock.intern()) {
          synchronized (secondLock.intern()) {
            User dbWinner = userDAO.getUserById(buyerId);
            User dbSeller = userDAO.getUserById(sellerId);

            if (dbWinner.getBalance() < finalPrice) {
              throw new InsufficientBalanceException("Tài khoản không đủ số dư để thanh toán!");
            }

            if (userDAO.updateBalance(buyerId, dbWinner.getBalance() - finalPrice)) {
              if (userDAO.updateBalance(sellerId, dbSeller.getBalance() + finalPrice)) {

                // CẬP NHẬT TRẠNG THÁI THÀNH PAID
                auction.setStatus(AuctionStatus.PAID);
                new AuctionDAO().updateAuctionState(auction);

                ServerMain.broadcast(new Message("SUCCESS", "AUCTION_UPDATE", auction));
                pushCheckoutNotifications(auction, buyerId, sellerId, dbWinner.getUsername(), finalPrice);

                clientHandler.sendMessage(new Message("SUCCESS", "CHECKOUT", "Thanh toán thành công!"));
              } else {
                clientHandler.sendMessage(new Message("FAIL", "CHECKOUT", "Lỗi chuyển tiền cho người bán."));
              }
            } else {
              clientHandler.sendMessage(new Message("FAIL", "CHECKOUT", "Lỗi khi trừ tiền người mua."));
            }
          }
        }
      }

    } catch (InsufficientBalanceException e) {
      clientHandler.sendMessage(new Message("FAIL", "CHECKOUT", e.getMessage()));
    } catch (Exception e) {
      logger.error("Lỗi Server khi xử lý thanh toán: ", e);
      clientHandler.sendMessage(new Message("ERROR", "CHECKOUT", "Lỗi Server khi xử lý thanh toán."));
    }
  }

  private void pushCheckoutNotifications(Auction auction, String buyerId, String sellerId, String buyerName, double finalPrice) {
    ServerThreadPool.submitTask(() -> {
      NotificationDAO notiDAO = new NotificationDAO();
      String timeStampStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
      String itemName = auction.getItem().getName();

      HashMap<String, String> usersMessage = new HashMap<>();
      usersMessage.put(sellerId, timeStampStr + " || Phiên đấu giá [" + itemName + "] đã được thanh toán bởi " + buyerName + " với số tiền: $" + finalPrice);
      usersMessage.put(buyerId, timeStampStr + " || Bạn đã thanh toán thành công số tiền $" + finalPrice + " cho món hàng [" + itemName + "].");

      for (Map.Entry<String, String> users : usersMessage.entrySet()) {
        String targetUserId = users.getKey();
        String targetMessage = users.getValue();
        boolean isOnline = false;

        for (ClientHandler client : ServerMain.activeClients) {
          if (client.getAuthenticatedUser() != null && client.getAuthenticatedUser().getUserId().equals(targetUserId)) {
            client.sendMessage(new Message("SUCCESS", "PUSH_NOTIFICATION_BELL", targetMessage));
            isOnline = true;
            break;
          }
        }
        if (!isOnline) {
          notiDAO.insertNotification(targetUserId, targetMessage);
        }
      }
    });
  }
}