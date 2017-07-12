package org.ekstep.graph.service.operation;

import static com.ilimi.graph.dac.util.Neo4jGraphUtil.NODE_LABEL;
import static com.ilimi.graph.dac.util.Neo4jGraphUtil.getNodeByUniqueId;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.ekstep.graph.service.request.validator.Neo4JEmbeddedDataVersionKeyValidator;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import com.ilimi.common.dto.Property;
import com.ilimi.common.dto.Request;
import com.ilimi.common.exception.ResourceNotFoundException;
import com.ilimi.common.exception.ServerException;
import com.ilimi.common.logger.PlatformLogger;
import com.ilimi.graph.common.DateUtils;
import com.ilimi.graph.common.Identifier;
import com.ilimi.graph.dac.enums.AuditProperties;
import com.ilimi.graph.dac.enums.GraphDACParams;
import com.ilimi.graph.dac.enums.SystemNodeTypes;
import com.ilimi.graph.dac.enums.SystemProperties;
import com.ilimi.graph.dac.exception.GraphDACErrorCodes;
import com.ilimi.graph.dac.util.Neo4jGraphFactory;
import com.ilimi.graph.dac.util.Neo4jGraphUtil;

/**
 * The Class Neo4JEmbeddedNodeOperations.
 * 
 * @author Mohammad Azharuddin
 */
public class Neo4JEmbeddedNodeOperations {

	Neo4JEmbeddedDataVersionKeyValidator versionValidator = new Neo4JEmbeddedDataVersionKeyValidator();

	/**
	 * Upsert node.
	 *
	 * @param graphId
	 *            the graph id
	 * @param node
	 *            the node
	 * @param request
	 *            the request
	 * @return the com.ilimi.graph.dac.model. node
	 */
	public com.ilimi.graph.dac.model.Node upsertNode(String graphId, com.ilimi.graph.dac.model.Node node,
			Request request) {
		GraphDatabaseService graphDb = Neo4jGraphFactory.getGraphDb(graphId, request);
		try (Transaction tx = graphDb.beginTx()) {
			PlatformLogger.log("Transaction Started For 'upsertNode' Operation. | [Node ID: '" + node.getIdentifier() + "']");
			String date = DateUtils.formatCurrentDate();
			PlatformLogger.log("Date: " + date);
			Node neo4jNode = null;
			try {
				neo4jNode = Neo4jGraphUtil.getNodeByUniqueId(graphDb, node.getIdentifier());
				PlatformLogger.log("Fetched Neo4J Node Id: " + neo4jNode.getId());

				validateNodeUpdate(neo4jNode, node);
				PlatformLogger.log("Node Update Validated: " + node.getIdentifier());

				PlatformLogger.log("Validating the Update Operation for Node Id: " + node.getIdentifier());
				versionValidator.validateUpdateOperation(graphId, neo4jNode, node, request);
				PlatformLogger.log("Node Update Operation has been Validated for Node Id: " + node.getIdentifier());

			} catch (ResourceNotFoundException e) {
				PlatformLogger.log("Node Doesn't Exist, Creating a New Node. | [Node ID: '" + node.getIdentifier() + "']");
				neo4jNode = graphDb.createNode(NODE_LABEL);
				if (StringUtils.isBlank(node.getIdentifier()))
					node.setIdentifier(
							Identifier.getIdentifier(graphId, Identifier.getUniqueIdFromNeo4jId(neo4jNode.getId())));
				PlatformLogger.log("Setting System Properties For Node. | [Node ID: '" + node.getIdentifier() + "']");
				neo4jNode.setProperty(SystemProperties.IL_UNIQUE_ID.name(), node.getIdentifier());
				neo4jNode.setProperty(SystemProperties.IL_SYS_NODE_TYPE.name(), node.getNodeType());
				neo4jNode.setProperty(AuditProperties.createdOn.name(), date);
				if (StringUtils.isNotBlank(node.getObjectType()))
					neo4jNode.setProperty(SystemProperties.IL_FUNC_OBJECT_TYPE.name(), node.getObjectType());
			}
			PlatformLogger.log("Setting Node Data. | [Node ID: '" + node.getIdentifier() + "']");
			setNodeData(graphDb, node, neo4jNode);
			if (null != node.getMetadata()
					&& null == node.getMetadata().get(GraphDACParams.SYS_INTERNAL_LAST_UPDATED_ON.name()))
				neo4jNode.setProperty(AuditProperties.lastUpdatedOn.name(), date);
			if (!StringUtils.isBlank(date)) {
				neo4jNode.setProperty(GraphDACParams.versionKey.name(), Long.toString(DateUtils.parse(date).getTime()));

				PlatformLogger.log("Setting Version Key. | [Node ID: '" + node.getIdentifier() + "']");
				node.getMetadata().put(GraphDACParams.versionKey.name(),
						Long.toString(DateUtils.parse(date).getTime()));
			}
			tx.success();
			PlatformLogger.log("Transaction For Operation 'upsertNode' Completed Successfully. | [Node ID: '"
					+ node.getIdentifier() + "']");
			return node;
		}
	}

