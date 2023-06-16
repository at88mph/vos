/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2023.                            (c) 2023.
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

package org.opencadc.vospace.server;

import ca.nrc.cadc.net.TransientException;
import org.opencadc.vospace.ContainerNode;
import org.opencadc.vospace.DataNode;
import org.opencadc.vospace.LinkNode;
import org.opencadc.vospace.LinkingException;
import org.opencadc.vospace.Node;
import org.opencadc.vospace.NodeNotFoundException;
import org.opencadc.vospace.VOSURI;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.security.auth.Subject;
import org.apache.log4j.Logger;
import org.opencadc.vospace.server.auth.VOSpaceAuthorizer;

/**
 * Utility class to follow and resolve the target of link nodes in a local vospace.
 * 
 * If the last node on the path is a link node, it will not be resolved.
 * 
 * @author adriand
 * @author majorb
 *
 */
public class PathResolver
{
    
    protected static final Logger LOG = Logger.getLogger(PathResolver.class);
    
    private NodePersistence nodePersistence;
    private VOSpaceAuthorizer voSpaceAuthorizer;
    private List<String> visitedPaths;
    private boolean resolveMetadata;
    
    // enforce a maximum visit limit to prevent stack overflows on
    // the recursive method
    private static final int VISIT_LIMIT_MAX = 40;
    private int visitLimit = 20;
    private int visitCount = 0;


    public PathResolver(NodePersistence nodePersistence, VOSpaceAuthorizer voSpaceAuthorizer) {
        this.nodePersistence = nodePersistence;
        this.voSpaceAuthorizer = voSpaceAuthorizer;
    }

    /**
     * Constructor.
     * 
     * @param nodePersistence node persistence to use
     */
    public PathResolver(NodePersistence nodePersistence)
    {
        this(nodePersistence, true);
    }

    public PathResolver(NodePersistence nodePersistence, boolean resolveMetadata)
    {
        if (nodePersistence == null)
        {
            throw new IllegalArgumentException("null node persistence.");
        }
        this.nodePersistence = nodePersistence;
        this.resolveMetadata = resolveMetadata;
    }


    /**
     * Resolves a node URI following the links and returns the end node
     * @param nodeURI
     * @param writable
     * @return
     * @throws NodeNotFoundException
     */
    public Node getNode(VOSURI nodeURI, boolean writable) throws NodeNotFoundException {
        Node node = nodePersistence.getRootNode();
        AccessControlContext acContext = AccessController.getContext();
        Subject subject = Subject.getSubject(acContext);

        voSpaceAuthorizer.checkServiceStatus(writable);

        Iterator<String> pathIter = Arrays.stream(nodeURI.getPath().split("/")).iterator();
        while (pathIter.hasNext()){
            node = nodePersistence.get((ContainerNode) node, pathIter.next());
            if (writable) {
                voSpaceAuthorizer.hasSingleNodeWritePermission(node, subject);
            } else {
                voSpaceAuthorizer.hasSingleNodeReadPermission(node, subject);
            }

            while (node instanceof LinkNode) {
                LOG.debug("Resolving link node " + node.getPath());
                if (visitCount > visitLimit)
                {
                    throw new IllegalArgumentException("Exceeded link limit.");
                }
                visitCount++;
                LOG.debug("visit number " + visitCount);

                LinkNode linkNode = (LinkNode) node;
                PathResolver.validateTargetURI(linkNode);
                // follow the link
                node = getNode(new VOSURI(linkNode.getTarget()), writable);
            }
            if (node instanceof ContainerNode) {
                continue;
            }
            if (node instanceof DataNode) {
                if (pathIter.hasNext()) {
                    throw new IllegalArgumentException("Illegal path"); //TODO - different exception
                }
            }
        }

        //TODO - is this still required?
        // HACK: this causes a query string embedded in the VOSURI (eg within LinkNode)
        // to be tacked onto the DataNode and added to the resulting data URL... TBD.
        if (nodeURI.getQuery() != null && node instanceof DataNode)
        {
            String fragment = null;
            if (nodeURI.getFragment() != null) {
                //TODO not sure how to pass the query and the fragment around with the node
                throw new UnsupportedOperationException("TODO");
                //fragment = uri.getFragment();

//            try
//            {
//                VOSURI nodeURI = LocalServiceURI.getURI(node);
//                URI queryUri = new URI(nodeURI.getScheme(),
//                                       nodeURI.getAuthority(),
//                                       nodeURI.getPath(),
//                                       uri.getQuery(),
//                                       fragment);
//                Node dataNode = new DataNode(new VOSURI(queryUri), node.properties);
//                dataNode.accepts.addAll(node.accepts);
//                dataNode.provides.addAll(node.provides);
//                dataNode.parent = node.parent;
//                dataNode.setName(node.getName());
//                return dataNode;
//            }
//            catch (URISyntaxException e)
//            {
//                throw new LinkingException("Unable to append query part to " + node.getUri());
//            }
            }
        }
        LOG.debug("returning node: " + node.getPath());
        return node;
    }


