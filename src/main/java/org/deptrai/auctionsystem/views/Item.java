package org.deptrai.auctionsystem.views;

public class Item {
    private String itemId;
    private String name;
    private String description;
    private double startingPrice;
    private double currentPrice;

    public Item(String itemId, String name, String description, double startingPrice, double currentPrice) {
        this.itemId = itemId;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentPrice = currentPrice;
    }

    // Getter để hiển thị lên UI
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getStartingPrice() { return startingPrice; }
    public double getCurrentPrice() { return currentPrice; }
}