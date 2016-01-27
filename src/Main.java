import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.xml.sax.SAXException;


public class Main {

	public static void main(String[] args) throws SAXException, IOException {
		Properties config = new Properties();
		FileInputStream file = new FileInputStream("./config.properties");
		config.load(file);
		
		Controller controller = new Controller(config);
		controller.loadDevices();
		controller.connectDeviceMqtt();
		
	}

}
