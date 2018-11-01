package ldcr.Onfline.bungeecord.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ldcr.Onfline.bungeecord.OnflineBungeecord;

public class MysqlConnection {
	private final class DatabaseCheckThread extends Thread {
		private boolean run = true;

		private DatabaseCheckThread() {
			super("Onfline-Database");
		}
		private void cancel() {
			run = false;
		}
		@Override
		public void run() {
			while(run) {
				try {
					Thread.sleep(10000);
					if (connection != null) {
						isExists("Onfline");
					}
				} catch (final Exception ignored) {}
			}
		}
	}

	private String address;
	private String port;
	private String user;
	private String password;
	private String database;
	private Connection connection;
	private DatabaseCheckThread recheckThread;

	public MysqlConnection(final String address, final String port, final String user, final String password, final String database) {
		if (!loadDriverMySQL()) {
			OnflineBungeecord.log("&c错误: 无法连接到数据库, 未发现Mysql数据库驱动");
			return;
		}

		this.address = address == null ? "localhost" : address;
		this.port = port == null ? "3306" : port;
		this.user = user == null ? "root" : user;
		this.password = password == null ? "password" : password;
		this.database = database == null ? "onfline" : database;
		connect();

		recheckThread = new DatabaseCheckThread();
		if (isConnection()) {
			recheckThread.setDaemon(true);
			recheckThread.start();
			OnflineBungeecord.log("&a数据库 ["+address+":"+port+" "+database+"] 已连接");
		}
	}

	public boolean isConnection() {
		try {
			if (connection == null || connection.isClosed())
				return false;
		} catch (final SQLException e) {
			return false;
		}
		return true;
	}

	public void closeConnection() {
		if (connection!=null) {
			try {
				connection.close();
			} catch (final Exception ignored) {}
			connection = null;
		}
		if (recheckThread!=null) {
			try {
				recheckThread.cancel();
			} catch (final Exception ignored) {}
			recheckThread = null;
		}
	}

	public boolean deleteValue(final String name, final String column, final Object columnValue) {
		PreparedStatement pstmt = null;
		try {
			pstmt = connection.prepareStatement("delete from `" + name + "` where `" + column + "` = ?");
			pstmt.setObject(1, columnValue);
			pstmt.executeUpdate();
			return true;
		} catch (final Exception e) {
			new RuntimeException("数据库操作出错",e).printStackTrace();
			if (e.getMessage().contains("closed")) {
				connect();
			}
		} finally {
			try { if (pstmt != null) { pstmt.close(); } } catch (final Exception ignored) {}
		}
		return false;
	}

	public boolean setValue(final String name, final String column, final Object columnValue, final String valueColumn, final Object value) {
		PreparedStatement pstmt = null;

		try {
			pstmt = connection.prepareStatement("update `" + name + "` set `" + valueColumn + "` = ? where `" + column + "` = ?");
			pstmt.setObject(1, value);
			pstmt.setObject(2, columnValue);
			pstmt.executeUpdate();
			return true;
		} catch (final Exception e) {
			new RuntimeException("数据库操作出错",e).printStackTrace();
			if (e.getMessage().contains("closed")) {
				connect();
			}
		} finally {
			try { if (pstmt != null) { pstmt.close(); } } catch (final Exception ignored) {}
		}
		return false;
	}

	public boolean intoValue(final String name, final Object... values) {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < values.length; i++) {
			sb.append("?, ");
		}
		PreparedStatement pstmt = null;

