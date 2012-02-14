/**
 * 
 */
package nl.topicuszorg.wicket.jquerysocket.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

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

	/** Async contexts */
	private final Map<String, AsyncContext> asyncContexts = new ConcurrentHashMap<String, AsyncContext>();

	/** Disconnect event listeners */
	private final Map<String, DisconnectEventListener> disconnectListeners = new ConcurrentHashMap<String, DisconnectEventListener>();

	/** For each client a timestamp of when it was last seen */
	private final Map<String, Date> disconnectListenerDate = new ConcurrentHashMap<String, Date>();

	/**
	 * Messages to send
	 */
	private final BlockingQueue<Message> messages = new LinkedBlockingQueue<Message>();

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
						else if (disconnectListeners.containsKey(message.getClientId()))
						{
							LOG.debug("No context found for client " + message.getClientId() + " removing listener");
							disconnectListeners.get(message.getClientId()).onDisconnect();
							disconnectListeners.remove(message.getClientId());
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
					Iterator<String> keyIterator = disconnectListeners.keySet().iterator();
					while (keyIterator.hasNext())
					{
						String clientId = keyIterator.next();
						if (!asyncContexts.containsKey(clientId))
						{
							Date timeAdded = disconnectListenerDate.get(clientId);
							// De client wel de tijd gunnen om te connecten
							if ((new Date().getTime() - timeAdded.getTime()) < (30 * 1000))
							{
								LOG.debug("client " + clientId + " disconnected");
								disconnectListeners.get(clientId).onDisconnect();
								disconnectListenerDate.remove(clientId);
								keyIterator.remove();
							}
						}
					}
					Thread.sleep(60 * 1000);
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

				sendStatus(ac.getResponse().getWriter(), clientid, "open");
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
	 * Internal message structure
	 * 
	 * @author sven
	 * 
	 */
	private class Message implements Serializable
	{
		/** */
		private static final long serialVersionUID = 1L;

		/** Message */
		private JSON json;

		/** Client id */
		private String clientId;

		/**
		 * @return the json
		 */
		public JSON getJson()
		{
			return json;
		}

		/**
		 * @param json
		 *            the json to set
		 */
		public void setJson(JSON json)
		{
			this.json = json;
		}

		/**
		 * @return the clientId
		 */
		public String getClientId()
		{
			return clientId;
		}

		/**
		 * @param clientId
		 *            the clientId to set
		 */
		public void setClientId(String clientId)
		{
			this.clientId = clientId;
		}

		/**
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((clientId == null) ? 0 : clientId.hashCode());
			result = prime * result + ((json == null) ? 0 : json.hashCode());
			return result;
		}

		/**
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
			{
				return true;
			}
			if (obj == null)
			{
				return false;
			}
			if (!(obj instanceof Message))
			{
				return false;
			}
			Message other = (Message) obj;
			if (!getOuterType().equals(other.getOuterType()))
			{
				return false;
			}
			if (clientId == null)
			{
				if (other.clientId != null)
				{
					return false;
				}
			}
			else if (!clientId.equals(other.clientId))
			{
				return false;
			}
			if (json == null)
			{
				if (other.json != null)
				{
					return false;
				}
			}
			else if (!json.equals(other.json))
			{
				return false;
			}
			return true;
		}

		/**
		 * 
		 * @return
		 */
		private StreamServlet getOuterType()
		{
			return StreamServlet.this;
		}
	}
}