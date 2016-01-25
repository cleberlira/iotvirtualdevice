import java.util.List;


public class VirtualSensor {
	private String name;
	private String type_value;
	private List<Object> values;
	
	public VirtualSensor(String name, String type_value, List<Object> values) {
		this.name = name;
		this.type_value = type_value;
		this.values = values;
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
}