		try {
			pstmt = connection.prepareStatement("insert into `" + name + "` values(null, " + sb.substring(0, sb.length() - 2) + ")");
			for (int i = 0; i < values.length; i++) {
				pstmt.setObject(i + 1, values[i]);
			}
			pstmt.executeUpdate();
			return true;
		} catch (final Exception e) {
			new RuntimeException("数据库操作出错",e).printStackTrace();
			if (e.getMessage().contains("closed")) {
				connect();
			}
		} finally {
			try { if (pstmt != null) { pstmt.close(); } } catch (final Exception ignored) {}
		}
		return false;
	}

	public boolean createTable(final String name, final String... columns) {
		final StringBuilder sb = new StringBuilder();
		for (final String column : columns) {
			if (!column.contains("/")) {
				sb.append("`" + column + "` text, ");
			} else {
				sb.append("`" + column.split("/")[0] + "` " + column.split("/")[1] + ", ");
			}
		}
		return execute("create table if not exists " + name + " (id int(1) not null primary key auto_increment, " + sb.substring(0, sb.length() - 2) + ")");
	}

	public boolean isExists(final String name) {
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = connection.prepareStatement("select table_name FROM information_schema.TABLES where table_name = ?");
			pstmt.setString(1, name);
			resultSet = pstmt.executeQuery();
			while (resultSet.next())
				return true;
		} catch (final Exception e) {
			new RuntimeException("数据库操作出错",e).printStackTrace();
			if (e.getMessage().contains("closed")) {
				connect();
			}
		} finally {
			try { if (resultSet != null) { resultSet.close(); }} catch (final Exception ignored) {}
			try { if (pstmt != null) { pstmt.close(); } } catch (final Exception ignored) {}
		}
		return false;
	}

	public boolean isExists(final String name, final String column, final Object columnValue) {
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = connection.prepareStatement("select * from `" + name + "` where `" + column + "` = ?");
			pstmt.setObject(1, columnValue);
			resultSet = pstmt.executeQuery();
			while (resultSet.next())
				return true;
		} catch (final Exception e) {
			new RuntimeException("数据库操作出错",e).printStackTrace();
			if (e.getMessage().contains("closed")) {
				connect();
			}
		} finally {
			try { if (resultSet != null) { resultSet.close(); }} catch (final Exception ignored) {}
			try { if (pstmt != null) { pstmt.close(); } } catch (final Exception ignored) {}
		}
		return false;
	}

	public Object getValue(final String name, final String column, final Object columnValue, final String valueColumn) {
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = connection.prepareStatement("select * from `" + name + "` where `" + column + "` = ? limit 1");
			pstmt.setObject(1, columnValue);
			resultSet = pstmt.executeQuery();
			while (resultSet.next())
				return resultSet.getObject(valueColumn);
		} catch (final Exception e) {
			new RuntimeException("数据库操作出错",e).printStackTrace();
			if (e.getMessage().contains("closed")) {
				connect();
			}
		} finally {
			try { if (resultSet != null) { resultSet.close(); } } catch (final Exception ignored) {}
			try { if (pstmt != null) { pstmt.close(); } } catch (final Exception ignored) {}
		}
		return null;
	}

	public Object getValueLast(final String name, final String column, final Object columnValue, final String valueColumn) {
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = connection.prepareStatement("select * from `" + name + "` where `" + column + "` = ? order by id desc limit 1");
			pstmt.setObject(1, columnValue);
			resultSet = pstmt.executeQuery();
			while (resultSet.next())
				return resultSet.getObject(valueColumn);
		} catch (final Exception e) {
			new RuntimeException("数据库操作出错",e).printStackTrace();
			if (e.getMessage().contains("closed")) {
				connect();
			}
		} finally {
			try { if (resultSet != null) { resultSet.close(); } } catch (final Exception ignored) {}
			try { if (pstmt != null) { pstmt.close(); } } catch (final Exception ignored) {}
		}
		return null;
	}

	public Map<String, Object> getValueLast(final String name, final String column, final Object columnValue, final String... valueColumn) {
		final HashMap<String, Object> map = new HashMap<>();
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = connection.prepareStatement("select * from `" + name + "` where `" + column + "` = ? order by id desc limit 1");
			pstmt.setObject(1, columnValue);
			resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				for (final String _column : valueColumn) {
					map.put(_column, resultSet.getObject(_column));
				}
				break;
			}
		} catch (final Exception e) {
			new RuntimeException("数据库操作出错",e).printStackTrace();
			if (e.getMessage().contains("closed")) {
				connect();
			}
		} finally {
			try { if (resultSet != null) { resultSet.close(); } } catch (final Exception ignored) {}
			try { if (pstmt != null) { pstmt.close(); } } catch (final Exception ignored) {}
		}
		return map;
	}

	public Map<String, Object> getValue(final String name, final String column, final Object columnValue, final String... valueColumn) {
		final HashMap<String, Object> map = new HashMap<>();
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = connection.prepareStatement("select * from `" + name + "` where `" + column + "` = ? limit 1");
			pstmt.setObject(1, columnValue);
			resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				for (final String _column : valueColumn) {
					map.put(_column, resultSet.getObject(_column));
				}
				break;
			}
		} catch (final Exception e) {
			new RuntimeException("数据库操作出错",e).printStackTrace();
			if (e.getMessage().contains("closed")) {
				connect();
			}
		} finally {
			try { if (resultSet != null) { resultSet.close(); } } catch (final Exception ignored) {}
			try { if (pstmt != null) { pstmt.close(); } } catch (final Exception ignored) {}
		}
		return map;
	}

	public List<HashMap<String,Object>> getValues(final String name, final String column, final Object columnValue, final String... valueColumn) {
		final LinkedList<HashMap<String,Object>> list = new LinkedList<>();
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = connection.prepareStatement("select * from `" + name + "` where `" + column + "` = ?");
			pstmt.setObject(1, columnValue);
			resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				final HashMap<String, Object> map = new HashMap<>();
				for (final String _column : valueColumn) {
					map.put(_column, resultSet.getObject(_column));
				}
				list.add(map);
			}
		} catch (final Exception e) {
			new RuntimeException("数据库操作出错",e).printStackTrace();
			if (e.getMessage().contains("closed")) {
				connect();
			}
		} finally {
			try { if (resultSet != null) { resultSet.close(); } } catch (final Exception ignored) {}
			try { if (pstmt != null) { pstmt.close(); } } catch (final Exception ignored) {}
		}
		return list;
	}

	public List<Object> getValues(final String name, final String column, final int size) {
		return getValues(name, column, size, false);
	}

	public List<Object> getValues(final String name, final String column,
			final int size, final boolean desc) {
		final List<Object> list = new LinkedList<>();
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			if (desc) {
				pstmt = connection.prepareStatement("select * from `" + name + "` order by ? desc " + (size < 0 ? "" : " limit " + size));
			} else {
				pstmt = connection.prepareStatement("select * from `" + name + "` order by ? " + (size < 0 ? "" : " limit " + size));
			}
			pstmt.setString(1, column);
			resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				list.add(resultSet.getObject(column));
			}
		} catch (final Exception e) {
			new RuntimeException("数据库操作出错",e).printStackTrace();
			if (e.getMessage().contains("closed")) {
				connect();
			}
		} finally {
			try { if (resultSet != null) { resultSet.close(); } } catch (final Exception ignored) {}
			try { if (pstmt != null) { pstmt.close(); } } catch (final Exception ignored) {}
		}
		return list;
	}

	public List<HashMap<String, Object>> getValues(final String name, final String sortColumn, final int size, final String... valueColumn) {
		return getValues(name, sortColumn, size, false, valueColumn);
	}

	/**
	 * 获取多项数据（多项多列）
	 * 
	 * @param name
	 *            名称
	 * @param sortColumn
	 *            参考列（该列类型必须为数字）
	 * @param size
	 *            获取数量（-1 为无限制）
	 * @param desc
	 *            是否倒序
	 * @param valueColumn
	 *            获取数据列
	 * @return {@link LinkedList}
	 */
	public List<HashMap<String, Object>> getValues(final String name, final String sortColumn, final int size, final boolean desc, final String... valueColumn) {
		final LinkedList<HashMap<String, Object>> list = new LinkedList<>();
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			if (desc) {
				pstmt = connection.prepareStatement("select * from `" + name + "` order by ? desc " + (size < 0 ? "" : " limit " + size));
			} else {
				pstmt = connection.prepareStatement("select * from `" + name + "` order by ? " + (size < 0 ? "" : " limit " + size));
			}
			pstmt.setString(1, sortColumn);
			resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				final HashMap<String, Object> map = new HashMap<>();
				for (final String _column : valueColumn) {
					map.put(_column, resultSet.getObject(_column));
				}
				list.add(map);
			}
		} catch (final Exception e) {
			new RuntimeException("数据库操作出错",e).printStackTrace();
			if (e.getMessage().contains("closed")) {
				connect();
			}
		} finally {
			try { if (resultSet != null) { resultSet.close(); } } catch (final Exception ignored) {}
			try { if (pstmt != null) { pstmt.close(); } } catch (final Exception ignored) {}
		}
		return list;
	}

	/**
	 * 获取多项数据（多项多列）
	 * 
	 * @param name
	 *            名称
	 * @param sortColumn
	 *            参考列（该列类型必须为数字）
	 * @param size
	 *            获取数量（-1 为无限制）
	 * @param desc
	 *            是否倒序
	 * @param valueColumn
	 *            获取数据列
	 * @return {@link LinkedList}
	 */
	public List<HashMap<String, Object>> getValues(final String name, final int size, final String... valueColumn) {
		final LinkedList<HashMap<String, Object>> list = new LinkedList<>();
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = connection.prepareStatement("select * from `" + name + "` " + (size < 0 ? "" : " limit " + size));
			resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				final HashMap<String, Object> map = new HashMap<>();
				for (final String _column : valueColumn) {
					map.put(_column, resultSet.getObject(_column));
				}
				list.add(map);
			}
		} catch (final Exception e) {
			new RuntimeException("数据库操作出错",e).printStackTrace();
			if (e.getMessage().contains("closed")) {
				connect();
			}
		} finally {
			try { if (resultSet != null) { resultSet.close(); } } catch (final Exception ignored) {}
			try { if (pstmt != null) { pstmt.close(); } } catch (final Exception ignored) {}
		}
		return list;
	}

	public boolean execute(final String sql) {
		PreparedStatement pstmt = null;
		try {
			pstmt = connection.prepareStatement(sql);
			pstmt.execute();
			return true;
		} catch (final Exception e) {
			new RuntimeException("数据库命令 {" +sql+"} 执行出错",e).printStackTrace();
			// 重连
			if (e.getMessage().contains("closed")) {
				connect();
			}
			return false;
		} finally {
			try { if (pstmt != null) { pstmt.close(); } } catch (final Exception ignored) {}
		}
	}

	public boolean connect() {
		try {
			connection = DriverManager.getConnection("jdbc:mysql://" + address + ":" + port + "/" + database + "?characterEncoding=utf-8&useSSL=false", user,password);
			return true;
		} catch (final SQLException e) {
			OnflineBungeecord.log("&c数据库连接失败: "+e.getClass().getName()+": "+e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	private boolean loadDriverMySQL() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			return true;
		} catch (final ClassNotFoundException e) {
			return false;
		}
	}
}
