package pt.isec.tppd.g24.logic.data.resources;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;

public class User implements Comparable {
   private String name;                               // Nome do utilizador
   private final String username;                     // identificador do utilizador
   private String password;
   private String picture_path;                       // Byte[] ou path da imagem?
   private Timestamp lastupdate;                      // regista ultima data de edicao
   private final Timestamp create;                    // regista data de criacao
   private InetAddress client_addr;                   // IP da coneccao do cliente
   
   public User(String name, String username, String password) {
      this.name = name;
      this.username = username;
      this.password = password;
      this.picture_path = "";
      this.lastupdate = new Timestamp(System.currentTimeMillis());
      this.create = new Timestamp(System.currentTimeMillis());
   }
   
   
   // Getters
   public String getPicturePath() {
      return picture_path;
   }
   
   public String getUsername() {
      return username;
   }
   
   public String getName() {
      return name;
   }
   
   public InetAddress getAddress() {
      return client_addr;
   }
   
   
   // Setters
   public void setName(String n) {
      this.name = n;
      updatesTimestamp();
   }
   
   public void setAddress(String addr) {
      try {
         this.client_addr = InetAddress.getByName(addr);
         updatesTimestamp();
      }catch (UnknownHostException e) {
         throw new Error(e);
      }
   }
   
   public void setAvatar(String path) {
      this.picture_path = path;
      updatesTimestamp();
   }
   
   public void setPassword(String pass) {
      this.password = pass;
      updatesTimestamp();
   }
   
   
   // Private functions
   private void updatesTimestamp() {
      this.lastupdate = new Timestamp(System.currentTimeMillis());
   }
   
   
   // Overrides
   @Override
   public int compareTo(Object o) {
      return this.getName().compareTo(((User) o).getName());
   }
   
   @Override
   public String toString() {
      return "User{" +
              "name='" + name + '\'' +
              ", username='" + username + '\'' +
              ", picture_path='" + picture_path + '\'' +
              ", lastupdate=" + lastupdate +
              ", create=" + create +
              ", client_addr=" + client_addr +
              '}';
   }
}
