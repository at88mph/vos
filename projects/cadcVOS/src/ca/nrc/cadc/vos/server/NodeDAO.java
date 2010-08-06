/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2009.                            (c) 2009.
*  Government of Canada                 Gouvernement du Canada
*  National Research Council            Conseil national de recherches
*  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
*  All rights reserved                  Tous droits réservés
*                                       
*  NRC disclaims any warranties,        Le CNRC dénie toute garantie
*  expressed, implied, or               énoncée, implicite ou légale,
*  statutory, of any kind with          de quelque nature que ce
*  respect to the software,             soit, concernant le logiciel,
*  including without limitation         y compris sans restriction
*  any warranty of merchantability      toute garantie de valeur
*  or fitness for a particular          marchande ou de pertinence
*  purpose. NRC shall not be            pour un usage particulier.
*  liable in any event for any          Le CNRC ne pourra en aucun cas
*  damages, whether direct or           être tenu responsable de tout
*  indirect, special or general,        dommage, direct ou indirect,
*  consequential or incidental,         particulier ou général,
*  arising from the use of the          accessoire ou fortuit, résultant
*  software.  Neither the name          de l'utilisation du logiciel. Ni
*  of the National Research             le nom du Conseil National de
*  Council of Canada nor the            Recherches du Canada ni les noms
*  names of its contributors may        de ses  participants ne peuvent
*  be used to endorse or promote        être utilisés pour approuver ou
*  products derived from this           promouvoir les produits dérivés
*  software without specific prior      de ce logiciel sans autorisation
*  written permission.                  préalable et particulière
*                                       par écrit.
*                                       
*  This file is part of the             Ce fichier fait partie du projet
*  OpenCADC project.                    OpenCADC.
*                                       
*  OpenCADC is free software:           OpenCADC est un logiciel libre ;
*  you can redistribute it and/or       vous pouvez le redistribuer ou le
*  modify it under the terms of         modifier suivant les termes de
*  the GNU Affero General Public        la “GNU Affero General Public
*  License as published by the          License” telle que publiée
*  Free Software Foundation,            par la Free Software Foundation
*  either version 3 of the              : soit la version 3 de cette
*  License, or (at your option)         licence, soit (à votre gré)
*  any later version.                   toute version ultérieure.
*                                       
*  OpenCADC is distributed in the       OpenCADC est distribué
*  hope that it will be useful,         dans l’espoir qu’il vous
*  but WITHOUT ANY WARRANTY;            sera utile, mais SANS AUCUNE
*  without even the implied             GARANTIE : sans même la garantie
*  warranty of MERCHANTABILITY          implicite de COMMERCIALISABILITÉ
*  or FITNESS FOR A PARTICULAR          ni d’ADÉQUATION À UN OBJECTIF
*  PURPOSE.  See the GNU Affero         PARTICULIER. Consultez la Licence
*  General Public License for           Générale Publique GNU Affero
*  more details.                        pour plus de détails.
*                                       
*  You should have received             Vous devriez avoir reçu une
*  a copy of the GNU Affero             copie de la Licence Générale
*  General Public License along         Publique GNU Affero avec
*  with OpenCADC.  If not, see          OpenCADC ; si ce n’est
*  <http://www.gnu.org/licenses/>.      pas le cas, consultez :
*                                       <http://www.gnu.org/licenses/>.
*
*  $Revision: 4 $
*
************************************************************************
*/

package ca.nrc.cadc.vos.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import ca.nrc.cadc.util.HexUtil;
import ca.nrc.cadc.vos.ContainerNode;
import ca.nrc.cadc.vos.DataNode;
import ca.nrc.cadc.vos.Node;
import ca.nrc.cadc.vos.NodeAlreadyExistsException;
import ca.nrc.cadc.vos.NodeNotFoundException;
import ca.nrc.cadc.vos.NodeProperty;
import ca.nrc.cadc.vos.VOS;
import ca.nrc.cadc.vos.VOS.NodeBusyState;

/**
 * Default implementation of the NodePersistence interface.
 * 
 * @author majorb
 *
 */
public abstract class NodeDAO implements NodePersistence
{
    
    private static Logger log = Logger.getLogger(NodeDAO.class);
    
    private static final int NODE_NAME_COLUMN_SIZE = 256;
    private static final int MAX_TIMESTAMP_LENGTH = 30;

    // Database connection.
    private JdbcTemplate jdbc;
    private DataSourceTransactionManager transactionManager;
    private DefaultTransactionDefinition defaultTransactionDef;
    private TransactionStatus transactionStatus;
    
    private boolean getNodesMarkedForDeletion = false;

    /**
     * NodeDAO Constructor.
     */
    public NodeDAO()
    {
    }
    
