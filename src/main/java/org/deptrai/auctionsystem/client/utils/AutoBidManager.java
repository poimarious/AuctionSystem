package org.deptrai.auctionsystem.client.utils;

import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.shared.network.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AutoBidManager {

  private static final Logger logger = LoggerFactory.getLogger(AutoBidManager.class);

  private static volatile AutoBidManager instance;

  private final Map<String, AutoBidConfig> activeAutoBids = new ConcurrentHashMap<>();
  private AutoBidManager() {
    SocketClient.addListener(this::onGlobalAuctionUpdated);
  }

  public static AutoBidManager getInstance() {
    if (instance == null) {
      synchronized (AutoBidManager.class) {
        if (instance == null) {
          instance = new AutoBidManager();
        }
      }
    }
    return instance;
  }

  public void startAutoBid(Auction auction, double maxBid, double increment) {
    activeAutoBids.put(auction.getAuctionId(), new AutoBidConfig(maxBid, increment));
    onGlobalAuctionUpdated(auction);
  }

  public void stopAutoBid(String auctionId) {
    activeAutoBids.remove(auctionId);
  }

  public boolean isAutoBidActive(String auctionId) {
    return activeAutoBids.containsKey(auctionId);
  }

  public AutoBidConfig getAutoBidConfig(String auctionId) {
    return activeAutoBids.get(auctionId);
  }

  private void onGlobalAuctionUpdated(Auction lastestAuction) {
    String auctionId = lastestAuction.getAuctionId();
    if (!activeAutoBids.containsKey(auctionId)) return;
    AutoBidConfig config = activeAutoBids.get(auctionId);
    User currentUser = SessionManager.getInstance().getCurrentUser();
    if (currentUser == null) {
      return;
    }

    String myId = currentUser.getUserId();
    String winnerId = lastestAuction.getWinner() != null ? lastestAuction.getWinner().getUserId() : "";

    if(!myId.equals(winnerId)) {
      double nextBidAmount = lastestAuction.getCurrentPrice() + config.increment;
      if(nextBidAmount <= config.maxBid) {
        new Thread(() -> {
          try {
            Thread.sleep(1000);
            sendAutoBidRequest(auctionId, myId,nextBidAmount);
          } catch(Exception e) {
            logger.error("",e);
          }
        }).start();
      } else {
        stopAutoBid(auctionId);
      }
    }
  }

  private void sendAutoBidRequest(String auctionId, String userId, double bidAmount) {
    Object[] payload = {auctionId, userId, bidAmount};
    SocketClient.runAsync(() -> {
      Message req = new Message("PLACE_BID", payload);
      Message res = SocketClient.sendRequest(req);

      if("FAIL".equals(res.getStatus())) {
        stopAutoBid(auctionId);
        logger.info("Can't auto-bid more, stopped auto-bid.");
      }
    });
  }

  public static class AutoBidConfig {
    public double maxBid;
    public double increment;
    public AutoBidConfig(double maxBid, double increment) {
      this.maxBid = maxBid;
      this.increment = increment;
    }
  }
}
