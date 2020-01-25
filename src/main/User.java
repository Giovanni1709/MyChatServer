package main;

import main.messages.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Random;

public class User extends Thread {

    private String username;
    private Socket userSocket;
    private ChatHub hub;
    private PrintWriter writer;
    private BufferedReader reader;
    private boolean activeReading;
    private Group group;
    private String key;

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

        this.start();
    }

    public void run() {
        key = " ";
        while (key.contains(" ")) {
            key = randomString(20);
        }
        writer.println(new HELOMessage(key).toStringForm());
        while (username == null) {
            username = verifyUser();
        }

        if (!username.equals("UnknownClient")) {
            System.out.println("User "+ username+ " has connected to the chat server.");;
            hub.addToOnlineUsers(this);
            activeReading = true;

            PingPong pingPong = new PingPong();
            pingPong.start();

            while (activeReading) {
                Message message = Message.create(readMessage());
                if (message instanceof BCSTMessage) {
                    BCSTMessage bcstMessage = (BCSTMessage) message;
                    hub.broadcastMessageToAll(this, bcstMessage);
                } else if (message instanceof USERMessage) {
                    Message containedMessage = ((USERMessage) message).getUserOrientedMessage();
                    if (containedMessage instanceof LISTMessage) {
                        ArrayList<String> onlineUsernames =hub.getAllOnlineUsers();
                        String content = "";
                        for (String usernames: onlineUsernames) {
                            content += usernames + " ";
                        }
                        sendMessage(new OKMessage(new USERMessage(new LISTMessage(content))));
                        System.out.println("User "+ username+ " requested a list of online users.");
                    }else if(containedMessage instanceof PMSGMessage){
                        PMSGMessage privateMessage = (PMSGMessage) containedMessage;
                        if (hub.sendMessageToOtherUser(privateMessage.getUsername(), new PMSGMessage(username, privateMessage.getPrivateMessage()))) {
                            sendMessage(new OKMessage(message));
                            System.out.println("User"+ username +" sent a message to user "+ privateMessage.getUsername());
                        } else {
                            sendMessage(new ERRMessage("designated user is not online"));
                        }
                    }
                } else if (message instanceof GROUPMessage) {
                    Message containedMessage = ((GROUPMessage) message).getGroupOrientedMessage();
                    if (containedMessage instanceof CREATEMessage) {
                        if (containedMessage.getContent().split(" ").length > 1) {
                            sendMessage(new ERRMessage("Groupname has an invalid format (only characters, numbers and underscores are allowed)."));
                        } else {
                            String groupname = containedMessage.getContent();
                            if (hub.groupExists(groupname)) {
                                sendMessage(new ERRMessage("Group already exists."));
                            } else {
                                group=hub.createGroup(groupname,this);
                                sendMessage(new OKMessage(new GROUPMessage(new CREATEMessage(groupname))));
                                System.out.println("User " + username + " created a group.");
                            }
                        }
                    } else if (containedMessage instanceof LISTMessage) {
                        ArrayList<String> groupnames = hub.getAllGroups();
                        String content = "";
                        for (String group: groupnames) {
                            content += group + " ";
                        }
                        sendMessage(new OKMessage(new GROUPMessage(new LISTMessage(content))));
                        System.out.println("User "+ username+ " requested a list of available users.");
                    } else if (containedMessage instanceof JOINMessage) {
                        if (group == null) {
                            group = hub.joinGroup(containedMessage.getContent(), this);
                            if (group != null) {
                                sendMessage(new OKMessage(message));
                                System.out.println("User " + username + " has joined in group " + group.getName() +".");
                            } else {
                                sendMessage(new ERRMessage("Group does not exist."));
                            }
                        } else {
                            sendMessage(new ERRMessage("You are already part of a group."));
                        }
                    } else if (containedMessage instanceof BCSTMessage) {
                        if (group != null) {
                            group.broadcastToAll(this, (BCSTMessage) containedMessage);
                            System.out.println("user "+ username + " broadcasted a message to group "+ group.getName()+".");
                        } else {
                            sendMessage(new ERRMessage("You are not part  a group yet."));
                        }
                    } else if (containedMessage instanceof LEAVEMEssage) {
                        if (group != null) {
                            group.leaveGroup(this);
                            group = null;
                            sendMessage(new OKMessage(message));
                            System.out.println("user "+ username + " left a group.");
                        } else {
                            sendMessage(new ERRMessage("You are not part a group yet."));
                        }
                    } else if (containedMessage instanceof KICKMessage) {
                        if (group != null) {
                            String error = group.kickMember(this, containedMessage.getContent());
                            if (error == null) {
                                sendMessage(new OKMessage(new GROUPMessage(new KICKMessage(containedMessage.getContent()))));
                                System.out.println("User " + username + " has kicked " + containedMessage.getContent() + " from a group.");
                            } else {
                                sendMessage(new ERRMessage(error));
                            }
                        } else {
                            sendMessage(new ERRMessage("You are not part a group yet."));
                        }
                    }
                } else if (message instanceof PONG) {
                    pingPong.wake((PONG) message);
                } else if (message instanceof QUIT) {
                    sendMessage(new OKMessage("goodbye"));
                    activeReading = false;
                    pingPong.setInactive();
                    hub.removeOnlineUser(this);
                    this.endConnection();
                }
            }
        } else {
            endConnection();
        }
    }

    public String verifyUser(){
        Message firstReply = Message.create(readMessage());
        if (firstReply instanceof HELOMessage) {
            String name = firstReply.getContent();
            if (name.split(" ").length > 1) {
                ERRMessage uNameError = new ERRMessage("username has an invalid format (only characters, numbers and underscores are allowed)");
                sendMessage(uNameError);
            } else {
                if (hub.isOnline(name)) {
                    ERRMessage uNameError = new ERRMessage("user already logged in");
                    sendMessage(uNameError);
                } else {
                    sendMessage(new OKMessage(firstReply.toStringForm()));
                    return name;
                }
            }
        } else {
            return "UnknownClient";
        }
        return null;
    }

    public void removeFromGroup() {
        group = null;
    }

    public void endConnection() {
        try {
            userSocket.close();
            hub.removeOnlineUser(this);
        } catch (IOException e) {
            System.out.println("Socket failed to close.");
            e.printStackTrace();
        }
        System.out.println("Socket for user "+ username+ " closed.");
    }

    public void sendMessage(Message message) {
        String encryptedMessage = AES.encrypt(message.toStringForm(), key);
//        System.out.println("sent:" + encryptedMessage);
        writer.println(encryptedMessage);
    }

    public String readMessage() {
        try {
            String message = AES.decrypt(reader.readLine(), key);
//            System.out.println("received: " +message);
            return message;
        } catch (IOException e) {
            System.out.println("Cannot read message");
//            e.printStackTrace();
        }
        return null;
    }

    private class PingPong extends Thread {
        private PONG pong;
        private boolean active = true;
        public void run() {
            try {
                while (active) {
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
                        this.active = false;
                        activeReading = false;
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

        public void setInactive() {
            this.active = false;
        }
    }
    public String randomString(int length) {
        Random random = new Random();
        byte[] array = new byte[length]; // length is bounded by 7
        for (int i = 0; i< length ; i++) {
            random.nextBytes(array);
        }
        String generatedString = new String(array, Charset.forName("UTF-8"));

        return generatedString;
    }
}
