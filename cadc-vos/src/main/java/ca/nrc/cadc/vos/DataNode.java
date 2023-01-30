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

package ca.nrc.cadc.vos;

import java.net.URI;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

/**
 * A VOSpace node that describes a data item.
 * 
 * @author majorb
 */
public class DataNode extends Node {
    
    private static Logger log = Logger.getLogger(DataNode.class);

    // The URI to the associated artifact.
    private final URI storageID;

    // The checksum of the artifact.
    private final URI contentChecksum;

    // The last modified date of the artifact.
    private final Date contentLastModified;

    // The content length of the artifact.
    private final long contentLength;

    // The type of the artifact.
    public String contentType;

    // The encoding of the artifact.
    public String contentEncoding;

    /**
     * DataNode constructor.
     *
     * @param name The name of the node.
     * @param storageID The URI of associated artifact.
     * @param contentChecksum The artifact checksum.
     * @param contentLastModified The artifact lastModified date.
     * @param contentLength The artifact contentLength.
     */
    public DataNode(String name, URI storageID, URI contentChecksum, Date contentLastModified, long contentLength) {
        this(name, storageID, contentChecksum, contentLastModified, contentLength, new TreeSet<>());
    }

    /**
     * Data node constructor.
     *
     * @param name The name of the node.
     * @param storageID The URI of associated artifact.
     * @param contentChecksum The artifact checksum.
     * @param contentLastModified The artifact lastModified date.
     * @param contentLength The artifact contentLength.
     * @param properties The node's properties.
     */
    public DataNode(String name, URI storageID, URI contentChecksum, Date contentLastModified,
                    long contentLength, Set<NodeProperty> properties) {
        super(name, properties);
        NodeUtil.assertNotNull(DataNode.class, "storageID", "storageID");
        NodeUtil.assertNotNull(DataNode.class, "contentChecksum", "contentChecksum");
        NodeUtil.assertNotNull(DataNode.class, "contentLastModified", "contentLastModified");
        NodeUtil.assertNotNull(DataNode.class, "contentLength", "contentLength");
        this.storageID = storageID;
        this.contentChecksum = contentChecksum;
        this.contentLastModified = contentLastModified;
        this.contentLength = contentLength;
    }

    public URI getStorageID() {
        return this.storageID;
    }

    public URI getContentChecksum() {
        return this.contentChecksum;
    }

    public Date getContentLastModified() {
        return this.contentLastModified;
    }

    public long getContentLength() {
        return this.contentLength;
    }

}
