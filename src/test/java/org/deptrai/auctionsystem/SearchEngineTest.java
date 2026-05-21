package org.deptrai.auctionsystem;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.deptrai.auctionsystem.client.utils.SearchEngine;
import org.deptrai.auctionsystem.shared.models.auction.AuctionStatus;
import org.deptrai.auctionsystem.shared.models.auction.AuctionSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SearchEngineTest {

    private List<AuctionSummary> mockSourceList;

    @BeforeEach
    void setUp() {
        // Khởi tạo một danh sách giả lập để dùng chung cho các bài Test
        mockSourceList = new ArrayList<>();

        // Constructor chuẩn: (auctionId, itemName, itemDescription, category, currentPrice, status, endTime, imageUrl, imageBytes)
        mockSourceList.add(new AuctionSummary(
                "1", "Macbook Pro M1", "Laptop siêu mỏng nhẹ của Apple", "Electronics",
                1000.0, AuctionStatus.RUNNING, LocalDateTime.now().plusDays(1), null, null));

        mockSourceList.add(new AuctionSummary(
                "2", "Bàn phím cơ Akko", "Bàn phím gõ rất êm, switch màu xanh", "Electronics",
                50.0, AuctionStatus.OPEN, LocalDateTime.now().plusDays(2), null, null));

        mockSourceList.add(new AuctionSummary(
                "3", "Bức tranh mùa thu", "Nghệ thuật đương đại phác họa cây táo", "Art",
                200.0, AuctionStatus.RUNNING, LocalDateTime.now().plusDays(3), null, null));
    }

    // ==========================================
    // BÀI TEST 1: KEYWORD RỖNG (TRẢ VỀ TẤT CẢ)
    // ==========================================
    @Test
    void testSearchAuctions_NullOrEmptyKeyword_ShouldReturnOriginalList() {
        List<AuctionSummary> resultNull = SearchEngine.searchAuctions(mockSourceList, null);
        assertEquals(3, resultNull.size(), "Nếu keyword null, phải trả về toàn bộ danh sách gốc");

        List<AuctionSummary> resultEmpty = SearchEngine.searchAuctions(mockSourceList, "");
        assertEquals(3, resultEmpty.size(), "Nếu keyword rỗng, phải trả về toàn bộ danh sách gốc");

        List<AuctionSummary> resultBlank = SearchEngine.searchAuctions(mockSourceList, "    ");
        assertEquals(3, resultBlank.size(), "Nếu keyword toàn dấu cách, phải trả về toàn bộ");
    }

    // ==========================================
    // BÀI TEST 2: TÌM KIẾM ĐÚNG THEO TÊN (BỎ QUA HOA/THƯỜNG)
    // ==========================================
    @Test
    void testSearchAuctions_MatchNameCaseInsensitive_ShouldReturnMatched() {
        List<AuctionSummary> result = SearchEngine.searchAuctions(mockSourceList, "mAcBoOk");

        assertEquals(1, result.size(), "Phải tìm thấy 1 sản phẩm khớp tên");
        assertEquals("Macbook Pro M1", result.get(0).getItemName(), "Sản phẩm tìm thấy phải là Macbook");
    }

    // ==========================================
    // BÀI TEST 3: TÌM KIẾM DỰA VÀO MÔ TẢ (DESCRIPTION)
    // ==========================================
    @Test
    void testSearchAuctions_MatchDescription_ShouldReturnMatched() {
        List<AuctionSummary> result = SearchEngine.searchAuctions(mockSourceList, "cây táo");

        assertEquals(1, result.size(), "Phải tìm thấy 1 sản phẩm có mô tả chứa từ khóa");
        assertEquals("Bức tranh mùa thu", result.get(0).getItemName());
    }

    // ==========================================
    // BÀI TEST 4: TỪ KHÓA TÌM THẤY Ở NHIỀU SẢN PHẨM
    // ==========================================
    @Test
    void testSearchAuctions_MatchMultiple_ShouldReturnAllMatched() {
        List<AuctionSummary> result = SearchEngine.searchAuctions(mockSourceList, "a");
        assertEquals(3, result.size(), "Phải trả về nhiều sản phẩm nếu chúng đều chứa từ khóa");
    }

    // ==========================================
    // BÀI TEST 5: TỪ KHÓA CÓ DẤU CÁCH THỪA (TRIM)
    // ==========================================
    @Test
    void testSearchAuctions_KeywordWithExtraSpaces_ShouldTrimAndMatch() {
        List<AuctionSummary> result = SearchEngine.searchAuctions(mockSourceList, "   Akko   ");

        assertEquals(1, result.size(), "Hệ thống phải tự động cắt khoảng trắng (trim) và tìm ra Akko");
        assertEquals("Bàn phím cơ Akko", result.get(0).getItemName());
    }

    // ==========================================
    // BÀI TEST 6: TỪ KHÓA KHÔNG TỒN TẠI (TRẢ VỀ RỖNG)
    // ==========================================
    @Test
    void testSearchAuctions_NoMatch_ShouldReturnEmptyList() {
        List<AuctionSummary> result = SearchEngine.searchAuctions(mockSourceList, "Tàu vũ trụ");

        assertNotNull(result, "Kết quả trả về không được là null");
        assertTrue(result.isEmpty(), "Kết quả trả về phải là một List rỗng (size = 0)");
    }
}