/**
 * 
 */
package nl.topicuszorg.wicket.jquerysocket.events;

/**
 * Event to trigger a callback to the JQueryStreamBehavior
 * 
 * @author Sven Rienstra
 * 
 */
public interface IPushUpdateEvent
{
	/**
	 * Optional client id, if you want to trigger an update on a specific client, return the client id here
	 * If you want to trigger an update on any client, return null.
	 * @return
	 */
	String getClientId();
}
