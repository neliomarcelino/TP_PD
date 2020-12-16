package pt.isec.tppd.g24.cliente;
import pt.isec.tppd.g24.*;

public class User {
   String name;
   String username;
   String password;
   String foto;
   
   public User(String username, String name) {
      this.username = username;
      this.name = name;
   }
   
   public User() {}
   
   public void setName(String name) {
      this.name = name;
   }
   
   public String getName() {
      return this.name;
   }
   
   public void setPassword(String password) {
      this.password = password;
   }
   
   public String getPassword() {
      return password;
   }
   
   public void setUsername(String username) {
      this.username = username;
   }
   
   public String getUsername() {
      return this.username;
   }
   
   public String getFoto() {
      return this.foto;
   }
   
   public void setFoto(String foto) {
      this.foto = foto;
   }
}
