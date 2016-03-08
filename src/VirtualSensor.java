import java.util.List;


public class VirtualSensor {
	private String name;
	private String type_value;
	private List<Object> values;
	private VirtualDevice device;
	
	public VirtualSensor(String name, String type_value, List<Object> values, VirtualDevice device) {
		this.name = name;
		this.type_value = type_value;
		this.values = values;
		this.device = device;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getType_value() {
		return type_value;
	}
	public void setType_value(String type_value) {
		this.type_value = type_value;
	}
	public List<Object> getValues() {
		return values;
	}
	public void setValues(List<Object> values) {
		this.values = values;
	}

	public VirtualDevice getDevice() {
		return device;
	}

	public void setDevice(VirtualDevice device) {
		this.device = device;
	}
}
