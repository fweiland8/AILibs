package autofe.db.model.database;

public class AggregatedAttribute extends AbstractAttribute {

	private AbstractAttribute parent;

	private AggregationFunction aggregationFunction;

	public AggregatedAttribute(String name, AttributeType type, AbstractAttribute parent,
			AggregationFunction aggregationFunction) {
		super(name, type);
		this.parent = parent;
		this.aggregationFunction = aggregationFunction;
	}

	public AbstractAttribute getParent() {
		return parent;
	}

	public void setParent(AbstractAttribute parent) {
		this.parent = parent;
	}

	public AggregationFunction getAggregationFunction() {
		return aggregationFunction;
	}

	public void setAggregationFunction(AggregationFunction aggregationFunction) {
		this.aggregationFunction = aggregationFunction;
	}

	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((aggregationFunction == null) ? 0 : aggregationFunction.hashCode());
		result = prime * result + ((parent == null) ? 0 : parent.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		AggregatedAttribute other = (AggregatedAttribute) obj;
		if (aggregationFunction != other.aggregationFunction)
			return false;
		if (parent == null) {
			if (other.parent != null)
				return false;
		} else if (!parent.equals(other.parent))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "AggregatedAttribute [parent=" + parent + ", aggregationFunction=" + aggregationFunction + ", name="
				+ name + ", type=" + type + ", isTarget=" + isTarget + "]";
	}

}