    /**
     * Returns true if nodes marked for deletion are
     * returned by the DAO
     */
    public boolean getGetNodesMarkedForDeletion()
    {
        return getNodesMarkedForDeletion;
    }
    
    /**
     * If true, retrieve nodes in the database that have
     * been marked for deletion
     * @param value The new value.
     */
    public void setGetNodesMarkedForDeletion(boolean value)
    {
        getNodesMarkedForDeletion = value;
    }
    
    /**
     * Perform NodeDAO initialization.  This method must be called
     * before any DAO operations are used.
     */
    public void init()
    {
        this.defaultTransactionDef = new DefaultTransactionDefinition();
        defaultTransactionDef
                .setIsolationLevel(DefaultTransactionDefinition.ISOLATION_READ_COMMITTED);
        DataSource dataSource = getDataSource();
        this.jdbc = new JdbcTemplate(dataSource);
        this.transactionManager = new DataSourceTransactionManager(dataSource);
    }
    
    /**
     * @return The name of the table for persisting nodes.
     */
    public abstract String getNodeTableName();

    /**
     * @return The name of the table for storing node properties.
     */
    public abstract String getNodePropertyTableName();
    
    /**
     * @return The node data source to use.
     */
    public abstract DataSource getDataSource();

    /**
     * Start a transaction to the data source.
     */
    protected void startTransaction()
    {
        if (transactionStatus != null)
            throw new IllegalStateException("transaction already in progress");
        log.debug("startTransaction");
        this.transactionStatus = transactionManager
                .getTransaction(defaultTransactionDef);
        log.debug("startTransaction: OK");
    }

    /**
     * Commit the transaction to the data source.
     */
    protected void commitTransaction()
    {
        if (transactionStatus == null)
            throw new IllegalStateException("no transaction in progress");
        log.debug("commitTransaction");
        transactionManager.commit(transactionStatus);
        this.transactionStatus = null;
        log.debug("commit: OK");
    }

    /**
     * Rollback the transaction to the data source.
     */
    protected void rollbackTransaction()
    {
        if (transactionStatus == null)
            throw new IllegalStateException("no transaction in progress");
        log.debug("rollbackTransaction");
        transactionManager.rollback(transactionStatus);
        this.transactionStatus = null;
        log.debug("rollback: OK");
    }
    
    /**
     * Get the node from the parent.  This method assumes that the nodeID
     * of the parent object is set.  If the parent object is null, it is
     * assumed to be at the root level.
     * @param name The node to get
     * @param parent The parent of the node, or null if this is a root.  This object
     * must have been retrived from the NodePersistence interface.
     */
    public Node getFromParent(String name, ContainerNode parent) throws NodeNotFoundException
    {
        if (name == null)
        {
            throw new NodeNotFoundException("Node parameter is null.");
        }
        SearchNode node = new SearchNode(name);
        node.setParent(parent);
        synchronized (this)
        {
            Node returnNode = getSingleNodeFromSelect(getSelectNodeByNameAndParentSQL(node));
            if (returnNode == null)
            {
                throw new NodeNotFoundException(node.getName());
            }
            returnNode.setPath( (parent == null) ? returnNode.getName() : parent.getPath() + "/" + returnNode.getName() );
            returnNode.setParent(parent);
            //log.debug("Node retrieved from parent: " + returnNode);
            return returnNode;
        }
    }
    
    /**
     * Put the node in the provided container.  This method assumes that the
     * nodeID of the parent is set.  If the parent object is null, it is
     * assumed to be at the root level.
     * @param node The node to put
     * @param parent The parent of the node.  This object must have been retrieved
     * from the NodePersistence interface.
     */
    public Node putInContainer(Node node, ContainerNode parent) throws NodeNotFoundException, NodeAlreadyExistsException
    {
        
        log.debug("NodeDAO.putInContainer(): node is: " + node);
        log.debug("NodeDAO.putInContainer(): node parent is: " + parent);
        
        if (node == null)
        {
            throw new NodeNotFoundException("Node parameter is null.");
        }
        
        synchronized (this)
        {
            try
            {
                // Start the transaction.
                startTransaction();
                
                // make sure the entire parent tree exists
                node.setParent(parent);
                
                // make sure this leaf does not already exist
                if (getSingleNodeFromSelect(getSelectNodeByNameAndParentSQL(node)) != null)
                {
                    throw new NodeAlreadyExistsException(node.getPath());
                }
                
                KeyHolder keyHolder = new GeneratedKeyHolder();
                
                final String insertSQL = getInsertNodeSQL(node);

                jdbc.update(new PreparedStatementCreator() {
                    public PreparedStatement createPreparedStatement(Connection connection)
                        throws SQLException
                    {
                        PreparedStatement ps = connection.prepareStatement(insertSQL,
                            Statement.RETURN_GENERATED_KEYS);
                        return ps;
                    }}, keyHolder);
                
                Long generatedID = new Long(keyHolder.getKey().longValue());
                node.appData = new NodeID(generatedID);

                // insert the node properties
                Iterator<NodeProperty> propertyIterator = node.getProperties().iterator();
                while (propertyIterator.hasNext())
                {
                    NodeProperty next = propertyIterator.next();
                    if (!NodePropertyMapper.isNodeTableProperty(next))
                    {
                        jdbc.update(getInsertNodePropertySQL(node, next));
                    }
                }

                // Commit the transaction.
                commitTransaction();
                
                log.debug("Inserted new node: " + node);
                
                return node;
                
            }
            catch (Throwable t)
            {
                rollbackTransaction();
                log.error("Put rollback for node: " + node, t);
                if (t instanceof NodeNotFoundException)
                {
                    throw (NodeNotFoundException) t;
                }
                else if (t instanceof NodeAlreadyExistsException)
                {
                    throw (NodeAlreadyExistsException) t;
                }
                else
                {
                    throw new IllegalStateException(t);
                }
            }
        }
    }
    