	/**
	 * Adds the node.
	 *
	 * @param graphId
	 *            the graph id
	 * @param node
	 *            the node
	 * @param request
	 *            the request
	 * @return the com.ilimi.graph.dac.model. node
	 */
	public com.ilimi.graph.dac.model.Node addNode(String graphId, com.ilimi.graph.dac.model.Node node,
			Request request) {
		GraphDatabaseService graphDb = Neo4jGraphFactory.getGraphDb(graphId, request);
		try (Transaction tx = graphDb.beginTx()) {
			PlatformLogger.log("Transaction Started For 'addNode' Operation. | [Node ID: '" + node.getIdentifier() + "']");

			String date = DateUtils.formatCurrentDate();
			PlatformLogger.log("Date: " + date);

			Node neo4jNode = graphDb.createNode(NODE_LABEL);
			if (StringUtils.isBlank(node.getIdentifier()))
				node.setIdentifier(
						Identifier.getIdentifier(graphId, Identifier.getUniqueIdFromNeo4jId(neo4jNode.getId())));

			PlatformLogger.log("Setting System Properties For Node. | [Node ID: '" + node.getIdentifier() + "']");
			neo4jNode.setProperty(SystemProperties.IL_UNIQUE_ID.name(), node.getIdentifier());
			neo4jNode.setProperty(SystemProperties.IL_SYS_NODE_TYPE.name(), node.getNodeType());
			if (StringUtils.isNotBlank(node.getObjectType()))
				neo4jNode.setProperty(SystemProperties.IL_FUNC_OBJECT_TYPE.name(), node.getObjectType());

			PlatformLogger.log("Setting Node Data. | [Node ID: '" + node.getIdentifier() + "']");
			setNodeData(graphDb, node, neo4jNode);
			neo4jNode.setProperty(AuditProperties.createdOn.name(), date);
			neo4jNode.setProperty(AuditProperties.lastUpdatedOn.name(), date);
			if (!StringUtils.isBlank(date)) {
				neo4jNode.setProperty(GraphDACParams.versionKey.name(), Long.toString(DateUtils.parse(date).getTime()));

				PlatformLogger.log("Setting Version Key. | [Node ID: '" + node.getIdentifier() + "']");
				node.getMetadata().put(GraphDACParams.versionKey.name(),
						Long.toString(DateUtils.parse(date).getTime()));
			}
			tx.success();
			PlatformLogger.log("Transaction For Operation 'addNode' Completed Successfully. | [Node ID: '"
					+ node.getIdentifier() + "']");
			return node;
		}
	}

