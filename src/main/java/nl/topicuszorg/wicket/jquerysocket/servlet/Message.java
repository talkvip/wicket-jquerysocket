package nl.topicuszorg.wicket.jquerysocket.servlet;

import java.io.Serializable;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import net.sf.json.JSON;
import nl.topicuszorg.wicket.jquerysocket.thread.NotifierThread;

/**
 * Internal message structure
 * 
 * @author sven
 * 
 */
public class Message implements Serializable, Delayed
{
	/** */
	private static final long serialVersionUID = 1L;

	/** Message */
	private JSON json;

	/** Client id */
	private String clientId;

	/** The time the message has to be send in nanoTime units */
	private Long time;

	/** The number of times the message has been tried to resend */
	private int resendCount;

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
	 * @see java.util.concurrent.Delayed#getDelay(java.util.concurrent.TimeUnit)
	 */
	@Override
	public long getDelay(TimeUnit unit)
	{
		if (time == null)
		{
			return 0;
		}
		return unit.convert(time - NotifierThread.now(), TimeUnit.NANOSECONDS);
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Delayed other)
	{
		if (other == this) // compare zero ONLY if same object
			return 0;
		long d = (getDelay(TimeUnit.NANOSECONDS) - other.getDelay(TimeUnit.NANOSECONDS));
		return (d == 0) ? 0 : ((d < 0) ? -1 : 1);
	}

	/**
	 * Set time
	 * 
	 * @param nanos
	 */
	public void setTime(long nanos)
	{
		time = nanos;
	}

	/**
	 * Up the resend counter
	 */
	public void resend()
	{
		resendCount++;
	}

	/**
	 * @return the resendCount
	 */
	public int getResendCount()
	{
		return resendCount;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
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
}