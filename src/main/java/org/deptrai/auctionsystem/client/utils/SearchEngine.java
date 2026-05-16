package org.deptrai.auctionsystem.client.utils;

import org.deptrai.auctionsystem.shared.models.auction.AuctionSummary;
import org.deptrai.auctionsystem.shared.models.auction.AuctionSummary;

import java.util.List;
import java.util.stream.Collectors;

public class SearchEngine {

  public static List<AuctionSummary> searchAuctions(List<AuctionSummary> sourceList, String keyword) {
    if (keyword == null || keyword.trim().isEmpty()) {
      return sourceList;
    }

    String lowerKeyword = keyword.toLowerCase().trim();
    return sourceList.stream().filter(dto -> {
      String name = dto.getItemName().toLowerCase();
      String desc = dto.getItemDescription().toLowerCase();
      return name.contains(lowerKeyword) || desc.contains(lowerKeyword);
    }).collect(Collectors.toList());
  }
}