package org.deptrai.auctionsystem.client.utils;

import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.auction.AuctionStatus;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.shared.network.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

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

  public void startAutoBid(Auction auction, double maxBid, double increment, Runnable onStop) {
    User currentUser = SessionManager.getInstance().getCurrentUser();
    if(currentUser != null) {
      Object[] payload = {currentUser.getUserId(), auction.getAuctionId(), maxBid};
      Message req  = new Message("START_AUTOBID", payload);
      Message res = SocketClient.sendRequest(req);
      if("SUCCESS".equals(res.getStatus())) {
        activeAutoBids.put(auction.getAuctionId(), new AutoBidConfig(maxBid, increment, onStop));
        onGlobalAuctionUpdated(auction);
      } else {
        if(onStop != null) {
          onStop.run();
        }
      }
    }
  }

  public void stopAutoBid(String auctionId) {
    AutoBidConfig config = activeAutoBids.remove(auctionId);

    User currentUser = SessionManager.getInstance().getCurrentUser();
    if(currentUser != null) {
      Object[] payload = {currentUser.getUserId(), auctionId};
      SocketClient.runAsync(() -> {
        SocketClient.sendRequest(new Message("STOP_AUTOBID", payload));
      });
    }
    if(config != null && config.onStop != null) {
      config.onStop.run();
    }
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

    if (!myId.equals(winnerId)) {
      double nextBidAmount = lastestAuction.getCurrentPrice() + config.increment;
      if (nextBidAmount <= config.maxBid) {
        SocketClient.runAsync(() -> {
          try {
            Thread.sleep(1000);
            if(lastestAuction.getStatus() == AuctionStatus.OPEN || lastestAuction.getStatus() == AuctionStatus.RUNNING) {
              sendAutoBidRequest(auctionId, myId, nextBidAmount);
            } else {
              if(config.onStop != null) {
                config.onStop.run();
              }
            }
          } catch (Exception e) {
            logger.error("", e);
          }
        });
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

      if ("FAIL".equals(res.getStatus())) {
        stopAutoBid(auctionId);
        logger.info("Can't auto-bid more, stopped auto-bid.");
      }
    });
  }
}
