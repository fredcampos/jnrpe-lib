/*
 * Copyright (c) 2011 Massimiliano Ziccardi Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package it.jnrpe.commands;

import java.util.HashMap;
import java.util.Map;

/**
 * This object manages all the configured commands.
 *
 * @author Massimiliano Ziccardi
 */
public final class CommandRepository
{
    /**
     * Contains all the commands. The KEY is the command name, while the value.
     * is the {@link CommandDefinition}.
     */
    private Map<String, CommandDefinition> m_mCommandDefs
                = new HashMap<String, CommandDefinition>();

    /**
     * Adds a new command definition to the repository.
     *
     * @param commandDef The command definition to be added
     */
    public void addCommandDefinition(final CommandDefinition commandDef)
    {
        m_mCommandDefs.put(commandDef.getName(), commandDef);
    }

    /**
     * Returns the named command definition.
     *
     * @param sName
     *            The command name
     * @return The command definition associated with <code>sName</code>. 
     * <code>null</code> if not found.
     */
    public CommandDefinition getCommand(final String sName)
    {
        return m_mCommandDefs.get(sName);
    }
}
