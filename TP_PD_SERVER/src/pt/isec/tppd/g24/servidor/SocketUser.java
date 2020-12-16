package pt.isec.tppd.g24.servidor;

import java.net.Socket;

public class SocketUser {
   String username;
   Socket socket;
   
   public SocketUser() { }
   
   public void setUsername(String username) {
      this.username = username;
   }
   
   public String getUsername() {
      return this.username;
   }
   
   public Socket getSocket() {
      return this.socket;
   }
   
   public void setSocket(Socket socket) {
      this.socket = socket;
   }
}
