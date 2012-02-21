/**
 * 
 */
package nl.topicuszorg.wicket.jquerysocket.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSON;
import net.sf.json.JSONObject;
import nl.topicuszorg.wicket.jquerysocket.DisconnectEventListener;
import nl.topicuszorg.wicket.jquerysocket.IStreamMessageDestination;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet to push messages to clients
 * 
 * 
 * @author Sven Rienstra
 * @author Dries Schulten
 * 
 */
public class StreamServlet extends WebSocketServlet implements IStreamMessageDestination
{
	/** */
	private static final long serialVersionUID = 1L;

	/** Allowed transports (WebSocket is excluded, connects in a different way) */
	private static final List<String> ALLOWED_TRANSPORTS = Arrays.asList("streamiframe", "streamxdr", "streamxhr",
		"sse");

	/** Default logger */
	private static final Logger LOG = LoggerFactory.getLogger(StreamServlet.class);

	/** Stream servlet attribute */
	public static final String STREAM_SERVLET_ATTRIBUTE = "streamservlet";

	/** Maximum resend count */
	private static final int MAX_RESEND_COUNT = 5;

	/** Map of currently known connections */
	private final Map<String, AbstractConnection> connections = new ConcurrentHashMap<String, AbstractConnection>();

	/**
	 * Messages to send
	 */
	private final DelayQueue<Message> messages = new DelayQueue<Message>();

