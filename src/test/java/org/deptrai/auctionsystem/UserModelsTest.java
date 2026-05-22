package org.deptrai.auctionsystem;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.deptrai.auctionsystem.shared.models.bid.Bid;
import org.deptrai.auctionsystem.shared.models.items.Art;
import org.deptrai.auctionsystem.shared.models.users.Bidder;
import org.deptrai.auctionsystem.shared.models.users.Seller;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class UserModelsTest {

    // ==========================================
    // NHÓM TEST DÀNH CHO BIDDER (NGƯỜI MUA)
    // ==========================================
    @Test
    @DisplayName("Test khởi tạo Bidder và kiểm tra danh sách Bid")
    void testBidder_ConstructorAndProperties() {
        Bidder bidder = new Bidder("bidder1", "Pass123!", "bidder@gmail.com");

        assertEquals("bidder1", bidder.getUsername());
        assertEquals("bidder@gmail.com", bidder.getEmail());
        assertNotNull(bidder.getBidHistory(), "Danh sách bidHistory phải được khởi tạo (không null)");
        assertEquals(0, bidder.getBidHistory().size(), "Danh sách ban đầu phải rỗng");

        // Test tính năng Setter
        List<Bid> newHistory = new ArrayList<>();
        newHistory.add(null); // Thêm phần tử giả để test việc gán List
        bidder.setBidHistory(newHistory);

        assertEquals(1, bidder.getBidHistory().size(), "Setter phải cập nhật đúng danh sách mới");
    }

    // ==========================================
    // NHÓM TEST DÀNH CHO SELLER (NGƯỜI BÁN)
    // ==========================================
    @Test
    @DisplayName("Test khởi tạo Seller và kiểm tra thông tin cơ bản")
    void testSeller_ConstructorAndProperties() {
        Seller seller = new Seller("seller1", "Pass123!", "seller@gmail.com");

        assertEquals("seller1", seller.getUsername());

        // Bài test này sẽ FAIL nếu bạn chưa sửa lỗi NullPointerException ở trên
        assertNotNull(seller.getListedItems(), "Danh sách listedItems phải được khởi tạo để tránh NullPointerException");
    }

    @Test
    @DisplayName("Test logic thêm và xóa Item khỏi danh sách của Seller")
    void testSeller_AddAndRemoveItem() {
        Seller seller = new Seller("seller1", "Pass123!", "seller@gmail.com");

        // Màng bảo vệ phụ: Trong trường hợp file Seller.java chưa kịp sửa, ta khởi tạo tay để test chạy tiếp
        if (seller.getListedItems() == null) {
            seller.setListedItems(new ArrayList<>());
        }

        // Tận dụng class Art (đã viết ở bài trước) để làm Item mẫu thử nghiệm
        Art mockItem1 = new Art("item_001", "Tranh 1", "Mô tả", 100.0, seller, "Tác giả", 2023);
        Art mockItem2 = new Art("item_002", "Tranh 2", "Mô tả", 200.0, seller, "Tác giả", 2024);

        // 1. Test Thêm Item
        seller.addItem(mockItem1);
        seller.addItem(mockItem2);

        assertEquals(2, seller.getListedItems().size(), "Danh sách phải có 2 sản phẩm sau khi add");
        assertTrue(seller.getListedItems().contains(mockItem1), "Danh sách phải chứa đúng Object vừa truyền vào");

        // 2. Test Xóa Item bằng ID
        seller.removeItem("item_001");

        assertEquals(1, seller.getListedItems().size(), "Danh sách chỉ còn 1 sản phẩm sau khi gọi hàm remove");
        assertEquals("item_002", seller.getListedItems().get(0).getItemId(), "Sản phẩm còn lại phải là item_002");

        // 3. Test Xóa ID không tồn tại (Kiểm tra vòng lặp for không bị lỗi)
        seller.removeItem("ID_KHONG_TON_TAI");
        assertEquals(1, seller.getListedItems().size(), "Truyền ID sai thì danh sách phải giữ nguyên, không được crash");
    }
}