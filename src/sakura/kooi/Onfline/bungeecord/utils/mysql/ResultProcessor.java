package sakura.kooi.Onfline.bungeecord.utils.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface ResultProcessor<T> {
	public T processValue(ResultSet result) throws SQLException;
}
