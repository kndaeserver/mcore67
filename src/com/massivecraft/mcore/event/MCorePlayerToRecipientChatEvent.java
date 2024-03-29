package com.massivecraft.mcore.event;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class MCorePlayerToRecipientChatEvent extends MCoreEvent
{
	// -------------------------------------------- //
	// REQUIRED EVENT CODE
	// -------------------------------------------- //
	
	private static final HandlerList handlers = new HandlerList();
	@Override public HandlerList getHandlers() { return handlers; }
	public static HandlerList getHandlerList() { return handlers; }
	
	// -------------------------------------------- //
	// FIELD
	// -------------------------------------------- //
	
	private final Player sender;
	public Player getSender() { return this.sender; }
	
	private final CommandSender recipient;
	public CommandSender getRecipient() { return this.recipient; }
	
	private String message;
	public String getMessage() { return this.message; }
	public void setMessage(String message) { this.message = message; }
	
	private String format;
	public String getFormat() { return this.format; }
	public void setFormat(String format) { this.format = format; }
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public MCorePlayerToRecipientChatEvent(Player sender, CommandSender recipient, String message, String format)
	{
		super(true);
		this.sender = sender;
		this.recipient = recipient;
		this.message = message;
		this.format = format;
	}
	
}
