import java.util.List;
import java.util.Random;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;


public class MQTTOperations implements MqttCallback{
	
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
			this.subscriber = new MqttClient(this.brokerUrl + ":" + this.brokerPort, this.serverId);
			this.subscriber.setCallback(this);
			this.subscriber.connect(connOpt);
			subscribeDevices(1);
			System.out.println("Topic devices subscribed");
			
			this.publisher = new MqttClient(this.brokerUrl + ":" + this.brokerPort, this.serverId + "_pub");
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
			
			this.subscriber = new MqttClient(this.brokerUrl + ":" + this.brokerPort, this.serverId);
			this.subscriber.setCallback(this);
			this.subscriber.connect(connOpt);
			subscribeDevices(1);
			
			this.publisher = new MqttClient(this.brokerUrl + ":" + this.brokerPort, this.serverId + "_publisher");
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
    public void messageArrived(String topic, MqttMessage message) throws Exception {
    	System.out.println("-------------------------------------------------");
		System.out.println("| Topic:" + topic);
		System.out.println("| Message: " + new String(message.getPayload()));
		System.out.println("-------------------------------------------------"); 
		
    	String messageContent = new String(message.getPayload());
    	if (messageContent.substring(0, 3).contentEquals(new String("GET"))){
    		VirtualDevice device = getDeviceOfTopic(topic);
    		MqttMessage answer = buildAnwserDevice(topic, device,message);
    		this.publisher.publish(topic + "/RES", answer);
		}   
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // TODO Auto-generated method stub

    }
    
    private void subscribeDevices(int qos){
    	for(int i=0;i<this.devices.size(); i++){
    		try {
    			String topic = topicPrefix + devices.get(i).getName() + "/#";
				this.subscriber.subscribe(topic, qos);
			} catch (MqttException e) {
				e.printStackTrace();
				System.exit(-1);
			}
    	}
    }
    
    private VirtualDevice getDeviceOfTopic(String topic){
    	String deviceName = topic.split("/")[1];
    	for(int i=0;i<this.devices.size(); i++){
    		if(this.devices.get(i).getName().contentEquals(deviceName))
    			return(this.devices.get(i));
    	}
        return null;
    }

	    private MqttMessage buildAnwserDevice(String topic, VirtualDevice device, MqttMessage message){
	    	Random randomGenerator = new Random();
			MqttMessage answer = new MqttMessage();
			
			String messageContent = new String(message.getPayload());
			String type = messageContent.split(" ")[1];
			String sensorName = messageContent.split(" ")[2];
			
			VirtualSensor sensor = device.getSensor(sensorName);
			int i = randomGenerator.nextInt(sensor.getValues().size());
			Object value = sensor.getValues().get(i);
			String msg = "POST "  + device.getName()+ ":{HEADER:{" + sensor.getName() + "},BODY:{"; //<resposta>}}"
			if(type.contentEquals("INFO") || type.contentEquals("VALUE")){
				msg = msg + value;
			}else if(type.contentEquals("STATE")){
				if(((String)value).contentEquals("true")){
					msg = msg + "T";
				}else{
					msg = msg + "F";
				}
			}
			msg = msg + "}}";
			answer.setPayload(msg.getBytes());
			return answer;
		}
    
    
	public void disconnect(){
		try {
			this.subscriber.disconnect();
			this.publisher.disconnect();
		} catch (MqttException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
	}
	
	public void test(){
		MqttMessage message = new MqttMessage();
	    message.setPayload("I'm alive".getBytes());
	    try {
			this.subscriber.publish("pahodemo/test", message);
		} catch (MqttPersistenceException e) {
			e.printStackTrace();
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
	

	
}
