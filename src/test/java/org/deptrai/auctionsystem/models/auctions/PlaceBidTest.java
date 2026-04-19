package org.deptrai.auctionsystem.models.auctions;

import org.deptrai.auctionsystem.models.auction.Auction;
import org.deptrai.auctionsystem.models.items.ArtFactory;
import org.deptrai.auctionsystem.models.items.Item;
import org.deptrai.auctionsystem.models.items.ItemFactory;
import org.deptrai.auctionsystem.models.users.Bidder;
import org.deptrai.auctionsystem.models.users.Seller;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;

public class PlaceBidTest {

    ItemFactory itemFactory = new ArtFactory();
    String name = "Kabuto Kunaigun";
    String description = " Kabuto's personal sidearm that has three modes to use the weapon in various forms.";
    double startingPrice = 60;
    Seller seller = new Seller("poi", "**+", "poimarious@gmail.com");
    Item item = itemFactory.createItem(name, description, startingPrice, seller);
    LocalDateTime endTime = LocalDateTime.of(2026, Month.JULY, 30, 0,0);
    Auction auction = new Auction(item, endTime);
    String username = "poimarious";
    String password = "poimaious1";
    String email = "pomarious@gmail.com";
    Bidder bidder1 = new Bidder(username, password, email);


    @Test
    void PlaceBidWhenAuctionIsClosingTest() {
        // Test if bidder places bid when auction is closing
        System.out.println("[TEST 1]");
        double amount = 65;
        bidder1.placeBid(auction, amount);
        System.out.println();
    }

    @Test
    void PlaceBidWhenAuctionIsOpeningTest() {
        // Test if bidder places bid when auction is opening
        System.out.println("[TEST 2]");
        auction.startAuction();
        double amount = 65;
        bidder1.placeBid(auction, amount);
        System.out.println();


    }

    @Test
    void TooLowPlaceBidTest() {
        // Test if bidder places bid when auction is opening, but amount is too low
        System.out.println("[TEST 3]");
        auction.startAuction();
        double amount = 45;
        bidder1.placeBid(auction, amount);
        System.out.println();
    }

    @Test
    void ObserverTest() {
        // Test if Obserer is OK
        System.out.println("[TEST 4]");
        auction.startAuction();
        String username2 = "TusBeo";
        String password2 = "TusBeo123@";
        String email2 = "TusBeo123@gmail.com";
        Bidder bidder2 = new Bidder(username2, password2, email2);
        auction.attach(bidder1);
        auction.attach(bidder2);

        double amount = 65;
        bidder1.placeBid(auction, amount);

        amount = 70;
        bidder2.placeBid(auction, amount);


        auction.closeAuction();
        System.out.println();
    }
}
