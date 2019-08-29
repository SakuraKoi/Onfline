package sakura.kooi.Onfline.bungeecord.utils.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;

public class HasResultProcessor implements ResultProcessor<Boolean> {

	@Override
	public Boolean processValue(final ResultSet result) throws SQLException {
		while (result.next())
			return true;
		return false;
	}

}
