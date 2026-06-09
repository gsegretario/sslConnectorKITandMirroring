package session;

import ClientImplementation.Client3DXImpl;

public class UserSession {
    private final Client3DXImpl client;
    private long lastLoginTime;

    public UserSession(Client3DXImpl client, long loginTime) {
        this.client = client;
        this.lastLoginTime = loginTime;
    }

    public Client3DXImpl getClient() {
        return client;
    }

    public long getLastLoginTime() {
        return lastLoginTime;
    }

    public void updateLoginTime(long time) {
        this.lastLoginTime = time;
    }
}