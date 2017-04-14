package org.onehippo.forge.security.support.springsecurity.demo.beans;
/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.util.Calendar;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSet;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.onehippo.cms7.essentials.dashboard.annotations.HippoEssentialsGenerated;

@HippoEssentialsGenerated(internalName = "hstspringsecdemo:newsdocument")
@Node(jcrType="hstspringsecdemo:newsdocument")
public class NewsDocument extends HippoDocument {

    /**
     * The document type of the news document.
     */
    public final static String DOCUMENT_TYPE = "hstspringsecdemo:newsdocument";

    private final static String TITLE = "hstspringsecdemo:title";
    private final static String DATE = "hstspringsecdemo:date";
    private final static String INTRODUCTION = "hstspringsecdemo:introduction";
    private final static String IMAGE = "hstspringsecdemo:image";
    private final static String CONTENT = "hstspringsecdemo:content";
    private final static String LOCATION = "hstspringsecdemo:location";
    private final static String AUTHOR = "hstspringsecdemo:author";
    private final static String SOURCE = "hstspringsecdemo:source";

    /**
     * Get the title of the document.
     *
     * @return the title
     */
    @HippoEssentialsGenerated(internalName = "hstspringsecdemo:title")
    public String getTitle() {
        return getProperty(TITLE);
    }

    /**
     * Get the date of the document.
     *
     * @return the date
     */
    @HippoEssentialsGenerated(internalName = "hstspringsecdemo:date")
    public Calendar getDate() {
        return getProperty(DATE);
    }

    /**
     * Get the introduction of the document.
     *
     * @return the introduction
     */
    @HippoEssentialsGenerated(internalName = "hstspringsecdemo:introduction")
    public String getIntroduction() {
        return getProperty(INTRODUCTION);
    }

    /**
     * Get the image of the document.
     *
     * @return the image
     */
    @HippoEssentialsGenerated(internalName = "hstspringsecdemo:image")
    public HippoGalleryImageSet getImage() {
        return getLinkedBean(IMAGE, HippoGalleryImageSet.class);
    }

    /**
     * Get the main content of the document.
     *
     * @return the content
     */
    @HippoEssentialsGenerated(internalName = "hstspringsecdemo:content")
    public HippoHtml getContent() {
        return getHippoHtml(CONTENT);
    }

    /**
     * Get the location of the document.
     *
     * @return the location
     */
    @HippoEssentialsGenerated(internalName = "hstspringsecdemo:location")
    public String getLocation() {
        return getProperty(LOCATION);
    }

    /**
     * Get the author of the document.
     *
     * @return the author
     */
    @HippoEssentialsGenerated(internalName = "hstspringsecdemo:author")
    public String getAuthor() {
        return getProperty(AUTHOR);
    }

    /**
     * Get the source of the document.
     *
     * @return the source
     */
    @HippoEssentialsGenerated(internalName = "hstspringsecdemo:source")
    public String getSource() {
        return getProperty(SOURCE);
    }

}

