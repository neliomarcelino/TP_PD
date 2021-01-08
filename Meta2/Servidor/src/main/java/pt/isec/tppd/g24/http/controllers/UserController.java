package pt.isec.tppd.g24.http.controllers;

import org.springframework.web.bind.annotation.*;
import pt.isec.tppd.g24.http.security.Token;
import pt.isec.tppd.g24.http.security.User;
import pt.isec.tppd.g24.servidor.Main;

@RestController
//@RequestMapping("user")
public class UserController
{
    @PostMapping("user/login")
    public User login(@RequestBody User user)
    {
        String res = Main.verificaUser(user.getUsername(), user.getPassword());
        if(res.equalsIgnoreCase("OK")){
            String token = Token.getNewToken(user.getUsername());
            user.setToken(token);
        }
        return user;
    }
}
