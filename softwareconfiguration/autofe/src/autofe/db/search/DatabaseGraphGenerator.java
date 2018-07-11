package autofe.db.search;

import autofe.db.model.Database;
import jaicore.search.structure.core.GraphGenerator;
import jaicore.search.structure.graphgenerator.GoalTester;
import jaicore.search.structure.graphgenerator.RootGenerator;
import jaicore.search.structure.graphgenerator.SingleRootGenerator;
import jaicore.search.structure.graphgenerator.SuccessorGenerator;

public class DatabaseGraphGenerator implements GraphGenerator<DatabaseNode, String> {

	private Database initialDatabase;
	
	public DatabaseGraphGenerator(Database initialDatabase) {
		super();
		this.initialDatabase = initialDatabase;
	}

	@Override
	public SingleRootGenerator<DatabaseNode> getRootGenerator() {
		return new SingleRootGenerator<DatabaseNode>() {

			@Override
			public DatabaseNode getRoot() {
				return new DatabaseNode(initialDatabase);
			}
			
			
		
		};
	}

	@Override
	public SuccessorGenerator<DatabaseNode, String> getSuccessorGenerator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GoalTester<DatabaseNode> getGoalTester() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isSelfContained() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setNodeNumbering(boolean nodenumbering) {
		// TODO Auto-generated method stub
		
	}

}