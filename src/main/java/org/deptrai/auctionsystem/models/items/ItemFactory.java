package org.deptrai.auctionsystem.models.items;

public abstract class ItemFactory <T extends ItemParams> {

//    public static Item createItem(Params params) {
//        switch(params.getType().toUpperCase()) {
//            case "ELECTRONIC":
//                return new Electronic(params);
//            case "ART":
//                return new Art(params);
//            case "VEHICLE":
//                return new Vehicle(params);
//            default:
//                throw new IllegalArgumentException("Item Type Error:" + params.getType());
//        }
//    }

    public abstract Item createItem(T params);
}
