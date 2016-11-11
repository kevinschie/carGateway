package de.kevinschie.carGateway;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.kevinschie.JSONReader.JSONReader;

public class Gateway implements ConfigurableComponent, CloudClientListener  
{	
	private static final Logger s_logger = LoggerFactory.getLogger(Gateway.class);
	
	// Cloud Application identifier
	private static final String APP_ID = "carGateway";
	
	private static final String   PUBLISH_RATE_PROP_NAME   = "publish.rate";
	private static final String   PUBLISH_QOS_PROP_NAME    = "publish.qos";
	private static final String   PUBLISH_RETAIN_PROP_NAME = "publish.retain";
	
	private CloudService                m_cloudService;
	private CloudClient      			m_cloudClient;
	
	private ScheduledExecutorService    m_worker;
	private ScheduledFuture<?>          m_handle;
	
	private Map<String, Object>         m_properties;
	
	private JSONReader					m_jsonReader;
	private ArrayList<JSONObject>		m_carMessages;
	
	// ----------------------------------------------------------------
	//
	//   Dependencies
	//
	// ----------------------------------------------------------------
	
	public Gateway() 
	{
		super();
		m_worker = Executors.newSingleThreadScheduledExecutor();
		m_jsonReader = new JSONReader();
	}

	public void setCloudService(CloudService cloudService) {
		m_cloudService = cloudService;
	}

	public void unsetCloudService(CloudService cloudService) {
		m_cloudService = null;
	}
	
		
	// ----------------------------------------------------------------
	//
	//   Activation APIs
	//
	// ----------------------------------------------------------------

	protected void activate(ComponentContext componentContext, Map<String,Object> properties) 
	{
		s_logger.info("Activating Gateway...");
		
		m_properties = properties;
		for (String s : properties.keySet()) {
			s_logger.info("Activate - "+s+": "+properties.get(s));
		}
		
		// get the mqtt client for this application
		try  {
			
			// Acquire a Cloud Application Client for this Application 
			s_logger.info("Getting CloudClient for {}...", APP_ID);
			m_cloudClient = m_cloudService.newCloudClient(APP_ID);
			m_cloudClient.addCloudClientListener(this);
			
			// Don't subscribe because these are handled by the default 
			// subscriptions and we don't want to get messages twice
			doUpdate(false);
			m_carMessages = m_jsonReader.ReadJSON();
		}
		catch (Exception e) {
			s_logger.error("Error during component activation", e);
			throw new ComponentException(e);
		}
		s_logger.info("Activating Gateway... Done.");
	}
	
	
	protected void deactivate(ComponentContext componentContext) 
	{
		s_logger.debug("Deactivating Gateway...");

		// shutting down the worker and cleaning up the properties
		m_worker.shutdown();
		
		// Releasing the CloudApplicationClient
		s_logger.info("Releasing CloudApplicationClient for {}...", APP_ID);
		m_cloudClient.release();

		s_logger.debug("Deactivating Gateway... Done.");
	}	
	
	
	public void updated(Map<String,Object> properties)
	{
		s_logger.info("Updated Gateway...");

		// store the properties received
		m_properties = properties;
		for (String s : properties.keySet()) {
			s_logger.info("Update - "+s+": "+properties.get(s));
		}
		
		// try to kick off a new job
		doUpdate(true);
		s_logger.info("Updated Gateway... Done.");
	}
	
	
	
	// ----------------------------------------------------------------
	//
	//   Cloud Application Callback Methods
	//
	// ----------------------------------------------------------------
	
	@Override
	public void onControlMessageArrived(String deviceId, String appTopic,
			KuraPayload msg, int qos, boolean retain) {
		s_logger.info("Got control message from device: {} to topic: {}!", deviceId, appTopic);
	}

	@Override
	public void onMessageArrived(String deviceId, String appTopic,
			KuraPayload msg, int qos, boolean retain) {
		s_logger.info("Got message from device: {} to topic: {}!", deviceId, appTopic);
	}

	@Override
	public void onConnectionLost() {
		s_logger.info("Lost connection!");
	}

	@Override
	public void onConnectionEstablished() {
		s_logger.info("Connection established!");
	}

	@Override
	public void onMessageConfirmed(int messageId, String appTopic) {
		s_logger.info("Message {} to {} confirmed!", messageId, appTopic);
	}

	@Override
	public void onMessagePublished(int messageId, String appTopic) {
		s_logger.info("Message {} published to {}!", messageId, appTopic);
	}
	
	// ----------------------------------------------------------------
	//
	//   Private Methods
	//
	// ----------------------------------------------------------------

	/**
	 * Called after a new set of properties has been configured on the service
	 */
	private void doUpdate(boolean onUpdate) 
	{
		// cancel a current worker handle if one if active
		if (m_handle != null) {
			m_handle.cancel(true);
		}
		
		// schedule a new worker based on the properties of the service
		int pubrate = (Integer) m_properties.get(PUBLISH_RATE_PROP_NAME);
		m_handle = m_worker.scheduleAtFixedRate(new Runnable() {		
			@Override
			public void run() {
				Thread.currentThread().setName(getClass().getSimpleName());
				doPublish();
			}
		}, 0, pubrate, TimeUnit.SECONDS);
	}
	
	
	/**
	 * Called at the configured rate to publish the next temperature measurement.
	 */
	private void doPublish() 
	{				
		// fetch the publishing configuration from the publishing properties
		String  topic  = "";
		Integer qos    = (Integer) m_properties.get(PUBLISH_QOS_PROP_NAME);
		Boolean retain = (Boolean) m_properties.get(PUBLISH_RETAIN_PROP_NAME);
				
		// Allocate a new payload
		KuraPayload payload = new KuraPayload();
		
		// Timestamp the message
		payload.setTimestamp(new Date());
		
		try {
			if(m_cloudClient.isConnected())
			{
				int i = 0;
				for (JSONObject jsonObject : m_carMessages) {
					topic = jsonObject.get("name").toString();
					String s = "{\"name\":\""+jsonObject.get("name")+"\",\"value\":"+jsonObject.get("value")+",\"timestamp\":"+jsonObject.get("timestamp")+"}";
					payload.addMetric("data"+i, s);
					if(jsonObject.get("name").toString().equals("engine_speed") && jsonObject.getInt("value") > 772)
					{
						qos = 2;
						s_logger.info("Important value: QoS 2");
					}
					m_cloudClient.publish(topic, payload, qos, retain);
					System.out.println("Sent message: '" + s + "'");
					s_logger.info("Published to {} message: {}", topic, payload);
					i++;
				}
			}
			else
			{
				s_logger.info("Cannot publish topic: "+topic);
				System.out.println("Cannot publish topic: "+topic);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (Exception ex) {
			s_logger.error("Cannot publish topic: "+topic, ex);
			System.out.println("Cannot publish topic: "+topic);
		}
	}
}

