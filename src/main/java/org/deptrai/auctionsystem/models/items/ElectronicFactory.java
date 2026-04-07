package org.deptrai.auctionsystem.models.items;

public class ElectronicFactory extends ItemFactory<ElectronicParams> {

    @Override
    public Item createItem(ElectronicParams p) {
        return new Electronic(p);
    }
}
