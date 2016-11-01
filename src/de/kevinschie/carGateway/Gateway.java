package de.kevinschie.carGateway;

import java.io.File;
import java.net.URL;
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
import de.kevinschie.SimulatorListener.SimulationListener;

public class Gateway implements ConfigurableComponent, CloudClientListener  
{	
	private static final Logger s_logger = LoggerFactory.getLogger(Gateway.class);
	
	// Cloud Application identifier
	private static final String APP_ID = "carGateway";
	
	private static final String	  JSON_FILE_DIRECTORY	   = new File("src/data/downtown-crosstown.json").getAbsolutePath();;//"json.directory";

	private static final String   PUBLISH_RATE_PROP_NAME   = "publish.rate";
	private static final String   PUBLISH_TOPIC_PROP_NAME  = "publish.semanticTopic";
	private static final String   PUBLISH_QOS_PROP_NAME    = "publish.qos";
	private static final String   PUBLISH_RETAIN_PROP_NAME = "publish.retain";
	
	private CloudService                m_cloudService;
	private CloudClient      			m_cloudClient;
	
	private ScheduledExecutorService    m_worker;
	private ScheduledFuture<?>          m_handle;
	
	private Map<String, Object>         m_properties;
	
	private JSONReader					m_jsonReader;
	//private SimulationListener			m_simListener;
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
		//m_simListener = new SimulationListener();
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
			//m_carMessages = m_jsonReader.ReadJSON(new File((String) m_properties.get(JSON_FILE_DIRECTORY)), "UTF-8");
			m_carMessages = m_jsonReader.ReadJSON();
			//m_simListener.listenToSimulator();
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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessageArrived(String deviceId, String appTopic,
			KuraPayload msg, int qos, boolean retain) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnectionLost() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnectionEstablished() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessageConfirmed(int messageId, String appTopic) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessagePublished(int messageId, String appTopic) {
		// TODO Auto-generated method stub
		
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
		String  topic  = (String) m_properties.get(PUBLISH_TOPIC_PROP_NAME);
		Integer qos    = (Integer) m_properties.get(PUBLISH_QOS_PROP_NAME);
		Boolean retain = (Boolean) m_properties.get(PUBLISH_RETAIN_PROP_NAME);
				
		// Allocate a new payload
		KuraPayload payload = new KuraPayload();
		
		// Timestamp the message
		payload.setTimestamp(new Date());
		
		// Add the temperature as a metric to the payload
		

		try {
			int i = 0;
			for (JSONObject jsonObject : m_carMessages) {
				String s = "{\"name\":\""+jsonObject.get("name")+"\",\"value\":"+jsonObject.get("value")+",\"timestamp\":"+jsonObject.get("timestamp")+"}";
				payload.addMetric("data"+i, s);
				i++;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		//payload.addMetric("data", m_carMessages);
		
		// Publish the message
		try {
			m_cloudClient.publish(topic, payload, qos, retain);
			s_logger.info("Published to {} message: {}", topic, payload);
		} 
		catch (Exception e) {
			s_logger.error("Cannot publish topic: "+topic, e);
		}
	}
}

