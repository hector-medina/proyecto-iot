package componentes;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.json.JSONException;
import org.json.JSONObject;
import utils.MySimpleLogger;

public class SmartCar_InicidentNotifier extends MyMqttClient {
	
	public SmartCar_InicidentNotifier(String clientId, SmartCar smartcar, String brokerURL) {
		super(clientId, smartcar, brokerURL);
	}
	
	
	public void alert(String smartCarID, String notificationType, RoadPlace place) {

		String myTopic = MqttTopics.roadAlerts(this.smartcar.getTopicBase(), place.getRoad());

		MqttTopic topic = myClient.getTopic(myTopic);


		// publish incident 'basic'
		// TIP: habrá que adaptar este mensaje si queremos conectarlo al servicio de tráfico SmartTraffic PTPaterna,
		//      para que siga la estructura allí propuesta (ver documento Seminario 3)
		JSONObject pubMsg = new JSONObject();
		try {
			JSONObject msg = new JSONObject();
			msg.put("rt", "traffic::alert");
			msg.put("incident-type", notificationType);
			msg.put("id", "INC_" + System.currentTimeMillis());
			msg.put("road-segment", place.getRoad());
			msg.put("starting-position", place.getKm());
			msg.put("ending-position", place.getKm());
			msg.put("vehicle-id", smartCarID);
			msg.put("description", notificationType);
			msg.put("status", "Active");

			pubMsg.put("id", "MSG_" + System.currentTimeMillis());
			pubMsg.put("type", "ROAD_INCIDENT");
			pubMsg.put("timestamp", System.currentTimeMillis());
			pubMsg.put("msg", msg);
		} catch (JSONException e1) {
			e1.printStackTrace();
		}
		
   		int pubQoS = 0;
		MqttMessage message = new MqttMessage(pubMsg.toString().getBytes());
    	message.setQos(pubQoS);
    	message.setRetained(false);

    	// Publish the message
    	MySimpleLogger.trace(this.clientId, "Publishing to topic " + topic + " qos " + pubQoS);
    	MqttDeliveryToken token = null;
    	try {
    		// publish message to broker
			token = topic.publish(message);
			MySimpleLogger.trace(this.clientId, pubMsg.toString());
	    	// Wait until the message has been delivered to the broker
			token.waitForCompletion();
			Thread.sleep(100);
		} catch (Exception e) {
			e.printStackTrace();
		}
	    		    	

	}
	
}