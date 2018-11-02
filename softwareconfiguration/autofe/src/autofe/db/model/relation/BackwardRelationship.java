package autofe.db.model.relation;

public class BackwardRelationship extends AbstractRelationship {

	public BackwardRelationship() {
		super();
	}

	public BackwardRelationship(String fromTableName, String toTableName, String commonAttributeName) {
		super(fromTableName, toTableName, commonAttributeName);
	}

	@Override
	public String toString() {
		return "BackwardRelationship [fromTableName=" + fromTableName + ", toTableName=" + toTableName
				+ ", commonAttributeName=" + commonAttributeName + "]";
	}

}