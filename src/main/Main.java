package main;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.Charset;
import java.sql.SQLOutput;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
        new Main().run();
    }

    public void run(){
        ChatHub chatHub01 = new ChatHub();

        try {
            ServerSocket server = new ServerSocket(1337);
            while (true) {
                new User(server.accept(),chatHub01);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
