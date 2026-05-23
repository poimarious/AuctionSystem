package org.deptrai.auctionsystem.client.utils;

public class AutoBidConfig {
  public double maxBid;
  public double increment;
  Runnable onStop;
  public AutoBidConfig(double maxBid, double increment, Runnable onStop) {
    this.maxBid = maxBid;
    this.increment = increment;
    this.onStop = onStop;
  }
}