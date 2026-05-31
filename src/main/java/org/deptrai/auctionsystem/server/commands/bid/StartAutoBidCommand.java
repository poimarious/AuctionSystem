package org.deptrai.auctionsystem.server.commands.bid;

import org.deptrai.auctionsystem.server.ClientHandler;
import org.deptrai.auctionsystem.server.commands.Command;
import org.deptrai.auctionsystem.server.dao.UserDAO;
import org.deptrai.auctionsystem.server.managers.AuctionManager;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.auction.AuctionStatus;
import org.deptrai.auctionsystem.shared.models.users.Bidder;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.shared.network.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ObjectOutputStream;

public class StartAutoBidCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(StartAutoBidCommand.class);

  @Override
  public void execute(ClientHandler clientHandler, Message request, ObjectOutputStream out) throws Exception {
    try {
      Object[] data = (Object[]) request.getData();
      String userId = (String) data[0];
      String auctionId = (String) data[1];
      double maxBid = (Double) data[2];

      double lockedBalance = getLockedBalance(userId, auctionId) + AuctionManager.getInstance().getTotalLockedAutoBid(userId, auctionId);

      UserDAO userDAO = new UserDAO();
      User user = userDAO.getUserById(userId);

      if(lockedBalance + maxBid <= user.getBalance()) {
        AuctionManager.getInstance().registerAutoBid(userId, auctionId, maxBid);
        clientHandler.sendMessage(new Message("SUCCESS", "START_AUTOBID", "Đã bật auto-bid thành công!"));
      } else {
        clientHandler.sendMessage(new Message("FAIL", "START_AUTOBID", "Số dư không đủ để bật auto-bid với mức giá này!"));
      }
    } catch(Exception e) {
      logger.error("Lỗi khi xử lí Auto-Bid:", e);
      clientHandler.sendMessage(new Message("ERROR", "START_AUTOBID", "Lỗi hệ thống Server."));
    }
  }

  private double getLockedBalance(String bidderId, String auctionId) {
    double lockedBalance = 0.0;
    for(Auction auction : AuctionManager.getInstance().getAllAuctions()) {
      if(auction.getAuctionId().equals(auctionId)) continue;
      if(auction.getStatus() == AuctionStatus.OPEN || auction.getStatus() == AuctionStatus.RUNNING || auction.getStatus() == AuctionStatus.FINISHED) {

        Bidder topBidder = auction.getWinner();
        if(topBidder != null && topBidder.getUserId().equals(bidderId)) {
          lockedBalance += auction.getCurrentPrice();
        }
      }
    }
    return lockedBalance;
  }
}
