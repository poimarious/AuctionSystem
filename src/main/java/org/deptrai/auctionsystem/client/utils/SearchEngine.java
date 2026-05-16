package org.deptrai.auctionsystem.client.utils;

import org.deptrai.auctionsystem.shared.models.auction.Auction;

import java.util.List;
import java.util.stream.Collectors;

public class SearchEngine {

  public static List<Auction> searchAuctions(List<Auction> sourceList, String keyword) {

    // if user searchs without any keyword, return all Auction
    if(keyword == null || keyword.trim().isEmpty()) {
      return sourceList;
    }

    String lowerKeyword = keyword.toLowerCase().trim();
    return sourceList.stream().filter(auction -> {
      String name = auction.getItem().getName().toLowerCase();
      String desc = auction.getItem().getDescription().toLowerCase();
      return name.contains(lowerKeyword) || desc.contains(lowerKeyword);
    }).collect(Collectors.toList());
  }


}
