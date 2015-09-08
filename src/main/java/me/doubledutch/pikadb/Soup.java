package me.doubledutch.pikadb;

import java.io.*;
import java.util.*;
import org.json.*;

public class Soup{
	private String name;
	private PageFile pageFile;
	private int rootPageId;
	private Column metaData;
	private Map<String,Column> columnMap;

	public Soup(String name,PageFile pageFile,int rootPageId) throws IOException{
		this.name=name;
		this.pageFile=pageFile;
		this.rootPageId=rootPageId;
		metaData=new Column(pageFile,rootPageId);
		loadColumns();
	}

	private void loadColumns() throws IOException{
		ObjectSet set=new ObjectSet(true);
		Map<String,Column> tmp=new HashMap<String,Column>();
		List<Variant> list=metaData.scan(set);
		int index=0;
		while(list.size()>index){
			Variant.String name=(Variant.String)list.get(index++);
			Variant.Integer pageId=(Variant.Integer)list.get(index++);
			// System.out.println("Found column "+name.getValue()+" at "+pageId.getValue());
			Column col=new Column(pageFile,pageId.getValue());
			tmp.put(name.getValue(),col);
		}
		columnMap=tmp;
	}

	private Column getColumn(String name) throws IOException{
		if(!columnMap.containsKey(name)){
			return createColumn(name);
		}
		return columnMap.get(name);
	}

	private Column createColumn(String name) throws IOException{
		Page page=pageFile.createPage();
		metaData.append(new Variant.String(-1,name));
		metaData.append(new Variant.Integer(-1,page.getId()));
		pageFile.saveChanges();
		Column col=new Column(pageFile,page.getId());
		columnMap.put(name,col);
		return col;
	}

	public void delete(int oid) throws IOException{
		for(Column col:columnMap.values()){
			col.delete(oid);
		}
	}

	public void update(int oid,JSONObject obj) throws IOException,JSONException{
		for(String key:JSONObject.getNames(obj)){
			Column col=getColumn(key);
			Object value=obj.get(key);
			Variant variant=Variant.createVariant(oid,value);
			col.delete(oid);
			col.append(variant);
		}
	}

	public void add(int oid,JSONObject obj) throws IOException,JSONException{
		for(String key:JSONObject.getNames(obj)){
			Column col=getColumn(key);
			Object value=obj.get(key);
			Variant variant=Variant.createVariant(oid,value);
			col.append(variant);
		}
	}

	public List<JSONObject> scan() throws IOException,JSONException{
		return scan(columnMap.keySet());
	}

	public List<JSONObject> scan(Collection<String> columns) throws IOException,JSONException{
		ObjectSet set=new ObjectSet(true);
		/*for(String columnName:columns){
			Column col=getColumn(columnName);
			List<Variant> list=col.scan(set);
			for(Variant v:list){
				set.addVariant(columnName,v);
			}
		}
		return set.getObjectList();*/
		return scan(set,columns);
	}

	public JSONObject scan(int oid)  throws IOException,JSONException{
		return scan(oid,columnMap.keySet());
	}

	public JSONObject scan(int oid,Collection<String> columns)  throws IOException,JSONException{
		ObjectSet set=new ObjectSet(false);
		set.addOID(oid);
		scan(set,columns);
		JSONObject obj=set.getObject(oid);
		if(JSONObject.getNames(obj)==null){
			return null;
		}
		return obj;
	}

	public List<JSONObject> scan(ObjectSet set) throws IOException,JSONException{
		return scan(set,columnMap.keySet());
	}

	public List<JSONObject> scan(ObjectSet set,Collection<String> columns) throws IOException,JSONException{
		for(String columnName:columns){
			//System.out.println("Scanning column "+columnName);
			Column col=columnMap.get(columnName);
			List<Variant> list=col.scan(set);
			for(Variant v:list){
				//System.out.println(" - Adding value");
				set.addVariant(columnName,v);
			}
		}
		return set.getObjectList();
	}
}