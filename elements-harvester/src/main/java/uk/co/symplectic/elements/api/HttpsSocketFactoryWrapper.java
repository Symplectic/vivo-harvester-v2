
package uk.co.symplectic.elements.api;

import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by ajpc2_000 on 08/07/2016.
 * FOR TESTING ONLY!!!
 */

public class HttpsSocketFactoryWrapper implements SecureProtocolSocketFactory {
    private final SecureProtocolSocketFactory base;
    private List<String> AllowedProtocols = new ArrayList<String>();

    public HttpsSocketFactoryWrapper(SecureProtocolSocketFactory base, String... protocols)
    {
        if(base == null || !(base instanceof SecureProtocolSocketFactory)) throw new IllegalArgumentException();
        this.base = base;
        for(String prot : protocols){ AllowedProtocols.add(prot);}
    }

    private Socket restrictAllowedProtocols(Socket socket)
    {
        if(!(socket instanceof SSLSocket)) return socket;
        SSLSocket sslSocket = (SSLSocket) socket;
        String[] protArray = new String[AllowedProtocols.size()];
        for(int i =0; i< AllowedProtocols.size(); i++){
            protArray[i] = AllowedProtocols.get(i);
        }
        sslSocket.setEnabledProtocols(protArray);
        return sslSocket;
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException
    {
        return restrictAllowedProtocols(base.createSocket(host, port));
    }
    @Override
    public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) throws IOException
    {
        return restrictAllowedProtocols(base.createSocket(host, port, localAddress, localPort));
    }
    @Override
    public Socket createSocket(String host, int port, InetAddress localAddress, int localPort, HttpConnectionParams params) throws IOException
    {
        return restrictAllowedProtocols(base.createSocket(host, port, localAddress, localPort, params));
    }
    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException
    {
        return restrictAllowedProtocols(base.createSocket(socket, host, port, autoClose));
    }

}

