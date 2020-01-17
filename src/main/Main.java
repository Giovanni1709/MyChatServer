package main;

import java.io.IOException;
import java.net.ServerSocket;

public class Main {
    public static void main(String[] args) {
        new Main().run();
    }

    public void run(){
        ChatHub chatHub01 = new ChatHub();

        try {
            ServerSocket server = new ServerSocket(1337);
            new User(server.accept(),chatHub01);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
