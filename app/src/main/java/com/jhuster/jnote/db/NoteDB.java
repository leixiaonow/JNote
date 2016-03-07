/*
 *  Copyright (C) 2015, Jhuster, All Rights Reserved
 *
 *  Author:  Jhuster(lujun.hust@gmail.com)
 *  
 *  https://github.com/Jhuster/JNote
 *  
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; version 2 of the License.
 */
package com.jhuster.jnote.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class NoteDB {
		
	protected static final String TAG = NoteDB.class.getSimpleName();//得到类的简写名称 NoteDB
	
	protected static final int DB_VERSION = 1; //版本
	protected static final String DB_NAME = "note_db";//数据库名称
	protected static final String DB_PRIMARY_KEY = "_id";//主键名称
	protected static final String DB_TABLE_NAME = "note";//表名称
	
	protected static final String DB_TABLE_COLUMN_TITLE   = "title";//笔记名称
	protected static final String DB_TABLE_COLUMN_CONTENT = "content";//笔记内容
	protected static final String DB_TABLE_COLUMN_DATE    = "date";//笔记时间
	
	protected static final String DB_DEFAULT_ORDERBY = DB_TABLE_COLUMN_DATE + " DESC"; //设置排序方式
	
	protected DatabaseHelper mDBHelper;
	protected SQLiteDatabase mDB;	
	
	protected static final NoteDB mInstance = new NoteDB();

	//根据表名建表 包括 id+标题+内容+日期
	//Sqlite中，一个自增长字段定义为INTEGER PRIMARY KEY AUTOINCREMENT，
	//那么在插入一个新数据时，只需要将这个字段的值指定为NULL，即可由引擎自动设定其值，引擎会设定为最大的rowid+1。
	//当然，也可以设置为非NULL的数字来自己指定这个值，但这样就必须自己小心，不要引起冲突。
	private final String DB_TABLE_CREATE_SQL = "create table " + DB_TABLE_NAME + " (_id integer primary key autoincrement, "          
	        + DB_TABLE_COLUMN_TITLE + " text not null, "
	        + DB_TABLE_COLUMN_CONTENT + " text not null, " 
            + DB_TABLE_COLUMN_DATE + " integer);";		

	//笔记类 包括 一个笔记的 名称+内容+时间
	public static class Note {
	    public long key = -1;
	    public String title;
	    public String content;
	    public long date;
	}

	//自继承自SQLiteOpenHelper的DatabaseHelper类
	protected class DatabaseHelper extends SQLiteOpenHelper {
		//构造函数，需要3个参数 Context,数据库名称，数据库版本，来创建数据库
		public DatabaseHelper(Context context,String dbName, int dbVersion) {
			super(context, dbName , null, dbVersion);
		}
		@Override//onCreate时建表
		public void onCreate(SQLiteDatabase db) {
		    db.execSQL(DB_TABLE_CREATE_SQL);			
		}
		@Override//当onUpgrade时
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		    db.execSQL("DROP TABLE IF EXISTS " + DB_TABLE_NAME);		
			onCreate(db);
		}
	}
	
	private NoteDB() {};//默认构造函数，什么都没有
	
	public static NoteDB getInstance() {
	    return mInstance;
	}//单例模式

	//打开数据库
	public boolean open(Context context) {		
		try {
			mDBHelper = new DatabaseHelper(context,DB_NAME,DB_VERSION);
			mDB = mDBHelper.getWritableDatabase();
		}
		catch (SQLException e) {
			e.printStackTrace();
			return false;
		}		
		return true;
	}

	//关闭数据库
	public void close() {
		mDB.close();
		mDBHelper.close();
	}

	//数据库笔记数目
	public int size() {                    
        int size = 0;
        Cursor mCursor = mDB.query(DB_TABLE_NAME,new String[]{DB_PRIMARY_KEY},null,null,null,null, 
                null, null);   
        if (mCursor != null) {
            size = mCursor.getCount();
        }        
        mCursor.close();        
        return size;
    }

	//插入一个Note
	public boolean insert(Note note) {
	    ContentValues values = new ContentValues();
	    values.put(DB_TABLE_COLUMN_TITLE, note.title);
	    values.put(DB_TABLE_COLUMN_CONTENT, note.content);
	    values.put(DB_TABLE_COLUMN_DATE, note.date);
	    note.key = mDB.insert(DB_TABLE_NAME,null,values); //返回的是增加行的自增id，自增ID默认与此相同

		if (note.key == -1) {
			Log.e(TAG,"db insert fail!");
			return false;
		}	
		return true;		
	}	

	//更改笔记
    public boolean update(Note note) {
        if (note.key == -1) {//更改笔记前，笔记已经存入数据库，已经有key值
           return false;
        }
        ContentValues values = new ContentValues();
        values.put(DB_TABLE_COLUMN_TITLE, note.title);
        values.put(DB_TABLE_COLUMN_CONTENT, note.content);
        values.put(DB_TABLE_COLUMN_DATE, note.date);
        String condition = DB_PRIMARY_KEY + "=" + "\'" + note.key + "\'";  
        if (!update(values,condition,null)) {
            return false;
        }
        return true;
    }


    protected boolean update(ContentValues values, String whereClause, String[] whereArgs) {        
        int rows = mDB.update(DB_TABLE_NAME,values, whereClause, whereArgs);
        if (rows <= 0) {           
            Log.d(TAG,"db update fail!");
            return false;
        }   
        return true;
    }

	//删除一行
	public boolean delete(int position) {  
	    long key = getkey(position,null);
        if (key == -1) {
            return false;
        }
        String condition = DB_PRIMARY_KEY + "=" + "\'" + key + "\'";
        return delete(condition,null);
    }

	protected boolean delete(String whereClause, String[] whereArgs) {		
		int rows = mDB.delete(DB_TABLE_NAME,whereClause,whereArgs);
		if (rows <= 0) {
			Log.e(TAG,"db delete fail!");
			return false;
		}
		return true;	
	}

	//删除所有行，表还在
	public boolean clear() {                
        return delete(null,null);
    }


	public Note get(int position) {	    
	    return get(position,null);
	}
	
	public Note get(long id) {      
	    String condition = DB_PRIMARY_KEY + "=" + "\'" + id + "\'";  	           
        List<Note> notes = query(condition);
        if (notes.isEmpty()) {
            return null;
        }
        return notes.get(0);
    }
	
	public Note get(int position,String condition) {  	    
	    Cursor cursor = mDB.query(DB_TABLE_NAME,null,condition,null,null,null,
	            DB_DEFAULT_ORDERBY,null);	           
        List<Note> notes = extract(position,cursor);
        if (notes.isEmpty()) {
            return null;
        }
        return notes.get(0);
	}
	
	public List<Note> query() {
	    Cursor cursor = mDB.query(DB_TABLE_NAME,null,null,null,null,null, 
                DB_DEFAULT_ORDERBY,null);              
        return extract(0,cursor);
	}
	
	public List<Note> query(String condition) {
	    Cursor cursor = mDB.query(DB_TABLE_NAME,null,condition,null,null,null, 
                DB_DEFAULT_ORDERBY,null);              
        return extract(0,cursor);
	}
	        
	public List<Note> query(int offset,int limit) {	    	        	    
	    return query(null,offset,limit);
	}
	
    public List<Note> query(String condition,int offset,int limit) {           
        Cursor cursor = mDB.query(DB_TABLE_NAME,null,condition,null,null,null, 
                DB_DEFAULT_ORDERBY, offset + "," + limit);              
        return extract(0,cursor);
    }

	//将查询结果解释成Note类，并设置offset返回Note列表
	protected List<Note> extract(int offset, Cursor cursor) {
	    
	    List<Note> notes = new ArrayList<Note>();
	    if (cursor == null || cursor.getCount() <= offset) {
            return notes;
        }

        cursor.moveToFirst();     
        cursor.moveToPosition(offset);
        
        do {            
            Note note = new Note();
            note.key = cursor.getLong(cursor.getColumnIndex(DB_PRIMARY_KEY));
            note.title = cursor.getString(cursor.getColumnIndex(DB_TABLE_COLUMN_TITLE));   
            note.content = cursor.getString(cursor.getColumnIndex(DB_TABLE_COLUMN_CONTENT));        
            note.date = cursor.getLong(cursor.getColumnIndex(DB_TABLE_COLUMN_DATE));
            notes.add(note);            
        } while(cursor.moveToNext());
        
        cursor.close();
        
        return notes;
	}
	
	protected long getkey(int position,String condition) {		
	    long key = -1;	
		Cursor cursor = mDB.query(true,DB_TABLE_NAME, new String[]{DB_PRIMARY_KEY},condition,null,null,null, 
		        DB_DEFAULT_ORDERBY, null);		
		if (cursor != null && cursor.getCount() > 0) {			
			cursor.moveToPosition(position);			
			key = cursor.getLong(cursor.getColumnIndex(DB_PRIMARY_KEY));				
			cursor.close();
		}		
		return key;
	}
}

