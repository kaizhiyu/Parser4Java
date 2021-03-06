package br.parser.xquery;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.basex.core.BaseXException;
import org.basex.query.QueryException;
import org.basex.query.QueryProcessor;
import org.basex.query.iter.Iter;
import org.basex.query.value.item.Item;

import br.parser.utils.XqueryFile;

public class ExecuteXquery {


	Manager baseXManager=null;
	List<String> methodCall = null;
	Queue<String> queueOfNodes = new LinkedList<String>();

	public ExecuteXquery(){

	}

	public void executor(String path, String file, String dbName, String systemSourcePath) throws SQLException, QueryException, FileNotFoundException, UnsupportedEncodingException
	{
		try 
		{
			baseXManager = new Manager(path, file, dbName);
			baseXManager.openDB();
			this.breathFirstSearch(systemSourcePath);
			baseXManager.closeDB();


		} catch (BaseXException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	private List<String> createMethodCall(String parent, List<String> children)
	{
		List<String> line = new ArrayList<String>();
		String caller = "";
		String called = "";
		String method = "";


		if (parent.split("\\.").length > 1) {

			String tmp[] = parent.split("\\(");
			String statement[] = tmp[0].split("\\.");
			for (int i=0; i< statement.length-1 ; i++)
				caller = caller + "." + statement[i];

			caller = caller.substring(1,caller.length());
			method = statement[statement.length-1] + "(" + tmp[1];
		}

		for (String child : children)
		{	
			if (child.split("\\.").length > 1) {
				String tmp[] = child.split("\\(");
				String statement[] = tmp[0].split("\\.");
				for (int i=0; i< statement.length-1 ; i++)
					called =  called + "." + statement[i];

				called = called.substring(1,called.length());
			}
			else
				called = child;

			line.add(caller + ";" + called + ";" + method + ";");
			called = "";
		}

		return line;
	}

	private void createFile(List<String> content, String filename, String systemSourcePath) throws FileNotFoundException, UnsupportedEncodingException {

		String file1 = systemSourcePath + "/Dynamic/CSV/";
		String file2 = systemSourcePath + "/Dynamic/CSV/"+ filename + ".csv";
		String file3 = systemSourcePath + "/Dynamic/Images/";
		Path dirPathObj1 = Paths.get(file1);
		Path dirPathObj2 = Paths.get(file2);
		Path dirPathObj3 = Paths.get(file3);

		boolean dirExists = Files.exists(dirPathObj1);
		if(!dirExists) {

			try 
			{	
				// Creating The New Directory Structure
				Files.createDirectories(dirPathObj1);
			} catch (IOException ioExceptionObj) 
			{
				System.out.println("Problem Occured While Creating The Directory Structure= " + ioExceptionObj.getMessage());
			}
		}
		
		dirExists = Files.exists(dirPathObj3);
		if(!dirExists) {

			try 
			{	
				// Creating The New Directory Structure
				Files.createDirectories(dirPathObj3);
			} catch (IOException ioExceptionObj) 
			{
				System.out.println("Problem Occured While Creating The Directory Structure= " + ioExceptionObj.getMessage());
			}
		}
		
		dirExists = Files.exists(dirPathObj2);
		if(!dirExists) {

			try 
			{	
				// Creating The New Directory Structure
				Files.createFile(dirPathObj2);
			} catch (IOException ioExceptionObj) 
			{
				System.out.println("Problem Occured While Creating The Directory Structure= " + ioExceptionObj.getMessage());
				ioExceptionObj.printStackTrace();
			}
		}

		PrintWriter writer = new PrintWriter(file2, "UTF-8");
		int counter = 1;


		for (String line : content)
		{
			if (line.split("\\;").length == 2)
				writer.println("[Start];" + line.split("\\;")[1] + ";start;");
			else
			{
				String newLine = line.split("\\;")[0] + ";" + line.split("\\;")[1] + ";" + counter + "." + line.split("\\;")[2] + ";";
				writer.println(newLine);
			}
			counter++;
		}
		writer.close();
	}

	private void breathFirstSearch(String systemSourcePath) throws SQLException, QueryException, BaseXException, FileNotFoundException, UnsupportedEncodingException
	{
		List<String> arrMethods = new ArrayList<String>();
		List<String> children = new ArrayList<String>();
		List<String> fileContent = new ArrayList<String>();
		String query = XqueryFile.getQuery("getNamesParentNode");
		QueryProcessor proc = new QueryProcessor(query, baseXManager.getContext());
		Iter iter = proc.iter();
		Item item;
		while ((item = iter.next()) != null) {
			arrMethods.add(item.toJava().toString());
		}
		proc.close();

		String rtn = " return ";

		for (String thread: arrMethods) {

			String beginQuery = "let $a:= //dataview//node//property[@value='" + thread + "']/../node ";
			String count = "count($a)";
			query = beginQuery + rtn + count;
			proc = new QueryProcessor(query, baseXManager.getContext());
			iter = proc.iter();
			String nodeNum = (iter.next()).toJava().toString();
			proc.close();

			if (Integer.valueOf(nodeNum) != 0){

				for (int i = 0; i< Integer.valueOf(nodeNum) ; i++) {
					String firstPath = "$a" + "[" + (i+1) + "]";
					String secondPath = "/property[@name='Name']/data(@value)" ;
					String endQuery = firstPath + secondPath;
					query = beginQuery + rtn + endQuery;
					proc = new QueryProcessor(query, baseXManager.getContext());
					iter = proc.iter();
					String value = (iter.next()).toJava().toString();
					proc.close();
					if (!value.equals("Self time"))
						children.add(value);
					queueOfNodes.add(firstPath + "-" + secondPath );
				}

				fileContent.addAll(this.createMethodCall(thread, children));
				children.clear();

				while (!queueOfNodes.isEmpty()) {

					String path = queueOfNodes.remove();
					String path0 = path.split("\\-")[0];
					String path1 = path.split("\\-")[1];
					beginQuery = "let $a:= //dataview//node//property[@value='" + thread + "']/../node ";

					//Get parent
					query = beginQuery + rtn + path0 + path1;
					proc = new QueryProcessor(query, baseXManager.getContext());
					iter = proc.iter();
					String parent = (iter.next()).toJava().toString();
					proc.close();

					//Calculates children of parent 
					count = "count(" + path0 + "/node)";					
					query = beginQuery + rtn + count;
					proc = new QueryProcessor(query, baseXManager.getContext());
					iter = proc.iter();
					nodeNum = (iter.next()).toJava().toString();
					proc.close();

					if (Integer.valueOf(nodeNum) != 0) {

						String falseFirstPath = "";
						falseFirstPath = path0 + "/node";

						for (int i = 0; i < Integer.valueOf(nodeNum); i ++) {

							String firstPath = falseFirstPath + "[" + (i+1) + "]";
							String endQuery = firstPath + path1;
							query = beginQuery + rtn + endQuery;
							proc = new QueryProcessor(query, baseXManager.getContext());
							iter = proc.iter();
							String value = (iter.next()).toJava().toString();
							proc.close();
							if (!value.equals("Self time"))
								children.add(value);

							queueOfNodes.add(firstPath + "-" + path1);
						}

						fileContent.addAll(this.createMethodCall(parent, children));
						children.clear();
					}
				}
				this.createFile(fileContent, thread, systemSourcePath);
			}
		}
	}
}
