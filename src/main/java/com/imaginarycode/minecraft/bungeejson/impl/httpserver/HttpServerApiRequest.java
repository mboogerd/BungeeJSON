/**
 * This file is part of BungeeJSON.
 *
 * BungeeJSON is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BungeeJSON is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BungeeJSON.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.imaginarycode.minecraft.bungeejson.impl.httpserver;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.imaginarycode.minecraft.bungeejson.api.ApiRequest;

import java.net.InetAddress;

public final class HttpServerApiRequest implements ApiRequest {
    private final InetAddress ia;
    private final ListMultimap<String, String> params;

    public HttpServerApiRequest(InetAddress ia, Multimap<String, String> params) {
        this.ia = ia;
        this.params = ImmutableListMultimap.copyOf(params);
    }

    @Override
    public final InetAddress getRemoteIp() {
        return ia;
    }

    @Override
    public final ListMultimap<String, String> getParams() {
        return params;
    }
}
