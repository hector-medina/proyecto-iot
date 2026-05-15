package componentes;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import utils.MySimpleLogger;

public abstract class MyMqttClient implements MqttCallback {

	protected MqttClient myClient;
	protected String clientId = null;
	protected String brokerURL = null;
	private static String awsIotRootCaPath = null;
	private static String awsIotCertificatePath = null;
	private static String awsIotPrivateKeyPath = null;

	protected SmartCar smartcar = null;

	
	public MyMqttClient(String clientId, SmartCar smartcar, String MQTTBrokerURL) {
		this.clientId = clientId;
		this.smartcar = smartcar;
		this.brokerURL = MQTTBrokerURL;
	}


	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		
		String payload = new String(message.getPayload());
		
		MySimpleLogger.trace(this.clientId, "-------------------------------------------------");
		MySimpleLogger.trace(this.clientId, "| Topic:" + topic);
		MySimpleLogger.trace(this.clientId, "| Message: " + payload);
		MySimpleLogger.trace(this.clientId, "-------------------------------------------------");
	}

	
	
	@Override
	public void connectionLost(Throwable t) {
		MySimpleLogger.warn(this.clientId, "Connection lost!");
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
	}

	public static void configureAwsIotCertificates(String rootCaPath, String certificatePath, String privateKeyPath) {
		awsIotRootCaPath = rootCaPath;
		awsIotCertificatePath = certificatePath;
		awsIotPrivateKeyPath = privateKeyPath;
	}

	public void connect() {
		// setup MQTT Client
		MqttConnectOptions connOpt = new MqttConnectOptions();
		
		connOpt.setCleanSession(true);
		connOpt.setKeepAliveInterval(30);
//			connOpt.setUserName(M2MIO_USERNAME);
//			connOpt.setPassword(M2MIO_PASSWORD_MD5.toCharArray());
		configureAwsIotSslIfNeeded(connOpt);
		
		// Connect to Broker
		try {
			MqttDefaultFilePersistence persistence = null;
			try {
				persistence = new MqttDefaultFilePersistence("/tmp");
			} catch (Exception e) {
			}
			if ( persistence != null )
				myClient = new MqttClient(this.brokerURL, this.clientId, persistence);
			else
				myClient = new MqttClient(this.brokerURL, this.clientId);

			myClient.setCallback(this);
			myClient.connect(connOpt);
		} catch (MqttException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		
		MySimpleLogger.trace(this.clientId, "Client connected to " + this.brokerURL);

	}

	private void configureAwsIotSslIfNeeded(MqttConnectOptions connOpt) {
		if (this.brokerURL == null || !this.brokerURL.startsWith("ssl://")) {
			return;
		}

		String rootCaPath = firstNonEmpty(awsIotRootCaPath, System.getProperty("aws.iot.rootCa"), System.getenv("AWS_IOT_ROOT_CA"));
		String certificatePath = firstNonEmpty(awsIotCertificatePath, System.getProperty("aws.iot.certificate"), System.getenv("AWS_IOT_CERTIFICATE"));
		String privateKeyPath = firstNonEmpty(awsIotPrivateKeyPath, System.getProperty("aws.iot.privateKey"), System.getenv("AWS_IOT_PRIVATE_KEY"));

		if (rootCaPath == null || certificatePath == null || privateKeyPath == null) {
			throw new IllegalStateException("AWS IoT SSL connection requires root CA, certificate and private key paths");
		}

		try {
			connOpt.setSocketFactory(AwsIotSslSocketFactory.create(rootCaPath, certificatePath, privateKeyPath));
			MySimpleLogger.trace(this.clientId, "AWS IoT TLS certificates configured");
		} catch (Exception e) {
			throw new IllegalStateException("Unable to configure AWS IoT TLS certificates", e);
		}
	}

	private static String firstNonEmpty(String... values) {
		for (String value : values) {
			if (value != null && !value.trim().isEmpty()) {
				return value.trim();
			}
		}
		return null;
	}
	
	
	public void disconnect() {
		
		// disconnect
		try {
			// wait to ensure subscribed messages are delivered
			Thread.sleep(120000);

			myClient.disconnect();
			MySimpleLogger.trace(this.clientId, "Client disconnected!");
		} catch (Exception e) {
			e.printStackTrace();
		}

		
	}

	
	public void subscribe(String theTopic) {
		
		// subscribe to topic
		try {
			int subQoS = 0;
			myClient.subscribe(theTopic, subQoS);
			MySimpleLogger.trace(this.clientId, "Client subscribed to the topic " + theTopic);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	

	public void unsubscribe(String theTopic) {
		
		// unsubscribe to topic
		try {
			int subQoS = 0;
			myClient.unsubscribe(theTopic);
			MySimpleLogger.trace(this.clientId, "Client UNsubscribed from the topic " + theTopic);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
