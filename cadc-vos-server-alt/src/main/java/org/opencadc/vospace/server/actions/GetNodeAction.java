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
 ************************************************************************
 */

package org.opencadc.vospace.server.actions;

import ca.nrc.cadc.auth.AuthenticationUtil;
import ca.nrc.cadc.auth.IdentityManager;
import ca.nrc.cadc.io.ResourceIterator;
import ca.nrc.cadc.net.ResourceNotFoundException;
import ca.nrc.cadc.net.TransientException;
import ca.nrc.cadc.rest.InlineContentHandler;
import ca.nrc.cadc.util.ObjectUtil;
import ca.nrc.cadc.util.StringUtil;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessControlException;
import java.security.AccessController;
import java.util.Iterator;
import java.util.List;
import javax.security.auth.Subject;
import org.apache.log4j.Logger;
import org.opencadc.vospace.ContainerNode;
import org.opencadc.vospace.LinkingException;
import org.opencadc.vospace.Node;
import org.opencadc.vospace.NodeProperty;
import org.opencadc.vospace.VOS;
import org.opencadc.vospace.VOSURI;
import org.opencadc.vospace.io.NodeWriter;
import org.opencadc.vospace.server.AbstractView;
import org.opencadc.vospace.server.PathResolver;
import org.opencadc.vospace.server.Utils;
import org.opencadc.vospace.server.Views;

/**
 * Class to perform the retrieval of a Node.
 *
 * @author majorb
 * @author adriand
 */
public class GetNodeAction extends NodeAction {

    protected static Logger log = Logger.getLogger(GetNodeAction.class);

    /**
     * Basic empty constructor.
     */
    public GetNodeAction() {
    }

    @Override
    protected InlineContentHandler getInlineContentHandler() {
        return null;
    }

    @Override
    protected Node getClientNode() {
        // No client node in a GET
        return null;
    }

    @Override
    public Node doAuthorizationCheck()
            throws AccessControlException, ResourceNotFoundException, LinkingException, TransientException {
        // resolve any container links
        PathResolver pathResolver = new PathResolver(nodePersistence, voSpaceAuthorizer);
        return pathResolver.getNode(nodePath, false);
    }

