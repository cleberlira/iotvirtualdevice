import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import net.sourceforge.jdistlib.Normal;

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
	Hashtable flowStatus;

	public MQTTOperations(String brokerUrl, String brokerPort, String serverId,
			String username, String password, List<VirtualDevice> devices) {
		MqttConnectOptions connOpt = new MqttConnectOptions();
		
		this.brokerUrl = brokerUrl;
		this.brokerPort = brokerPort;
		this.serverId = serverId;
		this.username = username;
		this.password = password;
		this.devices = devices;

		this.flowStatus = new Hashtable<String, Object>();

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
			
                        System.out.println("Topic devices publisher");
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
                        System.out.println("Topic devices subscribed");
                        
			this.publisher = new MqttClient(this.brokerUrl + ":"
					+ this.brokerPort, this.serverId + "_publisher");
			this.publisher.setCallback(this);
			this.publisher.connect(connOpt);

			System.out.println("Topic devices publisher");
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}

	}

	@Override
	public void messageArrived(final String topic, final MqttMessage message)
			throws Exception {
		
                System.out.println("-------------------------------------------------");
		System.out.println("| Topic:" + topic);
		System.out.println("| Message: " + new String(message.getPayload()));
		System.out.println("-------------------------------------------------");
		String messageContent = new String(message.getPayload());

		if (messageContent.substring(0, 3).contentEquals(new String("GET"))) {
			VirtualDevice device = getDeviceOfTopic(topic);
                        
                        //alterar função de resposta
			MqttMessage answer = buildGetAnwserDevice(topic, device, message);
			this.publisher.publish(topic + "/RES", answer);
		} else if (messageContent.substring(0, 4).contentEquals(
				new String("FLOW"))) {
			final MqttClient publisherInt = this.publisher;
			final VirtualDevice device = getDeviceOfTopic(topic);
			String sensorName = messageContent.split(" ")[2];

			Thread flow = getThreadByName(device.getName() + sensorName);
			if (flow != null) {
				flow.interrupt();
			}
			if (!isFlowSetOff(messageContent)){
				flow = new Thread() {
					public void run() {
						try {
							while (true) {
                                                                //alterar funções de resposta
								MqttMessage answer = buildFlowAnwserDevice(
										topic, device, message);
								publisherInt.publish(topic + "/RES",
								answer);
							}
						} catch (InterruptedException v) {
							System.out.println(v);
						} catch (MqttPersistenceException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (MqttException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				};
				flow.start();
				flow.setName(device.getName() + sensorName);
			}
		}

	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		// TODO Auto-generated method stub

	}
	
	private boolean isFlowSetOff(String msg){
		String sensorName = msg.split(" ")[2];
		String configuration = msg.split(sensorName + " ")[1];
		JSONObject confJSON = new JSONObject(configuration);
		if (confJSON.has("turn")){
			return(!confJSON.getBoolean("turn"));
		}else{
			return false;
		}
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
           
        
      	private MqttMessage buildGetAnwserDevice(String topic,
			VirtualDevice device, MqttMessage message) {
		Random randomGenerator = new Random();
		MqttMessage answer = new MqttMessage();
                
		//GET INFO temperatureSensor
		String messageContent = new String(message.getPayload());
		String type = messageContent.split(" ")[1];
		String sensorName = messageContent.split(" ")[2];

		VirtualSensor sensor = device.getSensor(sensorName);
		int i = randomGenerator.nextInt(sensor.getValues().size());
		Object value = sensor.getValues().get(i);

		JSONObject response = new JSONObject();
		JSONObject header = new JSONObject();
		JSONObject body = new JSONObject();
		header.put("NAME", sensor.getDevice().getName());
		body.put(sensor.getName(), value);
		response.put("METHOD", "GET");
		response.put("CODE", "POST");
		response.put("HEADER", header);
		response.put("BODY", body);

		answer.setPayload(response.toString().getBytes());
		return answer;
	}

	private double statisticalDistributionGaussTemp(){
            Random random =  new Random();
            
            double value;
            
            do {
                value = random.nextGaussian() * 10 + 10;
            } while (value <= 17);

            return 0;
        }
        
        
        private double[] statisticalDistribution(String function, int amount){
            
            double result[] = new double[amount];
            switch (function) {
                case "normal":  
                        Normal normal = new Normal(0,1);

                        System.out.println(normal.density(0, false));
                        System.out.println(normal.cumulative(0, false, false));

                        result = normal.random(amount);

                        break;
                case "exponential":
                        
                        break;
                case "normalTemperature":
                    Random random =  new Random();
            
                    double value;
                    for (int i = 0; i < amount; i++) {
                        
                        do {
                            value = random.nextGaussian() * 10 + 10;
                        } while (value <= 17);
                        
                        result[i] = value;
                    }
                                       
                    
                    break;
                    
            }
            
            
            
           return result;            
        }
                
        
        
        
        private MqttMessage buildFlowAnwserDevice(String topic,
			VirtualDevice device, MqttMessage message)
			throws InterruptedException {
		Random randomGenerator = new Random();
		MqttMessage answer = new MqttMessage();
		
                // {"CODE":"GET","DATA":"INFO","VAR":"temp"}
		// FLOW INFO tenperatureSensor {collect:5000, publish:30000}
                String messageContent = new String(message.getPayload());
		String type = messageContent.split(" ")[1];
		String sensorName = messageContent.split(" ")[2];
		String configuration = messageContent.split(sensorName + " ")[1];
		
		VirtualSensor sensor = device.getSensor(sensorName);
                String statiscDistribution = sensor.getStatisticalDistribution();
                                             
		Vector<String> results = new Vector<String>();
		JSONObject confJSON = new JSONObject(configuration);
		int publishValue = confJSON.getInt("publish");
		int publish = confJSON.getInt("publish");
		int collect = confJSON.getInt("collect");
		
                int amount = publish/collect;
                double[] values = statisticalDistribution("normalTemperature", amount);               
                
                while (publish > 0) {
			//int i = randomGenerator.nextInt(sensor.getValues().size());
			//Object value = sensor.getValues().get(i);
			//results.add((String) value);
			Thread.sleep(collect);
			publish -= collect;
		}
                
                for (int i = 0; i < values.length; i++) {
                    results.add(String.valueOf(values[i]));
                }
                
                                
		JSONObject response = new JSONObject();
		JSONObject header = new JSONObject();
		JSONObject body = new JSONObject();
		JSONObject flow = new JSONObject();
		flow.put("collect", collect);
		flow.put("publish", publishValue);
		header.put("NAME", sensor.getDevice().getName());
		body.put(sensor.getName(), results.toArray());
		body.put("FLOW", flow);
		response.put("METHOD", "FLOW");
		
		response.put("CODE", "POST");
		response.put("HEADER", header);
		response.put("BODY", body);

		System.out.println(response);
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

	public Thread getThreadByName(String threadName) {
		for (Thread t : Thread.getAllStackTraces().keySet()) {
			if (t.getName().equals(threadName))
				return t;
		}
		return null;
	}
}