	/**
	 * Notifier thread
	 */
	private final Thread notifier = new Thread(new Runnable()
	{
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
					Message message = messages.take();

					// Send to all clients
					if (message.getClientId() == null)
					{
						LOG.debug("Send message to all clients");
						for (AbstractConnection connection : connections.values())
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
						AbstractConnection connection = connections.get(message.getClientId());

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
							messages.add(message);
						}

					}
				}
				catch (InterruptedException e)
				{
					break;
				}
			}
		}
	});

	/**
	 * Thread to find disconnected clients
	 */
	private final Thread disconnectThread = new Thread(new Runnable()
	{
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

					Iterator<AbstractConnection> iterator = connections.values().iterator();
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

	});

	/**
	 * Constructor for debugging
	 */
	public StreamServlet()
	{

	}

	/**
	 * Handle a forced disconnect i.e. connection was no longer available while sending a message
	 * 
	 * @param connection
	 *            the {@link AbstractConnection}
	 */
	private void doDisconnectNoConnection(AbstractConnection connection)
	{
		if (connection.getDisconnectEventListener() != null)
		{
			connection.getDisconnectEventListener().onDisconnect();
		}

		connections.remove(connection.getClientId());
	}

	/**
	 * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
	 */
	@Override
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
		notifier.start();
		disconnectThread.start();

		getServletContext().setAttribute(STREAM_SERVLET_ATTRIBUTE, this);
	}

	/**
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
		IOException
	{
		response.setHeader("Access-Control-Allow-Origin", "*");

		if (StringUtils.isNotBlank(request.getParameter("data")))
		{
			JSONObject json = JSONObject.fromObject(request.getParameter("data"));
			String clientid = json.getString("socket");

			AbstractConnection connection = connections.get(clientid);

			if (connection == null)
			{
				LOG.error("No connection found for client" + clientid + " on heartbeat post");
			}
			else
			{
				connection.setLastSeen(new Date());
			}

			if ("heartbeat".equals(json.getString("type")))
			{
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("type", "heartbeat");

				Message message = new Message();
				message.setClientId(clientid);
				message.setJson(JSONObject.fromObject(map));
				messages.add(message);
			}
		}
	}

	/**
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	// GET method is used to establish a stream connection
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		// Content-Type header
		response.setContentType("text/plain");
		response.setCharacterEncoding("utf-8");

		// Access-Control-Allow-Origin header
		response.setHeader("Access-Control-Allow-Origin", "*");

		// Transport type
		if (!ALLOWED_TRANSPORTS.contains(request.getParameter("transport")))
		{
			// Unknown transport -> bad request
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
				String.format("Transport %s not supported", request.getParameter("transport")));
		}
		else
		{
			final String clientId = request.getParameter("id");
			LOG.debug("Get request for client " + clientId);

			if (request.isAsyncSupported())
			{
				AsyncContext ac = request.startAsync();

				// Should be 0, but that causes a hang in Jetty on al un-connected connection acceptance threads?
				ac.setTimeout((TimeUnit.MINUTES.toMillis(10L)));

				final StreamConnection connection = new StreamConnection();
				connection.setClientId(clientId);
				connection.setAsyncContext(ac);
				connection.setLastSeen(new Date());

				PrintWriter writer = response.getWriter();
				writer.print(Arrays.toString(new float[400]).replaceAll(".", " ") + "\n");
				writer.flush();

				ac.addListener(new AsyncListener()
				{
					/**
					 * @see javax.servlet.AsyncListener#onComplete(javax.servlet.AsyncEvent)
					 */
					@Override
					public void onComplete(AsyncEvent event) throws IOException
					{
						LOG.debug("client " + clientId + " completed");
						disconnect();
					}

					/**
					 * @see javax.servlet.AsyncListener#onTimeout(javax.servlet.AsyncEvent)
					 */
					@Override
					public void onTimeout(AsyncEvent event) throws IOException
					{
						LOG.debug("client " + clientId + " timed out");
						disconnect();
					}

					/**
					 * @see javax.servlet.AsyncListener#onError(javax.servlet.AsyncEvent)
					 */
					@Override
					public void onError(AsyncEvent event) throws IOException
					{
						LOG.debug("client " + clientId + " got an error");
						disconnect();
					}

					/**
					 * @see javax.servlet.AsyncListener#onStartAsync(javax.servlet.AsyncEvent)
					 */
					@Override
					public void onStartAsync(AsyncEvent event) throws IOException
					{

					}

					/**
					 * Do a normal disconnect and cleanup
					 * 
					 * @throws IOException
					 */
					private void disconnect() throws IOException
					{
						connection.sendStatus("close");
						doDisconnectNoConnection(connection);
					}
				});
				connections.put(clientId, connection);

				connection.sendStatus("open");
			}
		}
	}

	/**
	 * @see javax.servlet.GenericServlet#destroy()
	 */
	@Override
	public void destroy()
	{
		messages.clear();
		connections.clear();
		notifier.interrupt();
		disconnectThread.interrupt();
	}

	/**
	 * @see nl.topicuszorg.wicket.jquerysocket.IStreamMessageDestination#sendMessage(java.lang.String)
	 */
	@Override
	public void sendMessage(String clientid, JSON json)
	{
		LOG.debug("Push message to client " + clientid + ": " + json.toString());

		if (!connections.containsKey(clientid))
		{
			LOG.warn("Trying to send message to specific client, but there's no client connected with clientid "
				+ clientid);
		}

		Message message = new Message();
		message.setClientId(clientid);
		message.setJson(json);
		if (!messages.contains(message))
		{
			messages.add(message);
		}
	}

	/**
	 * @see nl.topicuszorg.wicket.jquerysocket.IStreamMessageDestination#addDisconnectEventListener(java.lang.String,
	 *      nl.topicuszorg.wicket.jquerysocket.DisconnectEventListener)
	 */
	@Override
	public void addDisconnectEventListener(String clientid, DisconnectEventListener eventListener)
	{
		AbstractConnection connection = connections.get(clientid);
		if (connection != null)
		{
			connection.setDisconnectEventListener(eventListener);
		}
		else
		{
			LOG.warn("No connection found voor client id " + clientid);
		}
	}

	/**
	 * @see org.eclipse.jetty.websocket.WebSocketFactory.Acceptor#doWebSocketConnect(javax.servlet.http.HttpServletRequest,
	 *      java.lang.String)
	 */
	@Override
	public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol)
	{
		final String clientId = request.getParameter("id");

		return new WebSocket.OnTextMessage()
		{
			/**
			 * @see org.eclipse.jetty.websocket.WebSocket#onOpen(org.eclipse.jetty.websocket.WebSocket.Connection)
			 */
			@Override
			public void onOpen(org.eclipse.jetty.websocket.WebSocket.Connection connection)
			{
				WebSocketConnection wsConn = new WebSocketConnection();
				wsConn.setClientId(clientId);
				wsConn.setSocket(connection);
				wsConn.setLastSeen(new Date());

				try
				{
					wsConn.sendStatus("open");
				}
				catch (IOException e)
				{
					LOG.error("Failed sending 'open' status to WebSocket on client " + clientId, e);
				}

				connections.put(clientId, wsConn);
			}

			/**
			 * @see org.eclipse.jetty.websocket.WebSocket#onClose(int, java.lang.String)
			 */
			@Override
			public void onClose(int closeCode, String message)
			{

			}

			/**
			 * @see org.eclipse.jetty.websocket.WebSocket.OnTextMessage#onMessage(java.lang.String)
			 */
			@Override
			public void onMessage(String data)
			{

			}
		};
	}

	/**
	 * @return current nanosecond time.
	 */
	protected static final long now()
	{
		return System.nanoTime();
	}
}
