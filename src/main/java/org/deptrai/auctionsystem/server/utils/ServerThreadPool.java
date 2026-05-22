package org.deptrai.auctionsystem.server.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * đây là threadpool làm các việc vặt trong clientHandler
 * */
public class ServerThreadPool {

  private static final int POOL_SIZE = 20;

  private static final ExecutorService threadPool = Executors.newFixedThreadPool(POOL_SIZE);

  public static void submitTask(Runnable task) {
    threadPool.submit(task);
  }
}
