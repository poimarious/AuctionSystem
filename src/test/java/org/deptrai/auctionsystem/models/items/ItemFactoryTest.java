package org.deptrai.auctionsystem.models.items;

import org.deptrai.auctionsystem.models.users.Seller;
import org.junit.jupiter.api.Test;

public class ItemFactoryTest {

    // info
    private String name;
    private String description;
    private double startingPrice;
    private Seller seller;

    @Test
    void testCreateElectronicItem() {

        ItemFactory electronicsFactory = new ElectronicsFactory();

        name = "Kabuto Kunaigun";
        description = " Kabuto's personal sidearm that has three modes to use the weapon in various forms.";
        startingPrice = 60;
        seller = new Seller("poi", "**+", "poimarious@gmail.com");


        Item item1 = electronicsFactory.createItem(name, description, startingPrice, seller);
        item1.printInfo();
    }

    @Test
    void testCreateArtItem() {

        ItemFactory artFactory = new ArtFactory();

        name = "Kamen Rider Kabuto";
        description = "Tendou Soji is the best";
        startingPrice = 66.36;
        seller = new Seller("poi", "**+", "poimarious@gmail.com");

        Item item2 = artFactory.createItem(name, description, startingPrice, seller);
        item2.printInfo();
    }

    @Test
    void testCreateVehicleItem() {

        ItemFactory vehicleFactory = new VehicleFactory();

        name = "Kabuto Extender";
        description = "Motorbike of Kabuto";
        startingPrice = 85.89;
        seller = new Seller("poi", "**+", "poimarious@gmail.com");

        Item item3 = vehicleFactory.createItem(name, description, startingPrice, seller);
        item3.printInfo();
    }
}
