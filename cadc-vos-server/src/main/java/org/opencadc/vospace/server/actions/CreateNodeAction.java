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
import ca.nrc.cadc.auth.AuthorizationTokenPrincipal;
import ca.nrc.cadc.auth.HttpPrincipal;
import ca.nrc.cadc.auth.IdentityManager;
import ca.nrc.cadc.net.HttpTransfer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.security.auth.Subject;
import org.apache.log4j.Logger;
import org.opencadc.vospace.ContainerNode;
import org.opencadc.vospace.Node;
import org.opencadc.vospace.NodeProperty;
import org.opencadc.vospace.VOS;
import org.opencadc.vospace.VOSURI;
import org.opencadc.vospace.io.NodeWriter;
import org.opencadc.vospace.server.NodeFault;
import org.opencadc.vospace.server.PathResolver;
import org.opencadc.vospace.server.Utils;

/**
 * Class to perform the creation of a Node action.
 *
 * @author majorb
 * @author adriand
 */
public class CreateNodeAction extends NodeAction {

    protected static Logger log = Logger.getLogger(CreateNodeAction.class);

    @Override
    public void doAction() throws Exception {
        final Node clientNode = getInputNode();
        if (clientNode == null) {
            throw NodeFault.InvalidArgument.getStatus("no node document");
        }
        final VOSURI clientNodeURI = getInputURI();
        final VOSURI target = getTargetURI();
        
        // validate doc vos path because some redundancy in API
        if (!target.equals(clientNodeURI)) {
            throw NodeFault.InvalidURI.getStatus("invalid input: vos URI mismatch: doc=" + clientNodeURI + " and path=" + target);
        }
        
        // get parent container node
        // TBD: resolveLinks=true?
        PathResolver pathResolver = new PathResolver(nodePersistence, voSpaceAuthorizer);
        Node serverNode = pathResolver.getNode(target.getParentURI().getPath(), true);
        if (!(serverNode instanceof ContainerNode)) {
            throw NodeFault.ContainerNotFound.getStatus(clientNodeURI.toString());
        }

        ContainerNode parent = (ContainerNode) serverNode;
        Node cur = nodePersistence.get(parent, target.getName());
        if (cur != null) {
            throw NodeFault.DuplicateNode.getStatus(clientNodeURI.toString());
        }

        Subject caller = AuthenticationUtil.getCurrentSubject();
        if (!voSpaceAuthorizer.hasSingleNodeWritePermission(parent, caller)) {
            throw NodeFault.PermissionDenied.getStatus(clientNodeURI.toString());
        }

        // attempt to set owner
        IdentityManager im = AuthenticationUtil.getIdentityManager();
        clientNode.parent = parent;

        // Ensure the caller is set.  It MAY be overridden below.
        clientNode.owner = caller;

        // Extract here to remove later.
        NodeProperty creatorJWT = clientNode.getProperty(VOS.PROPERTY_URI_CREATOR_JWT);
        if (Utils.isAdmin(caller, nodePersistence)) {
            if (creatorJWT != null && clientNode.ownerDisplay != null) {
                // The "creator" property clashes with the "creator-jwt" property.
                throw NodeFault.InvalidArgument.getStatus("BUG: " + VOS.PROPERTY_URI_CREATOR + " property already exists, but " + VOS.PROPERTY_URI_CREATOR_JWT
                                                              + " is present. This should not happen.");
            }

            if (clientNode.ownerDisplay != null) {
                // admin is allowed to assign a different owner
                try {
                    Subject tmp = new Subject();
                    // HACK: known IdentityManager implementations prefer the HttpPrincipal aka
                    // network username for toDisplayString(Subject) so that's the value that
                    // normally gets put into node.ownerDisplay and rendered in node documents
                    // so choosing HttpPrincipal here makes output and input (set owner) use the
                    // same values... but seems kind of sketchy.
                    tmp.getPrincipals().add(new HttpPrincipal(clientNode.ownerDisplay));
                    clientNode.owner = im.augment(tmp);
                } catch (Exception ex) {
                    log.error("failed to map " + clientNode.ownerDisplay + " to a known user", ex);
                    throw ex;
                }
            } else if (creatorJWT != null) {
                log.debug("Creating user allocation on behalf of OIDC account.");
                Subject nodeOwner = new Subject();
                final String token = creatorJWT.getValue();
                final AuthorizationTokenPrincipal authorizationTokenPrincipal = new AuthorizationTokenPrincipal(AuthenticationUtil.AUTHORIZATION_HEADER,
                                                                                                                AuthenticationUtil.CHALLENGE_TYPE_BEARER
                                                                                                                    + " " + token);

                nodeOwner.getPrincipals().add(authorizationTokenPrincipal);

                nodeOwner = AuthenticationUtil.validateSubject(nodeOwner);
                nodeOwner = AuthenticationUtil.augmentSubject(nodeOwner);

                log.debug("Setting new allocation node owner to " + im.toDisplayString(nodeOwner));
                clientNode.owner = nodeOwner;
                clientNode.ownerID = null; // just in case

                log.debug("Creating user allocation on behalf of OIDC account: OK");
            }
        }

        // Ensure it's always removed after being handled in the admin block.  This property is never persisted, as it's just a means to an end for the
        // creator property.
        if (creatorJWT != null) {
            clientNode.getProperties().remove(creatorJWT);
        }

        // inherit
        if (parent.inheritPermissions != null && parent.inheritPermissions) {
            // explicit clientNode settings override inherit
            if (clientNode.isPublic == null) {
                clientNode.isPublic = parent.isPublic;
            }
            if (clientNode.getReadOnlyGroup().isEmpty()) {
                clientNode.getReadOnlyGroup().addAll(parent.getReadOnlyGroup());
            }
            if (clientNode.getReadWriteGroup().isEmpty()) {
                clientNode.getReadWriteGroup().addAll(parent.getReadWriteGroup());
            }
            if (clientNode instanceof ContainerNode) {
                ContainerNode cn = (ContainerNode) clientNode;
                if (cn.inheritPermissions == null) {
                    cn.inheritPermissions = parent.inheritPermissions;
                }
            }
        } else {
            // null is equivalent to false, but always set/persist this??
            if (clientNode.isPublic == null) {
                clientNode.isPublic = false;
            }
            if (clientNode instanceof ContainerNode) {
                ContainerNode cn = (ContainerNode) clientNode;
                if (cn.inheritPermissions == null) {
                    cn.inheritPermissions = false;
                }
            }
        }

        // pick out eligible admin-only props (they are immutable to normal users)
        final List<NodeProperty> allowedAdminProps = Utils.getAdminProps(clientNode, nodePersistence.getAdminProps(), caller,
                nodePersistence);
        
        // sanitize input properties into clean set
        Set<NodeProperty> np = new HashSet<>();
        Utils.updateNodeProperties(np, clientNode.getProperties(), nodePersistence.getImmutableProps());
        clientNode.getProperties().clear();
        clientNode.getProperties().addAll(np);
        clientNode.getProperties().addAll(allowedAdminProps);

        log.debug("Putting node " + target.getName() + " in path " + target.getPath());
        Node storedNode = nodePersistence.put(clientNode);
        storedNode.ownerDisplay = im.toDisplayString(storedNode.owner);
        
        // output modified node
        NodeWriter nodeWriter = getNodeWriter();
        syncOutput.setCode(201);
        syncOutput.setHeader(HttpTransfer.CONTENT_TYPE, getMediaType());
        // TODO: should the VOSURI in the output target or actual? eg resolveLinks=true
        nodeWriter.write(localServiceURI.getURI(storedNode), storedNode, syncOutput.getOutputStream(), VOS.Detail.max);
    }
}
