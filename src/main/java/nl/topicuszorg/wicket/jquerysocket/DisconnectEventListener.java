/**
 * 
 */
package nl.topicuszorg.wicket.jquerysocket;

/**
 * If you want to be informed when a client disconnects, implement this eventlistener.
 * 
 * 
 * @author Sven Rienstra
 * @see {@link JQuerySocketBehavior#JQueryStreamBehavior(DisconnectEventListener)}
 */
public interface DisconnectEventListener
{
	/** On disconnect */
	void onDisconnect();
}