    /**
     * Return a new VOSURI representing the target URI of the link node.
     * 
     * @param linkNode node to validate
     * @return A VOSURI of the target of the link node.
     * @throws LinkingException If the target is non vospace, not local, or
     * an invalid URI.
     */
    public static VOSURI validateTargetURI(LinkNode linkNode)
    {
        VOSURI nodeURI = LocalServiceURI.getURI(linkNode);
        URI targetURI = linkNode.getTarget();

        LOG.debug("Validating target: " + targetURI.toASCIIString());
        // check the scheme
        if (nodeURI.getScheme() == null ||
                targetURI.getScheme() == null ||
                !nodeURI.getScheme().equals(targetURI.getScheme()))
        {
            throw new IllegalArgumentException("Unsupported link target: " + targetURI);
        }
        
        try
        {
            VOSURI returnURI = new VOSURI(targetURI);
            
            String nodeAuth = nodeURI.getAuthority();
            String targetAuth = returnURI.getAuthority();
            
            if (nodeAuth == null || targetAuth == null)
            {
                throw new LinkingException("Non-local VOSpace target: " + targetURI);
            }
            
            nodeAuth = nodeAuth.replace('~', '!');
            targetAuth = targetAuth.replace('~', '!');
            
            // authorities must match
            if (!nodeAuth.equals(targetAuth))
            {                
                throw new LinkingException("Non-local VOSpace target: " + targetURI);
            }
            
            return returnURI;
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Invalid linking target URI: " + targetURI, e);
        }
    }
    
    /**
     * Set the limit for number of link reference resolutions.
     * Default The value is 20.
     * @param visitLimit link reference resolutions
     */
    public void setVisitLimit(int visitLimit)
    {
        if (visitLimit > VISIT_LIMIT_MAX)
        {
            throw new IllegalArgumentException(
                    "Too high a visit limit.  Must be below " + VISIT_LIMIT_MAX);
        }
        this.visitLimit = visitLimit;
    }

}

