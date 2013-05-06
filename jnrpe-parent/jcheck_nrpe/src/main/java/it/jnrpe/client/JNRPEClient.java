/*
 * Copyright (c) 2013 Massimiliano Ziccardi Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package it.jnrpe.client;

import it.jnrpe.ReturnValue;
import it.jnrpe.Status;
import it.jnrpe.net.JNRPERequest;
import it.jnrpe.net.JNRPEResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.cli2.CommandLine;
import org.apache.commons.cli2.DisplaySetting;
import org.apache.commons.cli2.Group;
import org.apache.commons.cli2.OptionException;
import org.apache.commons.cli2.builder.ArgumentBuilder;
import org.apache.commons.cli2.builder.DefaultOptionBuilder;
import org.apache.commons.cli2.builder.GroupBuilder;
import org.apache.commons.cli2.commandline.Parser;
import org.apache.commons.cli2.option.DefaultOption;
import org.apache.commons.cli2.util.HelpFormatter;
import org.apache.commons.cli2.validation.NumberValidator;

/**
 *  This class represent a simple JNRPE client that can be used to invoke
 *  commands installed inside JNRPE by code.
 *  It is the JAVA equivalent of check_nrpe.
 *  
 *  @author Massimiliano Ziccardi
 */
public class JNRPEClient {
	private final String m_sServerIP;
	private final int m_iServerPort;
	private final boolean m_bSSL;
	private int m_iTimeout = 10;
	
	/**
	 * Instantiates a JNRPE client.
	 * @param sJNRPEServerIP The IP where the JNRPE is installed
	 * @param iJNRPEServerPort The port where the JNRPE server listens
	 */
	public JNRPEClient(final String sJNRPEServerIP, final int iJNRPEServerPort, final boolean bSSL) {
		m_sServerIP = sJNRPEServerIP;
		m_iServerPort = iJNRPEServerPort;
		m_bSSL = bSSL;
	}

	/**
	 * Creates a custom TrustManager that trusts any certificate
	 * @return The custom trustmanager
	 */
	private TrustManager getTrustManager()
	{
		// Trust all certificates
		return new X509TrustManager() {
			
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}
			
			public void checkServerTrusted(X509Certificate[] chain, String authType)
					throws CertificateException {
				
			}
			
			public void checkClientTrusted(X509Certificate[] chain, String authType)
					throws CertificateException {
			}
		};
	}
	
	/**
	 * Inovoke a command installed in JNRPE.
	 * 
	 * @param sCommandName The name of the command to be invoked
	 * @param arguments The arguments to pass to the command (will substitute the $ARGSx$ parameters)
	 * @return The value returned by the server
	 * @throws JNRPEClientException Thrown on any communication error.
	 */
	public ReturnValue sendCommand(String sCommandName, String... arguments) throws JNRPEClientException 
	{
		SocketFactory socketFactory = null;
		
		Socket s = null;
		try
		{
			if (!m_bSSL)
			{
				socketFactory = SocketFactory.getDefault();
			}
			else
			{
				SSLContext sslContext = SSLContext.getInstance("SSL");
		        sslContext.init(null, null,
		                    	new java.security.SecureRandom());

		        sslContext.init(null, new TrustManager[] {getTrustManager()}, new SecureRandom());
				
				socketFactory = sslContext.getSocketFactory();
			}
			
			s = socketFactory.createSocket();
			s.setSoTimeout(m_iTimeout * 1000);
			s.connect(new InetSocketAddress(m_sServerIP, m_iServerPort));
			JNRPERequest req = new JNRPERequest(sCommandName, arguments);
	
			s.getOutputStream().write(req.toByteArray());
	
			InputStream in = s.getInputStream();
			JNRPEResponse res = new JNRPEResponse(in);
	
			return new ReturnValue(Status.fromIntValue(res.getResultCode()), res.getStringMessage());
		}
		catch (Exception e)
		{
			//e.printStackTrace();
			throw new JNRPEClientException(e);
		}
		finally
		{
			if (s != null)
			{
				try 
				{
					s.close();
				} 
				catch (IOException e) 
				{
					// Ignore
				}
			}
		}
	}

	/**
	 * Sets the connection timeout in seconds
	 * @param iTimeout The new connection timeout. Default : 10
	 */
	public void setTimeout(int iTimeout)
	{
		m_iTimeout = iTimeout;
	}
	
	/**
	 * Returns the currently configured connection timeout in seconds
	 * @return The connection timeout
	 */
	public int getTimeout()
	{
		return m_iTimeout;
	}
	
