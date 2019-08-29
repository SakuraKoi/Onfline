package sakura.kooi.Onfline.bungeecord.utils.mysql;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface ValueSupplier {
	public void supplyValue(PreparedStatement query) throws SQLException;
}
