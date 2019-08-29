package sakura.kooi.Onfline.bungeecord.utils.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.mysql.jdbc.exceptions.MySQLSyntaxErrorException;

import net.md_5.bungee.api.plugin.Plugin;


public class MysqlDataSource {
	private final String server;
	private final String port;
	private final String username;
	private final String password;
	private final String database;
	private final String connectionUrl;

	private Connection connection;
	private final Plugin plugin;
	private Thread keepAliveThread;

	public MysqlDataSource(final String server, final String port, final String username, final String password,
			final String database, final Plugin plugin) {
		this.plugin = plugin;
		this.server = server;
		this.username = username;
		this.port = port;
		this.password = password;
		this.database = database;
		connectionUrl = "jdbc:mysql://" + server + ":" + port + "/" + database
				+ "?characterEncoding=utf-8&useSSL=false";
	}

	public void connectDatabase() throws SQLException {
		if (!loadDriverMySQL())
			throw new SQLException("未发现 Mysql数据库 连接驱动");
		reConnect();
		keepAliveThread = new Thread(() -> {
			while (true) {
				try {
					Thread.sleep(20000);

					if (connection == null) {
						new SQLException("数据库连接已断开, 请重新连接数据库").printStackTrace();
						return;
					} else {
						isExists("keepAlive");
					}
				} catch (final Exception e) {
					new SQLException("数据库连接中断, 尝试重连...", e).printStackTrace();
				}
			}
		});
		if (isConnected()) {
			keepAliveThread.start();
		}
	}

	public boolean isConnected() {
		try {
			if (connection == null || connection.isClosed())
				return false;
		} catch (final SQLException e) {
			return false;
		}
		return true;
	}

	@SuppressWarnings("deprecation")
	public void disconnectDatabase() {
		if (connection != null) {
			try {
				connection.close();
			} catch (final Exception e) {
			}
		}
		if (keepAliveThread != null) {
			try {
				keepAliveThread.stop();
			} catch (final Exception e) {
			}
		}
	}

	public void deleteTable(final String name) throws SQLException {
		execute("drop table if exists " + name);
	}

	public void truncateTable(final String name) throws SQLException {
		execute("truncate table " + name);
	}

	public void clearTable(final String name) throws SQLException {
		execute("delete from " + name);
	}

	public void renameTable(final String name, final String newName) throws SQLException {
		execute("rename table `" + name + "` to `" + newName + "`");
	}

	/**
	 * 删除数据
	 * 
	 * @param name
	 *            名称
	 * @param column
	 *            参考列
	 * @param columnValue
	 *            参考值
	 * @throws SQLException
	 */
	public void deleteValue(final String name, final String column, final Object columnValue) throws SQLException {
		PreparedStatement pstmt = null;
		final ResultSet resultSet = null;
		try {
			pstmt = connection.prepareStatement("delete from `" + name + "` where `" + column + "` = ?");
			pstmt.setObject(1, columnValue);
			pstmt.executeUpdate();
		} catch (final Exception e) {
			if (e.getMessage().contains("closed")) {
				try {
					reConnect();
				} catch (final SQLException e1) {
					throw new SQLException("数据库连接出错", e);
				}
			}
			throw e;
		} finally {
			freeResult(resultSet, pstmt);
		}
	}

	/**
	 * 写入数据
	 * 
	 * @param name
	 *            名称
	 * @param column
	 *            参考列
	 * @param columnValue
	 *            参考值
	 * @param valueColumn
	 *            数据列
	 * @param value
	 *            数据值
	 * @return boolean
	 * @throws SQLException
	 */
	public void setValue(final String name, final String column, final Object columnValue, final String valueColumn,
			final Object value) throws SQLException {
		setValue(name, column, columnValue, valueColumn, value, false);
	}

