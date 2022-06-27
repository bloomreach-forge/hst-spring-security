/*
 * Copyright 2018-2022 Bloomreach Inc. (https://www.bloomreach.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.hippoSecurityPlugin.instructions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

public final class Utils {

    public static final String CHARSET_NAME = "UTF-8";

    private Utils() {
    }

    public static void prettyPrint(final File file, final byte[] bytes) throws Exception {
        final Source source = new StreamSource(new ByteArrayInputStream(bytes));
        final DOMResult result = new DOMResult();
        final TransformerFactory transformerFactory = TransformerFactory.newInstance();
        final Transformer identityTransformer = transformerFactory.newTransformer();
        identityTransformer.transform(source, result);
        final org.w3c.dom.Document doc = (org.w3c.dom.Document) result.getNode();
        doc.setXmlStandalone(true);
        final Transformer transformer = transformerFactory.newTransformer();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, CHARSET_NAME);
        transformer.setOutputProperty(OutputKeys.ENCODING, CHARSET_NAME);
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(new DOMSource(doc), new StreamResult(new OutputStreamWriter(out, StandardCharsets.UTF_8)));
        final String data = out.toString(CHARSET_NAME);
        Files.asCharSink(file, Charsets.UTF_8).write(data);
    }
}
