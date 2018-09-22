import java.util.List;


public class VirtualSensor {
	private String name;
	private String typeValue;
        private String statisticalDistribution;
        private List<Object> values;
	private VirtualDevice device;
	
	public VirtualSensor(String name, String typeValue, List<Object> values, VirtualDevice device) {
		this.name = name;
		this.typeValue = typeValue;
		this.values = values;
		this.device = device;
	}
	
	public VirtualSensor(String name, String typeValue, VirtualDevice device, String statisticalDistribution) {
		this.name = name;
		this.typeValue = typeValue;
		this.device = device;
                this.statisticalDistribution = statisticalDistribution;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getTypeValue() {
		return typeValue;
	}
	public void setTypeValue(String typeValue) {
		this.typeValue = typeValue;
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

        /**
         * @return the statisticalDistribution
         */
        public String getStatisticalDistribution() {
            return statisticalDistribution;
        }

        /**
         * @param statisticalDistribution the statisticalDistribution to set
         */
        public void setStatisticalDistribution(String statisticalDistribution) {
            this.statisticalDistribution = statisticalDistribution;
        }
}