	/**
	 * 写入数据
	 * 
	 * @param name
	 *            名称
	 * @param column
	 *            参考列
	 * @param columnValue
	 *            参考值
	 * @param valueColumn
	 *            数据列
	 * @param value
	 *            数据值
	 * @param append
	 *            是否追加（数据列类型必须为数字）
	 * @return boolean
	 * @throws SQLException
	 */
	public void setValue(final String name, final String column, final Object columnValue, final String valueColumn,
			final Object value, final boolean append) throws SQLException {
		PreparedStatement pstmt = null;
		final ResultSet resultSet = null;
		try {
			if (append) {
				pstmt = connection.prepareStatement("update `" + name + "` set `" + valueColumn + "` = `" + valueColumn
						+ "` + ? where `" + column + "` = ?");
			} else {
				pstmt = connection.prepareStatement(
						"update `" + name + "` set `" + valueColumn + "` = ? where `" + column + "` = ?");
			}
			pstmt.setObject(1, value);
			pstmt.setObject(2, columnValue);
			pstmt.executeUpdate();
		} catch (final Exception e) {
			if (e.getMessage().contains("closed")) {
				try {
					reConnect();
				} catch (final SQLException e1) {
					throw new SQLException("数据库连接出错", e);
				}
			}
			throw e;
		} finally {
			freeResult(resultSet, pstmt);
		}
	}

