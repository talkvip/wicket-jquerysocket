package nl.topicuszorg.wicket.jquerysocket.web;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import nl.topicuszorg.wicket.jquerysocket.JQuerySocketBehavior;
import nl.topicuszorg.wicket.jquerysocket.events.IPushJavaScriptEvent;
import nl.topicuszorg.wicket.jquerysocket.events.IPushUpdateEvent;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.odlabs.wiquery.core.javascript.JsQuery;
import org.odlabs.wiquery.core.javascript.JsUtils;

/**
 * @author schulten
 */
public class TestPage extends WebPage
{
	/** Default */
	private static final long serialVersionUID = 1L;

	/** */
	private static AtomicInteger counter = new AtomicInteger();

	/** */
	public TestPage()
	{
		final Label datumLabel = new Label("date", new AbstractReadOnlyModel<String>()
		{
			/** Default */
			private static final long serialVersionUID = 1L;

			/** */
			@Override
			public String getObject()
			{
				return new Date().toString();
			}
		});
		datumLabel.setOutputMarkupId(true);
		add(datumLabel);

		final JQuerySocketBehavior streamBehavior = new JQuerySocketBehavior()
		{
			/** Default */
			private static final long serialVersionUID = 1L;

			/** */
			@Override
			protected void respond(AjaxRequestTarget target)
			{
				target.add(datumLabel);
				target.appendJavaScript("alert('hello world');");
			}
		};
		add(streamBehavior);

		add(new AjaxLink<Void>("link")
		{
			/** Default */
			private static final long serialVersionUID = 1L;

			/** */
			@Override
			public void onClick(AjaxRequestTarget target)
			{
				send(getPage(), Broadcast.BUBBLE, new UpdateEvent());
			}
		});

		add(new AjaxLink<Void>("link2")
		{
			/** Default */
			private static final long serialVersionUID = 1L;

			/** */
			@Override
			public void onClick(AjaxRequestTarget target)
			{
				counter.addAndGet(1);
				send(getPage(), Broadcast.BUBBLE, new JsEvent());
			}
		});
	}

	/**
	 * @author schulten
	 */
	private static class UpdateEvent implements IPushUpdateEvent
	{
		/**
		 * @see nl.topicuszorg.wicket.jquerysocket.events.IPushUpdateEvent#getClientId()
		 */
		@Override
		public String getClientId()
		{
			// No specific id
			return null;
		}
	}

	/**
	 * @author schulten
	 */
	private static class JsEvent implements IPushJavaScriptEvent
	{
		/**
		 * @see nl.topicuszorg.wicket.jquerysocket.events.IPushJavaScriptEvent#getJavaScript()
		 */
		@Override
		public String getJavaScript()
		{
			return new JsQuery().$("#pushed")
				.chain("append", JsUtils.quotes(String.format("%s<br/>", counter.get()))).render().toString();
		}

		/**
		 * @see nl.topicuszorg.wicket.jquerysocket.events.IPushJavaScriptEvent#getClientId()
		 */
		@Override
		public String getClientId()
		{
			// No specific client
			return null;
		}
	}
}