    @Override
    public void performNodeAction(Node clientNode, Node serverNode)
            throws URISyntaxException, TransientException, ResourceNotFoundException, IOException {
        long start;
        long end;

        if (serverNode instanceof ContainerNode) {
            ContainerNode node = (ContainerNode) serverNode;

            log.debug("node: " + node);

            // Sorting parameters
            String sortParam = syncInput.getParameter(QUERY_PARAM_SORT_KEY);
            URI sortParamURI = null;

            // Validate sortCol passed in against values in cadc-vos/vos.java
            if (sortParam != null) {
                sortParamURI = URI.create(sortParam);
                if ((sortParamURI != VOS.PROPERTY_URI_DATE)
                        && (sortParamURI != VOS.PROPERTY_URI_CONTENTLENGTH)) {
                    throw new IllegalArgumentException("Sort by " + sortParam + " not supported.");
                }
            }

            // Asc/Desc order parameter
            String sortOrderParam = syncInput.getParameter(QUERY_PARAM_ORDER_KEY);
            Boolean sortAsc = null;
            if (sortOrderParam != null) {
                if (sortOrderParam.equalsIgnoreCase("asc")) {
                    sortAsc = true;
                } else if (sortOrderParam.equalsIgnoreCase("desc")) {
                    sortAsc = false;
                } else {
                    throw new IllegalArgumentException("Sort order parameter should be asc | desc");
                }
            }

            String startPath = null;
            // Paging parameters
            String pageLimitString = syncInput.getParameter(QUERY_PARAM_LIMIT);
            String startURI = syncInput.getParameter(QUERY_PARAM_URI);

            // parse the pageLimit
            Integer pageLimit = null;
            if (pageLimitString != null) {
                try {
                    pageLimit = new Integer(pageLimitString);
                    if (pageLimit < 0) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("value for limit must be a positive integer.");
                }
            }

            // validate startURI
            if (StringUtil.hasText(startURI)) {
                startPath = new VOSURI(startURI).getPath();
                if (!nodePath.equals(Utils.getParentPath(startPath))) {
                    throw new IllegalArgumentException("uri parameter not a child of target uri.");
                }
            }

            // get the children as requested
            start = System.currentTimeMillis();
            // request for a subset of children
            if (sortParamURI == null && sortAsc == null) {
                IdentityManager im = AuthenticationUtil.getIdentityManager();
                ResourceIterator<Node> resourceIterator = nodePersistence.iterator(node, pageLimit, startURI);
                try {
                    while (resourceIterator.hasNext()) {
                        Node child = resourceIterator.next();
                        child.owner = im.toSubject(node.ownerID);
                        child.ownerDisplay = im.toDisplayString(node.owner);
                        node.getNodes().add(child);
                    }
                } finally {
                    resourceIterator.close();
                }
                resourceIterator.close();
            } else {
                throw new IllegalArgumentException("Alternate sorting options not supported (yet?).");
            }
            log.debug(String.format(
                    "Get children on resolveMetadata=[%b] returned [%s] nodes with startURI=[%s], pageLimit=[%s].",
                    resolveMetadata, node.getNodes().size(), startURI, pageLimit));

            end = System.currentTimeMillis();
            log.debug("nodePersistence.getChildren() elapsed time: " + (end - start) + "ms");

            if (VOS.Detail.max.getValue().equals(detailLevel)) {
                // add a property to child nodes if they are visible to
                // this request
                doTagChildrenAccessRights(node);
            }
        }

        start = System.currentTimeMillis();

        // get the properties if no detail level is specified (null) or if the
        // detail level is something other than 'min'.
        if (!VOS.Detail.min.getValue().equals(detailLevel)) {
            nodePersistence.getProperties(serverNode);

            if (VOS.Detail.max.getValue().equals(detailLevel)) {
                // to get here the node must have been readable so tag it as such
                doTagReadable(serverNode);
                if (isWritable(serverNode)) {
                    doTagWritable(serverNode);
                }
            }
        }

        end = System.currentTimeMillis();
        log.debug("nodePersistence.getProperties() elapsed time: " + (end - start) + "ms");

        AbstractView view;
        String viewReference = syncInput.getParameter(QUERY_PARAM_VIEW);
        try {
            view = getView();
        } catch (Exception ex) {
            log.error("failed to load view: " + viewReference, ex);
            // this should generate an InternalFault in NodeAction
            throw new RuntimeException("view was configured but failed to load: " + viewReference);
        }

        if (view == null) {
            // no view specified or found--return the xml representation
            final NodeWriter nodeWriter = getNodeWriter();
            nodeWriter.setStylesheetURL(getStylesheetURL());

            // clear the properties from server node if the detail
            // level is set to 'min'
            if (VOS.Detail.min.getValue().equals(detailLevel)) {
                serverNode.getProperties().clear();
            }

            //TODO if the request has gone through a link, change the
            // node paths back to be the 'unresolved path'
            //            if (!nodePath.equals(Utils.getPath(serverNode)))
            //            {
            //                log.debug("returning node paths back to one that include a link");
            //                //TODO - fix below
            //                unresolveNodePaths(new VOSURI(nodePath), serverNode);
            //            }
            fillAcceptsAndProvides(serverNode);
            syncOutput.setCode(200);
            syncOutput.setHeader("Content-Type", getMediaType());
            nodeWriter.write(localServiceURI.getURI(serverNode), serverNode, syncOutput.getOutputStream());
        } else {
            URL url = new URL(syncInput.getRequestURI());
            view.setNode(serverNode, viewReference, url);
            URL redirectURL = view.getRedirectURL();
            if (redirectURL != null) {
                syncOutput.setCode(HttpURLConnection.HTTP_SEE_OTHER);
                syncOutput.setHeader("Location", redirectURL);
            } else {
                // return a representation for the view
                String contentMD5 = view.getContentMD5();
                if (contentMD5 != null && (contentMD5.length() == 32)) {
                    //TODO is contentMD5 in hex format?
                    syncOutput.setDigest(URI.create("md5:" + contentMD5));
                }
                view.write(syncOutput.getOutputStream());
            }
        }
    }

