package session;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import ClientImplementation.Client3DXImpl;
import ClientImplementation.Client3dExperienceFactory;
import java.util.concurrent.TimeUnit;


public class SessionManager {

    private static final long SESSION_TTL = TimeUnit.MINUTES.toMillis(20);

    private static final ConcurrentHashMap<String, UserSession> sessions = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    private static Object getLock(String userKey) {
        return locks.computeIfAbsent(userKey, k -> new Object());
    }

    public static Client3DXImpl getClient(String userKey, HashMap<String, String> loginMap) throws Exception {

        UserSession session = sessions.get(userKey);

        if (session != null && !isExpired(session)) {
        	
            return session.getClient();
        }

        synchronized (getLock(userKey)) {

            session = sessions.get(userKey);

            if (session != null && !isExpired(session)) {
                return session.getClient();
            }

            Client3DXImpl client = Client3dExperienceFactory.newInstance();
            client.init(loginMap);

            UserSession newSession =
                new UserSession(client, System.currentTimeMillis());

            sessions.put(userKey, newSession);

            return client;
        }
    }

    private static boolean isExpired(UserSession session) {
        return System.currentTimeMillis() - session.getLastLoginTime() > SESSION_TTL;
    }
}
