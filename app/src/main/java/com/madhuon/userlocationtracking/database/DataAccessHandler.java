package com.madhuon.userlocationtracking.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Log;

import com.madhuon.userlocationtracking.ApplicationThread;
import com.madhuon.userlocationtracking.database.DataBaseUpgrade;
import com.madhuon.userlocationtracking.database.Palm3FoilDatabase;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class DataAccessHandler <T> {

    private static final String LOG_TAG = DataAccessHandler.class.getName();

    private Context context;
    private SQLiteDatabase mDatabase;
    private String var = "";
    String queryForLookupTable = "select Name from LookUp where id=" + var;
    private int value;

    public DataAccessHandler() {

    }

    SimpleDateFormat simpledatefrmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    String currentTime = simpledatefrmt.format(new Date());


    public DataAccessHandler(final Context context) {
        this.context = context;
        try {
            mDatabase = Palm3FoilDatabase.openDataBaseNew();
            DataBaseUpgrade.upgradeDataBase(context, mDatabase);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public DataAccessHandler(final Context context, boolean firstTime) {
        this.context = context;
        try {
            mDatabase = Palm3FoilDatabase.openDataBaseNew();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //To Get Only One Int value from DB
    public Integer getOnlyOneIntValueFromDb(String query) {
        Log.v(LOG_TAG, "@@@ query " + query);
        Cursor mOprQuery = null;
        try {
            mOprQuery = mDatabase.rawQuery(query, null);
            if (mOprQuery != null && mOprQuery.moveToFirst()) {
                return mOprQuery.getInt(0);
            }

            return null;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != mOprQuery)
                mOprQuery.close();

            closeDataBase();
        }
        return null;
    }
    //To Get Only One value from DB
    public String getOnlyOneValueFromDb(String query) {
        Log.v(LOG_TAG, "@@@ query " + query);
        Cursor mOprQuery = null;
        try {
            mOprQuery = mDatabase.rawQuery(query, null);
            if (mOprQuery != null && mOprQuery.moveToFirst()) {
                return mOprQuery.getString(0);
            }

            return null;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != mOprQuery)
                mOprQuery.close();

            closeDataBase();
        }
        return "";
    }


    //To insert Data received from Server
    public synchronized void insertData(boolean fromMaster, String tableName, List<LinkedHashMap> mapList, final ApplicationThread.OnComplete<String> oncomplete) {
        int checkCount = 0;
        try {
            List<ContentValues> values1 = new ArrayList<>();
            for (int i = 0; i < mapList.size(); i++) {
                checkCount++;
                List<LinkedHashMap.Entry> entryList = new ArrayList<>((mapList.get(i)).entrySet());

                ContentValues contentValues = new ContentValues();
                for (LinkedHashMap.Entry temp : entryList) {
                    String keyToInsert = temp.getKey().toString();
                    if (keyToInsert.equalsIgnoreCase("ServerUpdatedStatus")) {
                        contentValues.put(keyToInsert, "1");
                    } else {
                        contentValues.put(temp.getKey().toString(), temp.getValue().toString());
                    }
                }
                values1.add(contentValues);
            }
            Log.v(LOG_TAG, "@@@@ log check " + checkCount + " here " + values1.size());
            boolean hasError = bulkinserttoTable(values1, tableName);
            if (hasError) {
                Log.v(LOG_TAG, "@@@ Error while inserting data ");
                if (null != oncomplete) {
                    oncomplete.execute(false, "failed to insert data", "");
                }
            } else {
                Log.v(LOG_TAG, "@@@ data inserted successfully for table :" + tableName);
                if (null != oncomplete) {
                    oncomplete.execute(true, "data inserted successfully", "");
                }
            }
        } catch (Exception e) {
            checkCount++;
            e.printStackTrace();
            Log.v(LOG_TAG, "@@@@ exception log check " + checkCount + " here " + mapList.size());
            if (checkCount == mapList.size()) {
                if (null != oncomplete)
                    oncomplete.execute(false, "data insertion failed", "" + e.getMessage());
            }
        } finally {
            closeDataBase();
        }
    }

    //Save Data into the table
    public synchronized void savedata(boolean fromMaster, String tableName, List<LinkedHashMap> mapList, final ApplicationThread.OnComplete<String> oncomplete) {
        int checkCount = 0;
        try {
            List<ContentValues> values1 = new ArrayList<>();
            for (int i = 0; i < mapList.size(); i++) {
                checkCount++;
                List<LinkedHashMap.Entry> entryList = new ArrayList<>((mapList.get(i)).entrySet());

                ContentValues contentValues = new ContentValues();
                for (LinkedHashMap.Entry temp : entryList) {
                    String keyToInsert = temp.getKey().toString();
                    if (keyToInsert.equalsIgnoreCase("ServerUpdatedStatus")) {
                        contentValues.put(keyToInsert, "0");
                    } else {
                        contentValues.put(temp.getKey().toString(), temp.getValue().toString());
                    }
                }
                values1.add(contentValues);
            }
            Log.v(LOG_TAG, "@@@@ log check " + checkCount + " here " + values1.size());
            boolean hasError = bulkinserttoTable(values1, tableName);
            if (hasError) {
                Log.v(LOG_TAG, "@@@ Error while inserting data ");
                if (null != oncomplete) {
                    oncomplete.execute(false, "failed to insert data", "");
                }
            } else {
                Log.v(LOG_TAG, "@@@ data inserted successfully for table :" + tableName);
                if (null != oncomplete) {
                    oncomplete.execute(true, "data inserted successfully", "");
                }
            }
        } catch (Exception e) {
            checkCount++;
            e.printStackTrace();
            Log.v(LOG_TAG, "@@@@ exception log check " + checkCount + " here " + mapList.size());
            if (checkCount == mapList.size()) {
                if (null != oncomplete)
                    oncomplete.execute(false, "data insertion failed", "" + e.getMessage());
            }
        } finally {
            closeDataBase();
        }
    }

    //Save Data
    public synchronized void saveData(String tableName, List<LinkedHashMap> mapList, final ApplicationThread.OnComplete<String> oncomplete) {
        savedata(false, tableName, mapList, oncomplete);
    }

    //Insert Data
    public synchronized void insertData(String tableName, List<LinkedHashMap> mapList, final ApplicationThread.OnComplete<String> oncomplete) {
        insertData(false, tableName, mapList, oncomplete);
    }

    //Delete Row in the Tables
    public synchronized void deleteRow(String tableName, String columnName, String value, boolean isWhere, final ApplicationThread.OnComplete<String> onComplete) {
        boolean isDataDeleted = true;
//        if (!ApplicationThread.dbThreadCheck())
//            Log.e(LOG_TAG, "called on non-db thread", new RuntimeException());

        try {
//            mDatabase = palm3FoilDatabase.getWritableDatabase();
            String query = "delete from " + tableName;
            if (isWhere) {
                query = query + " where " + columnName + " = '" + value + "'";
            }
            mDatabase.execSQL(query);
        } catch (Exception e) {
            isDataDeleted = false;
            Log.e(LOG_TAG, "@@@ master data deletion failed for " + tableName + " error is " + e.getMessage());
            onComplete.execute(false, null, "master data deletion failed for " + tableName + " error is " + e.getMessage());
        } finally {
            closeDataBase();

            if (isDataDeleted) {
                Log.v(LOG_TAG, "@@@ master data deleted successfully for " + tableName);
                onComplete.execute(true, null, "master data deleted successfully for " + tableName);
            }

        }
    }

    //To get the count from table
    public String getCountValue(String query) {
//        mDatabase = palm3FoilDatabase.getWritableDatabase();
        Cursor mOprQuery = null;
        try {
            mOprQuery = mDatabase.rawQuery(query, null);
            if (mOprQuery != null && mOprQuery.moveToFirst()) {
                return mOprQuery.getString(0);
            }

            return null;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mOprQuery.close();
            closeDataBase();
        }
        return "";
    }


    public void closeDataBase() {
//        if (mDatabase != null)
//            mDatabase.close();
    }

    public void executeRawQuery(String query) {
        try {
            if (mDatabase != null) {
                mDatabase.execSQL(query);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
    }


    //Bulk Insert to the Table
    public boolean bulkinserttoTable(List<ContentValues> cv, final String tableName) {
        boolean isError = false;
        mDatabase.beginTransaction();
        try {
            for (int i = 0; i < cv.size(); i++) {
                ContentValues stockResponse = cv.get(i);
                long id = mDatabase.insert(tableName, null, stockResponse);
                if (id < 0) {
                    isError = true;
                }
            }
            mDatabase.setTransactionSuccessful();
        } finally {
            mDatabase.endTransaction();
        }
        return isError;
    }

    public LinkedHashMap<String, String> getvechileData(final String query) {
        Log.v(LOG_TAG, "@@@ Generic Query " + query);
        LinkedHashMap<String, String> mGenericData = new LinkedHashMap<>();
        Cursor genericDataQuery = null;
        try {
            genericDataQuery = mDatabase.rawQuery(query, null);
            if (genericDataQuery.moveToFirst()) {
                do {
                    mGenericData.put(genericDataQuery.getString(0), genericDataQuery.getString(2));
                } while (genericDataQuery.moveToNext());
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        } finally {
//            palm3FoilDatabase.closeDataBase();
            if (null != genericDataQuery)
                genericDataQuery.close();

            closeDataBase();
        }
        return mGenericData;
    }

    public synchronized void updateData(String tableName, List<LinkedHashMap> list, Boolean isClaues, String whereCondition, final ApplicationThread.OnComplete<String> oncomplete) {
        boolean isUpdateSuccess = false;
        int checkCount = 0;
        try {
            for (int i = 0; i < list.size(); i++) {
                checkCount++;
                List<Map.Entry> entryList = new ArrayList<Map.Entry>((list.get(i)).entrySet());
                String query = "update " + tableName + " set ";
                String namestring = "";

                System.out.println("\n==> Size of Entry list: " + entryList.size());
                StringBuffer columns = new StringBuffer();
                for (Map.Entry temp : entryList) {
                    columns.append(temp.getKey());
                    columns.append("='");
                    columns.append(temp.getValue());
                    columns.append("',");
                }

                namestring = columns.deleteCharAt(columns.length() - 1).toString();
                query = query + namestring + "" + whereCondition;
                mDatabase.execSQL(query);
                isUpdateSuccess = true;
                Log.v(LOG_TAG, "@@@ query for  " + query);
            }
        } catch (Exception e) {
            checkCount++;
            e.printStackTrace();
            isUpdateSuccess = false;
        } finally {
            closeDataBase();
            if (checkCount == list.size()) {
                if (isUpdateSuccess) {
                    Log.v(LOG_TAG, "@@@ data updated successfully for " + tableName);
                    oncomplete.execute(true, null, "data updated successfully for " + tableName);
                } else {
                    oncomplete.execute(false, null, "data updation failed for " + tableName);
                }
            }
        }
    }

    //tp get single list data from db
    public List<String> getSingleListData(final String query) {
        Log.v(LOG_TAG, "@@@ Generic Query " + query);
        List<String> mGenericData = new ArrayList<>();
        Cursor genericDataQuery = null;
        try {
            genericDataQuery = mDatabase.rawQuery(query, null);
            if (genericDataQuery.moveToFirst()) {
                do {
                    String plotCode = genericDataQuery.getString(0);
                    mGenericData.add(plotCode);
                } while (genericDataQuery.moveToNext());
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        } finally {
            if (null != genericDataQuery)
                genericDataQuery.close();
            closeDataBase();
        }
        return mGenericData;
    }

}
