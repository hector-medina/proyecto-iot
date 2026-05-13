package componentes;


import org.eclipse.paho.client.mqttv3.MqttMessage;
import utils.MySimpleLogger;

public class SmartCar_RoadInfoSubscriber extends MyMqttClient {

	protected SmartCar theSmartCar;
	
	public SmartCar_RoadInfoSubscriber(String clientId, SmartCar smartcar, String MQTTBrokerURL) {
		super(clientId, smartcar, MQTTBrokerURL);
		this.smartcar = smartcar;
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		
		super.messageArrived(topic, message);			// esto muestra el mensaje por pantalla ... comentar para no verlo
		String payload = new String(message.getPayload());
		
		// PROCESS THE MESSGE
		// topic - contains the topic
		// payload - contains the message

	}

	

}
