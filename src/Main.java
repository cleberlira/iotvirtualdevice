import java.io.IOException;
import java.util.List;

import org.xml.sax.SAXException;


public class Main {

	public static void main(String[] args) throws SAXException, IOException {
		Init init = new Init("/home/leandrojsa/Doutorado/Smart/eclipse/workspace/IoT_VirtualDevice/virtual_devices");
		List<VirtualDevice> devices = init.getDevices();
		MQTTOperations mqtt = new  MQTTOperations("tcp://localhost", "1883", "virtual-device", "", "", devices);
		mqtt.test();

	}

}
