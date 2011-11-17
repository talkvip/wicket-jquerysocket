/**
 * 
 */
package nl.topicuszorg.wicket.jquerystream.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
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
import nl.topicuszorg.wicket.jquerystream.DisconnectEventListener;
import nl.topicuszorg.wicket.jquerystream.StreamMessageDestination;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author sven
 * 
 */
public class StreamServlet extends HttpServlet implements StreamMessageDestination
{
	/** */
	private static final long serialVersionUID = 1L;

	/** Default logger */
	private static final Logger LOG = LoggerFactory.getLogger(StreamServlet.class);

	/** Stream servlet attribute */
	public static final String STREAM_SERVLET_ATTRIBUTE = "streamservlet";

	/**
	 * Async contexts
	 */
	private final Map<String, AsyncContext> asyncContexts = new ConcurrentHashMap<String, AsyncContext>();

	/** Disconnect event listeners */
	private final Map<String, DisconnectEventListener> disconnectListeners = new ConcurrentHashMap<String, DisconnectEventListener>();

	/** Per disconnect listener tijd van toevoegen */
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
				asyncContexts.values().remove(asyncContext);
			}
		}
	});

	/**
	 * Zoek naar gedisconnecte clients
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
		// default message format is message-size ; message-data ;
		writer.print(message.length());
		writer.print(";");
		writer.print(message);
		writer.print(";");
		writer.flush();
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

		String clientid = request.getParameter("clientid");
		LOG.debug("Get request for client " + clientid);

		PrintWriter writer = response.getWriter();

		// Id
		final String id = clientid;
		writer.print(id);
		writer.print(';');

		// Padding
		for (int i = 0; i < 1024; i++)
		{
			writer.print(' ');
		}
		writer.print(';');
		writer.flush();

		final AsyncContext ac = request.startAsync();
		ac.addListener(new AsyncListener()
		{
			/**
			 * @see javax.servlet.AsyncListener#onComplete(javax.servlet.AsyncEvent)
			 */
			@Override
			public void onComplete(AsyncEvent event) throws IOException
			{
				LOG.debug("client " + id + " completed");
				asyncContexts.remove(id);
			}

			/**
			 * @see javax.servlet.AsyncListener#onTimeout(javax.servlet.AsyncEvent)
			 */
			@Override
			public void onTimeout(AsyncEvent event) throws IOException
			{
				LOG.debug("client " + id + " timed out");
				asyncContexts.remove(id);
			}

			/**
			 * @see javax.servlet.AsyncListener#onError(javax.servlet.AsyncEvent)
			 */
			@Override
			public void onError(AsyncEvent event) throws IOException
			{
				LOG.debug("client " + id + " got an error");
				asyncContexts.remove(id);
			}

			/**
			 * @see javax.servlet.AsyncListener#onStartAsync(javax.servlet.AsyncEvent)
			 */
			@Override
			public void onStartAsync(AsyncEvent event) throws IOException
			{

			}
		});
		asyncContexts.put(id, ac);

		writer.close();
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
	 * @see nl.topicuszorg.wicket.jquerystream.StreamMessageDestination#sendMessage(java.lang.String)
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
	 * @see nl.topicuszorg.wicket.jquerystream.StreamMessageDestination#addDisconnectEventListener(java.lang.String,
	 *      nl.topicuszorg.wicket.jquerystream.DisconnectEventListener)
	 */
	@Override
	public void addDisconnectEventListener(String clientid, DisconnectEventListener eventListener)
	{
		disconnectListeners.put(clientid, eventListener);
		disconnectListenerDate.put(clientid, new Date());
	}

	/**
	 * Het message
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
