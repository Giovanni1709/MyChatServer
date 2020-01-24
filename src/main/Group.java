package main;

import main.messages.BCSTMessage;
import main.messages.GROUPMessage;
import main.messages.OKMessage;

import java.util.ArrayList;

public class Group {
    private String name;
    private User owner;
    private ArrayList<User> members;

    public Group(String name, User owner) {
        this.name = name;
        this.owner = owner;
        this.members = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void joinGroup(User user){
        if (owner == null) {
            owner = user;
        }else {
            members.add(user);
        }
    }

    public void leaveGroup(User user) {
        if (user.getUsername().equals(owner.getUsername())) {
            owner = null;
            if (members.size() > 0) {
                owner = members.remove(0);
            }
        } else {
            members.remove(user);
        }
    }

    public void broadcastToAll(User user, BCSTMessage bcstMessage) {
        if (user.getUsername().equals(owner.getUsername())) {
            owner.sendMessage(new OKMessage(new GROUPMessage(bcstMessage)));
        } else {
            owner.sendMessage(new GROUPMessage(new BCSTMessage(user.getUsername() + " " + bcstMessage.getContent())));
        }

        for (User member : members) {
            if (member.getUsername().equals(user.getUsername())) {
                member.sendMessage(new OKMessage(new GROUPMessage(bcstMessage)));
            } else {
                member.sendMessage(new GROUPMessage(new BCSTMessage(user.getUsername() + " " + bcstMessage.getContent())));
            }
        }
    }

    public String kickMember(User user, String username) {
        if (user.getUsername().equals(owner.getUsername())) {
            for (User member : members) {
                if (member.getUsername().equals(username)) {
                    members.remove(member);
                    member.removeFromGroup();
                    return null;
                }
            }
            return "user isn't currently joined in the group";
        } else {
            return "you are not the owner of the group";
        }
    }



}
