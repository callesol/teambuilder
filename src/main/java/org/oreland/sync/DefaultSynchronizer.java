/*
 * Copyright (C) 2012 jonas.oreland@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.oreland.sync;

import org.oreland.sync.util.FormValues;
import org.oreland.sync.util.SyncHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class DefaultSynchronizer implements Synchronizer {

    protected final Set<String> cookies = new HashSet<String>();
    protected final FormValues formValues = new FormValues();
    protected final CookieHandler cookieHandler = CookieManager.getDefault();

    public DefaultSynchronizer() {
        super();
        logout();
    }

    @Override
    public void reset() {
    }

    @Override
    public Status connect() {
        return null;
    }

    public void logout() {
        clearCookies();
        formValues.clear();
    }

    protected void addCookies(HttpURLConnection conn) {
    }

    protected void getCookies(HttpURLConnection conn) throws URISyntaxException {
    }

    protected void clearCookies() {
        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
    }

    protected String getFormValues(HttpURLConnection conn) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder buf = new StringBuilder();
        String s = null;
        while ((s = in.readLine()) != null) {
            buf.append(s);
        }
        String html = buf.toString();
        Map<String, String> values = SyncHelper.parseHtml(html);
        formValues.putAll(values);
        return html;
    }
}
