package br.parser.database;

import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import br.parser.main.MyAnalysis;

public class QueryClass {
	
	

	public void createTables() throws Exception {

		Properties p = new Properties();
		InputStream in = MyAnalysis.class.getClassLoader().getResourceAsStream("configuration.properties");
		p.load(in);

		SqliteDb mydb = new SqliteDb(p.getProperty("dbdriver"),p.getProperty("db"));
		mydb.executeStmt("create table IF NOT EXISTS call (id_call integer primary key, package_call text, class_call text, method_call text) ");
		mydb.executeStmt("create table IF NOT EXISTS caller (id_caller integer primary key, id_call integer, package_caller text, class_caller text, method_caller text, "
				+ "FOREIGN KEY (id_call) REFERENCES call (id_call) ON DELETE CASCADE ON UPDATE NO ACTION ) ");

		mydb.executeStmt("delete from caller"); 
		mydb.executeStmt("delete from call"); 
		mydb.closeConnection();
	}

	public void insertCallsAndCallers(List<String> values) throws Exception{

		for (String str: values)
		{
			String info[] = str.split("\\|");
			String infoCall = info[0];
			String infoCaller = info[1];

			String pkgCall[] = infoCall.split("\\.");
			String pkgCaller[] = infoCaller.split("\\.");

			int id = this.preparingString(pkgCall, -1);
			this.preparingString(pkgCaller, id);
		}
	}

	private int preparingString(String pkg[], int id) throws Exception {

		Properties p = new Properties();
		InputStream in = MyAnalysis.class.getClassLoader().getResourceAsStream("configuration.properties");
		p.load(in);

		final Pattern textPattern = Pattern.compile("^(?=.*[A-Z])");
		String _package = "";
		String _class = "";
		String method = "";
		int signal = -1;


		for (int i = 0 ; i< pkg.length; i++)
		{
			if (textPattern.matcher(pkg[i]).find() == false)
			{
				if (signal == -1)
				{
					if (i == 0)
						_package = pkg[i];
					else
						_package = _package + "." + pkg[i];
				}
				else
				{
					if (method == "")
						method = pkg[i];
					else
						method = method + "." + pkg[i];
				}
			}
			else
			{
				if (textPattern.matcher(pkg[i]).find() == true)
				{
					if (signal == -1)
					{
						_class = pkg[i];
						signal = 1;
					}
					else
					{
						if (method == "")
							method = pkg[i];
						else
							method = method + "." + pkg[i];
					}
				}
			}
		}

		SqliteDb mydb = new SqliteDb(p.getProperty("dbdriver"),p.getProperty("db"));
		if (id == -1)
			mydb.executeStmt("insert into call(package_call, class_call, method_call) values ('" + _package + "','" + _class + "','" + method + "');"); 
		else
			mydb.executeStmt("insert into caller(id_call, package_caller, class_caller, method_caller) values ('"+ id + "','" + _package + "','" + _class + "','" + method + "');"); 


		ResultSet rs = mydb.executeQry("select last_insert_rowid();");
		id = rs.getInt(1);
		mydb.closeConnection();
		return id;
	}

	private List<String> resultSetToArrayList(ResultSet rs) throws SQLException {

		List<String> list = new ArrayList<String>();
		while (rs.next()) {
			list.add(rs.getObject(1).toString() + "," + rs.getObject(2).toString() + "," + rs.getObject(3).toString());
		}
		return list;
	}
	
	private List<String> resultSetToArrayList1(ResultSet rs) throws SQLException {

		List<String> list = new ArrayList<String>();
		while (rs.next()) {
			list.add(rs.getObject(1).toString());
		}
		return list;
	}

	public List<String> getPackageCall() throws Exception{

		Properties p = new Properties();
		InputStream in = MyAnalysis.class.getClassLoader().getResourceAsStream("configuration.properties");
		p.load(in);

		List<String> lst = new ArrayList<String>();

		SqliteDb mydb = new SqliteDb(p.getProperty("dbdriver"),p.getProperty("db"));
		ResultSet rs = mydb.executeQry("select a.package_call, b.package_caller, count(*)  from call a, caller b where a.id_call = b.id_call "
				+ "group by  a.package_call, b.package_caller");

		lst = this.resultSetToArrayList(rs);
		mydb.closeConnection();
		return lst;
	}

	public List<String> getMethodCall() throws Exception{

		Properties p = new Properties();
		InputStream in = MyAnalysis.class.getClassLoader().getResourceAsStream("configuration.properties");
		p.load(in);

		List<String> lst = new ArrayList<String>();

		SqliteDb mydb = new SqliteDb(p.getProperty("dbdriver"),p.getProperty("db"));
		ResultSet rs = mydb.executeQry("select a.package_call || '/' ||a.class_call || '/' || a.method_call || '|' || b.package_caller|| '/' ||b.class_caller || '/' || b.method_caller "
				+ "from call a, caller b where a.id_call = b.id_call ;");
		
		lst = this.resultSetToArrayList1(rs);
		mydb.closeConnection();
		return lst;
	}

}
