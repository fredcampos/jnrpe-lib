/*
 * Copyright (c) 2011 Massimiliano Ziccardi
 *  
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */
package it.jnrpe.server;

import java.io.File;
import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;

class XmlJNRPEConfiguration extends JNRPEConfiguration
{

    @Override
    public void load(File confFile) throws ConfigurationException
    {
        // Parse an ini file
        ServerSection serverConf = getServerSection();
        CommandsSection commandSection = getCommandSection();
        try
        {
            XMLConfiguration confParser = new XMLConfiguration(confFile);

            List<String> vBindAddresses = confParser.getList("server.bind[@address]");
            int iBindListSize = vBindAddresses.size();
            
            for (int i = 0; i < iBindListSize; i++)
            {
                String sAddress = confParser.getString("server.bind(" + i + ")[@address]");
                String sSSL =confParser.getString("server.bind(" + i + ")[@SSL]");
                if (sSSL == null)
                    sSSL = "false";
                
                serverConf.addBindAddress(sAddress, sSSL.equalsIgnoreCase("true"));
            }
            
            String sAcceptParams = confParser.getString("server[@accept-params]", "true");

            serverConf.setAcceptParams(sAcceptParams.equalsIgnoreCase("true") || sAcceptParams.equalsIgnoreCase("yes"));

            serverConf.setPluginPath(confParser.getString("server.plugin[@path]", "."));

            List<String> vAllowedAddresses = confParser.getList("server.allow[@ip]");
            if (vAllowedAddresses != null)
            {
                for (String sAddress : vAllowedAddresses)
                {
                    serverConf.addAllowedAddress(sAddress);
                }
            }

            // Get the commands count ( searching for a better way...)
            // int iCommandsCount =
            Object obj = confParser.getProperty("commands.command[@name]");

            int iCount = 0;

            if (obj != null)
            {
                if (obj instanceof List)
                {
                    iCount = ((List) obj).size();
                }
                else
                    iCount = 1;
            }

            // Loop through configured commands
            for (int i = 0; i < iCount; i++)
            {
                String sCommandName = (String) confParser.getProperty("commands.command(" + i + ")[@name]");
                String sPluginName = (String) confParser.getProperty("commands.command(" + i + ")[@plugin_name]");
                String sWholeCommandLine = (String) confParser.getProperty("commands.command(" + i + ")[@params]");
                if (sWholeCommandLine == null)
                    sWholeCommandLine = "";
                else
                    sWholeCommandLine += " ";
                
                // Loop through command arguments...
                
                Object argsObj = confParser.getProperty("commands.command(" + i + ").arg[@name]");
                int iArgsCount = 0;
                if (argsObj != null)
                {
                    if (argsObj instanceof List)
                    {
                        iArgsCount = ((List) argsObj).size();
                    }
                    else
                        iArgsCount = 1;
                }

                StringBuffer commandLineBuffer = new StringBuffer(sWholeCommandLine);
                
                for (int j = 0; j < iArgsCount; j++)
                {
                    String sArgName = (String) confParser.getProperty("commands.command(" + i + ").arg(" + j + ")[@name]");
                    String sArgValue = (String) confParser.getProperty("commands.command(" + i + ").arg(" + j + ")[@value]");

                    if (sArgName.length() > 1)
                        commandLineBuffer.append("--");
                    else
                        commandLineBuffer.append("-");
                    
                    commandLineBuffer.append(sArgName).append(" ");
                    
                    if (sArgValue != null)
                    {
                        boolean bQuote = sArgValue.indexOf(' ') != -1;
                        
                        // FIXME : handle quote escaping...
                        if (bQuote)
                            commandLineBuffer.append("\"");
                        
                        commandLineBuffer.append(sArgValue);

                        if (bQuote)
                            commandLineBuffer.append("\"");
                        
                        commandLineBuffer.append(" ");
                        
                    }
                }
                sWholeCommandLine = commandLineBuffer.toString();
                
                commandSection.addCommand(sCommandName, sPluginName, sWholeCommandLine);
            }
        }
        catch (org.apache.commons.configuration.ConfigurationException e)
        {
            throw new ConfigurationException(e);
        }
    }

}