    /**
     * Delete the node.  If deleteChildren is true, also recursively delete
     * any children of the node.
     * @param persistentNode The node to delete.  This node must
     * have been retrived by the NodePersistence interface.
     */
    public void delete(Node persistentNode, boolean deleteChildren) throws NodeNotFoundException
    {
        
        if (persistentNode == null)
        {
            throw new IllegalArgumentException("Cannot delete the root (null)");
        }
        
        if (persistentNode.appData == null)
        {
            throw new IllegalArgumentException("Node recevied on delete is not persistent.");
        }
        
        synchronized (this)
        {
            try
            {
                
                startTransaction();
                
                // delete the node properties
                jdbc.update(getDeleteNodePropertiesSQL(persistentNode));
                
                // delete the node
                jdbc.update(getDeleteNodeSQL(persistentNode));
                
                if (deleteChildren)
                {
                    // collect and delete children of the node
                    this.deleteChildrenOf(persistentNode);
                }
                
                commitTransaction();
                
            } catch (Throwable t)
            {
                rollbackTransaction();
                log.error("Delete rollback for node: " + persistentNode, t);
                if (t instanceof NodeNotFoundException)
                {
                    throw (NodeNotFoundException) t;
                }
                else
                {
                    throw new IllegalStateException(t);
                }
            }
            
        }
        log.debug("Node deleted: " + persistentNode);
    }
    
    public void markForDeletion(Node persistentNode, boolean markChildren) throws NodeNotFoundException
    {
        
        if (persistentNode == null)
        {
            throw new IllegalArgumentException("Cannot mark the root (null) as deleted");
        }
        
        if (persistentNode.appData == null)
        {
            throw new IllegalArgumentException("Node recevied on markMarkForDeletion is not persistent.");
        }
        
        synchronized (this)
        {
            try
            {
                
                startTransaction();
                
                // mark the node for deletion
                jdbc.update(getMarkNodeForDeletionSQL(persistentNode));
                
                if (markChildren)
                {
                    // collect and delete children of the node
                    this.markForDeletionChildrenOf(persistentNode, true);
                }
                
                commitTransaction();
                
            } catch (Throwable t)
            {
                rollbackTransaction();
                log.error("Mark for deletion rollback for node: " + persistentNode, t);
                if (t instanceof NodeNotFoundException)
                {
                    throw (NodeNotFoundException) t;
                }
                else
                {
                    throw new IllegalStateException(t);
                }
            }
            
        }
        log.debug("Node marked for deletion: " + persistentNode);
    }
    
