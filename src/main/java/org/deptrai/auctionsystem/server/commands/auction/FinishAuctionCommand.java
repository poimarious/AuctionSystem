package org.deptrai.auctionsystem.server.commands.auction;

import org.deptrai.auctionsystem.server.ClientHandler;
import org.deptrai.auctionsystem.server.ServerMain;
import org.deptrai.auctionsystem.server.commands.Command;
import org.deptrai.auctionsystem.server.dao.AuctionDAO;
import org.deptrai.auctionsystem.server.dao.NotificationDAO;
import org.deptrai.auctionsystem.server.managers.AuctionManager;
import org.deptrai.auctionsystem.server.utils.ServerThreadPool;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.auction.AuctionStatus;
import org.deptrai.auctionsystem.shared.models.bid.Bid;
import org.deptrai.auctionsystem.shared.models.users.Bidder;
import org.deptrai.auctionsystem.shared.network.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ObjectOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FinishAuctionCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(FinishAuctionCommand.class);

  @Override
  public void execute(ClientHandler clientHandler, Message request, ObjectOutputStream out) throws Exception {
    try {
      String auctionId = (String) request.getData();
      Auction auction = AuctionManager.getInstance().getAuctionById(auctionId);

      if (auction == null) {
        clientHandler.sendMessage(new Message("FAIL", "FINISH_AUCTION", "Không tìm thấy phiên đấu giá."));
        return;
      }

      synchronized (auction) {
        if (auction.getStatus() == AuctionStatus.OPEN || auction.getStatus() == AuctionStatus.RUNNING) {
          if (!LocalDateTime.now().isBefore(auction.getEndTime())) {

            auction.setStatus(AuctionStatus.FINISHED);
            if (new AuctionDAO().updateAuctionState(auction)) {
              logger.info("Phiên đấu giá [{}] đã KẾT THÚC.", auction.getItem().getName());

              ServerMain.broadcast(new Message("SUCCESS", "AUCTION_UPDATE", auction));
              pushFinishNotifications(auction);
            }
          }
        }
      }

      clientHandler.sendMessage(new Message("SUCCESS", "FINISH_AUCTION", "Đã xử lý xong"));

    } catch (Exception e) {
      logger.error("Lỗi khi kết thúc phiên đấu giá: ", e);
    }
  }

  private void pushFinishNotifications(Auction auction) {
    ServerThreadPool.submitTask(() -> {
      NotificationDAO notiDAO = new NotificationDAO();
      String timeStampStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
      String itemName = auction.getItem().getName();

      String sellerId = (auction.getItem() != null && auction.getItem().getSeller() != null)
          ? auction.getItem().getSeller().getUserId() : null;

      Map<String, String> targetUsers = new HashMap<>();
      Bidder winner = auction.getWinner();
      String winnerId = (winner != null) ? winner.getUserId() : null;
      double finalPrice = auction.getCurrentPrice();

      if (sellerId != null) {
        if (winner == null) {
          targetUsers.put(sellerId, timeStampStr + " || Phiên đấu giá [" + itemName + "] đã KẾT THÚC nhưng không có người đặt giá.");
        } else {
          targetUsers.put(sellerId, timeStampStr + " || Phiên đấu giá [" + itemName + "] đã KẾT THÚC và được bán với giá $" + finalPrice + " cho " + winner.getUsername() + ".");
        }
      }

      if (winner != null) {
        Set<String> participantIds = new HashSet<>();
        for (Bid bid : auction.getBids()) {
          if (bid.getBidder() != null) participantIds.add(bid.getBidder().getUserId());
        }

        for (String pId : participantIds) {
          if (pId.equals(winnerId)) {
            targetUsers.put(pId, timeStampStr + " || Phiên đấu giá [" + itemName + "] đã KẾT THÚC. Chúc mừng, bạn đã THẮNG với mức giá $" + finalPrice + "!");
          } else {
            targetUsers.put(pId, timeStampStr + " || Phiên đấu giá [" + itemName + "] đã KẾT THÚC. Rất tiếc, bạn đã THUA người ra giá cao nhất.");
          }
        }
      }

      for (Map.Entry<String, String> entry : targetUsers.entrySet()) {
        String targetUserId = entry.getKey();
        String msgText = entry.getValue();
        boolean isOnline = false;

        for (ClientHandler client : ServerMain.activeClients) {
          if (client.getAuthenticatedUser() != null && client.getAuthenticatedUser().getUserId().equals(targetUserId)) {
            client.sendMessage(new Message("SUCCESS", "PUSH_NOTIFICATION_BELL", msgText));
            isOnline = true;
            break;
          }
        }
        if (!isOnline) notiDAO.insertNotification(targetUserId, msgText);
      }
    });
  }
}