	/**
	 * Update node.
	 *
	 * @param graphId
	 *            the graph id
	 * @param node
	 *            the node
	 * @param request
	 *            the request
	 */
	public com.ilimi.graph.dac.model.Node updateNode(String graphId, com.ilimi.graph.dac.model.Node node,
			Request request) {
		GraphDatabaseService graphDb = Neo4jGraphFactory.getGraphDb(graphId, request);
		try (Transaction tx = graphDb.beginTx()) {
			PlatformLogger.log("Transaction Started For 'updateNode' Operation. | [Node ID: '" + node.getIdentifier() + "']");

			// Initializing Date for Stamping
			String date = DateUtils.formatCurrentDate();

			Node neo4jNode = Neo4jGraphUtil.getNodeByUniqueId(graphDb, node.getIdentifier());
			validateNodeUpdate(neo4jNode, node);

			PlatformLogger.log("Node Update Validated: " + node.getIdentifier());

			PlatformLogger.log("Validating the Update Operation for Node Id: " + node.getIdentifier());
			versionValidator.validateUpdateOperation(graphId, neo4jNode, node, request);
			PlatformLogger.log("Node Update Operation has been Validated for Node Id: " + node.getIdentifier());

			PlatformLogger.log("Setting Node Data. | [Node ID: '" + node.getIdentifier() + "']");
			setNodeData(graphDb, node, neo4jNode);
			if (null != node.getMetadata()
					&& null == node.getMetadata().get(GraphDACParams.SYS_INTERNAL_LAST_UPDATED_ON.name()))
				neo4jNode.setProperty(AuditProperties.lastUpdatedOn.name(), date);
			if (!StringUtils.isBlank(date)) {
				neo4jNode.setProperty(GraphDACParams.versionKey.name(), Long.toString(DateUtils.parse(date).getTime()));

				PlatformLogger.log("Setting Version Key. | [Node ID: '" + node.getIdentifier() + "']");
				node.getMetadata().put(GraphDACParams.versionKey.name(),
						Long.toString(DateUtils.parse(date).getTime()));
			}
			tx.success();
			PlatformLogger.log("Transaction For Operation 'updateNode' Completed Successfully. | [Node ID: '"
					+ node.getIdentifier() + "']");
			return node;
		}
	}

	/**
	 * Import nodes.
	 *
	 * @param graphId
	 *            the graph id
	 * @param nodes
	 *            the nodes
	 * @param request
	 *            the request
	 */
	public void importNodes(String graphId, List<com.ilimi.graph.dac.model.Node> nodes, Request request) {
		GraphDatabaseService graphDb = Neo4jGraphFactory.getGraphDb(graphId, request);
		try (Transaction tx = graphDb.beginTx()) {
			PlatformLogger.log("Transaction Started For 'importNodes' Operation.");
			String date = DateUtils.formatCurrentDate();
			PlatformLogger.log("Date: " + date);
			for (com.ilimi.graph.dac.model.Node node : nodes) {
				PlatformLogger.log("Iterating For Node ID: " + node.getIdentifier());
				Node neo4jNode = null;
				try {
					neo4jNode = Neo4jGraphUtil.getNodeByUniqueId(graphDb, node.getIdentifier());
					PlatformLogger.log("Fetched Neo4J Node: " + neo4jNode.getId());

					PlatformLogger.log("Validating the Update Operation for Node Id: " + node.getIdentifier());
					versionValidator.validateUpdateOperation(graphId, neo4jNode, node, request);
					PlatformLogger.log("Node Update Operation has been Validated for Node Id: " + node.getIdentifier());
				} catch (ResourceNotFoundException e) {
					PlatformLogger.log("Node Doesn't Exist, Creating a New Node. | [Node ID: '" + node.getIdentifier() + "']");
					neo4jNode = graphDb.createNode(NODE_LABEL);
					neo4jNode.setProperty(AuditProperties.createdOn.name(), date);
				}
				if (StringUtils.isBlank(node.getIdentifier()))
					node.setIdentifier(
							Identifier.getIdentifier(graphId, Identifier.getUniqueIdFromNeo4jId(neo4jNode.getId())));
				PlatformLogger.log("Setting System Properties For Node. | [Node ID: '" + node.getIdentifier() + "']");
				neo4jNode.setProperty(SystemProperties.IL_UNIQUE_ID.name(), node.getIdentifier());
				neo4jNode.setProperty(SystemProperties.IL_SYS_NODE_TYPE.name(), node.getNodeType());
				if (StringUtils.isNotBlank(node.getObjectType()))
					neo4jNode.setProperty(SystemProperties.IL_FUNC_OBJECT_TYPE.name(), node.getObjectType());
				PlatformLogger.log("Setting Node Data. | [Node ID: '" + node.getIdentifier() + "']");
				setNodeData(graphDb, node, neo4jNode);
				neo4jNode.setProperty(AuditProperties.lastUpdatedOn.name(), date);
				if (!StringUtils.isBlank(date))
					neo4jNode.setProperty(GraphDACParams.versionKey.name(),
							Long.toString(DateUtils.parse(date).getTime()));
			}
			tx.success();
			PlatformLogger.log("Transaction For Operation 'importNodes' Completed Successfully.");
		}
	}

