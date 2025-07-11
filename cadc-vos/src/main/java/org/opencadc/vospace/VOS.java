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

package org.opencadc.vospace;

import java.net.URI;
import java.util.Set;
import java.util.TreeSet;

/**
 * Holder of commonly used constants.
 *
 * @author zhangsa
 *
 */
public class VOS {
    public static final String GMS_PROTOCOL = "https";

    public static final int VOSPACE_20 = 20;
    public static final int VOSPACE_21 = 21;
    
    public static enum Detail {
        min("min"),
        max("max"),
        raw("raw"),
        properties("properties");

        private String value;

        private Detail(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Detail getValue(String value) {
            if (value.equals(min.getValue())) {
                return min;
            } else if (value.equals(max.getValue())) {
                return max;
            } else if (value.equals(raw.getValue())) {
                return raw;
            } else if (value.equals(properties.getValue())) {
                return properties;
            }
            return null;
        }
    }

    /*
     * Default property delimiter for multi-valued properties
     */
    public static final String DEFAULT_PROPERTY_VALUE_DELIM = ",";
    
    public static final String VOSPACE_URI_NAMESPACE = "ivo://ivoa.net/vospace";

    // Denotes a name given to the resource
    public static final URI PROPERTY_URI_TITLE = URI.create(VOSPACE_URI_NAMESPACE + "/core#title");

    // Denotes an entity primarily responsible for making the resource
    public static final URI PROPERTY_URI_CREATOR = URI.create(VOSPACE_URI_NAMESPACE + "/core#creator");

    // Denotes the topic of the resource
    public static final URI PROPERTY_URI_SUBJECT = URI.create(VOSPACE_URI_NAMESPACE + "/core#subject");

    // Denotes an account of the resource
    public static final URI PROPERTY_URI_DESCRIPTION = URI.create(VOSPACE_URI_NAMESPACE + "/core#description");

    // Denotes an entity responsible for making the resource available
    public static final URI PROPERTY_URI_PUBLISHER = URI.create(VOSPACE_URI_NAMESPACE + "/core#publisher");

    // Denotes an entity responsible for making contributions to this resource
    public static final URI PROPERTY_URI_CONTRIBUTOR = URI.create(VOSPACE_URI_NAMESPACE + "/core#contributor");

    // timestamp of the last modification to the node metadata or stored bytes
    public static final URI PROPERTY_URI_DATE = URI.create(VOSPACE_URI_NAMESPACE + "/core#date");

    // Denotes the nature or genre of the resource
    public static final URI PROPERTY_URI_TYPE = URI.create(VOSPACE_URI_NAMESPACE + "/core#type");

    // Denotes the file format, physical medium, or dimensions of the resource
    public static final URI PROPERTY_URI_FORMAT = URI.create(VOSPACE_URI_NAMESPACE + "/core#format");

    // Denotes an unambiguous reference to the resource within a given context
    public static final URI PROPERTY_URI_IDENTIFIER = URI.create(VOSPACE_URI_NAMESPACE + "/core#identifier");

    // Denotes a related resource from which the described resource is derived
    public static final URI PROPERTY_URI_SOURCE = URI.create(VOSPACE_URI_NAMESPACE + "/core#source");

    // Denotes a language of the resource
    public static final URI PROPERTY_URI_LANGUAGE = URI.create(VOSPACE_URI_NAMESPACE + "/core#language");

    // Denotes a related resource
    public static final URI PROPERTY_URI_RELATION = URI.create(VOSPACE_URI_NAMESPACE + "/core#relation");

    // Denotes the spatial or temporal topic of the resource,
    // the spatial applicability of the resource,
    // or the jurisdiction under which the resource is relevant
    public static final URI PROPERTY_URI_COVERAGE = URI.create(VOSPACE_URI_NAMESPACE + "/core#coverage");

    // Denotes information about rights held in and over the resource
    public static final URI PROPERTY_URI_RIGHTS = URI.create(VOSPACE_URI_NAMESPACE + "/core#rights");

    // Denotes the amount of space available within a container
    public static final URI PROPERTY_URI_AVAILABLESPACE = URI.create(VOSPACE_URI_NAMESPACE + "/core#availableSpace");

    // SHALL be used as the protocol URI for a HTTP GET transfer
    public static final URI PROTOCOL_HTTP_GET = URI.create(VOSPACE_URI_NAMESPACE + "/core#httpget");

    // SHALL be used as the protocol URI for a HTTP PUT transfer
    public static final URI PROTOCOL_HTTP_PUT = URI.create(VOSPACE_URI_NAMESPACE + "/core#httpput");

