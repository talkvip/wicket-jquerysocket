/**
 * 
 */
package nl.topicuszorg.wicket.jquerysocket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import nl.topicuszorg.wicket.jquerysocket.events.IPushJavaScriptEvent;
import nl.topicuszorg.wicket.jquerysocket.events.IPushUpdateEvent;
import nl.topicuszorg.wicket.jquerysocket.js.SocketResourceReference;
import nl.topicuszorg.wicket.jquerysocket.servlet.StreamServlet;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.util.collections.MiniMap;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * There are two ways to use this behaviour. You can either push JavaScript to client which will be executed or you can
 * trigger a callback from the client to this behaviour, {@link #respond(AjaxRequestTarget target)} will be called.
 * </P>
 * 
 * <p>
 * In both cases you will need to add this behaviour to your page or panel, also you will need to add the
 * {@link StreamServlet} to you web.xml.
 * </p>
 * 
 * <p>
 * If you want to push JavaScript to the client implement the {@link IPushJavaScriptEvent} interface and send a Wicket
 * event using {@link Component#send(org.apache.wicket.event.IEventSink, org.apache.wicket.event.Broadcast, Object)}
 * with the implementation of the {@link IPushJavaScriptEvent} as payload.
 * <p>
 * 
 * <p>
 * If you want to trigger a callback to this behaviour implement the {@link IPushUpdateEvent} interface and send a
 * Wicket event using
 * {@link Component#send(org.apache.wicket.event.IEventSink, org.apache.wicket.event.Broadcast, Object)} with the
 * implementation of the {@link IPushUpdateEvent} as payload.
 * <p>
 * 
 * @author Sven Rienstra
 * @author Dries schulten
 */
public abstract class JQuerySocketBehavior extends AbstractDefaultAjaxBehavior
{
	/** */
	private static final long serialVersionUID = 1L;

	/** Default logger */
	private static final Logger LOG = LoggerFactory.getLogger(JQuerySocketBehavior.class);

	/** Client id */
	private final String clientid;

	/** Callback to this behavior */
	private String callBack;

	/** Transports, default ws and stream */
	private List<Transport> transports = new ArrayList<Transport>(Arrays.asList(Transport.WS, Transport.STREAM));

	/**
	 * Constructor
	 */
	public JQuerySocketBehavior()
	{
		clientid = UUID.randomUUID().toString();
	}

	/**
	 * 
	 * @param eventListener
	 *            disconnect event listener
	 * @see DisconnectEventListener
	 */
	public JQuerySocketBehavior(DisconnectEventListener eventListener)
	{
		this();
		JQuerySocketService.addDisconnectEventListener(clientid, eventListener);
	}

	/**
	 * @return the transports
	 */
	public List<Transport> getTransports()
	{
		return transports;
	}

	/**
	 * @param transports
	 *            the transports to set
	 */
	public void setTransports(List<Transport> transports)
	{
		this.transports = transports;
	}

	/**
	 * @see org.apache.wicket.behavior.Behavior#onConfigure(org.apache.wicket.Component)
	 */
	@Override
	public void onConfigure(Component component)
	{
		callBack = getCallbackScript().toString();
		super.onConfigure(component);
	}

	/**
	 * Make a callback to this behaviour
	 * 
	 * @deprecated use wicket event system
	 */
	@Deprecated
	public void triggerUpdate()
	{
		pushJavaScript(callBack);
	}

	/**
	 * Make a callback to this behaviour, only use of you need to make an callback from a thread where no application is
	 * avaible
	 * 
	 */
	public void callbackBehavior()
	{
		JQuerySocketService.sendMessage(clientid, callBack.toString());
	}

	/**
	 * Push JavaScript to client
	 * 
	 * @param javaScript
	 */
	public void pushJavaScript(CharSequence javaScript)
	{
		JQuerySocketService.sendMessage(clientid, javaScript.toString());
	}

	/**
	 * @see org.apache.wicket.ajax.AbstractDefaultAjaxBehavior#renderHead(org.apache.wicket.Component,
	 *      org.apache.wicket.markup.html.IHeaderResponse)
	 */
	@Override
	public void renderHead(Component component, IHeaderResponse response)
	{
		response.renderJavaScriptReference(SocketResourceReference.get());

		Map<String, Object> vars = new MiniMap<String, Object>(4);
		vars.put("clientid", clientid);
		vars.put("url", JQuerySocketService.getServletUrl());
		vars.put("debug", LOG.isDebugEnabled());
		vars.put("transports", getTransportsString());

		response.renderJavaScript(
			new PackageTextTemplate(JQuerySocketBehavior.class, "js/JQuerySocketBehavior.js").asString(vars), "jqs-"
				+ clientid);

		super.renderHead(component, response);
	}

	/**
	 * @return
	 */
	private StringBuilder getTransportsString()
	{
		StringBuilder transportsString = new StringBuilder();
		boolean first = true;
		for (Transport transport : this.transports)
		{
			if (!first)
			{
				transportsString.append(",");
			}
			else
			{
				first = false;
			}

			transportsString.append("\"");
			transportsString.append(transport.name().toLowerCase());
			transportsString.append("\"");
		}
		return transportsString;
	}

	/**
	 * @return the clientid
	 */
	public String getClientid()
	{
		return clientid;
	}

	/**
	 * @see org.apache.wicket.behavior.Behavior#onEvent(org.apache.wicket.Component, org.apache.wicket.event.IEvent)
	 */
	@Override
	public void onEvent(Component component, IEvent<?> event)
	{
		if (event.getPayload() instanceof IPushUpdateEvent)
		{
			IPushUpdateEvent pushUpdateEvent = (IPushUpdateEvent) event.getPayload();
			if (StringUtils.isBlank(pushUpdateEvent.getClientId()) || pushUpdateEvent.getClientId().equals(clientid))
			{
				JQuerySocketService.sendMessage(clientid, callBack);
			}
		}
		else if (event.getPayload() instanceof IPushJavaScriptEvent)
		{
			IPushJavaScriptEvent pushJavaScriptEvent = (IPushJavaScriptEvent) event.getPayload();
			if (StringUtils.isBlank(pushJavaScriptEvent.getClientId())
				|| pushJavaScriptEvent.getClientId().equals(clientid))
			{
				JQuerySocketService.sendMessage(clientid, pushJavaScriptEvent.getJavaScript());
			}
		}
	}
}
