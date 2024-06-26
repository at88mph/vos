/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2022.                            (c) 2022.
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

package org.opencadc.cavern.files;

import ca.nrc.cadc.auth.AuthenticationUtil;
import ca.nrc.cadc.io.ByteCountOutputStream;
import ca.nrc.cadc.io.ByteLimitExceededException;
import ca.nrc.cadc.io.MultiBufferIO;
import ca.nrc.cadc.io.WriteException;
import ca.nrc.cadc.net.ResourceNotFoundException;
import ca.nrc.cadc.net.TransientException;
import ca.nrc.cadc.rest.InlineContentException;
import ca.nrc.cadc.rest.InlineContentHandler;
import ca.nrc.cadc.util.HexUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.AccessControlException;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import javax.security.auth.Subject;
import org.apache.log4j.Logger;
import org.opencadc.permissions.WriteGrant;
import org.opencadc.vospace.ContainerNode;
import org.opencadc.vospace.DataNode;
import org.opencadc.vospace.Node;
import org.opencadc.vospace.NodeProperty;
import org.opencadc.vospace.VOS;
import org.opencadc.vospace.VOSURI;

/**
 * @author pdowler
 * @author majorb
 * @author jeevesh
 */
public class PutAction extends FileAction {
    private static final Logger log = Logger.getLogger(PutAction.class);

    private static final String INPUT_STREAM = "in";

    public PutAction() {
        super();
    }

    private static Long getLimit(DataNode node) {
        if (node == null) {
            return null;
        } else {
            Long limit = null;
            ContainerNode curNode = node.parent;

            // Walk up the parent tree to find a Node with the Quota property.
            while (curNode != null && limit == null) {
                String limitValue = curNode.getPropertyValue(VOS.PROPERTY_URI_QUOTA);
                if (limitValue != null) {
                    // TODO: Handle if bytesUsed is null?  Check PROPERTY_URI_CONTENTLENGTH?
                    limit = Long.parseLong(limitValue) - (curNode.bytesUsed == null ? 0L : curNode.bytesUsed);
                }
                curNode = curNode.parent;
            }
            return limit;
        }
    }

    @Override
    protected InlineContentHandler getInlineContentHandler() {
        return new InlineContentHandler() {
            public Content accept(String name, String contentType,
                                  InputStream inputStream)
                throws InlineContentException, IOException {
                InlineContentHandler.Content c = new InlineContentHandler.Content();
                c.name = INPUT_STREAM;
                c.value = inputStream;
                return c;
            }
        };
    }