    /**
     * Look for the stylesheet URL in the request context.
     *
     * @return The String URL of the stylesheet for this action. Null if no reference is provided.
     */
    public String getStylesheetURL() {
        log.debug("Stylesheet Reference is: " + stylesheetReference);
        if (stylesheetReference != null) {
            String scheme = URI.create(syncInput.getRequestURI()).getScheme();
            String server = URI.create(syncInput.getRequestURI()).getAuthority();
            StringBuilder url = new StringBuilder();
            url.append(scheme);
            url.append("://");
            url.append(server);
            if (!stylesheetReference.startsWith("/")) {
                url.append("/");
            }
            url.append(stylesheetReference);
            return url.toString();
        }
        return null;
    }

    private void unresolveNodePaths(VOSURI vosURI, Node node) {
        try {
            // change the target node
            ObjectUtil.setField(node, vosURI, "uri");

            // change any children
            if (node instanceof ContainerNode) {
                ContainerNode containerNode = (ContainerNode) node;
                VOSURI childURI;
                for (Node child : containerNode.getNodes()) {
                    childURI = new VOSURI(vosURI.toString() + "/" + child.getName());
                    ObjectUtil.setField(child, childURI, "uri");
                }
            }
        } catch (Exception e) {
            log.debug("failed to unresolve node paths", e);
            throw new RuntimeException(e);
        }
    }

    private void doTagChildrenAccessRights(ContainerNode cn) {
        for (final Node n : cn.getNodes()) {
            // to get to this point, the parent node must have been readable.
            // Need to check only the child node
            AccessControlContext acContext = AccessController.getContext();
            Subject subject = Subject.getSubject(acContext);
            if (voSpaceAuthorizer.hasSingleNodeReadPermission(n, subject)) {
                doTagReadable(n);
            }
            if (isWritable(n)) {
                doTagWritable(n);
            }
        }
    }

    private boolean isWritable(Node node) {
        AccessControlContext acContext = AccessController.getContext();
        Subject subject = Subject.getSubject(acContext);

        Iterator<Node> nodes = Utils.getNodeList(node).descendingIterator();
        while (nodes.hasNext()) {
            if (!voSpaceAuthorizer.hasSingleNodeWritePermission(nodes.next(), subject)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tag the given node with a 'readable' property to indicate that it is
     * viewable (readable) by the requester.
     *
     * @param n The Node to tag.
     */
    private void doTagReadable(final Node n) {
        final NodeProperty canReadProperty =
                new NodeProperty(VOS.PROPERTY_URI_READABLE,
                        Boolean.TRUE.toString());
        canReadProperty.readOnly = true;

        n.getProperties().add(canReadProperty);
    }

    /**
     * Tag the given node with a 'writable' property to indicate that it is
     * updatable (writable) by the requester.
     *
     * @param n The Node to tag.
     */
    private void doTagWritable(final Node n) {
        final NodeProperty canWriteProperty =
                new NodeProperty(VOS.PROPERTY_URI_WRITABLE,
                        Boolean.TRUE.toString());
        canWriteProperty.readOnly = true;

        n.getProperties().add(canWriteProperty);
    }

    private void fillAcceptsAndProvides(Node node) {
        Views views = new Views();
        List<AbstractView> viewList;
        try {
            viewList = views.getViews();
        } catch (Exception e) {
            log.error("Could not get view list: " + e.getMessage());
            return;
        }
        //TODO
        //        for (AbstractView view : viewList)
        //        {
        //            if (view.canAccept(node))
        //            {
        //                node.accepts.add(view.getURI());
        //            }
        //            if (view.canProvide(node))
        //            {
        //                node.provides.add(view.getURI());
        //            }
        //        }

    }

}
