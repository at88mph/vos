/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONN�ES ASTRONOMIQUES  **************
 *
 *  (c) 2009.                            (c) 2009.
 *  Government of Canada                 Gouvernement du Canada
 *  National Research Council            Conseil national de recherches
 *  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
 *  All rights reserved                  Tous droits r�serv�s
 *
 *  NRC disclaims any warranties,        Le CNRC d�nie toute garantie
 *  expressed, implied, or               �nonc�e, implicite ou l�gale,
 *  statutory, of any kind with          de quelque nature que ce
 *  respect to the software,             soit, concernant le logiciel,
 *  including without limitation         y compris sans restriction
 *  any warranty of merchantability      toute garantie de valeur
 *  or fitness for a particular          marchande ou de pertinence
 *  purpose. NRC shall not be            pour un usage particulier.
 *  liable in any event for any          Le CNRC ne pourra en aucun cas
 *  damages, whether direct or           �tre tenu responsable de tout
 *  indirect, special or general,        dommage, direct ou indirect,
 *  consequential or incidental,         particulier ou g�n�ral,
 *  arising from the use of the          accessoire ou fortuit, r�sultant
 *  software.  Neither the name          de l'utilisation du logiciel. Ni
 *  of the National Research             le nom du Conseil National de
 *  Council of Canada nor the            Recherches du Canada ni les noms
 *  names of its contributors may        de ses  participants ne peuvent
 *  be used to endorse or promote        �tre utilis�s pour approuver ou
 *  products derived from this           promouvoir les produits d�riv�s
 *  software without specific prior      de ce logiciel sans autorisation
 *  written permission.                  pr�alable et particuli�re
 *                                       par �crit.
 *
 *  This file is part of the             Ce fichier fait partie du projet
 *  OpenCADC project.                    OpenCADC.
 *
 *  OpenCADC is free software:           OpenCADC est un logiciel libre ;
 *  you can redistribute it and/or       vous pouvez le redistribuer ou le
 *  modify it under the terms of         modifier suivant les termes de
 *  the GNU Affero General Public        la �GNU Affero General Public
 *  License as published by the          License� telle que publi�e
 *  Free Software Foundation,            par la Free Software Foundation
 *  either version 3 of the              : soit la version 3 de cette
 *  License, or (at your option)         licence, soit (� votre gr�)
 *  any later version.                   toute version ult�rieure.
 *
 *  OpenCADC is distributed in the       OpenCADC est distribu�
 *  hope that it will be useful,         dans l�espoir qu�il vous
 *  but WITHOUT ANY WARRANTY;            sera utile, mais SANS AUCUNE
 *  without even the implied             GARANTIE : sans m�me la garantie
 *  warranty of MERCHANTABILITY          implicite de COMMERCIALISABILIT�
 *  or FITNESS FOR A PARTICULAR          ni d�AD�QUATION � UN OBJECTIF
 *  PURPOSE.  See the GNU Affero         PARTICULIER. Consultez la Licence
 *  General Public License for           G�n�rale Publique GNU Affero
 *  more details.                        pour plus de d�tails.
 *
 *  You should have received             Vous devriez avoir re�u une
 *  a copy of the GNU Affero             copie de la Licence G�n�rale
 *  General Public License along         Publique GNU Affero avec
 *  with OpenCADC.  If not, see          OpenCADC ; si ce n�est
 *  <http://www.gnu.org/licenses/>.      pas le cas, consultez :
 *                                       <http://www.gnu.org/licenses/>.
 *
 *  $Revision: 4 $
 *
 ************************************************************************
 */

package ca.nrc.cadc.vos;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * Writes a Node as XML to an output.
 * 
 * @author jburke
 */
public class NodeWriter
{
    /*
     * The VOSpace Namespace.
     */
    protected static Namespace voSpaceNamespace;
    protected static Namespace nodeNamespace;
    protected static Namespace vostNamespace;
    protected static Namespace xsiNamespace;
    static
    {
//        voSpaceNamespace = Namespace.getNamespace(NodeReader.VOSPACE_SCHEMA);
        voSpaceNamespace = Namespace.NO_NAMESPACE;
        nodeNamespace = Namespace.getNamespace("http://www.ivoa.net/xml/VOSpaceTypes-v2.0");
        vostNamespace = Namespace.getNamespace("vost", "http://www.ivoa.net/xml/VOSpaceTypes-v2.0");
        xsiNamespace = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
    }

    private static Logger log = Logger.getLogger(NodeWriter.class);

    /**
     * Write a ContainerNode to a StringBuilder.
     */
    public NodeWriter() { }

    public void write(ContainerNode node, StringBuilder builder)
        throws IOException
    {
        write(node, new StringBuilderWriter(builder));
    }

