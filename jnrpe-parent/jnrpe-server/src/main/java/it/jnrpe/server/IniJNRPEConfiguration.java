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

import it.jnrpe.utils.StringUtils;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;

/**
 * This class represent an INI file configuration
 * 
 * @author Massimiliano Ziccardi
 */
class IniJNRPEConfiguration extends JNRPEConfiguration
{

    @Override
    public void load(File confFile) throws ConfigurationException
    {
     // Parse an ini file
        ServerSection serverConf = getServerSection();
        CommandsSection commandSection = getCommandSection();
        try
        {
            HierarchicalINIConfiguration confParser = new HierarchicalINIConfiguration(confFile);
            
            List<String> vBindAddresses = confParser.getList("server.bind-address");
            if (vBindAddresses != null)
            {
                for (String sAddress : vBindAddresses)
                {
                    serverConf.addBindAddress(sAddress);
                }
            }
            
            // Defaults accept params
            String sAcceptParams = confParser.getString("server.accept-params", "true");
            serverConf.setAcceptParams(sAcceptParams.equalsIgnoreCase("true") || sAcceptParams.equalsIgnoreCase("yes"));
            serverConf.setPluginPath(confParser.getString("server.plugin-path", "."));

            List<String> vAllowedAddresses = confParser.getList("server.allow-address");
            if (vAllowedAddresses != null)
            {
                for (String sAddress : vAllowedAddresses)
                {
                    serverConf.addAllowedAddress(sAddress);
                }
            }
            
            SubnodeConfiguration sc = confParser.getSection("commands");

            if (sc != null)
            {
                for (Iterator<String> it = sc.getKeys(); it.hasNext(); )
                {
                    String sCommandName = it.next();
                    
                    String sCommandLine = sc.getString(sCommandName);
                    // first element of the command line is the plugin name
                    String[] vElements = StringUtils.split(sCommandLine, false);
                    String sPluginName = vElements[0];
    
                    // Rebuilding the commandline
                    StringBuffer cmdLine = new StringBuffer();
                    
                    for (int i = 1; i < vElements.length; i++)
                        cmdLine.append(quoteAndEscape(vElements[i])).append(" ");
                    
                    commandSection.addCommand(sCommandName, sPluginName, cmdLine.toString());
                }
            }
        }
        catch (org.apache.commons.configuration.ConfigurationException e)
        {
            throw new ConfigurationException(e);
        }
        
    }

    private String quoteAndEscape(final String string)
    {
        if (string.indexOf(' ') == -1)
            return string;
        
        String res = "\"" + string.replaceAll("\"", "\\\"") + "\"";
        return res;
    }
    
}
