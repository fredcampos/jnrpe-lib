/*
 * Copyright (c) 2008 Massimiliano Ziccardi Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package it.jnrpe;

import it.jnrpe.commands.CommandInvoker;
import it.jnrpe.events.EventsUtil;
import it.jnrpe.events.IJNRPEEventListener;
import it.jnrpe.events.LogEvent;
import it.jnrpe.utils.StreamManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

/**
 * Thread that listen on a given IP:PORT. When a request is received, a
 * {@link JNRPEServerThread} is created to serve it.
 *
 * @author Massimiliano Ziccardi
 */
class JNRPEListenerThread extends Thread implements IJNRPEListener
{
    /**
     * Default time to wait before killing staled commands (milliseconds).
     */
    private static final int DEFAULT_COMMAND_EXECUTION_TIMEOUT = 20000;

    /**
     * The ServerSocket.
     */
    private ServerSocket m_serverSocket = null;

    /**
     * The list of all the accepted clients.
     */
    private List<InetAddress> m_vAcceptedHosts = new ArrayList<InetAddress>();

    /**
     * The thread factory to be used to create server threads.
     */
    private ThreadFactory m_threadFactory = null;

    /**
     * The address to bind to.
     */
    private final String m_sBindingAddress;
    /**
     * The port to listen to.
     */
    private final int m_iBindingPort;
    /**
     * The command invoker to be used to serve the requests.
     */
    private final CommandInvoker m_commandInvoker;

    /**
     * <code>true</code> if the connection must be encrypted.
     */
    private boolean m_bSSL = false;

    /**
     * The command execution timeout (milliseconds).
     */
    private int m_iCommandExecutionTimeout = DEFAULT_COMMAND_EXECUTION_TIMEOUT;

    /**
     * The default keystore file name.
     */
    private static final String KEYSTORE_NAME = "keys.jks";
    /**
     * The default ketstore password.
     */
    private static final String KEYSTORE_PWD = "p@55w0rd";

    /**
     * The set of event listeners.
     */
    private Set<IJNRPEEventListener> m_vEventListeners = null;

    /**
     * Set to  true if the server is shutting down
     */
    private boolean m_bShutdown = false;
    
    /**
     * Builds a listener thread.
     *
     * @param vEventListeners
     *            The event listeners
     * @param sBindingAddress
     *            The address to bind to
     * @param iBindingPort
     *            The port to bind to
     * @param commandInvoker
     *            The command invoker to be used to serve the request
     */
    JNRPEListenerThread(final Set<IJNRPEEventListener> vEventListeners,
            final String sBindingAddress, final int iBindingPort,
            final CommandInvoker commandInvoker)
    {
        m_sBindingAddress = sBindingAddress;
        m_iBindingPort = iBindingPort;
        m_commandInvoker = commandInvoker;
        m_vEventListeners = vEventListeners;
        // try
        // {
        // init();
        // }
        // catch (Exception e)
        // {
        // throw new BindException(e.getMessage());
        // }
    }

    /**
     * Enables the SSL.
     */
    public void enableSSL()
    {
        m_bSSL = true;
    }

    /**
     * Creates an SSLServerSocketFactory.
     *
     * @return the newly creates SSL Server Socket Factory
     * @throws KeyStoreException
     * @throws CertificateException
     * @throws IOException
     * @throws UnrecoverableKeyException
     * @throws KeyManagementException
     */
    private SSLServerSocketFactory getSSLSocketFactory()
            throws KeyStoreException, CertificateException, IOException,
            UnrecoverableKeyException, KeyManagementException
    {

        // Open the KeyStore Stream
        StreamManager h = new StreamManager();

        // if (sKeyStoreFile == null)
        // throw new KeyStoreException("KEYSTORE HAS NOT BEEN SPECIFIED");
        // if (!new File(sKeyStoreFile).exists())
        // throw new KeyStoreException("COULD NOT FIND KEYSTORE '" +
        // sKeyStoreFile + "'");
        //
        // if (sKeyStorePwd == null)
        // throw new
        // KeyStoreException("KEYSTORE PASSWORD HAS NOT BEEN SPECIFIED");

        SSLContext ctx;
        KeyManagerFactory kmf;

        try
        {
            InputStream ksStream = getClass().getClassLoader()
                    .getResourceAsStream(KEYSTORE_NAME);
            h.handle(ksStream);
            ctx = SSLContext.getInstance("SSLv3");

            kmf = KeyManagerFactory.getInstance(KeyManagerFactory
                    .getDefaultAlgorithm());

            // KeyStore ks = getKeystore(sKeyStoreFile, sKeyStorePwd,
            // sKeyStoreType);
            // KeyStore ks = KeyStore.getInstance(sKeyStoreType);
            KeyStore ks = KeyStore.getInstance("JKS");
            char[] passphrase = KEYSTORE_PWD.toCharArray();
            ks.load(ksStream, passphrase);

            kmf.init(ks, passphrase);
            ctx.init(kmf.getKeyManagers(), null,
                    new java.security.SecureRandom());
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new SSLException("Unable to initialize SSLSocketFactory.\n"
                    + e.getMessage());
        }
        finally
        {
            h.closeAll();
        }

        return ctx.getServerSocketFactory();
    }

