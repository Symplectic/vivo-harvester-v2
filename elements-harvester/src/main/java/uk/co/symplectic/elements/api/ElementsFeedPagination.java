/*
 * ******************************************************************************
 *   Copyright (c) 2019 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 *   Version :  ${git.branch}:${git.commit.id}
 * ******************************************************************************
 */
package uk.co.symplectic.elements.api;

/**
 *  class representing the concept of where a particular API response is in a feed consisting of multiple pages.
 *  Note that post v5.5 API endpoint spec most of these will be null,
 *  as data on first last and previous will not be supplied by the API any more.
 */
@SuppressWarnings("unused")
public class ElementsFeedPagination {
    private int itemsPerPage;
    private String firstURL;
    private String lastURL;
    private String previousURL;
    private String nextURL;

    int getItemsPerPage() {
        return itemsPerPage;
    }

    String getFirstURL() {
        return firstURL;
    }

    String getLastURL() {
        return lastURL;
    }

    String getPreviousURL() {
        return previousURL;
    }

    String getNextURL() {
        return nextURL;
    }

    public void setItemsPerPage(int itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
    }

    public void setFirstURL(String firstURL) {
        this.firstURL = firstURL;
    }

    public void setLastURL(String lastURL) {
        this.lastURL = lastURL;
    }

    public void setPreviousURL(String previousURL) {
        this.previousURL = previousURL;
    }

    public void setNextURL(String nextURL) {
        this.nextURL = nextURL;
    }
}