    /**
     * Update the properties associated with this node.  New properties are added,
     * changed property values are updated, and properties marked for deletion are
     * removed.
     * @param updatedPersistentNode The node whos properties to update.  This node must
     * have been retrived by the NodePersistence interface.
     */
    public Node updateProperties(Node updatedPersistentNode) throws NodeNotFoundException
    {
        
        Node currentDbNode = getFromParent(updatedPersistentNode.getName(), updatedPersistentNode.getParent());
        
        synchronized (this)
        {
            try
            {
                
                startTransaction();
                
                // Iterate through the user properties and the db properties,
                // potentially updating, deleting or adding new ones

                for (NodeProperty nextProperty : updatedPersistentNode.getProperties())
                {
                    
                    // Does this property exist already?
                    if (currentDbNode.getProperties().contains(nextProperty))
                    {
                        if (nextProperty.isMarkedForDeletion())
                        {
                            // delete the property
                            log.debug("Deleting node property: " + nextProperty.getPropertyURI());
                            jdbc.update(getDeleteNodePropertySQL(currentDbNode, nextProperty));
                        }
                        else
                        {
                            // update the property value if it is different
                            int propertyIndex = currentDbNode.getProperties().indexOf(nextProperty);
                            String currentValue = currentDbNode.getProperties().get(propertyIndex).getPropertyValue();
                            if (!currentValue.equals(nextProperty.getPropertyValue()))
                            {
                                log.debug("Updating node property: " + nextProperty.getPropertyURI());
                                jdbc.update(getUpdateNodePropertySQL(currentDbNode, nextProperty));
                            }
                            else
                            {
                                log.debug("Not updating node property: " + nextProperty.getPropertyURI());
                            }
                        }
                    }
                    else
                    {
                        // insert the new property
                        log.debug("Inserting node property: " + nextProperty.getPropertyURI());
                        jdbc.update(getInsertNodePropertySQL(currentDbNode, nextProperty));
                    }
                }
                
                commitTransaction();
                
            } catch (Throwable t)
            {
                rollbackTransaction();
                log.error("Update rollback for node: " + updatedPersistentNode, t);
                if (t instanceof NodeNotFoundException)
                {
                    throw (NodeNotFoundException) t;
                }
                else
                {
                    throw new IllegalStateException(t);
                }
            }
            
        }
        
        log.debug("Node updated: " + currentDbNode);

        // return the new node from the database
        return getFromParent(updatedPersistentNode.getName(), updatedPersistentNode.getParent());

    }
    
    public void setBusyState(DataNode persistentNode, NodeBusyState state) throws NodeNotFoundException
    {
        synchronized (this)
        {
            try
            {
                
                startTransaction();
                
                // set the new state
                jdbc.update(getSetBusyStateSQL(persistentNode, state));
                
                commitTransaction();
                
            } catch (Throwable t)
            {
                rollbackTransaction();
                log.error("Set busy state rollback for node: " + persistentNode, t);
                if (t instanceof NodeNotFoundException)
                {
                    throw (NodeNotFoundException) t;
                }
                else
                {
                    throw new IllegalStateException(t);
                }
            }
            
        }
        log.debug("Node busy state updated for: " + persistentNode);
    }
    
    /**
     * Move the node to the new path
     * @throws UnsupporedOperationException Until implementation is complete.
     */
    public void move(Node node, String newPath)
    {
        throw new UnsupportedOperationException("Move not implemented.");
    }
    

    /**
     * Copy the node to the new path
     * @throws UnsupporedOperationException Until implementation is complete.
     */
    public void copy(Node node, String copyToPath)
    {
        throw new UnsupportedOperationException("Copy not implemented.");
    }
    
    /**
     * Return the single node matching the select statement or null if none
     * were found.
     * 
     * @param sql The SQL to execute
     * @return The single node or null.
     */
    protected Node getSingleNodeFromSelect(String sql)
    {
        log.debug("getSingleNodeFromSelect SQL: " + sql);

        List<Node> nodeList = jdbc.query(sql, new NodeMapper());

        if (nodeList.size() > 1)
        {
            throw new IllegalStateException("More than one node returned for SQL: "
                    + sql);
        }
        if (nodeList.size() == 1)
        {
            Node returnNode = (Node) nodeList.get(0);
            
            // get the children if this is a container node
            if (returnNode instanceof ContainerNode)
            {
                List<Node> children = jdbc.query(getSelectNodesByParentSQL(returnNode), new NodeMapper());
                ((ContainerNode) returnNode).setNodes(children);
            }
            
            // get the properties for the node
            List<NodeProperty> returnNodeProperties = jdbc.query(getSelectNodePropertiesByID(returnNode), new NodePropertyMapper());
            returnNode.getProperties().addAll(returnNodeProperties);
            
            return returnNode;
        }
        return null;
    }
    
    /**
     * Delete the children of the provided node.
     * @param node
     */
    protected void deleteChildrenOf(Node node)
    {
        List<Node> children = jdbc.query(getSelectNodesByParentSQL(node), new NodeMapper());
        for (Node next : children)
        {
            deleteChildrenOf(next);
        }
        jdbc.update(getDeleteNodePropertiesByParentSQL(node));
        jdbc.update(getDeleteNodesByParentSQL(node));
    }
    
    /**
     * Mark the children of the provided node for deletion
     * @param node
     */
    protected void markForDeletionChildrenOf(Node node, boolean root)
    {
        List<Node> children = jdbc.query(getSelectNodesByParentSQL(node), new NodeMapper());
        for (Node next : children)
        {
            markForDeletionChildrenOf(next, false);
        }
        if (!root)
        {
            jdbc.update(getMarkNodeForDeletionSQL(node));
        }
    }
    
    /**
     * The the nodeID of the provided node.
     * @param node
     * @return
     */
    protected Long getNodeID(Node node)
    {
        if (node == null || node.appData == null)
        {
            return null;
        }
        if (node.appData instanceof NodeID)
        {
            return ((NodeID) node.appData).getId();
        }
        return null;
    }
    