    // SHALL be used as the protocol URI for a HTTPS GET transfer
    public static final URI PROTOCOL_HTTPS_GET = URI.create(VOSPACE_URI_NAMESPACE + "/core#httpsget");

    // SHALL be used as the protocol URI for a HTTPS PUT transfer
    public static final URI PROTOCOL_HTTPS_PUT = URI.create(VOSPACE_URI_NAMESPACE + "/core#httpsput");
    
    // prototype mount protocol
    public static final URI PROTOCOL_SSHFS = URI.create("ivo://cadc.nrc.ca/vospace#SSHFS");

    // SHALL be used as the view URI to indicate that a service will accept any view for an import operation
    public static final URI VIEW_ANY = URI.create(VOSPACE_URI_NAMESPACE + "/core#anyview");

    // SHALL be used as the view URI to import or export data as a binary file
    public static final URI VIEW_BINARY = URI.create(VOSPACE_URI_NAMESPACE + "/core#binaryview");

    // SHALL be used by a client to indicate that the service should choose the most appropriate view for a data export
    public static final URI VIEW_DEFAULT = URI.create(VOSPACE_URI_NAMESPACE + "/core#defaultview");


    /*
     * Standard Node Properties defined by the CADC
     */

    // Property used for identifying a transaction
    public static final URI PROPERTY_URI_RUNID = URI.create(VOSPACE_URI_NAMESPACE + "/core#runid");

    // The size of the resource
    public static final URI PROPERTY_URI_CONTENTLENGTH = URI.create(VOSPACE_URI_NAMESPACE + "/core#length");

    // The quota of a Container Node.
    public static final URI PROPERTY_URI_QUOTA = URI.create(VOSPACE_URI_NAMESPACE + "/core#quota");

    // The content encoding of the resource
    public static final URI PROPERTY_URI_CONTENTENCODING = URI.create(VOSPACE_URI_NAMESPACE + "/core#encoding");

    // The MD5 Checksum of the resource
    public static final URI PROPERTY_URI_CONTENTMD5 = URI.create(VOSPACE_URI_NAMESPACE + "/core#MD5");

    // The groups who can read the resource
    public static final URI PROPERTY_URI_GROUPREAD = URI.create(VOSPACE_URI_NAMESPACE + "/core#groupread");
    public static final String PROPERTY_DELIM_GROUPREAD = " ";

    // The groups who can write to the resource
    public static final URI PROPERTY_URI_GROUPWRITE = URI.create(VOSPACE_URI_NAMESPACE + "/core#groupwrite");
    public static final String PROPERTY_DELIM_GROUPWRITE = " ";
    
    // Flag indicating if the Node is public (true/false)
    public static final URI PROPERTY_URI_ISPUBLIC = URI.create(VOSPACE_URI_NAMESPACE + "/core#ispublic");

    // proposed: timestamp of the last modification to the stored bytes (DataNode only)
    public static final URI PROPERTY_URI_CONTENTDATE = URI.create(VOSPACE_URI_NAMESPACE + "/core#content-date");

    /*
     * CADC Node Properties
     */

    // Custom prefix for CADC properties.
    public static final String VOSPACE_CUSTOM_URI_NAMESPACE = "ivo://opencadc.org/vospace";

    // Flag indicating if the Node locked (true/false)
    public static final URI PROPERTY_URI_ISLOCKED = URI.create("ivo://cadc.nrc.ca/vospace/core#islocked");

    // Flag indicating that the Node passed a read-permission check
    public static final URI PROPERTY_URI_READABLE = URI.create("ivo://cadc.nrc.ca/vospace/core#readable");

    // Flag indicating that the Node passed a write-permission check
    public static final URI PROPERTY_URI_WRITABLE = URI.create("ivo://cadc.nrc.ca/vospace/core#writable");

    // Flag indicating that the Node children inherit the parent node permissions
    public static final URI PROPERTY_URI_INHERIT_PERMISSIONS = URI.create("ivo://cadc.nrc.ca/vospace/core#inheritPermissions");

    // Denotes the JSON Web Token (Bearer Token) passed in to represent the owner of the resource
    public static final URI PROPERTY_URI_CREATOR_JWT = URI.create(VOSPACE_CUSTOM_URI_NAMESPACE + "/core#creatorJWT");

    /*
    * List of IVOA Standard Faults
     */
    // 500 erors
    public static final String IVOA_FAULT_INTERNAL_FAULT = "InternalFault";

