package pt.isec.tppd.g24.servidor;

import pt.isec.tppd.g24.*;
import java.io.Serializable;
import java.util.*;

public class UserRmi{
    private String username;
	private UserInterface listener;

    public UserRmi(UserInterface listener, String username){
        this.username = username;
        this.listener = listener;
    }

    public String getUsername() {return username;}
    public UserInterface getInterface() {return listener;}
}
