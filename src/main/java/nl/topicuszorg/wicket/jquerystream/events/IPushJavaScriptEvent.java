package nl.topicuszorg.wicket.jquerystream.events;

/**
 * Event to push JavaScript to a client
 * @author Sven Rienstra
 *
 */
public interface IPushJavaScriptEvent
{
	/**
	 * The JavaScript code with should be pushed to the client
	 * @return
	 */
	String getJavaScript();

	/**
	 * Optional client id, if you want to push the JavaScript to a specific client, return the client id here
	 * If you want this JavaScript to be pushed to any client, return null.
	 * @return
	 */
	String getClientId();
}
