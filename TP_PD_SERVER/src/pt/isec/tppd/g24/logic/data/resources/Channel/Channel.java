package pt.isec.tppd.g24.logic.data.resources.Channel;

import pt.isec.tppd.g24.logic.data.resources.Message;
import pt.isec.tppd.g24.logic.data.resources.User;

import java.sql.Timestamp;
import java.util.ArrayList;

public class Channel implements Comparable {
   private String name;
   private String description;
   private String password;
   private User admin;
   private ArrayList<User> users;               // lista de todos os utilizadores
   private ArrayList<Message> msg_list;         // lista de todas as mensagens
   private Timestamp lastupdate;                // regista ultima data de edicao
   private Timestamp create;                    // regista data de criacao
   
   public Channel(String nome, String description, String password, User admin) {
      this.name = nome;
      this.description = description;
      this.password = password;
      this.admin = admin;
      this.lastupdate = new Timestamp(System.currentTimeMillis());
      this.create = new Timestamp(System.currentTimeMillis());
      this.msg_list = new ArrayList<>();
      
      this.users = new ArrayList<>();
      users.add(admin);
   }
   
   
   // Getters
   public String getName() {
      return name;
   }
   
   public String getDescription() {
      return description;
   }
   
   public User getAdmin() {
      return admin;
   }
   
   public Timestamp getLastUpdate() {
      return lastupdate;
   }
   
   public ArrayList<Message> getMessages(){
      return msg_list;
   }
   
   
   // Setters
   public void setName(String name) {
      this.name = name;
      updatesTimestamp();
   }
   
   public void setDescription(String description) {
      this.description = description;
      updatesTimestamp();
   }
   
   public void setPassword(String password) {
      this.password = password;
      updatesTimestamp();
   }
   
   public void setAdmin(User admin) {
      this.admin = admin;
      updatesTimestamp();
   }
   
   
   // Private functions
   private void updatesTimestamp() {
      this.lastupdate = new Timestamp(System.currentTimeMillis());
   }
   
   
   // Add to array
   public Boolean addUser(User u) {
      try {
         users.add(u);
         return true;
      } catch (UnsupportedOperationException e) {
         return false;
      }
   }
   
   public Boolean addMsg(Message msg) {
      try {
         msg_list.add(msg);
         return true;
      } catch (UnsupportedOperationException e) {
         return false;
      }
   }
   
   
   // Overrides
   @Override
   public int compareTo(Object o) {
      return this.getName().compareTo(((Channel) o).getName());
   }
   
   @Override
   public String toString() {
      return "Channel{" +
              "name='" + getName() + '\'' +
              ", description='" + getDescription() + '\'' +
              ", admin=" + getAdmin() +
              ", users=" + users +
              ", msg_list=" + msg_list +
              ", lastupdate=" + getLastUpdate() +
              ", create=" + create +
              '}';
   }
}