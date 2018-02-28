import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class Controller {
	private Properties config;
	private List<VirtualDevice> devices;
	
	public Controller(Properties config) throws IOException, SAXException {
		this.config = config;
		devices = new ArrayList<VirtualDevice>();
	}
	
	public MQTTOperations connectDeviceMqtt(){
		MQTTOperations mqtt = new MQTTOperations(this.config.getProperty("broker_mqtt.url"),
												 this.config.getProperty("broker_mqtt.port"),
												 this.config.getProperty("virtual_devices.id"),
												 this.config.getProperty("broker_mqtt.username"),
												 this.config.getProperty("broker_mqtt.password"),
												 this.devices);
		return mqtt;
	}


	public void loadDevices() throws SAXException, IOException{
		
		File folder = new File(this.config.getProperty("virtual_devices.path"));
		List<VirtualDevice> devices = new ArrayList<VirtualDevice>();;
		List<File> xmlFiles = new ArrayList<File>();
		
		for (File pf : folder.listFiles()) {
                    if (pf.isFile() && (pf.getName().endsWith(".xml")||pf.getName().endsWith(".XML"))) {
                      xmlFiles.add(pf);
                    }
                }
                
		for (File file : xmlFiles) {
			devices.add(createDeviceUpdate(file));
			
		}
		this.devices = devices;
	}
	
        private VirtualDevice createDevice(File file) throws SAXException, IOException{
		VirtualDevice device = null;
		
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder;
			dBuilder = dbFactory.newDocumentBuilder();
			Document docDevice = dBuilder.parse(file);
			device = new VirtualDevice(docDevice.getDocumentElement().getAttribute("name"));
			
			NodeList sensorList = docDevice.getElementsByTagName("sensor");
			List<VirtualSensor> sensors = new ArrayList<VirtualSensor>();
			for(int i=0;i < sensorList.getLength(); i++){
				List<Object> values = new ArrayList<Object>();
				Element eSensor = (Element) sensorList.item(i);
				NodeList valueList = eSensor.getElementsByTagName("value");
				for(int j=0;j < valueList.getLength();j++){
					values.add(valueList.item(j).getTextContent());
				}
				String name = eSensor.getAttribute("name");
				Element eValue = (Element) eSensor.getElementsByTagName("values").item(0);
				String type_value = eValue.getAttribute("type");
				sensors.add(new VirtualSensor(name, type_value, values, device));
			}
			device.setSensors(sensors);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		
		return device;
	}

        
        
        
        
            private VirtualDevice createDeviceUpdate(File file) throws SAXException, IOException{
		VirtualDevice device = null;
		
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder;
			dBuilder = dbFactory.newDocumentBuilder();
			Document docDevice = dBuilder.parse(file);
			device = new VirtualDevice(docDevice.getDocumentElement().getAttribute("name"));
			
                        System.out.println("Device => " + device.getName() + "  -----------------");
                        
			NodeList sensorList = docDevice.getElementsByTagName("sensor");
			List<VirtualSensor> sensors = new ArrayList<VirtualSensor>();
			for(int i=0; i < sensorList.getLength(); i++){
				
                                Element eSensor = (Element) sensorList.item(i);
				Element valuesElement = (Element) eSensor.getElementsByTagName("values").item(0);
                                Element distributionElement = (Element) eSensor.getElementsByTagName("distribution").item(0);
				
                                String typeValue = valuesElement.getTextContent();
                                String distribution =  distributionElement.getTextContent();
				
                                String name = eSensor.getAttribute("name");
				
				VirtualSensor virtualSensor = new VirtualSensor(name, typeValue, device, distribution);
                                System.out.println("Virtual Sensor Name => "+virtualSensor.getName() + " --------------");
                                System.out.println("Virtual Sensor Type Value => "+virtualSensor.getTypeValue());
                                System.out.println("Virtual Sensor Distribution => "+virtualSensor.getStatisticalDistribution());
                                sensors.add(virtualSensor);
			}
                        
			device.setSensors(sensors);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		
		return device;
	}


	public List<VirtualDevice> getDevices() {
		return devices;
	}


}
