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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSON;
import net.sf.json.JSONObject;
import nl.topicuszorg.wicket.jquerysocket.DisconnectEventListener;
import nl.topicuszorg.wicket.jquerysocket.IStreamMessageDestination;

import org.apache.commons.lang.StringUtils;
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
public class StreamServlet extends HttpServlet implements IStreamMessageDestination
{
	/** */
	private static final long serialVersionUID = 1L;

	/** Allowed transports */
	private static final List<String> ALLOWED_TRANSPORTS = Arrays.asList("streamiframe", "streamxdr", "streamxhr",
		"sse");

	/** Default logger */
	private static final Logger LOG = LoggerFactory.getLogger(StreamServlet.class);

	/** Stream servlet attribute */
	public static final String STREAM_SERVLET_ATTRIBUTE = "streamservlet";

	/** Maximum resend count */
	private static final int MAX_RESEND_COUNT = 5;

	/** Async contexts */
	private final Map<String, AsyncContext> asyncContexts = new ConcurrentHashMap<String, AsyncContext>();

	/** Disconnect event listeners */
	private final Map<String, DisconnectEventListener> disconnectListeners = new ConcurrentHashMap<String, DisconnectEventListener>();

	/** For each client a timestamp of when it was last seen */
	private final Map<String, Date> disconnectListenerDate = new ConcurrentHashMap<String, Date>();

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
						for (AsyncContext asyncContext : asyncContexts.values())
						{
							LOG.debug("Send message to all clients");
							send(asyncContext, message.getJson());
						}
					}
					// Send to specific client
					else
					{
						AsyncContext asyncContext = asyncContexts.get(message.getClientId());

						if (asyncContext != null)
						{
							LOG.debug("Send message to " + message.getClientId());
							send(asyncContext, message.getJson());
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

		/**
		 * Send message
		 */
		private void send(AsyncContext asyncContext, JSON json)
		{
			try
			{
				sendMessage(asyncContext.getResponse().getWriter(), json.toString());
			}
			catch (Exception e)
			{
				LOG.debug("Got an exception while sending message " + json.toString(), e);

				asyncContexts.values().remove(asyncContext);
			}
		}
	});

	/**
	 * Thread to find disconnected clients
	 */
	private final Thread disconnectThread = new Thread(new Runnable()
	{
		@Override
		public void run()
		{
			while (true)
			{
				try
				{
					LOG.debug("Disconnect thread working");

					Iterator<String> keyIterator = disconnectListeners.keySet().iterator();
					while (keyIterator.hasNext())
					{
						String clientId = keyIterator.next();
						if (!asyncContexts.containsKey(clientId))
						{
							Date timeAdded = disconnectListenerDate.get(clientId);
							// The client needs some time to connect
							if ((new Date().getTime() - timeAdded.getTime()) < (TimeUnit.SECONDS.toMillis(30)))
							{
								LOG.debug("client " + clientId + " disconnected");
								disconnectListeners.get(clientId).onDisconnect();
								disconnectListenerDate.remove(clientId);
								keyIterator.remove();
							}
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
	 * Send message
	 * 
	 * @param writer
	 * @param message
	 * @throws IOException
	 */
	private void sendMessage(PrintWriter writer, String message) throws IOException
	{
		writer.print("data: ");
		writer.print(message);
		writer.print("\n");
		writer.flush();
	}

	/**
	 * Send status message
	 * 
	 * @param writer
	 * @param clientid
	 * @param status
	 * @throws IOException
	 */
	private void sendStatus(PrintWriter writer, String clientid, String status) throws IOException
	{
		JSONObject json = new JSONObject();

		json.put("socket", clientid);
		json.put("type", status);

		sendMessage(writer, json.toString());
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
			final String clientid = request.getParameter("id");
			LOG.debug("Get request for client " + clientid);

			if (request.isAsyncSupported())
			{
				final AsyncContext ac = request.startAsync();

				if (StringUtils.isNotBlank(request.getParameter("heartbeat")))
				{
					ac.setTimeout(0L);
				}

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
						LOG.debug("client " + clientid + " completed");
						disconnect();
					}

					/**
					 * @see javax.servlet.AsyncListener#onTimeout(javax.servlet.AsyncEvent)
					 */
					@Override
					public void onTimeout(AsyncEvent event) throws IOException
					{
						LOG.debug("client " + clientid + " timed out");
						disconnect();
					}

					/**
					 * @see javax.servlet.AsyncListener#onError(javax.servlet.AsyncEvent)
					 */
					@Override
					public void onError(AsyncEvent event) throws IOException
					{
						LOG.debug("client " + clientid + " got an error");
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
					 * Do disconnect and cleanup
					 * 
					 * @throws IOException
					 */
					private void disconnect() throws IOException
					{
						sendStatus(ac.getResponse().getWriter(), clientid, "close");
						asyncContexts.remove(clientid);
					}
				});
				asyncContexts.put(clientid, ac);

				// sendStatus(ac.getResponse().getWriter(), clientid, "open");
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
		asyncContexts.clear();
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

		if (!asyncContexts.containsKey(clientid))
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
		disconnectListeners.put(clientid, eventListener);
		disconnectListenerDate.put(clientid, new Date());
	}

	/**
	 * Returns current nanosecond time.
	 */
	static final long now()
	{
		return System.nanoTime();
	}

}
