package com.massivecraft.mcore.cmd;

import java.util.*;
import java.util.Map.Entry;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.massivecraft.mcore.Lang;
import com.massivecraft.mcore.MCore;
import com.massivecraft.mcore.cmd.arg.ArgReader;
import com.massivecraft.mcore.cmd.arg.ArgResult;
import com.massivecraft.mcore.cmd.req.Req;
import com.massivecraft.mcore.cmd.req.ReqHasPerm;
import com.massivecraft.mcore.mixin.Mixin;
import com.massivecraft.mcore.util.BukkitCommandUtil;
import com.massivecraft.mcore.util.PermUtil;
import com.massivecraft.mcore.util.Txt;

public abstract class MCommand
{	
	// -------------------------------------------- //
	// COMMAND BEHAVIOR
	// -------------------------------------------- //
	
	// FIELD: subCommands
	// The sub-commands to this command
	protected List<MCommand> subCommands;
	public List<MCommand> getSubCommands() { return this.subCommands; }
	public void setSubCommands(List<MCommand> subCommands) { this.subCommands = subCommands; }
	
	public void addSubCommand(MCommand subCommand)
	{
		subCommand.commandChain.addAll(this.commandChain);
		subCommand.commandChain.add(this);
		this.subCommands.add(subCommand);
	}
	
	// FIELD: aliases
	// The different names this commands will react to  
	protected List<String> aliases;
	public List<String> getAliases() { return this.aliases; }
	public void setAliases(List<String> aliases) { this.aliases = aliases; }
	
	public void addAliases(String... aliases) { this.aliases.addAll(Arrays.asList(aliases)); }
	public void addAliases(List<String> aliases) { this.aliases.addAll(aliases); }
	
	// FIELD: requiredArgs
	// These args must always be sent
	protected List<String> requiredArgs;
	public List<String> getRequiredArgs() { return this.requiredArgs; }
	public void setRequiredArgs(List<String> requiredArgs) { this.requiredArgs = requiredArgs; }
	
	public void addRequiredArg(String arg) { this.requiredArgs.add(arg); }
	
	// FIELD: optionalArgs
	// These args are optional
	protected Map<String, String> optionalArgs;
	public Map<String, String> getOptionalArgs() { return this.optionalArgs; }
	public void setOptionalArgs(Map<String, String> optionalArgs) { this.optionalArgs = optionalArgs; }
	
	public void addOptionalArg(String arg, String def) { this.optionalArgs.put(arg, def); }
	
	// FIELD: errorOnToManyArgs
	// Should an error be thrown if "to many" args are sent.
	protected boolean errorOnToManyArgs;
	public boolean getErrorOnToManyArgs() { return this.errorOnToManyArgs; }
	public void setErrorOnToManyArgs(boolean val) { this.errorOnToManyArgs = val; }
	
	// FIELD: requirements
	// All these requirements must be met for the command to be executable;
	protected List<Req> requirements;
	public List<Req> getRequirements() { return this.requirements; }
	public void getRequirements(List<Req> requirements) { this.requirements = requirements; }
	
	public void addRequirements(Req... requirements) { this.requirements.addAll(Arrays.asList(requirements)); }
	
	// FIELD: desc
	// This field may be left blank and will in such case be loaded from the permissions node instead.
	// Thus make sure the permissions node description is an action description like "eat hamburgers" or "do admin stuff".
	protected String desc = null;
	public void setDesc(String desc) { this.desc = desc; }
	
	public String getDesc()
	{
		if (this.desc != null) return this.desc;
		
		String perm = this.getDescPermission();
		if (perm != null)
		{
			String pdesc = PermUtil.getDescription(this.getDescPermission());
			if (pdesc != null)
			{
				return pdesc;
			}
		}
			
		return "*info unavailable*";
	}
	
	// FIELD: descPermission
	// This permission node IS NOT TESTED AT ALL. It is rather used in the method above.
	protected String descPermission;
	public void setDescPermission(String descPermission) { this.descPermission = descPermission; }
	
