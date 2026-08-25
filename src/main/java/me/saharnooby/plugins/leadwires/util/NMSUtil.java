package me.saharnooby.plugins.leadwires.util;

import lombok.NonNull;
import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NMSUtil {

	private static final String VERSION;
	private static final int MAJOR;
	private static final int MINOR;
	private static final int RELEASE;

	static {

		String name = Bukkit.getServer().getClass().getName();

		if (name.equals("org.bukkit.craftbukkit.CraftServer")) {
			// Example: "1.21-R0.1-SNAPSHOT" or "26.2.build.2622-stable"
			String bukkitVersion = Bukkit.getBukkitVersion();
			String versionPrefix = bukkitVersion.split("-")[0];
			String[] allParts = versionPrefix.split("\\.");

			// Collect only leading numeric parts
			java.util.List<String> numericParts = new java.util.ArrayList<>();
			for (String part : allParts) {
				if (part.matches("\\d+")) {
					numericParts.add(part);
				} else {
					break;
				}
			}

			VERSION = String.join(".", numericParts);
			MAJOR = !numericParts.isEmpty() ? Integer.parseInt(numericParts.get(0)) : 1;
			MINOR = numericParts.size() > 1 ? Integer.parseInt(numericParts.get(1)) : 0;
			RELEASE = numericParts.size() > 2 ? Integer.parseInt(numericParts.get(2)) : 0;
		} else {
			VERSION = name.substring(23, name.lastIndexOf('.'));
			Pattern pattern = Pattern.compile("v1_(\\d{1,2})_R(\\d{1,2})");
			Matcher matcher = pattern.matcher(VERSION);
			if (!matcher.matches()) {
				throw new IllegalStateException("Invalid server version \"" + VERSION + "\", server class is \"" + name + "\"");
			}
			MAJOR = 1;
			MINOR = Integer.parseInt(matcher.group(1));
			RELEASE = Integer.parseInt(matcher.group(2));
		}
	}

	/**
	 * @return Server version, for example 'v1_14_R1'.
	 */
	public static String getVersion() {
		return VERSION;
	}

	/**
	 * @return Minor server version. For legacy 1.x versions returns the minor (e.g. 21 from 1.21).
	 * For new versioning (26.x) returns the major version so all legacy ">= N" checks pass.
	 */
	public static int getMinorVersion() {
		return MAJOR > 1 ? MAJOR : MINOR;
	}

	/**
	 * @return Release version, for example '2' from 'v1_16_R2'.
	 */
	public static int getReleaseVersion() {
		return RELEASE;
	}

	public static Class<?> getNMSClass(@NonNull String name) throws ClassNotFoundException {
		return Class.forName("net.minecraft." + (NMSUtil.getMinorVersion() >= 17 ? "" : "server." + getVersion() + '.') + name);
	}

}
