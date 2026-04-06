package com.mejbri.pfe.netopssynchro.service;

import com.jcraft.jsch.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
@RequiredArgsConstructor
public class SshService {

    private final EncryptionService encryptionService;

    public String execute(String host, int port, String username,
                          String encryptedPassword, String command) {
        JSch jsch = new JSch();
        Session session = null;
        try {
            session = jsch.getSession(username, host, port);
            session.setPassword(encryptionService.decrypt(encryptedPassword));
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(10000);

            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayOutputStream err = new ByteArrayOutputStream();
            channel.setOutputStream(out);
            channel.setErrStream(err);
            channel.connect();

            while (!channel.isClosed()) Thread.sleep(100);

            channel.disconnect();
            String error = err.toString();
            return error.isBlank() ? out.toString() : out + "\nSTDERR: " + error;
        } catch (Exception e) {
            return "SSH_ERROR: " + e.getMessage();
        } finally {
            if (session != null) session.disconnect();
        }
    }

    public boolean testConnection(String host, int port, String username,
                                  String encryptedPassword) {
        JSch jsch = new JSch();
        Session session = null;
        try {
            session = jsch.getSession(username, host, port);
            session.setPassword(encryptionService.decrypt(encryptedPassword));
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(5000);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (session != null) session.disconnect();
        }
    }
}