    /**
     * Initializes the object.
     *
     * @throws IOException
     * @throws KeyManagementException
     * @throws KeyStoreException
     * @throws CertificateException
     * @throws UnrecoverableKeyException
     */
    private void init() throws IOException, KeyManagementException,
            KeyStoreException, CertificateException, UnrecoverableKeyException
    {
        InetAddress addr = InetAddress.getByName(m_sBindingAddress);
        ServerSocketFactory sf = null;

        if (m_bSSL)
        {
            // TODO: configurazione keystore
            sf = getSSLSocketFactory();
            // sf = getSSLSocketFactory(m_Binding.getKeyStoreFile(),
            // m_Binding.getKeyStorePassword(), "JKS");
        }
        else
        {
            sf = ServerSocketFactory.getDefault();
        }

        m_serverSocket = sf.createServerSocket(m_iBindingPort, 0, addr);
        if (m_serverSocket instanceof SSLServerSocket)
        {
            ((SSLServerSocket) m_serverSocket)
                    .setEnabledCipherSuites(((SSLServerSocket) m_serverSocket)
                            .getSupportedCipherSuites());
        }

        // Init the thread factory
        m_threadFactory = new ThreadFactory(m_iCommandExecutionTimeout,
                m_commandInvoker);
    }

    /**
     * Adds an host to the list of accepted hosts.
     *
     * @param sHost
     *            The hostname or IP
     * @throws UnknownHostException
     *             thrown if the host name can't be translated to an IP.
     */
    public void addAcceptedHosts(final String sHost)
            throws UnknownHostException
    {
        InetAddress addr = InetAddress.getByName(sHost);
        m_vAcceptedHosts.add(addr);
    }

    /**
     * Executes the thread.
     */
    public void run()
    {
        try
        {
            init();

            EventsUtil.sendEvent(m_vEventListeners, this, LogEvent.INFO,
                    "Listening on " + m_sBindingAddress + ":" + m_iBindingPort);
            
            while (true)
            {
                Socket clientSocket = m_serverSocket.accept();
                clientSocket.setSoLinger(false, 10);
                clientSocket.setSoTimeout(m_iCommandExecutionTimeout);
                
                if (!canAccept(clientSocket.getInetAddress()))
                {
                    clientSocket.close();
                    continue;
                }

                JNRPEServerThread kk = m_threadFactory
                        .createNewThread(clientSocket);
                kk.configure(this, m_vEventListeners);
                kk.start();
            }
        }
        catch (SocketException se)
        {
            if (!m_bShutdown)
            {
                EventsUtil.sendEvent(m_vEventListeners, this, LogEvent.ERROR,
                        "Unable to listen on " + m_sBindingAddress + ":" + m_iBindingPort + ": " + se.getMessage(), se);
            }
            
            // This exception is thrown when the server socket is closed.
            // Ignoring

        }
        catch (Exception e)
        {
        	System.out.println ("*** DBEX2");
            EventsUtil.sendEvent(m_vEventListeners, this, LogEvent.ERROR,
                    e.getMessage(), e);
        }
        catch (Throwable e)
        {
        	System.out.println ("*** DBEX3");
            EventsUtil.sendEvent(m_vEventListeners, this, LogEvent.ERROR,
                    e.getMessage(), e);
        }

        exit();
    }

    /**
     * Closes the listener.
     */
    private synchronized void exit()
    {
        notify();
        EventsUtil.sendEvent(m_vEventListeners, this, LogEvent.INFO,
                "Listener Closed");
    }

    /**
     * @see it.jnrpe.IJNRPEListener#close()
     */
    public synchronized void shutdown()
    {
        m_bShutdown = true;
        
        try
        {
        	if (m_serverSocket != null)
        	{
        		m_serverSocket.close();
        		wait();
        	}
        }
        catch (InterruptedException ie)
        {

        }
        catch (IOException e)
        {
        }
    }

    /**
     * Returns <code>true</code> if the request must be accepted.
     *
     * @param inetAddress
     *            The client IP address
     * @return <code>true</code> if the request must be accepted.
     */
    private boolean canAccept(final InetAddress inetAddress)
    {
        for (InetAddress addr : m_vAcceptedHosts)
        {
            if (addr.equals(inetAddress))
                return true;
        }

        // System.out.println ("Refusing connection to " + inetAddress);
        EventsUtil.sendEvent(m_vEventListeners, this, LogEvent.INFO,
                "Connection refused from : " + inetAddress);

        return false;
    }
}
