package main;

import main.messages.BCSTMessage;
import main.messages.OKMessage;
import java.util.ArrayList;

public class ChatHub {
    private ArrayList<User> onlineUsers;

    public ChatHub(){
        onlineUsers = new ArrayList();
    }

    public void addToOnlineUsers(User user) {
        onlineUsers.add(user);
    }

    public void removeOnlineUser(User user) {
        onlineUsers.remove(user);
    }

    public boolean isOnline(String uName) {
        for (User user:onlineUsers)
            if (user.getUsername().equals(uName)) {
                return true;
            }
        return false;
    }

    public void broadcastMessageToAll(User brodcastingUser, BCSTMessage bcstMessage) {
        BCSTMessage bcstMessageToOther = new BCSTMessage(brodcastingUser.getUsername() + " " + bcstMessage.getContent());
        for (User user: onlineUsers) {
            if (user.equals(brodcastingUser)) {
                user.sendMessage(new OKMessage(bcstMessage));
            } else {
                user.sendMessage(bcstMessageToOther);
            }
        }
    }
}
