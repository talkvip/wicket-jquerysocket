package nl.topicuszorg.wicket.jquerysocket.thread;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

import nl.topicuszorg.wicket.jquerysocket.servlet.AbstractConnection;
import nl.topicuszorg.wicket.jquerysocket.servlet.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Notifier thread
 * 
 * @author Remco Zigterman
 */
public abstract class NotifierThread extends Thread
{
	/** Default logger */
	private static final Logger LOG = LoggerFactory.getLogger(NotifierThread.class);

	/** Maximum resend count */
	private static final int MAX_RESEND_COUNT = 5;

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
				// Waits until a message arrives
				Message message = getMessages().take();

				// Send to all clients
				if (message.getClientId() == null)
				{
					LOG.debug("Send message to all clients");
					for (AbstractConnection connection : getConnections().values())
					{
						try
						{
							connection.sendMessage(message);
						}
						catch (IOException e)
						{
							LOG.debug(String.format("Got an exception while sending message %s to client %s",
								message.getJson().toString(), connection.getClientId()), e);
							doDisconnectNoConnection(connection);
						}
					}
				}
				// Send to specific client
				else
				{
					AbstractConnection connection = getConnections().get(message.getClientId());

					if (connection != null)
					{
						LOG.debug("Send message to " + connection.getClientId());
						try
						{
							connection.sendMessage(message);
						}
						catch (IOException e)
						{
							LOG.debug(String.format("Got an exception while sending message %s to client %s",
								message.getJson().toString(), connection.getClientId()), e);
							doDisconnectNoConnection(connection);
						}
					}
					else if (message.getResendCount() < MAX_RESEND_COUNT)
					{
						// Try to resend the message, maybe the client hasn't connected yet
						LOG.debug("Trying to resend message to client " + message.getClientId()
							+ " in 250 milliseconds, resend count #" + (message.getResendCount() + 1));
						message.setTime(now() + TimeUnit.MILLISECONDS.toNanos(250));
						message.resend();
						getMessages().add(message);
					}

				}
			}
			catch (InterruptedException e)
			{
				break;
			}
		}
	}

	/**
	 * @return current nanosecond time.
	 */
	public static final long now()
	{
		return System.nanoTime();
	}

	/**
	 * Returns the map with connections
	 * 
	 * @return Map with connections
	 */
	abstract protected Map<String, AbstractConnection> getConnections();

	/**
	 * Returns the queue with messages to send
	 * 
	 * @return Message queue
	 */
	abstract protected DelayQueue<Message> getMessages();

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
