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

  public void placeBid(Auction auction, double amount) {
    try {
      Bid newBid = new Bid(this, auction, amount, LocalDateTime.now());

      auction.placeBid(newBid); // Error prone

      this.bidHistory.add(newBid);
      System.out.println("Bid placed successfully by " + super.getUsername());
    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw e;
    }
  }

  @Override
  public boolean login() {
    return false;
  }

  @Override
  public void logout() {}

  @Override
  public String getInfo() {
    return "Bidder: " + getUsername() + " | Email: " + getEmail();
  }

  // region Getters
  public List<Bid> getBidHistory() {
    return bidHistory;
  }

  // endregion

  // region Setters
  public void setBidHistory(List<Bid> bidHistory) {
    this.bidHistory = bidHistory;
  }
  // endregion
}
