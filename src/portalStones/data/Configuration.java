package portalStones.data;

public class Configuration {
	public Locale locale;
	public boolean craftable;
	public boolean drops;
	public boolean fromRedstone;
	public boolean update;
	public int dropChance;
	public enum Locale {
		English, German, French;
	}

	public Configuration(boolean craftable, boolean drops, boolean fromRedstone, int dropChance, String locale,
			boolean update) {
		switch (locale) {
		case "francais":
			this.locale = Locale.French;
			break;
		case "english":
			this.locale = Locale.English;
			break;
		case "deutsche":
			this.locale = Locale.German;
			break;
		}
		this.craftable = craftable;
		this.drops = drops;
		this.fromRedstone = fromRedstone;
		this.dropChance = dropChance;
		this.update = update;
	}
}
