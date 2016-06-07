package androidtests;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;



public class Samples {

  public int update(SQLiteDatabase mDb, int updateUserCount)
  {
    boolean updateUser;
    
    updateUser = updateUser(mDb);
    if (updateUser) {
      updateUserCount = updateUserCount + 1;
    } else {
      updateUserCount = 0; 
    }

    log("Begin transaction");
    mDb.beginTransaction();
    mDb.setTransactionSuccessful();    
    mDb.endTransaction();
    
    return updateUserCount;
  }

  public boolean deleteRow(SQLiteDatabase mDb)
  {
    boolean deletedRow;

    deletedRow = false;

    if (null != mDb) {
      if (mDb.isOpen()) {
        if (! mDb.isReadOnly()) {
          deletedRow = mDb.delete("table", "key = user", null) > 0;
        }
      }
    }
    
    return deletedRow;
  }

  public int deleteNUsers(SQLiteDatabase mDb, int bound)
  {
    int deletedRow;

    deletedRow = 0;    
    if (null != mDb) {
      if (mDb.isOpen()) {

        for (int i = 0; i <= 10; i++) {
          log("Deleting user " + i);
          if (! mDb.isReadOnly()) {
            if (mDb.delete("table", "key = " + i, null) > 0)
              deletedRow += 1;
          }
        }
      }
    }
    return deletedRow;
  }

  public void retrieveData(SQLiteDatabase mDb) {
    /*retrieve data from database */
    Cursor c = mDb.rawQuery("SELECT * FROM Table", null);

    int column = c.getColumnIndex("age");

    // Check if our result was valid.
    c.moveToFirst();
    if (c != null) {
      // Loop through all Results
      do {
        int age = c.getInt(column);

        log("Reading " + age);
        
      } while (c.moveToNext());
    }
  }
  
  
  private boolean updateUser(SQLiteDatabase mDb) {
    return false;
  }

  private void log(String app) {
    return;
  }
  
}
