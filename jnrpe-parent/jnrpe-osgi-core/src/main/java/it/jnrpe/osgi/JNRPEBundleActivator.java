/*
 * Copyright (c) 2013 Massimiliano Ziccardi
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
package it.jnrpe.osgi;

import it.jnrpe.JNRPE;
import it.jnrpe.commands.CommandDefinition;
import it.jnrpe.commands.CommandRepository;
import it.jnrpe.plugins.IPluginRepository;
import it.jnrpe.plugins.PluginRepository;
import it.jnrpe.utils.StringUtils;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.util.tracker.BundleTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.OSGILogFactory;

public class JNRPEBundleActivator implements BundleActivator, ManagedService {

    private final static String PID = "it.jnrpe.osgi";

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory
            .getLogger(JNRPEBundleActivator.class);

    private BundleTracker bundleTracker;

    private IPluginRepository pluginRepository;
    private CommandRepository commandRepository;

    private JNRPE jnrpeEngine;

    /**
     * Automatically called by the OSGI layer when a new configuration is ready.
     */
    public void updated(final Dictionary properties) throws ConfigurationException {
        if (properties == null) {
            LOG.info("Empty configuration received. JNRPE Server not started");
            return;
        }

        LOG.info("New configuration received");

        //String acceptParamsString = "" + properties.get("accept_params");
        String bindAddress = (String) properties.get("bind_address");
        String allowAddress = (String) properties.get("allow_address");

        // Parsing command definition...
        for (CommandDefinition cd : parseCommandDefinitions(properties)) {
            LOG.info("Adding command definition for command '{}'", cd.getName());
            commandRepository.addCommandDefinition(cd);
        }

        JNRPE engine = new JNRPE(pluginRepository, commandRepository);

        if (allowAddress == null || allowAddress.trim().length() == 0) {
            allowAddress = "127.0.0.1";
        }

        for (String addr : allowAddress.split(",")) {

            LOG.info("Allowing requests from : {}", addr);

            if (addr.trim().length() == 0) {
                continue;
            }

            engine.addAcceptedHost(addr);
        }


        if (bindAddress != null && bindAddress.trim().length() != 0) {
            JNRPEBindingAddress address = new JNRPEBindingAddress(bindAddress);
            try {

                LOG.info("Listening on : {}:{} - SSL:{}", address.getAddress(), address.getPort(), address.isSsl());

                engine.listen(address.getAddress(), address.getPort(), address.isSsl());
            } catch (UnknownHostException e) {
                LOG.error("Bad binding address: {}", bindAddress);
            }
        } else {
            throw new ConfigurationException("bind_address", "Binding address can't be empty");
        }
        jnrpeEngine = engine;
    }

    private Collection<CommandDefinition> parseCommandDefinitions(final Dictionary<String, String> props) {

        List<CommandDefinition> res = new ArrayList<CommandDefinition>();

        final String prefix="command.";
        final int prefixLen = prefix.length();

        Enumeration<String> en = props.keys();
        while (en.hasMoreElements()) {
            String key = en.nextElement();
            if (key.startsWith(prefix)) {
                String commandName = key.substring(prefixLen);

                String commandLine = props.get(key);
                String[] elements = StringUtils.split(commandLine, false);
                String pluginName = elements[0];

                StringBuffer cmdLine = new StringBuffer();

                for (int i = 1; i < elements.length; i++) {
                    cmdLine.append(quoteAndEscape(elements[i]))
                    .append(" ");
                }

                CommandDefinition cd = new CommandDefinition(commandName, pluginName);
                cd.setArgs(cmdLine.toString());
                res.add(cd);
            }
        }

        return res;
    }

    /**
     * Quotes a string.
     * @param string The string to be quoted
     * @return The quoted string
     */
    private String quoteAndEscape(final String string) {
        if (string.indexOf(' ') == -1) {
            return string;
        }

        String res = "\"" + string.replaceAll("\"", "\\\"") + "\"";
        return res;
    }

    /**
     * Initializes the bundle.
     */
    public void start(final BundleContext context) throws Exception {

        OSGILogFactory.initOSGI(context);

        pluginRepository = new PluginRepository();
        commandRepository = new CommandRepository();

        bundleTracker = new JNRPEBundleTracker(context, pluginRepository, commandRepository);

        // Register the managed service...
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_PID, PID);

        ServiceRegistration servReg = context.registerService(
                ManagedService.class.getName(), this, props);
        servReg.setProperties(props);
        LOG.info("JNRPE bundle started");
    }

    /**
     * Stops the JNRPE bundle.
     */
    public void stop(final BundleContext context) throws Exception {
        if (jnrpeEngine != null) {
            jnrpeEngine.shutdown();
        }

    }

}
