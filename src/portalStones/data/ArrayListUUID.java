package portalStones.data;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;

public class ArrayListUUID implements PersistentDataType<byte[], ArrayList<UUID>> {
	public Class<byte[]> getPrimitiveType() {
		return byte[].class;
	}

	@SuppressWarnings("unchecked")
	public Class<ArrayList<UUID>> getComplexType() {
		return (Class<ArrayList<UUID>>) new ArrayList<UUID>().getClass();
	}

	public byte[] toPrimitive(ArrayList<UUID> complex, PersistentDataAdapterContext context) {
		ByteBuffer bb = ByteBuffer.wrap(new byte[16 * complex.size()]);
		for (UUID id : complex) {
			bb.putLong(id.getMostSignificantBits());
			bb.putLong(id.getLeastSignificantBits());
		}
		return bb.array();
	}

	public ArrayList<UUID> fromPrimitive(byte[] primitive, PersistentDataAdapterContext context) {
		ArrayList<UUID> ids = new ArrayList<>();
		ByteBuffer bb = ByteBuffer.wrap(primitive);
		for (int i = 0; i < primitive.length; i += 16) {
			long firstLong = bb.getLong();
			long secondLong = bb.getLong();
			ids.add(new UUID(firstLong, secondLong));
		}
		return ids;
	}
}
