import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.xml.sax.SAXException;


public class Main {

	public static void main(String [] args) throws SAXException, IOException {
		
                               
                if(true){
			Properties config = new Properties();
			
                        
                        File configFile = new File("/home/brennomello/NetBeansProjects/IoT_SimulatedDevices/config_fog.properties");
                        FileInputStream file = new FileInputStream(configFile);
               
			config.load(file);
			
			Controller controller = new Controller(config);
			controller.loadDevices();
			System.out.println("OPA");
			controller.connectDeviceMqtt();
                        
		}else{
			System.out.println("To execute IoT Virtual Device you need pass config.properties path as parameter");
			System.exit(-1);
		}
		
	}

}
