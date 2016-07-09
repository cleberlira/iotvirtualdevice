import java.util.List;
import java.util.Random;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.json.JSONObject;

public class MQTTOperations implements MqttCallback {

	public static String topicPrefix = "dev/";

	private String brokerUrl;
	private String brokerPort;
	private String serverId;
	private String username;
	private String password;
	private MqttClient subscriber;
	private MqttClient publisher;
	private List<VirtualDevice> devices;

	public MQTTOperations(String brokerUrl, String brokerPort, String serverId,
			String username, String password, List<VirtualDevice> devices) {

		MqttConnectOptions connOpt = new MqttConnectOptions();

		this.brokerUrl = brokerUrl;
		this.brokerPort = brokerPort;
		this.serverId = serverId;
		this.username = username;
		this.password = password;
		this.devices = devices;

		try {
			if (!this.username.isEmpty())
				connOpt.setUserName(this.username);
			if (!this.password.isEmpty())
				connOpt.setPassword(this.password.toCharArray());

			this.subscriber = new MqttClient(this.brokerUrl + ":"
					+ this.brokerPort, this.serverId);
			this.subscriber.setCallback(this);
			this.subscriber.connect(connOpt);
			subscribeDevices(1);
			System.out.println("Topic devices subscribed");

			this.publisher = new MqttClient(this.brokerUrl + ":"
					+ this.brokerPort, this.serverId + "_pub");
			this.publisher.setCallback(this);
			this.publisher.connect(connOpt);
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}

	}

	@Override
	public void connectionLost(Throwable cause) {
		try {
			MqttConnectOptions connOpt = new MqttConnectOptions();

			this.subscriber = new MqttClient(this.brokerUrl + ":"
					+ this.brokerPort, this.serverId);
			this.subscriber.setCallback(this);
			this.subscriber.connect(connOpt);
			subscribeDevices(1);

			this.publisher = new MqttClient(this.brokerUrl + ":"
					+ this.brokerPort, this.serverId + "_publisher");
			this.publisher.setCallback(this);
			this.publisher.connect(connOpt);

			System.out.println("Topic devices subscribed");
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}

	}

	@Override
	public void messageArrived(String topic, MqttMessage message)
			throws Exception {
		System.out.println("-------------------------------------------------");
		System.out.println("| Topic:" + topic);
		System.out.println("| Message: " + new String(message.getPayload()));
		System.out.println("-------------------------------------------------");
		String messageContent = new String(message.getPayload());

		try {
			JSONObject json = new JSONObject(messageContent);
			
			if (json.get("CODE").toString().contentEquals("GET")) {
				VirtualDevice device = getDeviceOfTopic(topic);
				MqttMessage answer = buildAnwserDevice(topic, device, json);
				this.publisher.publish(topic + "/RES", answer);
			}
		} catch (org.json.JSONException e) {

		}
		

	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		// TODO Auto-generated method stub

	}

	private void subscribeDevices(int qos) {
		for (int i = 0; i < this.devices.size(); i++) {
			try {
				String topic = topicPrefix + devices.get(i).getName() + "/#";
				this.subscriber.subscribe(topic, qos);
			} catch (MqttException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}

	private VirtualDevice getDeviceOfTopic(String topic) {
		String deviceName = topic.split("/")[1];
		for (int i = 0; i < this.devices.size(); i++) {
			if (this.devices.get(i).getName().contentEquals(deviceName))
				return (this.devices.get(i));
		}
		return null;
	}

	private MqttMessage buildAnwserDevice(String topic, VirtualDevice device,
			JSONObject message) {
		Random randomGenerator = new Random();
		MqttMessage answer = new MqttMessage();
		
		// {"CODE":"GET","DATA":"INFO","VAR":"temp"}
		
		String type = message.getString("DATA");
		String sensorName = message.getString("VAR");

		VirtualSensor sensor = device.getSensor(sensorName);
		int i = randomGenerator.nextInt(sensor.getValues().size());
		Object value = sensor.getValues().get(i);

		JSONObject response = new JSONObject();
		JSONObject header = new JSONObject();
		JSONObject body = new JSONObject();
		header.put("NAME", sensor.getDevice().getName());
		body.put(sensor.getName(), value);
		response.put("CODE", "POST");
		response.put("HEADER", header);
		response.put("BODY", body);

		answer.setPayload(response.toString().getBytes());
		return answer;
	}

	public void disconnect() {
		try {
			this.subscriber.disconnect();
			this.publisher.disconnect();
		} catch (MqttException e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}
}