	public String getDescPermission()
	{
		if (this.descPermission != null) return this.descPermission;
		// Otherwise we try to find one.
		for (Req req : this.requirements)
		{
			if ( ! (req instanceof ReqHasPerm)) continue;
			return ((ReqHasPerm)req).getPerm();
		}
		return null;
	}
	
	// FIELD: help
	// This is a multi-line help text for the command.
	protected List<String> help = new ArrayList<String>();
	public void setHelp(List<String> val) { this.help = val; }
	public void setHelp(String... val) { this.help = Arrays.asList(val); }
	public List<String> getHelp() { return this.help; }
	
	// FIELD: visibilityMode
	protected VisibilityMode visibilityMode;
	public VisibilityMode getVisibilityMode() { return this.visibilityMode; }
	public void setVisibilityMode(VisibilityMode visibilityMode) { this.visibilityMode = visibilityMode; }
	
	// -------------------------------------------- //
	// EXECUTION INFO
	// -------------------------------------------- //
	
	// FIELD: args
	// Will contain the arguments, or and empty list if there are none.
	protected List<String> args;
	public List<String> getArgs() { return this.args; }
	public void setArgs(List<String> args) { this.args = args; }

	// FIELD: commandChain
	// The command chain used to execute this command
	protected List<MCommand> commandChain = new ArrayList<MCommand>();
	public List<MCommand> getCommandChain() { return this.commandChain; }
	public void setCommandChain(List<MCommand> commandChain) { this.commandChain = commandChain; }
	
	// FIELDS: sender, me, senderIsConsole
	public CommandSender sender;
	public Player me;
	public boolean senderIsConsole;
	
	// -------------------------------------------- //
	// BUKKIT INTEGRATION
	// -------------------------------------------- //
	
	public boolean register()
	{
		return register(MCore.get(), true);
	}
	
	public boolean register(Plugin plugin)
	{
		return this.register(plugin, true);
	}
	
	public boolean register(boolean override)
	{
		return this.register(MCore.get(), override);
	}
	
	public boolean register(Plugin plugin, boolean override)
	{
		boolean ret = false;
		
		SimpleCommandMap scm = BukkitCommandUtil.getBukkitCommandMap();
		
		for (String alias : this.getAliases())
		{
			BukkitGlueCommand bgc = new BukkitGlueCommand(alias, this, plugin);
			
			if (override)
			{
				// Our commands are more important than your commands :P
				Map<String, Command> knownCommands = BukkitCommandUtil.getKnownCommandsFromSimpleCommandMap(scm);
				String lowerLabel = bgc.getName().trim().toLowerCase();
				knownCommands.remove(lowerLabel);
			}
			
			if (scm.register(MCore.get().getDescription().getName(), bgc))
			{
				ret = true;
			}
		}
		
		return ret;
	}
	
	// -------------------------------------------- //
	// CONSTRUCTORS AND EXECUTOR
	// -------------------------------------------- //
	
	public MCommand()
	{
		this.descPermission = null;
		
		this.subCommands = new ArrayList<MCommand>();
		this.aliases = new ArrayList<String>();
		
		this.requiredArgs = new ArrayList<String>();
		this.optionalArgs = new LinkedHashMap<String, String>();
		
		this.requirements = new ArrayList<Req>();
		
		this.errorOnToManyArgs = true;
		
		this.desc = null;
		
		this.visibilityMode = VisibilityMode.VISIBLE; 
	}
	
	// The commandChain is a list of the parent command chain used to get to this command.
	public void execute(CommandSender sender, List<String> args, List<MCommand> commandChain)
	{
		// Set the execution-time specific variables
		this.sender = sender;
		this.senderIsConsole = true;
		this.me = null;
		if (sender instanceof Player)
		{
			this.me = (Player) sender;
			this.senderIsConsole = false;
		}
		
		this.fixSenderVars();
		
		this.args = args;
		this.commandChain = commandChain;

		// Is there a matching sub command?
		if (args.size() > 0 )
		{
			for (MCommand subCommand: this.subCommands)
			{
				if (subCommand.aliases.contains(args.get(0)))
				{
					args.remove(0);
					commandChain.add(this);
					subCommand.execute(sender, args, commandChain);
					return;
				}
			}
		}
		
		if ( ! validCall(this.sender, this.args)) return;
		
		perform();
	}
	