    /**
     * Return the value of the specified property.
     */
    protected String getPropertyValue(Node node, String propertyURI)
    {   
        final NodeProperty searchProperty = new NodeProperty(propertyURI, null);
        for (NodeProperty nodeProperty : node.getProperties())
        {
            if (nodeProperty.equals(searchProperty))
            {
                return nodeProperty.getPropertyValue();
            }
        }
        return null;
    }
    
    /**
     * @param node The node to query for.
     * @return The SQL string for finding the node in the database by
     * name and parentID.
     */
    protected String getSelectNodeByNameAndParentSQL(Node node)
    {
        String parentWhereClause = null;
        Long parentNodeID =  getNodeID(node.getParent());
        if (parentNodeID == null)
        {
            parentWhereClause = "(parentID is null or parentID = 0)";
        }
        else
        {
            parentWhereClause = "parentID = " + parentNodeID;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("select nodeID, parentID, name, type, busyState, markedForDeletion, owner, isPublic, groupRead, groupWrite, ");
        sb.append("contentLength, contentType, contentEncoding, contentMD5, createdOn, lastModified from ");
        sb.append(getNodeTableName());
        sb.append(" where name = '");
        sb.append(node.getName());
        sb.append("' and ");
        sb.append(parentWhereClause);
        if (!getNodesMarkedForDeletion)
        {
            sb.append(" and markedForDeletion = 0");
        }
        return sb.toString();
    }
    
    /**
     * @param parent The node to query for.
     * @return The SQL string for finding nodes given the parent. 
     */
    protected String getSelectNodesByParentSQL(Node parent)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("select nodeID, parentID, name, type, busyState, markedForDeletion, owner, isPublic, groupRead, groupWrite, ");
        sb.append("contentLength, contentType, contentEncoding, contentMD5, createdOn, lastModified from ");
        sb.append(getNodeTableName());
        sb.append(" where parentID = ");
        sb.append(getNodeID(parent));
        if (!getNodesMarkedForDeletion)
        {
            sb.append(" and markedForDeletion = 0");
        }
        return sb.toString();
    }
    
