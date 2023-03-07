package com.webgrambnetwork;

import android.support.v4.internal.view.SupportMenu;

import org.littleshoot.proxy.ProxyAuthenticator;

import java.util.Random;

public class UserProxyAuthenticator implements ProxyAuthenticator {
    public String password = createPassword();
    public String username = createUsername();

    UserProxyAuthenticator() {
    }

    public boolean authenticate(String username2, String password2) {
        return this.username.equals(username2) && this.password.equals(password2);
    }

    public String getRealm() {
        return null;
    }

    private String createUsername() {
        return "webgrambx" + Integer.toHexString(new Random().nextInt(SupportMenu.USER_MASK));
    }

    private String createPassword() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        while (sb.length() < 20) {
            sb.append(Integer.toHexString(random.nextInt(20)));
        }
        return sb.toString();
    }
}