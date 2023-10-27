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

package org.opencadc.vospace.server;

import java.net.URI;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import org.apache.log4j.Logger;
import org.opencadc.vospace.Node;
import org.opencadc.vospace.NodeProperty;

/**
 * Utility methods
 *
 * @author adriand
 */
public class Utils {
    final Logger log = Logger.getLogger(Utils.class);

    /**
     * Returns the path of the parent with no leading or trailing backslashes
     *
     * @param nodePath the path of the node
     * @return the path of the parent
     */
    public static String getParentPath(final String nodePath) {
        if (isRoot(nodePath)) {
            return null; // there is no parent of the root
        }
        String np = nodePath;
        if (np.endsWith("/")) {
            np = np.substring(0, np.length() - 1);
        }
        if (nodePath.startsWith("/")) {
            np = np.substring(1, np.length());
        }
        int index = np.lastIndexOf("/");
        if (index > 0) {
            return np.substring(0, index);
        } else {
            return null;
        }
    }

    public static boolean isRoot(String nodePath) {
        if (nodePath == null || nodePath.length() == 0 || nodePath.equals("/")) {
            return true;
        }
        return false;
    }

    /**
     * Get a linked list of nodes from leaf to root.
     *
     * @param leaf leaf node
     * @return list of nodes, with leaf first and root last
     */
    public static LinkedList<Node> getNodeList(Node leaf) {
        LinkedList<Node> nodes = new LinkedList<Node>();
        Node cur = leaf;
        while (cur != null) {
            nodes.add(cur);
            cur = cur.parent;
        }
        return nodes;
    }


    public static String getPath(Node node) {
        StringBuilder sb = new StringBuilder();
        sb.append(node.getName());
        Node tmp = node.parent;
        while (tmp != null) {
            sb.insert(0, tmp.getName() + "/");
            tmp = tmp.parent;
        }
        return sb.toString();
    }

    /**
     * Takes a set of old properties and updates it with a new set of properties. Essentially
     * this means updating values or removing and adding elements. It is not a straight 
     * replacement.
     *
     * @param oldProps set of old Node Properties that are being updated
     * @param newProps set of new Node Properties to be used for the update
     * @param immutable set of immutable property keys to skip
     */
    public static void updateNodeProperties(Set<NodeProperty> oldProps, Set<NodeProperty> newProps, Set<URI> immutable) {
        for (Iterator<NodeProperty> newIter = newProps.iterator(); newIter.hasNext(); ) {
            NodeProperty newProperty = newIter.next();
            if (!immutable.contains(newProperty.getKey())) {
                if (oldProps.contains(newProperty)) {
                    oldProps.remove(newProperty);
                }
                if (!newProperty.isMarkedForDeletion()) {
                    oldProps.add(newProperty);
                }
            }
        }
    }
}