    /**
     * Write a ContainerNode to an OutputStream.
     *
     * @param node Node to write.
     * @param out OutputStream to write to.
     * @throws IOException if the writer fails to write.
     */
    public void write(ContainerNode node, OutputStream out)
        throws IOException
    {
        OutputStreamWriter outWriter;
        try
        {
            outWriter = new OutputStreamWriter(out, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException("UTF-8 encoding not supported", e);
        }
        write(node, new BufferedWriter(outWriter));
    }

    /**
     * Write a ContainerNode to a Writer.
     *
     * @param node Node to write.
     * @param writer Writer to write to.
     * @throws IOException if the writer fails to write.
     */
    public void write(ContainerNode node, Writer writer)
        throws IOException
    {
        // Create the root node element
        Element root = getRootElement(node);

        // properties element
        root.setContent(getPropertiesElement(node));

        // nodes element
        root.setContent(getNodesElement(node));

        // write out the Document
        write(root, writer);
    }

    /**
     * Write a DataNode to a StringBuilder.
     * 
     * @param node Node to write.
     * @param builder StringBuilder to write to.
     * @throws IOException if the writer fails to write.
     */
    public void write(DataNode node, StringBuilder builder)
        throws IOException
    {
        write(node, new StringBuilderWriter(builder));
    }

    /**
     * Write a DataNode to an OutputStream.
     *
     * @param node Node to write.
     * @param out OutputStream to write to.
     * @throws IOException if the writer fails to write.
     */
    public void write(DataNode node, OutputStream out)
        throws IOException
    {
        OutputStreamWriter outWriter;
        try
        {
            outWriter = new OutputStreamWriter(out, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException("UTF-8 encoding not supported", e);
        }
        write(node, new BufferedWriter(outWriter));
    }

    /**
     * Write a DataNode to a Writer.
     *
     * @param node Node to write.
     * @param writer Writer to write to.
     * @throws IOException if the writer fails to write.
     */
    public void write(DataNode node, Writer writer)
        throws IOException
    {
        // Create the root node element
        Element root = getRootElement(node);

        // busy attribute
        root.setAttribute("busy", (node.isBusy() ? "true" : "false"));

        // properties element
        root.setContent(getPropertiesElement(node));

        // write out the Document
        write(root, writer);
    }

    /**
     *  Build the root Element of a Node.
     *
     * @param node Node.
     * @return root Element.
     */
    protected Element getRootElement(Node node)
    {
        // Create the root element (node).
        Element root = new Element("node", voSpaceNamespace);
//        root.setNamespace(nodeNamespace);
//        root.setNamespace(vostNamespace);
//        root.setNamespace(xsiNamespace);
        root.setAttribute("uri", node.getPath());
        root.setAttribute("type", "vost:" + node.getClass().getSimpleName(), xsiNamespace);
        return root;
    }

    /**
     * Build the properties Element of a Node.
     *
     * @param node Node.
     * @return properties Element.
     */
    protected Element getPropertiesElement(Node node)
    {
        Element properties  = new Element("properties", voSpaceNamespace);
        for (NodeProperty nodeProperty : node.getProperties())
        {
            Element property = new Element("property", voSpaceNamespace);
            property.setAttribute("uri", nodeProperty.getPropertyURI());
            property.setText(nodeProperty.getPropertyValue());
            property.setAttribute("readOnly", (nodeProperty.isReadOnly() ? "true" : "false"));
            properties.addContent(property);
        }
        return properties;
    }

    /**
     * Build the nodes Element of a ContainerNode.
     * 
     * @param node Node.
     * @return nodes Element.
     */
    protected Element getNodesElement(ContainerNode node)
    {
        Element nodes = new Element("nodes", voSpaceNamespace);
        for (Node childNode : node.getNodes())
        {
            Element nodeElement = new Element("node", voSpaceNamespace);
            nodeElement.setAttribute("uri", childNode.getPath());
            nodes.addContent(nodeElement);
        }
        return nodes;
    }

    /**
     * Write to root Element to a writer.
     *
     * @param root Root Element to write.
     * @param writer Writer to write to.
     * @throws IOException if the writer fails to write.
     */
    protected void write(Element root, Writer writer)
        throws IOException
    {
        XMLOutputter outputter = new XMLOutputter();
        outputter.setFormat(Format.getPrettyFormat());
        outputter.output(new Document(root), writer);
    }

    /**
     * Class wraps a Writer around a StringBuilder.
     */
    public class StringBuilderWriter extends Writer
    {
        private StringBuilder sb;

        public StringBuilderWriter(StringBuilder sb)
        {
            this.sb = sb;
        }

        @Override
        public void write(char[] cbuf)
            throws IOException
        {
            sb.append(cbuf);
        }

        @Override
        public void write(char[] cbuf, int off, int len)
            throws IOException
        {
            sb.append(cbuf, off, len);
        }

        @Override
        public void write(int c)
            throws IOException
        {
            sb.append((char) c);
        }

        @Override
        public void write(String str)
            throws IOException
        {
            sb.append(str);
        }

        @Override
        public void write(String str, int off, int len)
            throws IOException
        {
            sb.append(str.substring(off, off + len));
        }

        @Override
        public void flush() throws IOException { }

        @Override
        public void close() throws IOException { }

        public void reset()
        {
            sb.setLength(0);
        }

    }

}
