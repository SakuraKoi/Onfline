package sakura.kooi.Onfline.bungeecord.utils.mysql;

public class Column {

	private final String name;
	private Object type;
	private int a;
	private int b;

	public Column(final String name) {
		this.name = name;
		type = ColumnString.TEXT;
	}

	public Column(final String name, final ColumnInteger type) {
		this(name);
		this.type = type;
		a = 12;
	}

	public Column(final String name, final ColumnInteger type, final int m) {
		this(name);
		this.type = type;
		a = m;
	}

	public Column(final String name, final ColumnFloat type, final int m, final int d) {
		this(name);
		this.type = type;
		a = m;
		b = d;
	}

	public Column(final String name, final ColumnChar type, final int n) {
		this(name);
		this.type = type;
		a = n;
	}

	public Column(final String name, final ColumnString type) {
		this(name);
		this.type = type;
	}

	@Override
	public String toString() {
		if ((type instanceof ColumnInteger) || (type instanceof ColumnChar))
			return "`" + name + "` " + type.toString().toLowerCase() + "(" + a + ")";
		else if (type instanceof ColumnFloat)
			return "`" + name + "` " + type.toString().toLowerCase() + "(" + a + "," + b + ")";
		else
			return "`" + name + "` " + type.toString().toLowerCase();
	}

	public static enum ColumnInteger {
		TINYINT, SMALLINT, MEDIUMINT, INT, BIGINT;
	}

	public static enum ColumnFloat {
		FLOAT, DOUBLE;
	}

	public static enum ColumnChar {
		CHAR, VARCHAR;
	}

	public static enum ColumnString {
		TINYTEXT, TEXT, MEDIUMTEXT, LONGTEXT;
	}
}