	/**
	 * Update property value.
	 *
	 * @param graphId
	 *            the graph id
	 * @param nodeId
	 *            the node id
	 * @param property
	 *            the property
	 * @param request
	 *            the request
	 */
	public void updatePropertyValue(String graphId, String nodeId, Property property, Request request) {
		GraphDatabaseService graphDb = Neo4jGraphFactory.getGraphDb(graphId, request);
		try (Transaction tx = graphDb.beginTx()) {
			PlatformLogger.log("Transaction Started For 'updatePropertyValue' Operation. | [Node ID: '" + nodeId + "']");
			String date = DateUtils.formatCurrentDate();
			Node node = getNodeByUniqueId(graphDb, nodeId);
			if (null == property.getPropertyValue()) {
				node.removeProperty(property.getPropertyName());
				PlatformLogger.log("Property '" + property.getPropertyName() + "' Removed For Node ID: " + nodeId);
			} else {
				node.setProperty(property.getPropertyName(), property.getPropertyValue());
				PlatformLogger.log("Property '" + property.getPropertyName() + "' Added For Node ID: " + nodeId);
			}
			node.setProperty(AuditProperties.lastUpdatedOn.name(), date);
			if (!StringUtils.isBlank(date))
				node.setProperty(GraphDACParams.versionKey.name(), Long.toString(DateUtils.parse(date).getTime()));
			tx.success();
			PlatformLogger.log("Transaction For Operation 'updatePropertyValue' Completed Successfully. | [Node ID: '" + nodeId
					+ "']");
		}
	}

	/**
	 * Update property values.
	 *
	 * @param graphId
	 *            the graph id
	 * @param nodeId
	 *            the node id
	 * @param metadata
	 *            the metadata
	 * @param request
	 *            the request
	 */
	public void updatePropertyValues(String graphId, String nodeId, Map<String, Object> metadata, Request request) {
		GraphDatabaseService graphDb = Neo4jGraphFactory.getGraphDb(graphId, request);
		try (Transaction tx = graphDb.beginTx()) {
			PlatformLogger.log("Transaction Started For 'updatePropertyValues' Operation. | [Node ID: '" + nodeId + "']");
			String date = DateUtils.formatCurrentDate();
			if (null != metadata && metadata.size() > 0) {
				Node node = getNodeByUniqueId(graphDb, nodeId);
				for (Entry<String, Object> entry : metadata.entrySet()) {
					if (null == entry.getValue()) {
						node.removeProperty(entry.getKey());
						PlatformLogger.log("Property '" + entry.getKey() + "' Removed For Node ID: " + nodeId);
					} else {
						node.setProperty(entry.getKey(), entry.getValue());
						PlatformLogger.log("Property '" + entry.getKey() + "' Added For Node ID: " + nodeId);
					}
				}
				node.setProperty(AuditProperties.lastUpdatedOn.name(), date);
				if (!StringUtils.isBlank(date))
					node.setProperty(GraphDACParams.versionKey.name(), Long.toString(DateUtils.parse(date).getTime()));
			}
			tx.success();
			PlatformLogger.log("Transaction For Operation 'updatePropertyValues' Completed Successfully. | [Node ID: '"
					+ nodeId + "']");
		}
	}

