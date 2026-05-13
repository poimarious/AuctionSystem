package org.deptrai.auctionsystem.client.utils;
import org.deptrai.auctionsystem.shared.models.auction.Auction;

public interface AuctionUpdateListener {
  void onAuctionUpdated(Auction updatedAuction);
}