//
//
//    /**
//     * Resolve the path of the node identified by parameter uri.
//     *
//     * @param uri node URI
//     * @return node instance
//     * @throws NodeNotFoundException If node not found
//     * @throws LinkingException If node link exception
//     * @throws TransientException If a transient error occurs
//     */
//public Node resolve(VOSURI uri) throws NodeNotFoundException, LinkingException, TransientException
//{
//    return resolveWithReadPermissionCheck(uri, null, false);
//}
//
//    public Node resolveWithReadPermissionCheck(VOSURI uri,
//                                               VOSpaceAuthorizer readAuthorizer,
//                                               boolean resolveLeafNodes)
//            throws NodeNotFoundException, LinkingException, TransientException
//    {
//        visitCount = 0;
//        visitedPaths = new ArrayList<>();
//        Node n = doResolve(uri, readAuthorizer);
//        if (resolveLeafNodes)
//            return resolveLeafNodeWithReadPermissionCheck(uri, n, readAuthorizer);
//        return n;
//    }
//
//    /**
//     * Resolve the path/node identified by parameter uri.  Check read permission on
//     * each link node resolution. This method expects that the argument node was
//     * returned from resolveWithReadPermissionCheck with resolveLeafNodes == false.
//     *
//     * @param uri leaf node identifier to resolve
//     * @param node node
//     * @return resolved node
//     * @throws NodeNotFoundException If node is not found
//     * @throws LinkingException If linked URI is invalid
//     * @throws TransientException If a transient error occurs
//     */
//    public Node resolveLeafNodeWithReadPermissionCheck(VOSURI uri, Node node,
//                                                       VOSpaceAuthorizer readAuthorizer)
//            throws NodeNotFoundException, LinkingException, TransientException
//    {
//
//        // resolve the leaf linknodes
//        while (node instanceof LinkNode)
//        {
//            LinkNode linkNode = (LinkNode) node;
//            PathResolver.validateTargetURI(linkNode);
//            // follow the link
//            node = doResolve(new VOSURI(linkNode.getTarget()), readAuthorizer);
//        }
//
//
//        return node;
//    }
//
//
//    /**
//     * Resolves a node URI following potential links
//     * @param vosuri requested path
//     * @param readAuthorizer authorizer to use
//     * @return resolved node
//     * @throws NodeNotFoundException If node not found
//     * @throws LinkingException If a transient error occurs
//     */
//    private Node doResolve(VOSURI vosuri,  VOSpaceAuthorizer readAuthorizer)
//            throws NodeNotFoundException, LinkingException, TransientException
//    {
//        if (visitCount > visitLimit)
//        {
//            throw new LinkingException("Exceeded link limit.");
//        }
//        visitCount++;
//        LOG.debug("visit number " + visitCount);
//
//        Node node;
//        if (readAuthorizer != null)
//        {
//            try
//            {
//                node = (Node) readAuthorizer.getReadPermission(vosuri.getURI());
//            }
//            catch(FileNotFoundException ex)
//            {
//                throw new NodeNotFoundException("not found: " + vosuri);
//            }
//        }
//        else
//        {
//            node = nodePersistence.get(vosuri, true, resolveMetadata);
//        }
//        LOG.debug("found node: " + node.getPath());
//
//        // extract the paths
//        String requestedPath = vosuri.getPath();
//        String nodePath = node.getPath();
//        LOG.debug("[requested][resulting] path: [" + requestedPath + "][" + nodePath + "]");
//
//        // check circular reference
//        if (visitedPaths.contains(nodePath))
//        {
//            LOG.debug("Found circular link: " + nodePath);
//            throw new LinkingException("Circular link reference: " + nodePath);
//        }
//        visitedPaths.add(nodePath);
//        LOG.debug("Added " + nodePath + " to visited list.");
//
//        if (!requestedPath.equals(nodePath))
//        {
//            if (!(node instanceof LinkNode))
//            {
//                throw new NodeNotFoundException(vosuri.toString());
//            }
//            LinkNode linkNode = (LinkNode) node;
//            String remainingPath = requestedPath.substring(nodePath.length());
//            LOG.debug("remainingPath: " + remainingPath);
//
//            VOSURI linkURI = validateTargetURI(linkNode);
//            LOG.debug("linkURI: " + linkURI.toString());
//            try
//            {
//                VOSURI resolvedURI = new VOSURI(linkURI.toString() + remainingPath);
//                LOG.debug("resolvedURI: " + resolvedURI.toString());
//                return doResolve(resolvedURI, readAuthorizer);
//            }
//            catch (URISyntaxException e)
//            {
//                throw new LinkingException("Invalid link URI");
//            }
//        }
//        LOG.debug("returning node: " + node.getPath());
//        return node;
//    }
// */
