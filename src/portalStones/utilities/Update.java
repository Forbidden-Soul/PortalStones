package portalStones.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.logging.Level;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import org.bukkit.command.CommandSender;
import portalStones.Main;

public class Update {
	public static boolean checkForUpdates() {
		try {
			DirContext dirContext = new InitialDirContext();
			Attributes attributes = dirContext.getAttributes("dns:///tournaments.gq", new String[] { "TXT" });
			NamingEnumeration<? extends Attribute> allAttributes = attributes.getAll();
			Attribute attribute = null;
			while (allAttributes.hasMore()) {
				attribute = allAttributes.next();
				for (int i = 0; i < attribute.size(); i++) {
					if (attribute.get(i).toString().startsWith("\"portalstones=")) {
						String update = attribute.get(i).toString();
						update = update.replaceAll("\"", "");
						update = update.replaceAll("portalstones=", "");
						String[] updates = update.split(" ");
						float currentVersion = Float.valueOf(updates[0]).floatValue();
						String[] versionPieces = Main.plugin.getDescription().getVersion().split("\\.", 3);
						float updateVersion = Float.valueOf(String.valueOf(versionPieces[0]) + "." + versionPieces[1]);
						if (currentVersion > updateVersion) {
							Main.plugin.getLogger().log(Level.INFO, "New version " + currentVersion + " available.");
							try {
								Main.plugin.getLogger().log(Level.INFO, "Downloding update...");
								ReadableByteChannel readableByteChannel = Channels
										.newChannel((new URL(updates[1])).openStream());
								FileOutputStream fileOutputStream = new FileOutputStream(
										new File(Main.plugin.getDataFolder(), "update.jar"));
								fileOutputStream.getChannel().transferFrom(readableByteChannel, 0L, Long.MAX_VALUE);
								fileOutputStream.close();
							} catch (MalformedURLException e) {
								Main.plugin.getLogger().log(Level.WARNING, "Invalid update link.");
							} catch (IOException e) {
								Main.plugin.getLogger().log(Level.WARNING, "Update failed to download.");
							}
							Main.plugin.getLogger().log(Level.INFO, "Download complete. Applying Update...");
							File source = new File(Main.plugin.getDataFolder(), "update.jar");
							File target = null;
							try {
								target = new File(Main.plugin.getClass().getProtectionDomain().getCodeSource()
										.getLocation().toURI());
							} catch (URISyntaxException e1) {
								Main.plugin.getLogger().log(Level.WARNING, "Updated failed to apply.");
							}
							try {
								@SuppressWarnings("resource")
								FileChannel in = new FileInputStream(source).getChannel();
								@SuppressWarnings("resource")
								FileChannel out = new FileOutputStream(target).getChannel();
								in.transferTo(0L, in.size(), out);
								out.close();
								in.close();
								source.delete();
								Main.plugin.getLogger().log(Level.INFO, "Update applied. Restarting the server.");
								Main.plugin.getServer().dispatchCommand(
										(CommandSender) Main.plugin.getServer().getConsoleSender(), "restart");
								return false;
							} catch (IOException e) {
								Main.plugin.getLogger().log(Level.WARNING, "Updated failed to apply.");
							}
						} else {
							Main.plugin.getLogger().log(Level.INFO, "Up to date.");
						}
					} else {
						Main.plugin.getLogger().log(Level.SEVERE,
								"Update service been bamboozled bruh. Please contact brood_harvester@hotmail.com.");
					}
				}
			}
		} catch (NamingException e) {
			Main.plugin.getLogger().log(Level.WARNING, "Could not contact update service.\n");
		}
		return false;
	}
}