    /**
     * @param parent The node for which properties are queried.
     * @return The SQL string for finding the node properties. 
     */
    protected String getSelectNodePropertiesByID(Node node)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("select nodePropertyID, propertyURI, propertyValue from ");
        sb.append(getNodePropertyTableName());
        sb.append(" where nodeID = ");
        sb.append(getNodeID(node));
        return sb.toString();
    }
    
    /**
     * @param node The node to insert
     * @return The SQL string for inserting the node in the database.
     */
    protected String getInsertNodeSQL(Node node)
    {
        
        long contentLength = 0;
        String contentType = null;
        String contentEncoding = null;
        byte[] contentMD5 = null;
        String groupRead = null;
        String groupWrite = null;
        boolean isPublic = false;
        
        String contentLengthString = getPropertyValue(node, VOS.PROPERTY_URI_CONTENTLENGTH);
        contentType = getPropertyValue(node, VOS.PROPERTY_URI_TYPE);
        contentEncoding = getPropertyValue(node, VOS.PROPERTY_URI_CONTENTENCODING);
        String contentMD5String = getPropertyValue(node, VOS.PROPERTY_URI_CONTENTMD5);
        groupRead = getPropertyValue(node, VOS.PROPERTY_URI_GROUPREAD);
        groupWrite = getPropertyValue(node, VOS.PROPERTY_URI_GROUPWRITE);
        String isPublicString = getPropertyValue(node, VOS.PROPERTY_URI_ISPUBLIC);
        
        if (contentLengthString != null)
        {
            try
            {
                contentLength = new Long(contentLengthString);
            } catch (NumberFormatException e)
            {
                log.warn("Content length is not a number, continuing.");
            }
        }
        
        if (contentMD5String != null)
        {
            contentMD5 = HexUtil.toBytes(contentMD5String);
        }
        
        if (isPublicString != null)
        {
            if (isPublicString.trim().equalsIgnoreCase("true"))
            {
                isPublic = true;
            }
        }
        
        if (node.getOwner() == null)
        {
            throw new IllegalArgumentException("Node owner cannot be null.");
        }
        
        
        
        StringBuilder sb = new StringBuilder();
        sb.append("insert into ");
        sb.append(getNodeTableName());
        sb.append(" (");
        sb.append("parentID,");
        sb.append("name,");
        sb.append("type,");
        sb.append("owner,");
        sb.append("isPublic,");
        sb.append("groupRead,");
        sb.append("groupWrite,");
        sb.append("contentLength,");
        sb.append("contentType,");
        sb.append("contentEncoding,");
        sb.append("contentMD5");
        sb.append(") values (");
        sb.append(getNodeID(node.getParent()));
        sb.append(",'");
        sb.append(node.getName());
        sb.append("','");
        sb.append(NodeMapper.getDatabaseTypeRepresentation(node));
        sb.append("','");
        sb.append(node.getOwner());
        sb.append("',");
        sb.append((isPublic) ? "1" : "0");
        sb.append(",");
        sb.append((groupRead == null) ? null : "'" + groupRead + "'");
        sb.append(",");
        sb.append((groupWrite == null) ? null : "'" + groupWrite + "'");
        sb.append(",");
        sb.append(contentLength);
        sb.append(",");
        sb.append((contentType == null) ? null : "'" + contentType + "'");
        sb.append(",");
        sb.append((contentEncoding == null) ? null : "'" + contentEncoding + "'");
        sb.append(",");
        sb.append((contentMD5== null) ? null : "'" + contentMD5 + "'");
        sb.append(")");
        return sb.toString();
    }
    
    /**
     * @param node The node for the property
     * @param nodeProperty  The property of the node
     * @return The SQL string for inserting the node property in the database.
     */
    protected String getInsertNodePropertySQL(Node node,
            NodeProperty nodeProperty)
    {
        StringBuilder sb = new StringBuilder();
        String value = nodeProperty.getPropertyValue();
        if (nodeProperty.getPropertyURI().equals(VOS.PROPERTY_URI_CONTENTENCODING))
        {
            sb.append("update ");
            sb.append(getNodeTableName());
            sb.append(" set contentEncoding = ");
            sb.append((value == null) ? null : "'" + value + "'");
            sb.append(" where nodeID = ");
            sb.append(getNodeID(node));
        }
        else if (nodeProperty.getPropertyURI().equals(VOS.PROPERTY_URI_CONTENTLENGTH))
        {
            Long contentLength = new Long(value);
            sb.append("update ");
            sb.append(getNodeTableName());
            sb.append(" set contentLength = ");
            sb.append(contentLength);
            sb.append(" where nodeID = ");
            sb.append(getNodeID(node));
        }
        else if (nodeProperty.getPropertyURI().equals(VOS.PROPERTY_URI_CONTENTMD5))
        {
            sb.append("update ");
            sb.append(getNodeTableName());
            sb.append(" set contentMD5 = ");
            sb.append((value == null) ? null : ("0x" + value));
            sb.append(" where nodeID = ");
            sb.append(getNodeID(node));
        }
        else if (nodeProperty.getPropertyURI().equals(VOS.PROPERTY_URI_TYPE))
        {
            sb.append("update ");
            sb.append(getNodeTableName());
            sb.append(" set contentType = ");
            sb.append((value == null) ? null : "'" + value + "'");
            sb.append(" where nodeID = ");
            sb.append(getNodeID(node));
        }
        else if (nodeProperty.getPropertyURI().equals(VOS.PROPERTY_URI_GROUPREAD))
        {
            sb.append("update ");
            sb.append(getNodeTableName());
            sb.append(" set groupRead = ");
            sb.append((value == null) ? null : "'" + value + "'");
            sb.append(" where nodeID = ");
            sb.append(getNodeID(node));
        }
        else if (nodeProperty.getPropertyURI().equals(VOS.PROPERTY_URI_GROUPWRITE))
        {
            sb.append("update ");
            sb.append(getNodeTableName());
            sb.append(" set groupWrite = ");
            sb.append((value == null) ? null : "'" + value + "'");
            sb.append(" where nodeID = ");
            sb.append(getNodeID(node));
        }
        else if (nodeProperty.getPropertyURI().equals(VOS.PROPERTY_URI_ISPUBLIC))
        {
            int isPublic = -1;
            if (value != null)
            {
                if (value.trim().equalsIgnoreCase("true"))
                {
                    isPublic = 1;
                }
                if (value.trim().equalsIgnoreCase("false"))
                {
                    isPublic = 0;
                }
            }
            if (isPublic == -1)
            {
                log.warn("Unrecognized value for property " + VOS.PROPERTY_URI_ISPUBLIC);
                sb.append("update ");
                sb.append(getNodeTableName());
                sb.append(" set isPublic=isPublic");
                sb.append(" where nodeID = ");
                sb.append(getNodeID(node));
            }
            else
            {
                sb.append("update ");
                sb.append(getNodeTableName());
                sb.append(" set isPublic = ");
                sb.append(isPublic);
                sb.append(" where nodeID = ");
                sb.append(getNodeID(node));
            }
        }
        else
        {
            sb.append("insert into ");
            sb.append(getNodePropertyTableName());
            sb.append(" (");
            sb.append("nodeID,");
            sb.append("propertyURI,");
            sb.append("propertyValue");
            sb.append(") values (");
            sb.append(((NodeID) node.appData).getId());
            sb.append(",'");
            sb.append(nodeProperty.getPropertyURI());
            sb.append("','");
            sb.append(nodeProperty.getPropertyValue());
            sb.append("')");
        }
        return sb.toString();
    }
    
    /**
     * @param node The node to delete
     * @return The SQL string for deleting the node from the database.
     */
    protected String getDeleteNodeSQL(Node node)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("delete from ");
        sb.append(getNodeTableName());
        sb.append(" where nodeID = ");
        sb.append(getNodeID(node));
        return sb.toString();
    }
    
    /**
     * @param node Delete the properties of this node.
     * @return The SQL string for performing property deletion.
     */
    protected String getDeleteNodePropertiesSQL(Node node)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("delete from ");
        sb.append(getNodePropertyTableName());
        sb.append(" where nodeID = ");
        sb.append(getNodeID(node));
        return sb.toString();
    }
    
    /**
     * @param parent The parent who's children are to be deleted.
     * @return The SQL string to perform this deletion.
     */
    protected String getDeleteNodesByParentSQL(Node parent)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("delete from ");
        sb.append(getNodeTableName());
        sb.append(" where parentID = ");
        sb.append(getNodeID(parent));
        return sb.toString();
    }
    
    /**
     * @param parent Delete the properties of the children of this parent.
     * @return The SQL string for performing this deletion.
     */
    protected String getDeleteNodePropertiesByParentSQL(Node parent)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("delete from ");
        sb.append(getNodePropertyTableName());
        sb.append(" where nodeID in (select nodeID from ");
        sb.append(getNodeTableName());
        sb.append(" where parentID = ");
        sb.append(getNodeID(parent));
        sb.append(")");
        return sb.toString();
    }
    
    /**
     * @param node The node for the properties.
     * @param nodeProperty The property in question.
     * @return The SQL string for performing this deletion.
     */
    protected String getDeleteNodePropertySQL(Node node, NodeProperty nodeProperty)
    {
        StringBuilder sb = new StringBuilder();
        if (nodeProperty.getPropertyURI().equals(VOS.PROPERTY_URI_CONTENTENCODING))
        {
            sb.append("update ");
            sb.append(getNodeTableName());
            sb.append(" set contentEncoding = null where nodeID = ");
            sb.append(getNodeID(node));
        }
        else if (nodeProperty.getPropertyURI().equals(VOS.PROPERTY_URI_CONTENTLENGTH))
        {
            sb.append("update ");
            sb.append(getNodeTableName());
            sb.append(" set contentLength = null where nodeID = ");
            sb.append(getNodeID(node));
        }
        else if (nodeProperty.getPropertyURI().equals(VOS.PROPERTY_URI_CONTENTMD5))
        {
            sb.append("update ");
            sb.append(getNodeTableName());
            sb.append(" set contentMD5 = null where nodeID = ");
            sb.append(getNodeID(node));
        }
        else if (nodeProperty.getPropertyURI().equals(VOS.PROPERTY_URI_TYPE))
        {
            sb.append("update ");
            sb.append(getNodeTableName());
            sb.append(" set contentType = null where nodeID = ");
            sb.append(getNodeID(node));
        }
        else if (nodeProperty.getPropertyURI().equals(VOS.PROPERTY_URI_GROUPREAD))
        {
            sb.append("update ");
            sb.append(getNodeTableName());
            sb.append(" set groupRead = null where nodeID = ");
            sb.append(getNodeID(node));
        }
        else if (nodeProperty.getPropertyURI().equals(VOS.PROPERTY_URI_GROUPWRITE))
        {
            sb.append("update ");
            sb.append(getNodeTableName());
            sb.append(" set groupWrite = null where nodeID = ");
            sb.append(getNodeID(node));
        }
        else
        {
            sb.append("delete from ");
            sb.append(getNodePropertyTableName());
            sb.append(" where nodeID = ");
            sb.append(getNodeID(node));
            sb.append(" and propertyURI = '");
            sb.append(nodeProperty.getPropertyURI());
            sb.append("'");
        }
        return sb.toString();
    }
    
    /**
     * @param node The node for which the properties are to be updated.
     * @param nodeProperty The node property in question.
     * @return The SQL string for performing this update.
     */
    protected String getUpdateNodePropertySQL(Node node, NodeProperty nodeProperty)
    {
        StringBuilder sb = new StringBuilder();
        String value = nodeProperty.getPropertyValue();
        if (nodeProperty.getPropertyURI().equals(VOS.PROPERTY_URI_CONTENTENCODING))
        {
            sb.append("update ");
            sb.append(getNodeTableName());
            sb.append(" set contentEncoding = ");
            sb.append((value == null) ? null : "'" + value + "'");
            sb.append(" where nodeID = ");
            sb.append(getNodeID(node));
        }
        else if (nodeProperty.getPropertyURI().equals(VOS.PROPERTY_URI_CONTENTLENGTH))
        {
            Long contentLength = new Long(value);
            sb.append("update ");
            sb.append(getNodeTableName());
            sb.append(" set contentLength = ");
            sb.append(contentLength);
            sb.append(" where nodeID = ");
            sb.append(getNodeID(node));
        }
        else if (nodeProperty.getPropertyURI().equals(VOS.PROPERTY_URI_CONTENTMD5))
        {
            sb.append("update ");
            sb.append(getNodeTableName());
            sb.append(" set contentMD5 = ");
            sb.append((value == null) ? null : ("0x" + value));
            sb.append(" where nodeID = ");
            sb.append(getNodeID(node));
        }
        else if (nodeProperty.getPropertyURI().equals(VOS.PROPERTY_URI_TYPE))
        {
            sb.append("update ");
            sb.append(getNodeTableName());
            sb.append(" set contentType = ");
            sb.append((value == null) ? null : "'" + value + "'");
            sb.append(" where nodeID = ");
            sb.append(getNodeID(node));
        }
        else if (nodeProperty.getPropertyURI().equals(VOS.PROPERTY_URI_GROUPREAD))
        {
            sb.append("update ");
            sb.append(getNodeTableName());
            sb.append(" set groupRead = ");
            sb.append((value == null) ? null : "'" + value + "'");
            sb.append(" where nodeID = ");
            sb.append(getNodeID(node));
        }
        else if (nodeProperty.getPropertyURI().equals(VOS.PROPERTY_URI_GROUPWRITE))
        {
            sb.append("update ");
            sb.append(getNodeTableName());
            sb.append(" set groupWrite = ");
            sb.append((value == null) ? null : "'" + value + "'");
            sb.append(" where nodeID = ");
            sb.append(getNodeID(node));
        }
        else if (nodeProperty.getPropertyURI().equals(VOS.PROPERTY_URI_DATE))
        {
            // let the trigger update the lastModified field
            sb.append("update ");
            sb.append(getNodeTableName());
            sb.append(" set lastModified = lastModified");
            sb.append(" where nodeID = ");
            sb.append(getNodeID(node));
        }
        else if (nodeProperty.getPropertyURI().equals(VOS.PROPERTY_URI_ISPUBLIC))
        {
            int isPublic = -1;
            if (value != null)
            {
                if (value.trim().equalsIgnoreCase("true"))
                {
                    isPublic = 1;
                }
                if (value.trim().equalsIgnoreCase("false"))
                {
                    isPublic = 0;
                }
            }
            if (isPublic == -1)
            {
                log.warn("Unrecognized value for property " + VOS.PROPERTY_URI_ISPUBLIC);
                sb.append("update ");
                sb.append(getNodeTableName());
                sb.append(" set isPublic=isPublic");
                sb.append(" where nodeID = ");
                sb.append(getNodeID(node));
            }
            else
            {
                sb.append("update ");
                sb.append(getNodeTableName());
                sb.append(" set isPublic = ");
                sb.append(isPublic);
                sb.append(" where nodeID = ");
                sb.append(getNodeID(node));
            }
        }
        else
        {
            sb.append("update ");
            sb.append(getNodePropertyTableName());
            sb.append(" set propertyValue = '");
            sb.append(nodeProperty.getPropertyValue());
            sb.append("' where nodeID = ");
            sb.append(getNodeID(node));
            sb.append(" and propertyURI = '");
            sb.append(nodeProperty.getPropertyURI());
            sb.append("'");
        }
        return sb.toString();
    }
    
    protected String getMarkNodeForDeletionSQL(Node node)
    {
        // since we're appending the timestamp to the name, ensure
        // the combined length is ok
        String newNodeName = node.getName();
        int maximumNameLength = NODE_NAME_COLUMN_SIZE - MAX_TIMESTAMP_LENGTH;
        if (newNodeName.length() > maximumNameLength)
        {
            newNodeName = newNodeName.substring(0, maximumNameLength - 1);
        }
        newNodeName += "-" + new Date().toString();

        StringBuilder sb = new StringBuilder();
        sb.append("update ");
        sb.append(getNodeTableName());
        sb.append(" set");
        sb.append(" name='");
        sb.append(newNodeName);
        sb.append("',");
        sb.append(" markedForDeletion=1 where nodeID = ");
        sb.append(getNodeID(node));
        return sb.toString();
    }
    
    protected String getSetBusyStateSQL(DataNode node, NodeBusyState state)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("update ");
        sb.append(getNodeTableName());
        sb.append(" set busyState=' ");
        sb.append(state.getValue());
        sb.append("' where nodeID = ");
        sb.append(getNodeID(node));
        return sb.toString();
    }
    
}
