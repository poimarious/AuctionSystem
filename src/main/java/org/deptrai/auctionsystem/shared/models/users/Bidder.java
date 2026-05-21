package org.deptrai.auctionsystem.shared.models.users;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.bid.Bid;

public class Bidder extends User {
  private List<Bid> bidHistory;

  public Bidder(String username, String password, String email) {
    super(username, password, email);
    bidHistory = new ArrayList<>();
  }

  public Bidder(
      String userId, String username, String password, String email, List<Bid> bidHistory) {
    super(userId, username, password, email);
    this.bidHistory = bidHistory;
  }

  // region Getter and Setter
  public List<Bid> getBidHistory() {
    return bidHistory;
  }

  public void setBidHistory(List<Bid> bidHistory) {
    this.bidHistory = bidHistory;
  }
  // endregion
}
