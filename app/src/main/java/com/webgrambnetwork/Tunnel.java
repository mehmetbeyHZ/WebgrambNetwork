package com.webgrambnetwork;


import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.net.InetSocketAddress;

public class Tunnel {
    private InetSocketAddress localAddress;
    private Session session;
    private JSch ssh = new JSch();
    private String user;

    Tunnel(String user2, String prvkey, InetSocketAddress localAddress2) throws JSchException {
        localAddress = localAddress2;
        user = user2;
    }

    public boolean hasConnectedSession() {
        return this.session != null && this.session.isConnected();
    }

    public synchronized void connect(String host, int port) throws Exception {
        if (!hasConnectedSession()) {
            session = ssh.getSession("tunnel", host);
            session.setPassword("NSqxZjE2aH");
            session.setTimeout(30000);
            session.setServerAliveInterval(30000);
            session.setServerAliveCountMax(2);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            session.setPortForwardingR(port, localAddress.getHostName(), localAddress.getPort());
        }
    }

    public void disconnect() {
        if (hasConnectedSession()) {
            this.session.disconnect();
        }
    }
}
