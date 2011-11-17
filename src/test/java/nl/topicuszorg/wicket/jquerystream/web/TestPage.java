package nl.topicuszorg.wicket.jquerystream.web;

import java.util.Date;

import nl.topicuszorg.wicket.jquerystream.JQueryStreamBehavior;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;

/**
 * @author schulten
 */
public class TestPage extends WebPage
{
	/** Default */
	private static final long serialVersionUID = 1L;

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

		final JQueryStreamBehavior streamBehavior = new JQueryStreamBehavior()
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
				streamBehavior.triggerUpdate();
			}
		});
	}
}
