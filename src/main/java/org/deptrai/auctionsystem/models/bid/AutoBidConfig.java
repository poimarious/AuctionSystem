package org.deptrai.auctionsystem.models.bid;

import org.deptrai.auctionsystem.models.users.Bidder;

import java.time.LocalDateTime;

public class AutoBidConfig {
    private Bidder bidder;
    private double maxBid;
    private double increment;
    private LocalDateTime registeredAt;

    public double execute(double currentPrice){
        return 0.0;
    }
}