    // 400 faults
    public static final String IVOA_FAULT_INVALID_URI = "InvalidURI";
    public static final String IVOA_FAULT_INVALID_ARG = "InvalidArgument";
    public static final String IVOA_FAULT_INVALID_DATA = "InvalidData";
    public static final String IVOA_FAULT_INVALID_TOKEN = "InvalidToken";
    public static final String IVOA_FAULT_TYPE_NOT_SUPPORTED = "TypeNotSupported";
    public static final String IVOA_FAULT_VIEW_NOT_SUPPORTED = "ViewNotSupported";
    public static final String IVOA_FAULT_OPTION_NOT_SUPPORTED = "OptionNotSupported";
    
    // 401
    public static final String IVOA_FAULT_PERMISSION_DENIED = "PermissionDenied";

    // 404 faults
    public static final String IVOA_FAULT_NODE_NOT_FOUND = "NodeNotFound";
    public static final String IVOA_FAULT_CONTAINER_NOT_FOUND = "ContainerNotFound";
    // 409
    public static final String IVOA_FAULT_DUPLICATE_NODE = "DuplicateNode";

    /**
     * List of CADC defined faults
     */
    // 401
    public static final String CADC_FAULT_NOT_AUTHENTICATED = "NotAuthenticated";
    // 404
    public static final String CADC_FAULT_UNREADABLE_LINK = "UnreadableLinkTarget";
    public static final String CADC_FAULT_CONTAINER_NOT_FOUND = "ContainerNotFound";
    // 413
    public static final String CADC_FAULT_REQUEST_TOO_LARGE = "RequestEntityToolarge";
    // 423
    public static final String CADC_FAULT_NODE_LOCKED = "NodeLocked";
    // 503
    public static final String CADC_FAULT_SERVICE_BUSY = "ServiceBusy";

    public static final Set<URI> VOSPACE_CORE_PROPERTIES = new TreeSet<>();

    static {
        VOSPACE_CORE_PROPERTIES.add(PROPERTY_URI_TITLE);
        VOSPACE_CORE_PROPERTIES.add(PROPERTY_URI_CREATOR);
        VOSPACE_CORE_PROPERTIES.add(PROPERTY_URI_SUBJECT);
        VOSPACE_CORE_PROPERTIES.add(PROPERTY_URI_DESCRIPTION);
        VOSPACE_CORE_PROPERTIES.add(PROPERTY_URI_PUBLISHER);
        VOSPACE_CORE_PROPERTIES.add(PROPERTY_URI_CONTRIBUTOR);
        VOSPACE_CORE_PROPERTIES.add(PROPERTY_URI_DATE);
        VOSPACE_CORE_PROPERTIES.add(PROPERTY_URI_TYPE);
        VOSPACE_CORE_PROPERTIES.add(PROPERTY_URI_FORMAT);
        VOSPACE_CORE_PROPERTIES.add(PROPERTY_URI_IDENTIFIER);
        VOSPACE_CORE_PROPERTIES.add(PROPERTY_URI_SOURCE);
        VOSPACE_CORE_PROPERTIES.add(PROPERTY_URI_LANGUAGE);
        VOSPACE_CORE_PROPERTIES.add(PROPERTY_URI_RELATION);
        VOSPACE_CORE_PROPERTIES.add(PROPERTY_URI_COVERAGE);
        VOSPACE_CORE_PROPERTIES.add(PROPERTY_URI_RIGHTS);
        VOSPACE_CORE_PROPERTIES.add(PROPERTY_URI_AVAILABLESPACE);
        VOSPACE_CORE_PROPERTIES.add(PROPERTY_URI_RUNID);
        
        VOSPACE_CORE_PROPERTIES.add(PROPERTY_URI_QUOTA);
        VOSPACE_CORE_PROPERTIES.add(PROPERTY_URI_CONTENTDATE);
        VOSPACE_CORE_PROPERTIES.add(PROPERTY_URI_CONTENTLENGTH);
        VOSPACE_CORE_PROPERTIES.add(PROPERTY_URI_CONTENTENCODING);
        VOSPACE_CORE_PROPERTIES.add(PROPERTY_URI_CONTENTMD5);
        VOSPACE_CORE_PROPERTIES.add(PROPERTY_URI_GROUPREAD);
        VOSPACE_CORE_PROPERTIES.add(PROPERTY_URI_GROUPWRITE);
        VOSPACE_CORE_PROPERTIES.add(PROPERTY_URI_ISPUBLIC);
        
    }
}
