package autofe.db.model.database;

public class BackwardRelationship {
	
	private AbstractAttribute commonAttribute;
	
	private Table from;
	
	private Table to;

	public AbstractAttribute getCommonAttribute() {
		return commonAttribute;
	}

	public void setCommonAttribute(AbstractAttribute commonAttribute) {
		this.commonAttribute = commonAttribute;
	}

	public Table getFrom() {
		return from;
	}

	public void setFrom(Table from) {
		this.from = from;
	}

	public Table getTo() {
		return to;
	}

	public void setTo(Table to) {
		this.to = to;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((commonAttribute == null) ? 0 : commonAttribute.hashCode());
		result = prime * result + ((from == null) ? 0 : from.hashCode());
		result = prime * result + ((to == null) ? 0 : to.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BackwardRelationship other = (BackwardRelationship) obj;
		if (commonAttribute == null) {
			if (other.commonAttribute != null)
				return false;
		} else if (!commonAttribute.equals(other.commonAttribute))
			return false;
		if (from == null) {
			if (other.from != null)
				return false;
		} else if (!from.equals(other.from))
			return false;
		if (to == null) {
			if (other.to != null)
				return false;
		} else if (!to.equals(other.to))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "BackwardRelationship [commonAttribute=" + commonAttribute + ", from=" + from + ", to=" + to + "]";
	}
	
	

}
