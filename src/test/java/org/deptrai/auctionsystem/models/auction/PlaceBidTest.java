package org.deptrai.auctionsystem.models.auction;

import org.deptrai.auctionsystem.exceptions.AuctionClosedException;
import org.deptrai.auctionsystem.exceptions.AuthenticationException;
import org.deptrai.auctionsystem.exceptions.InvalidBidException;
import org.deptrai.auctionsystem.models.items.ArtFactory;
import org.deptrai.auctionsystem.models.items.Item;
import org.deptrai.auctionsystem.models.items.ItemFactory;
import org.deptrai.auctionsystem.models.users.Bidder;
import org.deptrai.auctionsystem.models.users.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.*;

public class PlaceBidTest {

    private Auction auction;
    private Bidder bidder1;
    private Bidder bidder2;
    private Item item;
    private Seller seller;

    @BeforeEach
    void setUp() {
        ItemFactory itemFactory = new ArtFactory();
        seller = new Seller("poi",
                "poi1",
                "poimarious@gmail.com");
        seller.setUserId("seller_01");
        item = itemFactory.createItem("Kabuto Kunaigun",
                "Kabuto's personal sidearm...",
                60.0,
                seller);
        LocalDateTime endTime = LocalDateTime.of(2026, Month.MAY, 30, 0, 0);

        auction = new Auction(item, endTime);
        bidder1 = new Bidder("poimarious",
                "poimaious1",
                "pomarious@gmail.com");
        bidder1.setUserId("bidder_01");
        bidder2 = new Bidder("TusBeo",
                "TusBeo123@",
                "TusBeo123@gmail.com");
        bidder2.setUserId("bidder_02");
    }

    @Test
    void testPlaceBidWhenAuctionIsClosed() {
        // Test if place bid when auction is closed
        double validAmount = 65.0;

        Exception exception = assertThrows(AuctionClosedException.class, () -> {
            bidder1.placeBid(auction, validAmount);
        }, "Need to throw AuctionClosedException");
    }

    // BVA for amount

    @ParameterizedTest
    @ValueSource(doubles = {
            -1.0,
            0.0,
            45.0,
            60.0
    })
    void testPlaceBidInvalidBoundaries(double invalidAmount) {
        auction.startAuction();

        assertThrows(InvalidBidException.class, () -> {
            bidder1.placeBid(auction, invalidAmount);
        }, "Need to throw InvalidBidException when place bid with: " + invalidAmount);
    }

    @Test
    void testPlaceBidValidBoundary() {

        auction.startAuction();
        double validAmount = 60.01;

        assertDoesNotThrow(() -> {
            bidder1.placeBid(auction, validAmount);
        }, "Pass");
    }

    @Test
    void testSellerPlaceBid() {
        // Seller cannot place bid in his auction

        auction.startAuction();
        double amount = 80;

        if (seller.getUserId() == null) {
            seller.setUserId("Seller_01");
        }

        Bidder bidder3 = new Bidder(
                seller.getUserId(),
                seller.getUsername(),
                seller.getPassword(),
                seller.getEmail(),
                new java.util.ArrayList<>()
        );

        assertThrows(AuthenticationException.class, () -> {
            bidder3.placeBid(auction, amount);
        });
    }

    // Observer test
    @Test
    void testObserverMultipleBidders() {
        // Arrange
        auction.startAuction();
        auction.attach(bidder1);
        auction.attach(bidder2);

        assertDoesNotThrow(() -> {
            bidder1.placeBid(auction, 65.0);
            bidder2.placeBid(auction, 70.0);
        }, "Pass");

        assertEquals(70.0, auction.getCurrentPrice(), "Current price must be 70.0");

        assertDoesNotThrow(() -> {
            auction.closeAuction();
        });
    }
}