	public void fixSenderVars() {};
	
	public void execute(CommandSender sender, List<String> args)
	{
		execute(sender, args, new ArrayList<MCommand>());
	}
	
	// This is where the command action is performed.
	public abstract void perform();
	
	
	// -------------------------------------------- //
	// CALL VALIDATION
	// -------------------------------------------- //
	
	/**
	 * In this method we validate that all prerequisites to perform this command has been met.
	 */
	public boolean validCall(CommandSender sender, List<String> args)
	{
		if ( ! this.requirementsAreMet(sender, true))
		{
			return false;
		}
		
		if ( ! this.validArgs(args, sender))
		{
			return false;
		}
		
		return true;
	}
	
	public boolean visibleTo(CommandSender sender)
	{
		if (this.getVisibilityMode() == VisibilityMode.VISIBLE) return true;
		if (this.getVisibilityMode() == VisibilityMode.INVISIBLE) return false;
		return this.requirementsAreMet(sender, false);
	}
	
	public boolean requirementsAreMet(CommandSender sender, boolean informSenderIfNot)
	{
		for (Req req : this.getRequirements())
		{
			if ( ! req.apply(sender, this))
			{
				if (informSenderIfNot)
				{
					this.msg(req.createErrorMessage(sender, this));
				}
				return false;
			}
		}
		return true;
	}
	
	public boolean validArgs(List<String> args, CommandSender sender)
	{
		if (args.size() < this.requiredArgs.size())
		{
			if (sender != null)
			{
				msg(Lang.COMMAND_TO_FEW_ARGS);
				sender.sendMessage(this.getUseageTemplate());
			}
			return false;
		}
		
		if (args.size() > this.requiredArgs.size() + this.optionalArgs.size() && this.errorOnToManyArgs)
		{
			if (sender != null)
			{
				// Get the to many string slice
				List<String> theToMany = args.subList(this.requiredArgs.size() + this.optionalArgs.size(), args.size());
				msg(Lang.COMMAND_TO_MANY_ARGS, Txt.implodeCommaAndDot(theToMany, Txt.parse("<aqua>%s"), Txt.parse("<b>, "), Txt.parse("<b> and "), ""));
				msg(Lang.COMMAND_TO_MANY_ARGS2);
				sender.sendMessage(this.getUseageTemplate());
			}
			return false;
		}
		return true;
	}
	public boolean validArgs(List<String> args)
	{
		return this.validArgs(args, null);
	}
	
	// -------------------------------------------- //
	// HELP AND USAGE INFORMATION
	// -------------------------------------------- //
	
	public String getUseageTemplate(List<MCommand> commandChain, boolean addDesc, boolean onlyFirstAlias, CommandSender sender)
	{
		StringBuilder ret = new StringBuilder();
		
		List<MCommand> commands = new ArrayList<MCommand>(commandChain);
		commands.add(this);
		
		String commandGoodColor = Txt.parse("<c>");
		String commandBadColor = Txt.parse("<bad>");
		
		ret.append(commandGoodColor);
		ret.append('/');
		
		boolean first = true;
		Iterator<MCommand> iter = commands.iterator();
		while(iter.hasNext())
		{
			MCommand mc = iter.next();
			if (sender != null && !mc.requirementsAreMet(sender, false))
			{
				ret.append(commandBadColor);
			}
			else
			{
				ret.append(commandGoodColor);
			}
			
			if (first && onlyFirstAlias)
			{
				ret.append(mc.aliases.get(0));
			}
			else
			{
				ret.append(Txt.implode(mc.aliases, ","));
			}
			
			if (iter.hasNext())
			{
				ret.append(' ');
			}
			
			first = false;
		}
		
		List<String> args = new ArrayList<String>();
		
		for (String requiredArg : this.requiredArgs)
		{
			args.add("<"+requiredArg+">");
		}
		
		for (Entry<String, String> optionalArg : this.optionalArgs.entrySet())
		{
			String val = optionalArg.getValue();
			if (val == null)
			{
				val = "";
			}
			else
			{
				val = "="+val;
			}
			args.add("["+optionalArg.getKey()+val+"]");
		}
		
		if (args.size() > 0)
		{
			ret.append(Txt.parse("<p>"));
			ret.append(' ');
			ret.append(Txt.implode(args, " "));
		}
		
		if (addDesc)
		{
			ret.append(' ');
			ret.append(Txt.parse("<i>"));
			ret.append(this.getDesc());
		}
		
		return ret.toString();
	}
	
