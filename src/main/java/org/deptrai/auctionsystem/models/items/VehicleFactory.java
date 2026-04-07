package org.deptrai.auctionsystem.models.items;

public class VehicleFactory extends ItemFactory<VehicleParams> {

    @Override
    public Item createItem(VehicleParams p) {
        return new Vehicle(p);
    }

}
