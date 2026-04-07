package org.deptrai.auctionsystem.models.items;

public class ArtFactory extends ItemFactory<ArtParams> {

    @Override
    public Item createItem(ArtParams p) {
        return new Art(p);
    }
}
