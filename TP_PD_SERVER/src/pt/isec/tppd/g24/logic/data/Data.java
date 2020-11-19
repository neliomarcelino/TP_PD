package pt.isec.tppd.g24.logic.data;

import pt.isec.tppd.g24.logic.data.resources.Channel.Channel;
import pt.isec.tppd.g24.logic.data.resources.User;

import java.util.ArrayList;

public class Data {
   private ArrayList<Channel> channels;      // lista de todos os canais
   private ArrayList<User> users;            // lista de todos os users
   
   public Data() {
      this.channels = new ArrayList<>();
      this.users = new ArrayList<>();
   }
}