    @Override
    public void doAction() throws Exception {
        VOSURI nodeURI = getNodeURI();
        DataNode node = null;
        Path target = null;
        boolean putStarted = false;
        boolean successful = false;

        long bytesWritten = 0L;
        try {
            log.debug("put: start " + nodeURI.getURI().toASCIIString());

            Subject caller = AuthenticationUtil.getCurrentSubject();
            boolean preauthGranted = false;
            if (preauthToken != null) {
                CavernURLGenerator cav = new CavernURLGenerator(nodePersistence);
                Object tokenUser = cav.validateToken(preauthToken, nodeURI, WriteGrant.class);
                preauthGranted = true;
                caller.getPrincipals().clear();
                if (tokenUser != null) {
                    Subject s = identityManager.toSubject(tokenUser);
                    caller.getPrincipals().addAll(s.getPrincipals());
                }
                // reset loggables
                logInfo.setSubject(caller);
                logInfo.setResource(nodeURI.getURI());
                logInfo.setPath(syncInput.getContextPath() + syncInput.getComponentPath());
                logInfo.setGrant("read: preauth-token");
            }
            log.debug("preauthGranted: " + preauthGranted);

            // PathResolver checks read permission
            String parentPath = nodeURI.getParentURI().getPath();
            // TODO: disable permission checks in resolver
            Node n = pathResolver.getNode(parentPath, true);
            if (n == null) {
                throw new ResourceNotFoundException("not found: parent container " + parentPath);
            }
            if (!(n instanceof ContainerNode)) {
                throw new IllegalArgumentException("parent is not a container node");
            }
            ContainerNode cn = (ContainerNode) n;
            n = nodePersistence.get(cn, nodeURI.getName());

            // only support data nodes for now
            if (n != null && !(DataNode.class.isAssignableFrom(n.getClass()))) {
                throw new IllegalArgumentException("not a data node");
            }
            node = (DataNode) n;
            if (node == null) {
                log.warn("target node: " + node + ": creating");
                node = new DataNode(nodeURI.getName());
                node.owner = caller;
                node.parent = cn;
                nodePersistence.put(node);
            }

            // check write permission
            if (!preauthGranted) {
                if (n != null && authorizer.hasSingleNodeWritePermission(n, AuthenticationUtil.getCurrentSubject())) {
                    log.debug("authorized to write to existing data node");
                } else if (authorizer.hasSingleNodeWritePermission(cn, AuthenticationUtil.getCurrentSubject())) {
                    log.debug("authorized to write to parent container");
                } else {
                    throw new AccessControlException("permission denied: write to " + nodeURI.getPath());
                }
            }

            target = nodePersistence.nodeToPath(nodeURI);

            InputStream in = (InputStream) syncInput.getContent(INPUT_STREAM);
            MessageDigest md = MessageDigest.getInstance("MD5");
            log.debug("copy: start " + target);
            putStarted = true;
            // this method replace sthe existing Node with a new one owned by the tomcat user (root)
            // and that only gets fixed down at nodePersistence.put(node);
            //Files.copy(vis, target, StandardCopyOption.REPLACE_EXISTING);

            // truncate: do not recreate file with wrong owner
            StandardOpenOption openOption = StandardOpenOption.TRUNCATE_EXISTING;
            DigestOutputStream out = new DigestOutputStream(
                Files.newOutputStream(target, StandardOpenOption.WRITE, openOption), md);
            ByteCountOutputStream bcos = new ByteCountOutputStream(out);
            MultiBufferIO io = new MultiBufferIO();
            io.copy(in, bcos);
            bcos.flush();
            log.debug("copy: done " + target);
            bytesWritten = bcos.getByteCount();

            URI expectedMD5 = syncInput.getDigest();
            byte[] md5 = md.digest();
            String propValue = HexUtil.toHex(md5);
            URI actualMD5 = URI.create("md5:" + propValue);
            if (expectedMD5 != null && !expectedMD5.equals(actualMD5)) {
                // upload failed: do not keep corrupt data
                log.debug("upload corrupt: " + expectedMD5 + " != " + propValue);
                OutputStream trunc = Files.newOutputStream(target, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                trunc.close();
                actualMD5 = null;
            }

            // re-read node from filesystem
            node = (DataNode) nodePersistence.get(cn, nodeURI.getName());

            // update Node
            node.owner = caller;
            node.ownerID = null; // just in case

            log.debug(nodeURI + " MD5: " + propValue);
            NodeProperty csp = node.getProperty(VOS.PROPERTY_URI_CONTENTMD5);
            if (actualMD5 == null) {
                // upload failed
                if (csp != null) {
                    node.getProperties().remove(csp);
                }
            } else {
                if (csp == null) {
                    // add
                    csp = new NodeProperty(VOS.PROPERTY_URI_CONTENTMD5, actualMD5.getSchemeSpecificPart());
                    node.getProperties().add(csp);
                } else {
                    // replace
                    csp.setValue(actualMD5.toASCIIString());
                }
            }
            // set/update content-type attr
            String contentType = syncInput.getHeader("content-type");
            if (contentType != null) {
                NodeProperty ctp = node.getProperty(VOS.PROPERTY_URI_TYPE);
                if (ctp == null) {
                    ctp = new NodeProperty(VOS.PROPERTY_URI_TYPE, contentType);
                    node.getProperties().add(ctp);
                } else {
                    ctp.setValue(contentType);
                }
            }

            nodePersistence.put(node);
            syncOutput.setHeader("content-length", 0); // empty response
            if (contentType != null) {
                syncOutput.setHeader("content-type", contentType);
            }

            if (actualMD5 != null) {
                syncOutput.setDigest(actualMD5);
            }
            syncOutput.setCode(201);
            successful = true;
        } catch (AccessDeniedException e) {
            // TODO: this is a deployment error because cavern doesn't have permission to filesystem
            log.debug("403 error with PUT: ", e);
            throw new AccessControlException(e.getLocalizedMessage());

        } catch (WriteException ex) {
            final Throwable cause = ex.getCause();
            if (cause != null && cause.getMessage().contains("quota")) {
                final Long limit = PutAction.getLimit(node);
                if (limit == null) {
                    // VOS.PROPERTY_URI_QUOTA attribute is not set on any parent node
                    log.warn("VOS.PROPERTY_URI_QUOTA attribute not set, " + ex.getMessage());
                } else {
                    throw new ByteLimitExceededException(cause.getMessage().trim(), limit);
                }
            }
            throw ex;
        } finally {
            if (bytesWritten > 0L) {
                logInfo.setBytes(bytesWritten);
            }
            if (successful) {
                log.debug("put: done " + nodeURI.getURI().toASCIIString());
            } else if (putStarted) {
                cleanupOnFailure(target, node);
            }
        }
    }

    private void cleanupOnFailure(Path target, DataNode node) {
        log.debug("clean up on put failure " + target);
        if (node != null) {
            try {
                nodePersistence.delete(node);
            } catch (TransientException bug) {
                log.error("Unable to clean up " + target + "\n" + bug.getMessage(), bug);
            }
        }
    }
    
    /*
    private void cleanUpOnFailure(Path target, DataNode node, Path rootPath) throws IOException {
        log.debug("clean up on put failure " + target);
        Files.delete(target);
        // restore empty DataNode: remove props that are no longer applicable
        NodeProperty npToBeRemoved = node.findProperty(VOS.PROPERTY_URI_CONTENTMD5);
        node.getProperties().remove(npToBeRemoved);
        try {
            nodePersistence.put(node);
        } catch (NodeNotSupportedException bug) {
            throw new RuntimeException("BUG: unexpected " + bug, bug);
        }
        return;
    }
    
    private void restoreOwnNGroup(Path rootPath, Node node) throws IOException {
        PosixPrincipal pp = NodeUtil.getOwner(node);
        Integer gid = NodeUtil.getDefaultGroup(pp);
        Path target = NodeUtil.nodeToPath(rootPath, node);
        NodeUtil.setPosixOwnerGroup(target, pp.getUidNumber(), gid);
    }
    */
}
