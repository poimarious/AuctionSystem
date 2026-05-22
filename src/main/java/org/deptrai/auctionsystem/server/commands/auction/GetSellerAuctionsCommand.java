package org.deptrai.auctionsystem.server.commands.auction;

import org.deptrai.auctionsystem.server.ClientHandler;
import org.deptrai.auctionsystem.server.commands.Command;
import org.deptrai.auctionsystem.server.managers.AuctionManager;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.network.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class GetSellerAuctionsCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(GetSellerAuctionsCommand.class);

  @Override
  public void execute(ClientHandler clientHandler, Message request, ObjectOutputStream out) throws Exception {
    try {
      String sellerId = (String) request.getData();
      List<Auction> sellerAuctions = new ArrayList<>();

      for (Auction auction : AuctionManager.getInstance().getAllAuctions()) {
        if (auction.getItem() != null && auction.getItem().getSeller() != null
            && auction.getItem().getSeller().getUserId().equals(sellerId)) {
          sellerAuctions.add(auction);
        }
      }

      out.reset();
      out.writeObject(new Message("SUCCESS", "GET_SELLER_AUCTIONS", sellerAuctions));
      out.flush();

    } catch (Exception e) {
      logger.error("Lỗi khi tải kho hàng Seller: ", e);
      out.writeObject(new Message("FAIL", "GET_SELLER_AUCTIONS", "Lỗi tải kho hàng."));
      out.flush();
    }
  }
}