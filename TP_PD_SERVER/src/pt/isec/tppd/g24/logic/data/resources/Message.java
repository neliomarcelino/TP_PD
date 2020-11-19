package pt.isec.tppd.g24.logic.data.resources;

import java.sql.Timestamp;

public class Message {
   private final int type;                            // tipo de mensagem (identificados no ficheiro Constants.java)
   private String text;                               // texto da mensagem
   private final User author;                         // autor da mensagem
   private Timestamp lastupdate;                      // regista ultima data de edicao
   private final Timestamp create;                    // regista data de criacao
   
   public Message(String text, User author, int type){
      this.type = type;
      this.author = author;
      this.text = text;
      this.lastupdate = new Timestamp(System.currentTimeMillis());
      this.create = new Timestamp(System.currentTimeMillis());
   }
   
   
   // Getters
   public String getText() {
      return this.text;
   }
   
   public User getAuthor() {
      return this.author;
   }
   
   public Timestamp getLastupdate() {
      return this.lastupdate;
   }
   
   public int getType() {
      return type;
   }
   
   public Timestamp getCreate() {
      return create;
   }
   
   
   // Setters
   public void setText(String text) {
      this.text = text;
      updatesTimestamp();
   }
   
   
   // Private functions
   private void updatesTimestamp() {
      this.lastupdate = new Timestamp(System.currentTimeMillis());
   }
   
   
   // Overrides
   @Override
   public String toString() {
      return "Message{" +
              "type=" + type +
              ", message='" + text + '\'' +
              ", author=" + author +
              ", lastupdate=" + lastupdate +
              ", create=" + create +
              '}';
   }
}
