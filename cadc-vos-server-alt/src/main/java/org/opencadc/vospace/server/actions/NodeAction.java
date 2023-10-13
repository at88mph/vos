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

import ca.nrc.cadc.net.ResourceNotFoundException;
import ca.nrc.cadc.net.TransientException;
import ca.nrc.cadc.rest.InlineContentHandler;
import ca.nrc.cadc.rest.RestAction;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessControlException;
import javax.naming.Context;
import javax.naming.InitialContext;
import org.apache.log4j.Logger;
import org.opencadc.vospace.LinkingException;
import org.opencadc.vospace.Node;
import org.opencadc.vospace.VOSURI;
import org.opencadc.vospace.io.JsonNodeWriter;
import org.opencadc.vospace.io.NodeParsingException;
import org.opencadc.vospace.io.NodeReader;
import org.opencadc.vospace.io.NodeWriter;
import org.opencadc.vospace.server.LocalServiceURI;
import org.opencadc.vospace.server.NodeFault;
import org.opencadc.vospace.server.NodePersistence;
import org.opencadc.vospace.server.PathResolver;
import org.opencadc.vospace.server.auth.VOSpaceAuthorizer;

/**
 * Abstract class encapsulating the behaviour of an action on a Node.  Clients
 * must ensure that setVosURI(), setNodeXML(), setVOSpaceAuthorizer(), and
 * setNodePersistence() are called before using any concrete implementations of
 * this class.
 *
 * @author majorb
 * @author adriand
 */
public abstract class NodeAction extends RestAction {
    protected static Logger log = Logger.getLogger(NodeAction.class);

    private static final String DEFAULT_FORMAT = "text/xml";
    private static final String JSON_FORMAT = "application/json";
    
    private static final String INLINE_CONTENT_TAG = "client-node";

    // some subclasses may need to determine hostname, request path, etc
    protected VOSpaceAuthorizer voSpaceAuthorizer;
    protected NodePersistence nodePersistence;
    protected LocalServiceURI localServiceURI;

    protected NodeAction() {
        super();
    }

    @Override
    public void initAction() throws Exception {
        String jndiNodePersistence = super.appName + "-" + NodePersistence.class.getName();
        try {
            Context ctx = new InitialContext();
            this.nodePersistence = ((NodePersistence) ctx.lookup(jndiNodePersistence));
            this.voSpaceAuthorizer = new VOSpaceAuthorizer(nodePersistence);        
            localServiceURI = new LocalServiceURI(nodePersistence.getResourceID());
        } catch (Exception oops) {
            throw new RuntimeException("BUG: NodePersistence implementation not found with JNDI key " + jndiNodePersistence, oops);
        }

        checkReadable();
        if ((this instanceof CreateNodeAction) ||
                (this instanceof DeleteNodeAction) || (this instanceof UpdateNodeAction)) {
            checkWritable();
        }

    }
    
    @Override
    protected final InlineContentHandler getInlineContentHandler() {
        return new InlineNodeHandler(INLINE_CONTENT_TAG);
    }
    
    // the absolute URI constructed from the request path
    protected final VOSURI getTargetURI() {
        URI resourceID = nodePersistence.getResourceID();
        LocalServiceURI loc = new LocalServiceURI(resourceID);

        String nodePath = syncInput.getPath();        
        if (nodePath == null) {
            nodePath = nodePersistence.getRootNode().getName();
        } else {
            nodePath = "/" + nodePath;
        }

        String suri = loc.getVOSBase().getURI().toASCIIString() + nodePath;
        try {
            return new VOSURI(suri);
        } catch (URISyntaxException e) {
            throw new RuntimeException("BUG: VOSURI syntax: " + suri, e);
        }
    }
    
    // get the Node from the input document
    protected final Node getInputNode() {
        Object o = syncInput.getContent(INLINE_CONTENT_TAG);
        if (o != null) {
            NodeReader.NodeReaderResult r = (NodeReader.NodeReaderResult) o;
            return r.node;
        }
        return null;
    }
    
    // get the VOSURI from the input node document
    protected final VOSURI getInputURI() {
        Object o = syncInput.getContent(INLINE_CONTENT_TAG);
        if (o != null) {
            NodeReader.NodeReaderResult r = (NodeReader.NodeReaderResult) o;
            return r.vosURI;
        }
        return null;
    }
    
    protected String getMediaType() throws Exception {
        String mediaType = DEFAULT_FORMAT;
        if (syncInput.getParameter("Accept") != null) {
            mediaType = syncInput.getParameter("Accept");
            if (!DEFAULT_FORMAT.equalsIgnoreCase(mediaType) && !JSON_FORMAT.equalsIgnoreCase(mediaType)) {
                throw NodeFault.InvalidArgument.getStatus("Media type " + mediaType + " not supported");
            }
        }
        return mediaType;
    }

    protected NodeWriter getNodeWriter() throws Exception {
        String mt = getMediaType();
        if (JSON_FORMAT.equals(mt)) {
            return new JsonNodeWriter();
        }
        return new NodeWriter();
    }

    /*
    protected AbstractView getView() throws Exception {
        if (syncInput.getParameter(QUERY_PARAM_VIEW) == null) {
            return null;
        }

        URI viewReference = URI.create(syncInput.getParameter(QUERY_PARAM_VIEW));

        // the default view is the same as no view
        if (viewReference.equals(VOS.VIEW_DEFAULT)) {
            return null;
        }

        final Views views = new Views();
        AbstractView view = views.getView(viewReference);

        if (view == null) {
            throw new UnsupportedOperationException(
                    "No view configured matching reference: " + viewReference);
        }
        view.setNodePersistence(nodePersistence);
        view.setVOSpaceAuthorizer(voSpaceAuthorizer);

        return view;
    }
    */
}
