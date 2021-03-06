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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.imaginarycode.minecraft.bungeejson.BungeeJSONPlugin;
import com.imaginarycode.minecraft.bungeejson.BungeeJSONUtilities;
import com.imaginarycode.minecraft.bungeejson.api.ApiRequest;
import com.imaginarycode.minecraft.bungeejson.api.RequestHandler;
import com.imaginarycode.minecraft.bungeejson.api.exceptions.NotAuthorizedException;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class HttpServerHandler extends SimpleChannelInboundHandler<Object> {
    private HttpResponse resp;

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        if (channelHandlerContext.channel().remoteAddress() instanceof InetSocketAddress) {
            if (o instanceof HttpRequest) {
                HttpRequest request = (HttpRequest) o;
                resp = getResponse(channelHandlerContext, request);
            }
            if (o instanceof LastHttpContent) {
                channelHandlerContext.channel().writeAndFlush(resp);
            }
        }
    }

    private HttpResponse getResponse(ChannelHandlerContext channelHandlerContext, HttpRequest hr) {
        QueryStringDecoder query = new QueryStringDecoder(hr.getUri());
        Multimap<String, String> params = createMultimapFromMap(query.parameters());
        ApiRequest ar = new HttpServerApiRequest(((InetSocketAddress) channelHandlerContext.channel().remoteAddress()).getAddress(),
                params);

        Object reply;
        HttpResponseStatus hrs;
        RequestHandler handler = BungeeJSONPlugin.getRequestManager().getHandlerForEndpoint(query.path());
        if (handler == null) {
            reply = BungeeJSONUtilities.error("No such endpoint exists.");
            hrs = HttpResponseStatus.NOT_FOUND;
        } else {
            try {
                if (handler.requiresAuthentication() && !BungeeJSONPlugin.getPlugin().getAuthenticationProvider().authenticate(ar, query.path())) {
                    hrs = HttpResponseStatus.FORBIDDEN;
                    reply = BungeeJSONUtilities.error("Access denied.");
                } else {
                    reply = handler.handle(ar);
                    hrs = HttpResponseStatus.OK;
                }
            } catch (NotAuthorizedException e) {
                hrs = HttpResponseStatus.FORBIDDEN;
                reply = BungeeJSONUtilities.error("Access denied: " + e.getMessage());
            } catch (Throwable throwable) {
                hrs = HttpResponseStatus.INTERNAL_SERVER_ERROR;
                reply = BungeeJSONUtilities.error("An internal error has occurred. Information has been logged to the console.");
                BungeeJSONPlugin.getPlugin().getLogger().log(Level.WARNING, "Error while handling " + hr.getUri() + " from " + ar.getRemoteIp(), throwable);
            }
        }

        String json = BungeeJSONPlugin.getPlugin().getGson().toJson(reply);
        DefaultFullHttpResponse hreply = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, hrs, Unpooled.copiedBuffer(json, CharsetUtil.UTF_8));
        // Add a reminder that we're still running the show.
        hreply.headers().set("Content-Type", "application/json; charset=UTF-8");
        hreply.headers().set("Server", "BungeeJSON/0.1");
        hreply.headers().set("Content-Length", json.length());
        return hreply;
    }

    private <K, V> Multimap<K, V> createMultimapFromMap(Map<K, List<V>> map) {
        Multimap<K, V> multimap = HashMultimap.create();
        for (Map.Entry<K, List<V>> entry : map.entrySet()) {
            multimap.get(entry.getKey()).addAll(entry.getValue());
        }
        return multimap;
    }
}
