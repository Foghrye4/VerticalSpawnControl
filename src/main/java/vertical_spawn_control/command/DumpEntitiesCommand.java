package vertical_spawn_control.command;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import vertical_spawn_control.VSCMod;

public class DumpEntitiesCommand extends CommandBase {
	@Override
	public String getName() {
		return "vsc";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 4;
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "/vsc dumpentities";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length < 1)
			throw new WrongUsageException(this.getUsage(sender), new Object[0]);
		if (args[0].equalsIgnoreCase("dumpentities")) {
			File file = new File(server.getDataDirectory(),"/logs/vsc_entity_list.txt");
			try(FileWriter fwriter = new FileWriter(file)) {
				for(ResourceLocation entityEntryKey :ForgeRegistries.ENTITIES.getKeys()) {
					fwriter.write(entityEntryKey.toString());
					fwriter.write("\n");
				}
				fwriter.close();
			} 
			catch(IOException e){
				VSCMod.logger.error(e);
			}
		} else {
			throw new WrongUsageException(this.getUsage(sender), new Object[0]);
		}
	}
}
