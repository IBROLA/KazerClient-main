package com.cheatbreaker.client.kazerui.module.modules.mechanic;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.MemoryCacheImageInputStream;

/** Worker-only bounded HTTPS decoder. Never accesses Minecraft, disk caches or GL. */
final class ChatImageLoader {
    static final int MAX_BYTES=4*1024*1024,MAX_PIXELS=1024*1024;
    static final List<String> HOSTS=Collections.unmodifiableList(Arrays.asList("imgur.com","i.imgur.com","discordapp.com",
        "discordapp.net","media.discordapp.net","hypixel.net","prnt.sc","ibb.co","wixmp.com","fbcdn.net",
        "cdninstagram.com","redd.it","gyazo.com","twimg.com","mcstats.com"));
    static boolean allowed(URL url,Collection<String> custom) {
        if(!url.getProtocol().equalsIgnoreCase("https") || url.getUserInfo()!=null || url.getPort()!=-1&&url.getPort()!=443)return false;
        String host=url.getHost().toLowerCase(Locale.ROOT);
        for(String domain:HOSTS)if(domainMatches(host,domain))return true;
        for(String domain:custom)if(domainMatches(host,domain))return true;
        return false;
    }
    private static boolean domainMatches(String host,String domain) {
        return domain.matches("[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?") && domain.indexOf('.')>0
            && !domain.matches("[0-9.]+") && (host.equals(domain)||host.endsWith("."+domain));
    }
    static boolean publicAddress(InetAddress address) {
        if(address.isAnyLocalAddress()||address.isLoopbackAddress()||address.isLinkLocalAddress()||address.isSiteLocalAddress()||address.isMulticastAddress())return false;
        byte[] b=address.getAddress();
        if(b.length==4) {
            int a=b[0]&255,c=b[1]&255;
            return a!=0 && a!=10 && a!=127 && a<224 && !(a==100&&c>=64&&c<=127)
                && !(a==169&&c==254) && !(a==172&&c>=16&&c<=31) && !(a==192&&c==168);
        }
        // IPv6 global unicast only; reject ULA and IPv4-mapped/local variants.
        return b.length==16 && (b[0]&0xe0)==0x20;
    }
    static byte[] readBounded(InputStream stream) throws IOException {
        return readBounded(stream,System.nanoTime()+12_000_000_000L);
    }
    private static byte[] readBounded(InputStream stream,long deadline) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream(8192);byte[] buffer=new byte[8192];int n;
        while((n=stream.read(buffer))!=-1) {
            if(Thread.currentThread().isInterrupted()||System.nanoTime()>deadline)throw new InterruptedIOException();
            if(out.size()+n>MAX_BYTES)throw new IOException("Image response exceeds limit");
            out.write(buffer,0,n);
        }
        return out.toByteArray();
    }
    static BufferedImage decode(byte[] data) throws IOException {
        if(data.length>MAX_BYTES)throw new IOException("Image response exceeds limit");
        try(MemoryCacheImageInputStream in=new MemoryCacheImageInputStream(new ByteArrayInputStream(data))) {
            Iterator<ImageReader> readers=ImageIO.getImageReaders(in);
            if(!readers.hasNext())throw new IOException("Unsupported image");
            ImageReader reader=readers.next();
            try {
                reader.setInput(in,true,true);
                int w=reader.getWidth(0),h=reader.getHeight(0);
                if(w<=0||h<=0||(long)w*h>MAX_PIXELS)throw new IOException("Image dimensions exceed limit");
                return reader.read(0);
            } finally {reader.dispose();}
        }
    }
    static BufferedImage load(String address,Collection<String> custom) throws IOException {
        URL target=new URL(address);long deadline=System.nanoTime()+12_000_000_000L;
        for(int redirect=0;redirect<=3;redirect++) {
            if(Thread.currentThread().isInterrupted()||System.nanoTime()>deadline)throw new InterruptedIOException();
            if(!allowed(target,custom))throw new IOException("Image host is not allowed");
            for(InetAddress ip:InetAddress.getAllByName(target.getHost()))if(!publicAddress(ip))throw new IOException("Non-public image host");
            HttpURLConnection connection=(HttpURLConnection)target.openConnection();
            connection.setConnectTimeout(3000);connection.setReadTimeout(3000);connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept","image/*");connection.setRequestProperty("User-Agent","KazerClient-Java");
            try {
                int status=connection.getResponseCode();
                if(status>=300&&status<400) {
                    String location=connection.getHeaderField("Location");
                    if(location==null)throw new IOException("Missing redirect target");
                    target=new URL(target,location);continue;
                }
                if(status!=200||connection.getContentLengthLong()>MAX_BYTES)throw new IOException("Invalid image response");
                try(InputStream in=connection.getInputStream()) {return decode(readBounded(in,deadline));}
            } finally {connection.disconnect();}
        }
        throw new IOException("Too many image redirects");
    }
    private ChatImageLoader() { }
}
