package org.deptrai.auctionsystem.server;

import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.bid.Bid;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static ExecutorService pool = Executors.newFixedThreadPool(8);
    // Server Socket instance that will listen for TCP connection requests
    private static ServerSocket serverSocket;
    private static Map<String, ClientHandler> clientsHandlers;
    private static Map<String, Auction> auctions;
    private static Server server;
    // Boolean that will control the listening loop of the server for new connections
    private static boolean isListening;
    private Server() {
        try {
            ServerSocket serverSocket = new ServerSocket(5050);

        } catch (Exception e) {
            e.printStackTrace();
        }
        this.clientsHandlers = new HashMap<String, ClientHandler>();
        this.auctions = new HashMap<String, Auction>();
        this.isListening = false;

    }

    private Server(int port) {
        try {
            serverSocket = new ServerSocket(port);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.clientsHandlers = new HashMap<String, ClientHandler>();
        this.auctions = new HashMap<String, Auction>();
        this.isListening = false;
    }

    //Method returns a singleton server object.
    public static Server getInstance() {
        if (server == null) {
            server = new Server();
        }
        return server;
    }
    public static Server getInstance(int port) {
        if (server == null) {
            server = new Server(port);
        }
        return server;
    }
    //Getters and Setters

    public static ExecutorService getPool() {
        return pool;
    }

    public static void setPool(ExecutorService pool) {
        Server.pool = pool;
    }

    public static ServerSocket getServerSocket() {
        return serverSocket;
    }

    public static void setServerSocket(ServerSocket serverSocket) {
        Server.serverSocket = serverSocket;
    }

    public static Map<String, ClientHandler> getClientsHandlers() {
        return clientsHandlers;
    }

    public static void setClientsHandlers(Map<String, ClientHandler> clientsHandlers) {
        Server.clientsHandlers = clientsHandlers;
    }

    public static Map<String, Auction> getAuctions() {
        return auctions;
    }

    public static void setAuctions(Map<String, Auction> auctions) {
        Server.auctions = auctions;
    }

    public static Server getServer() {
        return server;
    }

    public static void setServer(Server server) {
        Server.server = server;
    }

    public void listen() throws IOException{
        this.isListening = true;
        while (this.isListening) { // listen until the variable is false
            Socket clientSocket = serverSocket.accept(); //Accept new connection

            ClientHandler clientThread = new ClientHandler();

            pool.execute(clientThread); // Executing clientThread


        }

    }



}
