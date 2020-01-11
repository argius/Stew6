package stew6;

import net.argius.stew.*;

/**
 * Utility methods for Command.
 */
public final class Commands {

    private Commands() { // empty
    }

    /**
     * Invokes specified commands.
     * @param env
     * @param commandString
     * @return true if it continues, or false if exit this application
     * @throws CommandException
     */
    public static boolean invoke(Environment env, String commandString) throws CommandException {
        CommandProcessor processor = new CommandProcessor(env);
        return processor.invoke(commandString);
    }

}
