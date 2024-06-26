/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2024.                            (c) 2024.
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
 *
 ************************************************************************
 */

package org.opencadc.util.fs;

import ca.nrc.cadc.util.Log4jInit;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;


public class XAttrCommandExecutorTest {
    static final String ROOT = "build/tmp/attr-tests";

    static {
        Log4jInit.setLevel("org.opencadc.util.fs", Level.DEBUG);
        Log4jInit.setLevel("ca.nrc.cadc.exec", Level.DEBUG);
    }

    static {
        try {
            final Path root = FileSystems.getDefault().getPath(XAttrCommandExecutorTest.ROOT);
            if (!Files.exists(root)) {
                Files.createDirectory(root);
            }
        } catch (IOException ex) {
            throw new RuntimeException("TEST SETUP: failed to create test dir: " + XAttrCommandExecutorTest.ROOT, ex);
        }
    }

    @Test
    public void setAndGetUserAttribute() throws Exception {
        final String folderName = "setAndGetUserAttribute-" + UUID.randomUUID();
        final FileSystem fs = FileSystems.getDefault();
        final Path target = fs.getPath(XAttrCommandExecutorTest.ROOT, folderName).toAbsolutePath();
        Files.createDirectory(target);

        Assert.assertNull("Should not already have user.foo attribute.",
                          XAttrCommandExecutor.get(target, "user.foo"));

        XAttrCommandExecutor.set(target, "user.foo", "bar");

        Assert.assertEquals("Wrong attribute value.", "bar",
                            XAttrCommandExecutor.get(target, "user.foo"));
    }

    @Test(expected = IOException.class)
    public void setAttributeBadNamespace() throws Exception {
        final String folderName = "setAttributeBadNamespace-" + UUID.randomUUID();
        final FileSystem fs = FileSystems.getDefault();
        final Path target = fs.getPath(XAttrCommandExecutorTest.ROOT, folderName).toAbsolutePath();
        Files.createDirectory(target);

        XAttrCommandExecutor.set(target, "bogus.attr", "bogusValue");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setAttributeNullKey() throws Exception {
        final String folderName = "setAttributeNullKey-" + UUID.randomUUID();
        final FileSystem fs = FileSystems.getDefault();
        final Path target = fs.getPath(XAttrCommandExecutorTest.ROOT, folderName).toAbsolutePath();
        Files.createDirectory(target);

        XAttrCommandExecutor.set(target, null, "bogusValue");
    }

    @Test
    public void getAttributeNoSuchKey() throws Exception {
        final String folderName = "getAttributeFail-" + UUID.randomUUID();
        final FileSystem fs = FileSystems.getDefault();
        final Path target = fs.getPath(XAttrCommandExecutorTest.ROOT, folderName).toAbsolutePath();
        Files.createDirectory(target);

        Assert.assertNull("Should be null for no such attribute.",
                          XAttrCommandExecutor.get(target, "user.bogus.attr"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getAttributeNull() throws Exception {
        final String folderName = "getAttributeFail-" + UUID.randomUUID();
        final FileSystem fs = FileSystems.getDefault();
        final Path target = fs.getPath(XAttrCommandExecutorTest.ROOT, folderName).toAbsolutePath();
        Files.createDirectory(target);

        XAttrCommandExecutor.get(target, null);
    }
}
