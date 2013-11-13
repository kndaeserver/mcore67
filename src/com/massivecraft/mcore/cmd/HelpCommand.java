package com.massivecraft.mcore.cmd;

import java.util.ArrayList;

import com.massivecraft.mcore.cmd.MCommand;
import com.massivecraft.mcore.cmd.arg.ARInteger;
import com.massivecraft.mcore.util.Txt;

public class HelpCommand extends MCommand
{
	private HelpCommand()
	{
		super();
		this.addAliases("?", "h", "����");
		this.setDesc("");
		this.addOptionalArg("ҳ��","1");
	}
	
	@Override
	public void perform()
	{
		if (this.commandChain.size() == 0) return;
		MCommand parentCommand = this.commandChain.get(this.commandChain.size()-1);
		
		ArrayList<String> lines = new ArrayList<String>();
		
		for (String helpline : parentCommand.getHelp())
		{
			lines.add(Txt.parse("<a>#<i> "+helpline));
		}
		
		for(MCommand subCommand : parentCommand.getSubCommands())
		{
			if (subCommand.visibleTo(sender))
			{
				lines.add(subCommand.getUseageTemplate(this.commandChain, true, true, sender));
			}
		}
		
		Integer pagenumber = this.arg(0, ARInteger.get(), 1);
		if (pagenumber == null) return;
		sendMessage(Txt.getPage(lines, pagenumber, "��������\""+parentCommand.getAliases().get(0)+"\"�İ���", sender));
	}
	
	// -------------------------------------------- //
	// INSTANCE
	// -------------------------------------------- //
	
	private static HelpCommand i = new HelpCommand();
	public static HelpCommand getInstance() { return i; }
	public static HelpCommand get() { return i; }
	
}