	/**
	 * Removes the property value.
	 *
	 * @param graphId
	 *            the graph id
	 * @param nodeId
	 *            the node id
	 * @param key
	 *            the key
	 * @param request
	 *            the request
	 */
	public void removePropertyValue(String graphId, String nodeId, String key, Request request) {
		GraphDatabaseService graphDb = Neo4jGraphFactory.getGraphDb(graphId, request);
		try (Transaction tx = graphDb.beginTx()) {
			PlatformLogger.log("Transaction Started For 'removePropertyValue' Operation. | [Node ID: '" + nodeId + "']");
			String date = DateUtils.formatCurrentDate();
			Node node = getNodeByUniqueId(graphDb, nodeId);
			node.removeProperty(key);
			PlatformLogger.log("Property '" + key + "' Removed For Node ID: " + nodeId);
			node.setProperty(AuditProperties.lastUpdatedOn.name(), date);
			if (!StringUtils.isBlank(date))
				node.setProperty(GraphDACParams.versionKey.name(), Long.toString(DateUtils.parse(date).getTime()));
			tx.success();
			PlatformLogger.log("Transaction For Operation 'removePropertyValue' Completed Successfully. | [Node ID: '" + nodeId
					+ "']");
		}
	}

	/**
	 * Removes the property values.
	 *
	 * @param graphId
	 *            the graph id
	 * @param nodeId
	 *            the node id
	 * @param keys
	 *            the keys
	 * @param request
	 *            the request
	 */
	public void removePropertyValues(String graphId, String nodeId, List<String> keys, Request request) {
		GraphDatabaseService graphDb = Neo4jGraphFactory.getGraphDb(graphId, request);
		try (Transaction tx = graphDb.beginTx()) {
			PlatformLogger.log("Transaction Started For 'removePropertyValues' Operation. | [Node ID: '" + nodeId + "']");
			String date = DateUtils.formatCurrentDate();
			Node node = getNodeByUniqueId(graphDb, nodeId);
			for (String key : keys) {
				node.removeProperty(key);
				PlatformLogger.log("Property '" + key + "' Removed For Node ID: " + nodeId);
			}
			node.setProperty(AuditProperties.lastUpdatedOn.name(), date);
			if (!StringUtils.isBlank(date))
				node.setProperty(GraphDACParams.versionKey.name(), Long.toString(DateUtils.parse(date).getTime()));
			tx.success();
			PlatformLogger.log("Transaction For Operation 'removePropertyValues' Completed Successfully. | [Node ID: '"
					+ nodeId + "']");
		}
	}

	/**
	 * Delete node.
	 *
	 * @param graphId
	 *            the graph id
	 * @param nodeId
	 *            the node id
	 * @param request
	 *            the request
	 */
	public void deleteNode(String graphId, String nodeId, Request request) {
		GraphDatabaseService graphDb = Neo4jGraphFactory.getGraphDb(graphId, request);
		try (Transaction tx = graphDb.beginTx()) {
			PlatformLogger.log("Transaction Started For 'deleteNode' Operation. | [Node ID: '" + nodeId + "']");
			Node node = getNodeByUniqueId(graphDb, nodeId);
			Iterable<Relationship> rels = node.getRelationships();
			if (null != rels) {
				for (Relationship rel : rels) {
					rel.delete();
				}
			}
			node.delete();
			tx.success();
			PlatformLogger.log("Transaction For Operation 'deleteNode' Completed Successfully. | [Node ID: '" + nodeId + "']");
		}
	}

