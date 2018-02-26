import java.util.List;


public class VirtualDevice {
	
        private String name;
	private List<VirtualSensor> sensors;
	
	public VirtualDevice(String name, List<VirtualSensor> sensors) {
		this.name = name;
		this.sensors = sensors;
	}
	
	public VirtualDevice(String name) {
		this.name = name;
		this.sensors = null;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List<VirtualSensor> getSensors() {
		return sensors;
	}
	public void setSensors(List<VirtualSensor> sensors) {
		this.sensors = sensors;
	}
	
	public VirtualSensor getSensor(String sensorName){
		for(int i=0;i<this.sensors.size(); i++){
			if(sensors.get(i).getName().contentEquals(sensorName))
				return sensors.get(i);
		}
		return null;
	}
	
	
}
