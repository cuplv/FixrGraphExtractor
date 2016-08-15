package androidtests;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.List;
import java.util.Iterator;


public class TransactionAPI {

  public void wrongTransaction(SQLiteOpenHelper helper){
    SQLiteDatabase mDb = helper.getWritableDatabase();
    mDb.beginTransaction();
    mDb.setTransactionSuccessful();
  }

  public void rightTransaction(SQLiteOpenHelper helper){
    SQLiteDatabase mDb = helper.getWritableDatabase();
    try {
      mDb.beginTransaction();
      mDb.setTransactionSuccessful();
    }
    catch (Exception e ) {
      mDb.beginTransaction();
    }
    finally {
      mDb.endTransaction();
    }
  }  
  
  public void syncUsers(List users, SQLiteOpenHelper helper){    
    SQLiteDatabase mDb = helper.getWritableDatabase();
    mDb.beginTransaction();
    for(Object u : users) {
      if(existsUser(0)) {
        updateUser(u);
      }else{
        createUserInfo(u);
      }
    }
    mDb.setTransactionSuccessful();
  }

  /* stubs for functions */
  public boolean existsUser(int userId) {return false;}
  public void updateUser(Object user){int i = 0;}
  public long createUserInfo(Object user) {return 0;}
}
