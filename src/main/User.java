package main;

import main.messages.*;

import java.awt.desktop.QuitEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class User extends Thread {
    private String username;
    private Socket userSocket;
    private ChatHub hub;
    private PrintWriter writer;
    private BufferedReader reader;
    private boolean activeReading;

    public String getUsername() {
        return username;
    }

    public User(Socket socket, ChatHub chatHub) {
        this.userSocket = socket;
        this.hub = chatHub;
        try {
            this.writer = new PrintWriter(socket.getOutputStream(), true);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        verifyUser();
    }

    public void run() {
        PingPong pingPong = new PingPong();
        pingPong.start();

        while (activeReading) {
            Message message = Message.create(readMessage());
            if (message instanceof BCSTMessage) {
                BCSTMessage bcstMessage = (BCSTMessage) message;
                hub.broadcastMessageToAll(this, bcstMessage);
            } else if (message instanceof PING) {
                pingPong.wake((PONG) message);
            } else if (message instanceof QUIT) {
                sendMessage(new OKMessage("goodbye"));
                activeReading = false;
                hub.removeOnlineUser(this);
                this.endConnection();
            }
        }
    }

    public void verifyUser(){
        HELOMessage firstMessage= new HELOMessage("welcome to myChatClient");
        sendMessage(firstMessage);
        Message firstReply = Message.create(readMessage());
        if (firstReply instanceof HELOMessage) {
            String name = firstReply.getContent();
            if (name.split(" ").length > 2) {
                ERRMessage uNameError = new ERRMessage("username has an invalid format (only characters, numbers and underscores are allowed)");
                sendMessage(uNameError);
                verifyUser();
            } else {
                if (hub.isOnline(name)) {
                    ERRMessage uNameError = new ERRMessage("user already logged in");
                    sendMessage(uNameError);
                    verifyUser();
                } else {
                    OKMessage loginConfirmation = new OKMessage(firstReply);
                    sendMessage(loginConfirmation);
                    hub.addToOnlineUsers(this);
                    this.username = name;
                    activeReading = true;
                    this.start();
                }
            }
        }
    }

    public void endConnection() {
        try {
            userSocket.close();
        } catch (IOException e) {
            System.out.println("Socket failed to close.");
            e.printStackTrace();
        }
    }

    public synchronized void sendMessage(Message message) {
        writer.println(message.toStringForm());
    }

    public synchronized String readMessage() {
        try {
            return reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private class PingPong extends Thread {
        private PONG pong;
        public void run() {
            try {
                while (true) {
                    this.sleep(5000);
                    PING ping = new PING();
                    sendMessage(ping);
                    synchronized(this) {
                        pong = null;
                        this.wait(3000);
                    }
                    if (pong == null) {
                        DSCN pongTimeOut = new DSCN("Pong timeout");
                        sendMessage(pongTimeOut);
                        endConnection();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public synchronized void wake(PONG pong) {
            this.pong = pong;
            this.notify();
        }
    }

}