	/**
	 * 插入数据
	 * 
	 * @param name
	 *            名称
	 * @param values
	 *            值
	 * @return boolean
	 * @throws SQLException
	 */
	public void intoValue(final String name, final Object... values) throws SQLException {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < values.length; i++) {
			sb.append("?, ");
		}
		PreparedStatement pstmt = null;
		final ResultSet resultSet = null;
		try {
			pstmt = connection.prepareStatement(
					"insert into `" + name + "` values(null, " + sb.substring(0, sb.length() - 2) + ")");
			for (int i = 0; i < values.length; i++) {
				pstmt.setObject(i + 1, values[i]);
			}
			pstmt.executeUpdate();
		} catch (final Exception e) {
			if (e.getMessage().contains("closed")) {
				try {
					reConnect();
				} catch (final SQLException e1) {
					throw new SQLException("数据库连接出错", e);
				}
			}
			throw e;
		} finally {
			freeResult(resultSet, pstmt);
		}
	}

	/**
	 * 创建数据表
	 * 
	 * @param name
	 *            名称
	 * @param columns
	 *            列表
	 * @return boolean
	 * @throws SQLException
	 */
	public void createTable(final String name, final Column... columns) throws SQLException {
		final StringBuilder sb = new StringBuilder();
		for (final Column column : columns) {
			sb.append(column.toString() + ", ");
		}
		execute("create table if not exists " + name + " (id int(1) not null primary key auto_increment, "
				+ sb.substring(0, sb.length() - 2) + ")");
	}

	/**
	 * 创建数据表
	 * 
	 * @param name
	 *            名称
	 * @param columns
	 *            列表
	 * @return boolean
	 * @throws SQLException
	 */
	public void createTable(final String name, final String... columns) throws SQLException {
		final StringBuilder sb = new StringBuilder();
		for (final String column : columns) {
			if (!column.contains("/")) {
				sb.append("`" + column + "` text, ");
			} else {
				sb.append("`" + column.split("/")[0] + "` " + column.split("/")[1] + ", ");
			}
		}
		execute("create table if not exists " + name + " (id int(1) not null primary key auto_increment, "
				+ sb.substring(0, sb.length() - 2) + ")");
	}

	/**
	 * 检查数据表是否存在
	 * 
	 * @param name
	 *            名称
	 * @return boolean
	 * @throws SQLException
	 */
	public boolean isExists(final String name) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = connection
					.prepareStatement("select table_name FROM information_schema.TABLES where table_name = ?");
			pstmt.setString(1, name);
			resultSet = pstmt.executeQuery();
			while (resultSet.next())
				return true;
		} catch (final Exception e) {
			if (e.getMessage().contains("closed")) {
				try {
					reConnect();
				} catch (final SQLException e1) {
					throw new SQLException("数据库连接出错", e);
				}
			}
			throw e;
		} finally {
			freeResult(resultSet, pstmt);
		}
		return false;
	}

	/**
	 * 检查数据是否存在
	 * 
	 * @param name
	 *            名称
	 * @param column
	 *            列表名
	 * @param columnValue
	 *            列表值
	 * @return boolean
	 * @throws SQLException
	 */
	public boolean isExists(final String name, final String column, final Object columnValue) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = connection.prepareStatement("select * from `" + name + "` where `" + column + "` = ?");
			pstmt.setObject(1, columnValue);
			resultSet = pstmt.executeQuery();
			while (resultSet.next())
				return true;
		} catch (final Exception e) {
			if (e.getMessage().contains("closed")) {
				try {
					reConnect();
				} catch (final SQLException e1) {
					throw new SQLException("数据库连接出错", e);
				}
			}
			throw e;
		} finally {
			freeResult(resultSet, pstmt);
		}
		return false;
	}

	/**
	 * 获取所有列表名称（不含主键）
	 * 
	 * @param name
	 *            名称
	 * @return {@link List}
	 * @throws SQLException
	 */
	public List<String> getColumns(final String name) throws SQLException {
		return getColumns(name, false);
	}

	/**
	 * 获取所有列表名称
	 * 
	 * @param name
	 *            名称
	 * @param primary
	 *            是否获取主键
	 * @return {@link List}
	 * @throws SQLException
	 */
	public List<String> getColumns(final String name, final boolean primary) throws SQLException {
		final List<String> list = new ArrayList<>();
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = connection
					.prepareStatement("select column_name from information_schema.COLUMNS where table_name = ?");
			pstmt.setString(1, name);
			resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				list.add(resultSet.getString(1));
			}
		} catch (final Exception e) {
			if (e.getMessage().contains("closed")) {
				try {
					reConnect();
				} catch (final SQLException e1) {
					throw new SQLException("数据库连接出错", e);
				}
			}
			throw e;
		} finally {
			freeResult(resultSet, pstmt);
		}
		// 是否获取主键
		if (!primary) {
			list.remove("id");
		}
		return list;
	}

	/**
	 * 获取单项数据
	 * 
	 * @param name
	 *            名称
	 * @param column
	 *            参考列
	 * @param columnValue
	 *            参考值
	 * @param valueColumn
	 *            数据列
	 * @return Object
	 * @throws SQLException
	 */
	public Object getValue(final String name, final String column, final Object columnValue, final String valueColumn)
			throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = connection.prepareStatement("select * from `" + name + "` where `" + column + "` = ? limit 1");
			pstmt.setObject(1, columnValue);
			resultSet = pstmt.executeQuery();
			while (resultSet.next())
				return resultSet.getObject(valueColumn);
		} catch (final Exception e) {
			if (e.getMessage().contains("closed")) {
				try {
					reConnect();
				} catch (final SQLException e1) {
					throw new SQLException("数据库连接出错", e);
				}
			}
			throw e;
		} finally {
			freeResult(resultSet, pstmt);
		}
		return null;
	}

	/**
	 * 获取单项数据（根据主键倒叙排列后的最后一项）
	 * 
	 * @param name
	 *            名称
	 * @param column
	 *            参考列
	 * @param columnValue
	 *            参考值
	 * @param valueColumn
	 *            数据列
	 * @return Object
	 * @throws SQLException
	 */
	public Object getValueLast(final String name, final String column, final Object columnValue,
			final String valueColumn) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = connection.prepareStatement(
					"select * from `" + name + "` where `" + column + "` = ? order by id desc limit 1");
			pstmt.setObject(1, columnValue);
			resultSet = pstmt.executeQuery();
			while (resultSet.next())
				return resultSet.getObject(valueColumn);
		} catch (final Exception e) {
			if (e.getMessage().contains("closed")) {
				try {
					reConnect();
				} catch (final SQLException e1) {
					throw new SQLException("数据库连接出错", e);
				}
			}
			throw e;
		} finally {
			freeResult(resultSet, pstmt);
		}
		return null;
	}

	/**
	 * 获取多项数据（根据主键倒叙排列后的最后一项）
	 * 
	 * @param name
	 *            名称
	 * @param column
	 *            参考列
	 * @param columnValue
	 *            参考值
	 * @param valueColumn
	 *            数据列
	 * @return {@link HashMap}
	 * @throws SQLException
	 */
	public HashMap<String, Object> getValueLast(final String name, final String column, final Object columnValue,
			final String... valueColumn) throws SQLException {
		final HashMap<String, Object> map = new HashMap<>();
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = connection.prepareStatement(
					"select * from `" + name + "` where `" + column + "` = ? order by id desc limit 1");
			pstmt.setObject(1, columnValue);
			resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				for (final String _column : valueColumn) {
					map.put(_column, resultSet.getObject(_column));
				}
				break;
			}
		} catch (final Exception e) {
			if (e.getMessage().contains("closed")) {
				try {
					reConnect();
				} catch (final SQLException e1) {
					throw new SQLException("数据库连接出错", e);
				}
			}
			throw e;
		} finally {
			freeResult(resultSet, pstmt);
		}
		return map;
	}

	/**
	 * 获取多项数据（单项多列）
	 * 
	 * @param name
	 *            名称
	 * @param column
	 *            参考列
	 * @param columnValue
	 *            参考值
	 * @param valueColumn
	 *            数据列
	 * @return {@link HashMap}
	 * @throws SQLException
	 */
	public HashMap<String, Object> getValue(final String name, final String column, final Object columnValue,
			final String... valueColumn) throws SQLException {
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
			if (e.getMessage().contains("closed")) {
				try {
					reConnect();
				} catch (final SQLException e1) {
					throw new SQLException("数据库连接出错", e);
				}
			}
			throw e;
		} finally {
			freeResult(resultSet, pstmt);
		}
		return map;
	}

	/**
	 * 获取多项数据（单列多列）
	 * 
	 * @param name
	 *            名称
	 * @param column
	 *            参考列
	 * @param size
	 *            获取数量（-1 为无限制）
	 * @return {@link List}
	 * @throws SQLException
	 */
	public List<Object> getValues(final String name, final String column, final int size) throws SQLException {
		return getValues(name, column, size, false);
	}

	/**
	 * 获取多项数据（单列多列）
	 * 
	 * @param name
	 *            名称
	 * @param column
	 *            参考列
	 * @param size
	 *            获取数量（-1 位无限制）
	 * @param desc
	 *            是否倒序
	 * @return {@link List}
	 * @throws SQLException
	 */
	public List<Object> getValues(final String name, final String column, final int size, final boolean desc)
			throws SQLException {
		final List<Object> list = new LinkedList<>();
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			if (desc) {
				pstmt = connection.prepareStatement(
						"select * from `" + name + "` order by ? desc " + (size < 0 ? "" : " limit " + size));
			} else {
				pstmt = connection.prepareStatement(
						"select * from `" + name + "` order by ? " + (size < 0 ? "" : " limit " + size));
			}
			pstmt.setString(1, column);
			resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				list.add(resultSet.getObject(column));
			}
		} catch (final Exception e) {
			if (e.getMessage().contains("closed")) {
				try {
					reConnect();
				} catch (final SQLException e1) {
					throw new SQLException("数据库连接出错", e);
				}
			}
			throw e;
		} finally {
			freeResult(resultSet, pstmt);
		}
		return list;
	}

	/**
	 * 获取多线数据（多项多列）
	 * 
	 * @param name
	 *            名称
	 * @param sortColumn
	 *            参考列（该列类型必须为数字）
	 * @param size
	 *            获取数量（-1 为无限制）
	 * @param valueColumn
	 *            获取数据列
	 * @return {@link LinkedList}
	 * @throws SQLException
	 */
	public LinkedList<HashMap<String, Object>> getValues(final String name, final String sortColumn, final int size,
			final String... valueColumn) throws SQLException {
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
	 * @throws SQLException
	 */
	public LinkedList<HashMap<String, Object>> getValues(final String name, final String sortColumn, final int size,
			final boolean desc, final String... valueColumn) throws SQLException {
		final LinkedList<HashMap<String, Object>> list = new LinkedList<>();
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			if (desc) {
				pstmt = connection.prepareStatement(
						"select * from `" + name + "` order by ? desc " + (size < 0 ? "" : " limit " + size));
			} else {
				pstmt = connection.prepareStatement(
						"select * from `" + name + "` order by ? " + (size < 0 ? "" : " limit " + size));
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
			if (e.getMessage().contains("closed")) {
				try {
					reConnect();
				} catch (final SQLException e1) {
					throw new SQLException("数据库连接出错", e);
				}
			}
			throw e;
		} finally {
			freeResult(resultSet, pstmt);
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
	 * @throws SQLException
	 */
	public LinkedList<HashMap<String, Object>> getValues(final String name, final int size, final String... valueColumn)
			throws SQLException {
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
			if (e.getMessage().contains("closed")) {
				try {
					reConnect();
				} catch (final SQLException e1) {
					throw new SQLException("数据库连接出错", e);
				}
			}
			throw e;
		} finally {
			freeResult(resultSet, pstmt);
		}
		return list;
	}

	public List<HashMap<String,Object>> getValues(final String name, final String column, final Object columnValue, final String... valueColumn) throws SQLException {
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
			if (e.getMessage().contains("closed")) {
				try {
					reConnect();
				} catch (final SQLException e1) {
					throw new SQLException("数据库连接出错", e);
				}
			}
			throw e;
		} finally {
			freeResult(resultSet, pstmt);
		}
		return list;
	}

	public <T> T query(final String sql, final ValueSupplier valueSupplier, final ResultProcessor<T> resultProcesser)
			throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = connection.prepareStatement(sql);
			if (valueSupplier != null) {
				valueSupplier.supplyValue(pstmt);
			}
			resultSet = pstmt.executeQuery();
			if (resultProcesser == null)
				return null;
			return resultProcesser.processValue(resultSet);
		} catch (final Exception e) {
			if (e.getMessage().contains("closed")) {
				try {
					reConnect();
				} catch (final SQLException e1) {
					throw new SQLException("数据库连接出错", e);
				}
			}
			throw e;
		} finally {
			freeResult(resultSet, pstmt);
		}
	}

	public boolean update(final String sql, final ValueSupplier valueSupplier) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = connection.prepareStatement(sql);
			if (valueSupplier != null) {
				valueSupplier.supplyValue(pstmt);
			}
			return pstmt.executeUpdate() == 1;
		} catch (final Exception e) {
			if (e.getMessage().contains("closed")) {
				try {
					reConnect();
				} catch (final SQLException e1) {
					throw new SQLException("数据库连接出错", e);
				}
			}
			throw e;
		} finally {
			freeResult(null, pstmt);
		}
	}

	public void updateBatch(final String sql, final ValueSupplier valueSupplier) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = connection.prepareStatement(sql);
			if (valueSupplier != null) {
				valueSupplier.supplyValue(pstmt);
			}
			pstmt.executeBatch();
		} catch (final Exception e) {
			if (e.getMessage().contains("closed")) {
				try {
					reConnect();
				} catch (final SQLException e1) {
					throw new SQLException("数据库连接出错", e);
				}
			}
			throw e;
		} finally {
			freeResult(null, pstmt);
		}
	}

	public void execute(final String sql) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = connection.prepareStatement(sql);
			pstmt.execute();
		} catch (final MySQLSyntaxErrorException e) {
			e.printStackTrace();
			throw e;
		} catch (final SQLException e) {
			if (e.getMessage().contains("closed")) {
				try {
					reConnect();
				} catch (final SQLException e1) {
					throw new SQLException("数据库连接出错", e);
				}
			}
			throw e;
		} finally {
			try {
				if (pstmt != null) {
					pstmt.close();
				}
			} catch (final Exception e) {
			}
		}
	}

	public void reConnect() throws SQLException {
		connection = DriverManager.getConnection(connectionUrl, username, password);
	}

	private void freeResult(final ResultSet resultSet, final PreparedStatement pstmt) {
		try {
			if (resultSet != null) {
				resultSet.close();
			}
		} catch (final Exception e) {
		}
		try {
			if (pstmt != null) {
				pstmt.close();
			}
		} catch (final Exception e) {
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
