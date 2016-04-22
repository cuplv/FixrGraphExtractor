package androidtests;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;

public class HelloWorldActivity extends Activity {

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      //setContentView(R.layout.activity_main);

      float f = savedInstanceState.getFloat("");
   }
   
   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
     //getMenuInflater().inflate(R.menu.activity_main, menu);
      return true;
   }
}
