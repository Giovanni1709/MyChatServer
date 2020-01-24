package main;

import main.messages.*;

import java.util.ArrayList;

public class ChatHub {
    private ArrayList<User> onlineUsers;
    private ArrayList<Group> groups;

    public ChatHub() {
        onlineUsers = new ArrayList();
        groups = new ArrayList<>();
    }

    public void addToOnlineUsers(User user) {
        onlineUsers.add(user);
    }

    public void removeOnlineUser(User user) {
        onlineUsers.remove(user);
    }

    public boolean isOnline(String uName) {
        for (User user : onlineUsers)
            if (user.getUsername().equals(uName)) {
                return true;
            }
        return false;
    }

    public void broadcastMessageToAll(User brodcastingUser, BCSTMessage bcstMessage) {
        BCSTMessage bcstMessageToOther = new BCSTMessage(brodcastingUser.getUsername() + " " + bcstMessage.getContent());
        for (User user : onlineUsers) {
            if (user.equals(brodcastingUser)) {
                user.sendMessage(new OKMessage(bcstMessage));
            } else {
                user.sendMessage(bcstMessageToOther);
            }
        }
        System.out.println("user " + brodcastingUser.getUsername() + " broadcasted a message: " + bcstMessage.getContent());
    }

    public ArrayList<String> getAllOnlineUsers() {
        ArrayList<String> onlineUsernames = new ArrayList<>();
        for (User user : onlineUsers) {
            onlineUsernames.add(user.getUsername());
        }
        return onlineUsernames;
    }

    public boolean sendMessageToOtherUser(String designatedUsername, PMSGMessage message) {
        for (User user : onlineUsers) {
            if (user.getUsername().equals(designatedUsername)) {
                user.sendMessage(message);
                return true;
            }
        }
        return false;
    }

    public boolean groupExists(String name){
        for (Group group : groups) {
            if (group.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public Group createGroup(String groupname, User user) {
        Group newGroup = new Group(groupname, user);
        groups.add(newGroup);
        return newGroup;
    }

    public ArrayList<String> getAllGroups() {
        ArrayList<String> groupnames = new ArrayList<>();
        for (Group group : groups) {
            groupnames.add(group.getName());
        }
        return groupnames;
    }

    public Group joinGroup(String groupname, User user) {
        for (Group group : groups) {
            if (group.getName().equals(groupname)) {
                group.joinGroup(user);
                return group;
            }
        }
        return null;
    }
}
