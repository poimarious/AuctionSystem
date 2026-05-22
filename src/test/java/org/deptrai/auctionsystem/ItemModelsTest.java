package org.deptrai.auctionsystem;

import static org.junit.jupiter.api.Assertions.*;

import org.deptrai.auctionsystem.shared.models.items.Art;
import org.deptrai.auctionsystem.shared.models.items.ArtFactory;
import org.deptrai.auctionsystem.shared.models.items.Item;
import org.deptrai.auctionsystem.shared.models.items.Vehicle;
import org.deptrai.auctionsystem.shared.models.items.VehicleFactory;
import org.deptrai.auctionsystem.shared.models.users.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ItemModelsTest {

    private Seller dummySeller;

    @BeforeEach
    void setUp() {
        // Khởi tạo một người bán giả lập dùng chung cho các bài test
        dummySeller = new Seller("seller_id_001", "ModelTester", "Pass123!", "modeltester@gmail.com");
    }

    // ==========================================
    // NHÓM TEST DÀNH CHO ITEM: ART (NGHỆ THUẬT)
    // ==========================================

    @Test
    @DisplayName("Test khởi tạo Art và kiểm tra Category")
    void testArt_ConstructorAndCategory() {
        Art art = new Art("item_art_001", "Bức họa Mona Lisa", "Bản sao chuẩn", 5000.0, dummySeller, "Leonardo da Vinci", 1503);

        assertEquals("item_art_001", art.getItemId());
        assertEquals("Bức họa Mona Lisa", art.getName());
        assertEquals(5000.0, art.getStartingPrice());
        assertEquals("Leonardo da Vinci", art.getArtist());
        assertEquals(1503, art.getYearCreated());
        assertEquals("Art", art.getCategory(), "Category của Art bắt buộc phải trả về chuỗi 'Art'");
    }

    @Test
    @DisplayName("Test tính năng Fluent Interface (Chain Setters) của Art")
    void testArt_FluentSetters() {
        Art art = new Art("Bức họa Vô Danh", "Chưa rõ", 100.0, dummySeller);

        // Test khả năng gọi liên hoàn (Fluent API) do bạn đã return `this` trong setter
        art.setArtist("Picasso").setYearCreated(1937);

        assertEquals("Picasso", art.getArtist());
        assertEquals(1937, art.getYearCreated());
    }

    @Test
    @DisplayName("Test ArtFactory sinh đúng đối tượng")
    void testArtFactory_ShouldCreateArtInstance() {
        ArtFactory factory = new ArtFactory();
        Item item = factory.createItem("Tranh đồng quê", "Sơn dầu", 200.0, dummySeller);

        // Xác nhận Factory Pattern hoạt động chuẩn xác
        assertNotNull(item);
        assertTrue(item instanceof Art, "Factory của Art phải sinh ra đúng object class Art");
        assertEquals("Art", item.getCategory());
        assertEquals("Tranh đồng quê", item.getName());
    }

    // ==========================================
    // NHÓM TEST DÀNH CHO ITEM: VEHICLE (PHƯƠNG TIỆN)
    // ==========================================

    @Test
    @DisplayName("Test khởi tạo Vehicle và kiểm tra Category")
    void testVehicle_ConstructorAndCategory() {
        Vehicle car = new Vehicle("item_veh_001", "Lexus RX350", "Xe lướt", 45000.0, dummySeller, "Toyota", 15000);

        assertEquals("item_veh_001", car.getItemId());
        assertEquals("Lexus RX350", car.getName());
        assertEquals(45000.0, car.getStartingPrice());
        assertEquals("Toyota", car.getMake());
        assertEquals(15000, car.getMileage());
        assertEquals("Vehicle", car.getCategory(), "Category của Vehicle bắt buộc phải trả về chuỗi 'Vehicle'");
    }

    @Test
    @DisplayName("Test tính năng Fluent Interface (Chain Setters) của Vehicle")
    void testVehicle_FluentSetters() {
        Vehicle car = new Vehicle("Xe máy cũ", "Wave Alpha", 500.0, dummySeller);

        // Test chuỗi setter
        car.setMake("Honda").setMileage(50000);

        assertEquals("Honda", car.getMake());
        assertEquals(50000, car.getMileage());
    }

    @Test
    @DisplayName("Test VehicleFactory sinh đúng đối tượng")
    void testVehicleFactory_ShouldCreateVehicleInstance() {
        VehicleFactory factory = new VehicleFactory();
        Item item = factory.createItem("Lamborghini Aventador", "Siêu xe", 300000.0, dummySeller);

        // Xác nhận
        assertNotNull(item);
        assertTrue(item instanceof Vehicle, "Factory của Vehicle phải sinh ra đúng object class Vehicle");
        assertEquals("Vehicle", item.getCategory());
        assertEquals("Lamborghini Aventador", item.getName());
    }
}