//	private static void printSourceMessage(Throwable e)
//	{
//		if (e.getCause() == null)
//			System.out.println (e.getClass().getName() + " : " + e.getMessage());
//		else
//			printSourceMessage(e.getCause());
//	}
	
	
	private static Group configureCommandLine()
    {
        DefaultOptionBuilder oBuilder = new DefaultOptionBuilder();
        ArgumentBuilder aBuilder = new ArgumentBuilder();
        GroupBuilder gBuilder = new GroupBuilder();

        DefaultOption nosslOption = oBuilder.withLongName("nossl").withShortName(
                "n").withDescription("Do no use SSL")
                .create();

        DefaultOption unknownOption = oBuilder.withLongName("unknown").withShortName(
                "u").withDescription("Make socket timeouts return an UNKNOWN state instead of CRITICAL")
                .create();
        
        DefaultOption hostOption = oBuilder.withLongName("host").withShortName("H")
                    .withDescription("The address of the host running the JNRPE/NRPE daemon")
                    .withArgument(aBuilder
                                    .withName("host")
                                    .withMinimum(1)
                                    .withMaximum(1)
                                    .create())
                .create();

        NumberValidator positiveInt = NumberValidator.getIntegerInstance();
        positiveInt.setMinimum(0);
        DefaultOption portOption = oBuilder.withLongName("port")
                .withShortName("p")
                .withDescription("The port on which the daemon is running (default=5666)")
                    .withArgument(aBuilder
                                    .withName("port")
                                    .withMinimum(1)
                                    .withMaximum(1)
                                    .withDefault(new Long(5666))
                                    .withValidator(positiveInt)
                                    .create())
                .create();

        DefaultOption timeoutOption = oBuilder.withLongName("timeout").withShortName(
                "t").withDescription("Number of seconds before connection times out (default=10)")
                .withArgument(aBuilder
                                    .withName("timeout")
                                    .withMinimum(1)
                                    .withMaximum(1)
                                    .withDefault(new Long(10))
                                    .withValidator(positiveInt)
                                    .create())
                .create();

        DefaultOption commandOption = oBuilder.withLongName("command").withShortName(
                "c").withDescription("The name of the command that the remote daemon should run")
                .withArgument(aBuilder
                                    .withName("command")
                                    .withMinimum(1)
                                    .withMaximum(1)
                                    .create())
                .create();
        
        DefaultOption argsOption = oBuilder.withLongName("arglist").withShortName(
                "a").withDescription("Optional arguments that should be passed to the command.  Multiple arguments should be separated by a space (' ').  If provided, this must be the last option supplied on the command line.")
                .withArgument(aBuilder
                                    .withName("arglist")
                                    .withMinimum(1)
                                    .create())
                .create();
        
        DefaultOption helpOption = oBuilder.withLongName("help").withShortName(
                "h").withDescription("Shows this help")
                .create();
        
        Group executionOption = gBuilder
                    .withOption(nosslOption)
                    .withOption(unknownOption)
                    .withOption(hostOption)
                    .withOption(portOption)
                    .withOption(timeoutOption)
                    .withOption(commandOption)
                    .withOption(argsOption)
                    .create();

        Group mainGroup = gBuilder.withOption(executionOption).withOption(
                helpOption).withMinimum(1).withMaximum(1).create();

        return mainGroup;
    }
	
	private static void printVersion()
    {
	    
        System.out.println("jcheck_nrpe version " + JNRPEClient.class.getPackage().getImplementationVersion());
        System.out.println("Copyright (c) 2013 Massimiliano Ziccardi");
        System.out.println("Licensed under the Apache License, Version 2.0");
        System.out.println();
    }
	
	private static void printUsage(Exception e)
    {
        printVersion();
        
        StringBuffer sbDivider = new StringBuffer("=");
        
        if (e != null) System.out.println(e.getMessage() + "\n");

        HelpFormatter hf = new HelpFormatter();
        while (sbDivider.length() < hf.getPageWidth())
            sbDivider.append("=");

        // DISPLAY SETTING
        hf.getDisplaySettings().clear();
        hf.getDisplaySettings().add(DisplaySetting.DISPLAY_GROUP_EXPANDED);
        hf.getDisplaySettings().add(DisplaySetting.DISPLAY_PARENT_CHILDREN);

        // USAGE SETTING

        hf.getFullUsageSettings().clear();
        hf.getFullUsageSettings().add(DisplaySetting.DISPLAY_PARENT_ARGUMENT);
        hf.getFullUsageSettings()
                .add(DisplaySetting.DISPLAY_ARGUMENT_BRACKETED);
        hf.getFullUsageSettings().add(DisplaySetting.DISPLAY_PARENT_CHILDREN);
        hf.getFullUsageSettings().add(DisplaySetting.DISPLAY_GROUP_EXPANDED);

        hf.setDivider(sbDivider.toString());

        hf.setGroup(configureCommandLine());
        hf.print();
        System.exit(0);
    }
	
	public static void main(String[] args)  throws Exception {
		
	    Parser parser = new Parser();
	    parser.setGroup(configureCommandLine());
	    
		boolean timeoutAsUnknown = false;
		
		try
		{
		    CommandLine cli = parser.parse(args);

		    if (cli.hasOption("--help"))
		        printUsage(null);
		    
			
			timeoutAsUnknown = cli.hasOption("--unknown");
			
			String sHost = (String) cli.getValue("--host");
			Long port = (Long) cli.getValue("--port", new Long(5666));
			String sCommand = (String) cli.getValue("--command");
//			String sArgs = (String) cli.getValue("--arglist");
//			
//			if (sArgs.startsWith("'") && sArgs.endsWith("'"))
//			{
//			    sArgs = sArgs.substring(1, sArgs.length() - 1);
//			}
			
			JNRPEClient client = new JNRPEClient(sHost, port.intValue(), !cli.hasOption("--nossl"));
			client.setTimeout(  ((Long) cli.getValue("--timeout", new Long(10))).intValue());
			
			ReturnValue ret = client.sendCommand(sCommand, (String[]) cli.getValues("--arglist").toArray(new String[0]));
			
			System.out.println (ret.getMessage());
			System.exit(ret.getStatus().intValue());
		}
		catch (JNRPEClientException exc)
		{
			Status returnStatus = null;
			
			if (timeoutAsUnknown && exc.getCause() != null && exc.getCause() instanceof SocketTimeoutException)
				returnStatus = Status.UNKNOWN;
			else
				returnStatus = Status.CRITICAL;
			
			System.out.println(exc.getMessage());
			System.exit(returnStatus.intValue());
		}
		catch (OptionException oe)
		{
			printUsage(oe);
		}
	}

}
