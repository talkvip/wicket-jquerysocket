/**
 * 
 */
package nl.topicuszorg.wicket.jquerystream;

import java.util.Map;
import java.util.UUID;

import nl.topicuszorg.wicket.jquerystream.js.StreamResourceReference;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.util.collections.MiniMap;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.odlabs.wiquery.core.behavior.WiQueryAbstractAjaxBehavior;
import org.odlabs.wiquery.core.javascript.JsStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author sven
 * 
 */
public abstract class JQueryStreamBehavior extends WiQueryAbstractAjaxBehavior
{
	/** */
	private static final long serialVersionUID = 1L;

	/** Default logger */
	private static final Logger LOG = LoggerFactory.getLogger(JQueryStreamBehavior.class);

	/** Client id */
	private final String clientid;

	/** Callback to this behavior */
	private String callBack;

	/**
	 * Constructor
	 */
	public JQueryStreamBehavior()
	{
		clientid = UUID.randomUUID().toString();
	}

	/**
	 * 
	 * @param eventListener
	 *            disconnect event listener
	 */
	public JQueryStreamBehavior(DisconnectEventListener eventListener)
	{
		this();
		JQueryStreamService.addDisconnectEventListener(clientid, eventListener);
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
	 * Zorg ervoor dat de client een callback maakt naar deze behavior
	 */
	@Deprecated
	public void triggerUpdate()
	{
		pushJavaScript(callBack);
	}

	/**
	 * Push javascript naar de client
	 * 
	 * @param javaScript
	 */
	@Deprecated
	public void pushJavaScript(CharSequence javaScript)
	{
		JQueryStreamService.sendMessage(clientid, javaScript.toString());
	}

	/**
	 * @see org.odlabs.wiquery.core.behavior.WiQueryAbstractAjaxBehavior#statement()
	 */
	@Override
	public JsStatement statement()
	{
		Map<String, Object> vars = new MiniMap<String, Object>(3);
		vars.put("clientid", clientid);
		vars.put("url", JQueryStreamService.getServletUrl());
		vars.put("debug", LOG.isDebugEnabled());

		return new JsStatement().append(new PackageTextTemplate(JQueryStreamBehavior.class,
			"js/JQueryStreamBehavior.js").asString(vars));
	}

	/**
	 * @see org.apache.wicket.ajax.AbstractDefaultAjaxBehavior#renderHead(org.apache.wicket.Component,
	 *      org.apache.wicket.markup.html.IHeaderResponse)
	 */
	@Override
	public void renderHead(Component component, IHeaderResponse response)
	{
		response.renderJavaScriptReference(StreamResourceReference.get());
		super.renderHead(component, response);
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
			IPushUpdateEvent pushUpdateEvent = (IPushUpdateEvent) event;
			if (StringUtils.isBlank(pushUpdateEvent.getClientId()) || pushUpdateEvent.getClientId().equals(clientid))
			{
				JQueryStreamService.sendMessage(clientid, callBack);
			}
		}
		else if (event.getPayload() instanceof IPushJavaScriptEvent)
		{
			IPushJavaScriptEvent pushJavaScriptEvent = (IPushJavaScriptEvent) event;
			if (StringUtils.isBlank(pushJavaScriptEvent.getClientId())
				|| pushJavaScriptEvent.getClientId().equals(clientid))
			{
				JQueryStreamService.sendMessage(clientid, pushJavaScriptEvent.getJavaScript());
			}
		}
	}
}
