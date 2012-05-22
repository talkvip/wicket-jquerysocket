package nl.topicuszorg.wicket.jquerysocket.thread;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import nl.topicuszorg.wicket.jquerysocket.servlet.AbstractConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread to find disconnected clients
 * 
 * @author Remco Zigterman
 */
public abstract class DisconnectThread extends Thread
{
	/** Default logger */
	private static final Logger LOG = LoggerFactory.getLogger(DisconnectThread.class);

	/**
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run()
	{
		while (true)
		{
			try
			{
				LOG.debug("Disconnect thread working");

				Iterator<AbstractConnection> iterator = getConnections().values().iterator();
				while (iterator.hasNext())
				{
					AbstractConnection connection = iterator.next();

					// The client needs some time to connect
					if ((new Date().getTime() - connection.getLastSeen().getTime()) > (TimeUnit.SECONDS
						.toMillis(30)))
					{
						LOG.debug("client " + connection.getClientId() + " disconnected");
						doDisconnectNoConnection(connection);
						iterator.remove();
					}
				}
				Thread.sleep(TimeUnit.SECONDS.toMillis(60));
			}
			catch (InterruptedException e)
			{
				break;
			}
		}
	}

	/**
	 * Returns the map with connections
	 * 
	 * @return Map with connections
	 */
	abstract protected Map<String, AbstractConnection> getConnections();

	/**
	 * Handle a disconnect when we can no longer send a message to the client, meaning a disconnect (almost always)
	 * engaged from the client. A server disconnect wil be able to send 'close' to the client before effectivly closing
	 * the connection.
	 * 
	 * @param connection
	 *            the {@link AbstractConnection}
	 */
	abstract protected void doDisconnectNoConnection(AbstractConnection connection);

}
