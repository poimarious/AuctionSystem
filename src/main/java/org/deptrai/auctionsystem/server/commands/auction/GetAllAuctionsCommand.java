package org.deptrai.auctionsystem.server.commands.auction;

import org.deptrai.auctionsystem.server.ClientHandler;
import org.deptrai.auctionsystem.server.commands.Command;
import org.deptrai.auctionsystem.server.managers.AuctionManager;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.auction.AuctionStatus;
import org.deptrai.auctionsystem.shared.models.auction.AuctionSummary;
import org.deptrai.auctionsystem.shared.models.users.Bidder;
import org.deptrai.auctionsystem.shared.network.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class GetAllAuctionsCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(GetAllAuctionsCommand.class);

  @Override
  public void execute(ClientHandler clientHandler, Message request, ObjectOutputStream out) throws Exception {
    try {
      String userId = (request.getData() instanceof String) ? (String) request.getData() : null;

      List<Auction> allAuctions = AuctionManager.getInstance().getAllAuctions();
      List<AuctionSummary> activeAuctionsDTO = new ArrayList<>();

      for (Auction auction : allAuctions) {
        boolean isOngoing = (auction.getStatus() == AuctionStatus.OPEN || auction.getStatus() == AuctionStatus.RUNNING);
        boolean isWonByMe = false;

        if (userId != null && (auction.getStatus() == AuctionStatus.FINISHED || auction.getStatus() == AuctionStatus.PAID)) {
          Bidder winner = auction.getWinner();
          if ((winner != null && winner.getUserId().equals(userId)) || auction.getItem().getSeller().getUserId().equals(userId)) {
            isWonByMe = true;
          }
        }

        if (isOngoing || isWonByMe) {
          AuctionSummary auctionSummary = new AuctionSummary(
              auction.getAuctionId(),
              auction.getItem().getName(),
              auction.getItem().getDescription(),
              auction.getItem().getCategory(),
              auction.getCurrentPrice(),
              auction.getStatus(),
              auction.getEndTime(),
              auction.getItem().getImageUrl(),
              auction.getItem().getImageBytes()
          );
          activeAuctionsDTO.add(auctionSummary);
        }
      }

      out.reset();
      out.writeObject(new Message("SUCCESS", "GET_ALL_AUCTIONS", activeAuctionsDTO));
      out.flush();

    } catch (Exception e) {
      logger.error("Lỗi khi gửi danh sách Auction cho Client: ", e);
      if (out != null) {
        out.reset();
        out.writeObject(new Message("FAIL", "GET_ALL_AUCTIONS", "Lỗi tải dữ liệu."));
        out.flush();
      }

    }
  }
}