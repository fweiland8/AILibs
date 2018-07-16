package autofe.db.search;

import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import autofe.db.model.database.Database;
import autofe.db.model.database.ForwardRelationship;
import autofe.db.model.database.Table;
import autofe.db.model.operation.BackwardAggregateOperation;
import autofe.db.model.operation.ForwardJoinOperation;
import autofe.db.util.DBUtils;
import jaicore.search.structure.core.NodeExpansionDescription;
import jaicore.search.structure.core.NodeType;
import jaicore.search.structure.graphgenerator.SuccessorGenerator;

public class DatabaseSuccessorGenerator implements SuccessorGenerator<DatabaseNode, String> {

	private static Logger LOG = LoggerFactory.getLogger(DatabaseSuccessorGenerator.class);

	@Override
	public Collection<NodeExpansionDescription<DatabaseNode, String>> generateSuccessors(DatabaseNode node) {
		Collection<NodeExpansionDescription<DatabaseNode, String>> toReturn = new ArrayList<>();

		Database db = node.getDatabase();
		Table targetTable = DBUtils.getTargetTable(db);

		LOG.debug("Expanding node {}", node);

		// Successors for forward relationships
		for (ForwardJoinOperation operation : DBUtils.getForwardJoinOperations(targetTable, db)) {
			LOG.debug("Found forward operation: " + operation);
			Database clone = DBUtils.clone(node.getDatabase());
			operation.applyTo(clone);
			DatabaseNode successor = new DatabaseNode(clone);
			toReturn.add(new NodeExpansionDescription<DatabaseNode, String>(node, successor, operation.toString(),
					NodeType.OR));
		}

		// Successors for backward relationships
		for (BackwardAggregateOperation operation : DBUtils.getBackwardAggregateOperations(targetTable, db)) {
			LOG.debug("Found backward operation: " + operation);
			Database clone = DBUtils.clone(node.getDatabase());
			operation.applyTo(clone);
			DatabaseNode successor = new DatabaseNode(clone);
			toReturn.add(new NodeExpansionDescription<DatabaseNode, String>(node, successor, operation.toString(),
					NodeType.OR));
		}

		LOG.info("Found {} successor nodes", toReturn.size());
		return toReturn;
	}

}