	/**
	 * Upsert root node.
	 *
	 * @param graphId
	 *            the graph id
	 * @param request
	 *            the request
	 * @return the node
	 */
	public com.ilimi.graph.dac.model.Node upsertRootNode(String graphId, Request request) {
		Node rootNode = null;
		GraphDatabaseService graphDb = Neo4jGraphFactory.getGraphDb(graphId, request);
		com.ilimi.graph.dac.model.Node node = new com.ilimi.graph.dac.model.Node();
		try (Transaction tx = graphDb.beginTx()) {
			PlatformLogger.log("Transaction Started For 'upsertRootNode' Operation");
			String rootNodeUniqueId = Identifier.getIdentifier(graphId, SystemNodeTypes.ROOT_NODE.name());
			try {
				rootNode = Neo4jGraphUtil.getNodeByUniqueId(graphDb, rootNodeUniqueId);
			} catch (ResourceNotFoundException e) {
				rootNode = graphDb.createNode(NODE_LABEL);
				rootNode.setProperty(SystemProperties.IL_UNIQUE_ID.name(), rootNodeUniqueId);
				rootNode.setProperty(SystemProperties.IL_SYS_NODE_TYPE.name(), SystemNodeTypes.ROOT_NODE.name());
				rootNode.setProperty(AuditProperties.createdOn.name(), DateUtils.formatCurrentDate());
				rootNode.setProperty("nodesCount", 0);
				rootNode.setProperty("relationsCount", 0);
				tx.success();
			}
			node = new com.ilimi.graph.dac.model.Node(graphId, rootNode);
			PlatformLogger.log("Transaction For Operation 'upsertRootNode' Completed Successfully");
		}
		return node;
	}

	/**
	 * Validate node update.
	 *
	 * @param neo4jNode
	 *            the neo 4 j node
	 * @param node
	 *            the node
	 */
	private void validateNodeUpdate(org.neo4j.graphdb.Node neo4jNode, com.ilimi.graph.dac.model.Node node) {
		if (neo4jNode.hasProperty(SystemProperties.IL_SYS_NODE_TYPE.name())) {
			String nodeType = (String) neo4jNode.getProperty(SystemProperties.IL_SYS_NODE_TYPE.name());
			if (!StringUtils.equals(nodeType, node.getNodeType()))
				throw new ServerException(GraphDACErrorCodes.ERR_UPDATE_NODE_INVALID_NODE_TYPE.name(),
						"Cannot update a node of type " + nodeType + " to " + node.getNodeType());
		}
		if (neo4jNode.hasProperty(SystemProperties.IL_FUNC_OBJECT_TYPE.name())) {
			String objectType = (String) neo4jNode.getProperty(SystemProperties.IL_FUNC_OBJECT_TYPE.name());
			if (!StringUtils.equals(objectType, node.getObjectType()))
				throw new ServerException(GraphDACErrorCodes.ERR_UPDATE_NODE_INVALID_NODE_TYPE.name(),
						"Cannot update a node of type " + objectType + " to " + node.getObjectType());
		}
	}

	/**
	 * Sets the node data.
	 *
	 * @param graphDb
	 *            the graph db
	 * @param node
	 *            the node
	 * @param neo4jNode
	 *            the neo 4 j node
	 */
	private void setNodeData(GraphDatabaseService graphDb, com.ilimi.graph.dac.model.Node node, Node neo4jNode) {
		Map<String, Object> metadata = node.getMetadata();
		if (null != metadata && metadata.size() > 0) {
			for (Entry<String, Object> entry : metadata.entrySet()) {
				if (null == entry.getValue()) {
					neo4jNode.removeProperty(entry.getKey());
				} else {
					neo4jNode.setProperty(entry.getKey(), entry.getValue());
				}
			}
		}
	}

}
