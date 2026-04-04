package edu.cs342.project2;

public class Main {
    public static void main(String[] args) {
        try {
            Server server = new Server(8080);
            server.start();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}