package ClientImplementation;

import org.slf4j.LoggerFactory;

public abstract class Client3dExperienceFactory {
	public static Client3DXImpl newInstance() throws Exception {
		return Client3dExperienceFactory.newInstance(LoggerFactory.getLogger(Client3DXImpl.class));
	}

	public static Client3DXImpl newInstance(final org.slf4j.Logger _logger) throws Exception {
		return new Client3DXImpl(_logger);
	}
}