	public String getUseageTemplate(List<MCommand> commandChain, boolean addDesc, boolean onlyFirstAlias)
	{
		return getUseageTemplate(commandChain, addDesc, onlyFirstAlias, null);
	}
	
	public String getUseageTemplate(List<MCommand> commandChain, boolean addDesc)
	{
		return getUseageTemplate(commandChain, addDesc, false);
	}
	
	public String getUseageTemplate(boolean addDesc)
	{
		return getUseageTemplate(this.commandChain, addDesc);
	}
	
	public String getUseageTemplate()
	{
		return getUseageTemplate(false);
	}
	
	// -------------------------------------------- //
	// MESSAGE SENDING HELPERS
	// -------------------------------------------- //
	
	// CONVENIENCE SEND MESSAGE
	
	public boolean sendMessage(String message)
	{
		return Mixin.message(this.sender, message);
	}
	
	public boolean sendMessage(String... messages)
	{
		return Mixin.message(this.sender, messages);
	}
	
	public boolean sendMessage(Collection<String> messages)
	{
		return Mixin.message(this.sender, messages);
	}
	
	// CONVENIENCE MSG
	
	public boolean msg(String msg)
	{
		return Mixin.msg(this.sender, msg);
	}
	
	public boolean msg(String msg, Object... args)
	{
		return Mixin.msg(this.sender, msg, args);
	}
	
	public boolean msg(Collection<String> msgs)
	{
		return Mixin.msg(this.sender, msgs);
	}
	
	// -------------------------------------------- //
	// ARGUMENT READERS
	// -------------------------------------------- //
	
	// argIsSet
	
	public boolean argIsSet(int idx)
	{
		if (this.args.size() < idx+1)
		{
			return false;
		}
		return true;
	}
	
	// arg
	
	public String arg(int idx)
	{
		if ( ! this.argIsSet(idx)) return null;
		return this.args.get(idx);
	}
	
	public <T> T arg(int idx, ArgReader<T> argReader)
	{
		return this.arg(idx, argReader, null);
	}
	
	public <T> T arg(int idx, ArgReader<T> argReader, T defaultNotSet)
	{
		String str = this.arg(idx);
		return this.arg(str, argReader, defaultNotSet);
	}
	
	// argConcatFrom
	
	public String argConcatFrom(int idx)
	{
		if ( ! this.argIsSet(idx)) return null;
		int from = idx;
		int to = args.size();
		if (to <= from) return "";
		return Txt.implode(this.args.subList(from, to), " ");
	}
	
	public <T> T argConcatFrom(int idx, ArgReader<T> argReader)
	{
		return this.argConcatFrom(idx, argReader, null);
	}
	
	public <T> T argConcatFrom(int idx, ArgReader<T> argReader, T defaultNotSet)
	{
		String str = this.argConcatFrom(idx);
		return this.arg(str, argReader, defaultNotSet);
	}
	
	// Core & Other
	
	public <T> T arg(String str, ArgReader<T> argReader, T defaultNotSet)
	{
		if (str == null) return defaultNotSet;
		ArgResult<T> result = argReader.read(str, this.sender);
		if (result.hasErrors()) this.msg(result.getErrors());
		return result.getResult();
	}
	
	public <T> T arg(ArgReader<T> argReader)
	{
		ArgResult<T> result = argReader.read(null, this.sender);
		if (result.hasErrors()) this.msg(result.getErrors());
		return result.getResult();
	}
}
