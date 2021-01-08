package pt.isec.tppd.g24.http.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pt.isec.tppd.g24.servidor.Main;

@RestController
public class MsgController {
    
    @PostMapping("/mensagemServer")
    public void enviaMensagemServer(@RequestHeader("mensagem") String mensagem, @RequestHeader("username") String username){
        Main.enviaMensagemServer(username, mensagem);
    }
    
    @GetMapping("/mensagens")
    public String ultimasNMensagens(@RequestParam(value = "numero", required = true) int numero){ // required é true se omitido
        return Main.ultimasNMensagens(numero); 
    }
}
