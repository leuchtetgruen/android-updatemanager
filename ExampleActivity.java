package com.example.test;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.widget.Toast;
import de.leuchtetgruen.UpdateListener;
import de.leuchtetgruen.UpdateManager;

public class ExampleActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		final Context ctx = this;

		UpdateManager umgr = new UpdateManager(ctx, "http://example.com/data.zip", "data.zip", "test");
		
		umgr.checkForUpdate(new UpdateListener() {
			
			@Override
			public void onSuccessfullyDownloadedUpdate(UpdateManager mgr) {
				Toast.makeText(ctx, "Sucessfully updated", Toast.LENGTH_SHORT).show();
			}
			
			@Override
			public void onStartedDownloadingUodate(UpdateManager mgr) {
				Toast.makeText(ctx, "Started Downloading", Toast.LENGTH_SHORT).show();
			}
			
			@Override
			public void onNotAllowedToLoadUpdate(UpdateManager mgr) {
				Toast.makeText(ctx, "Not allowed to update", Toast.LENGTH_SHORT).show();
			}
			
			@Override
			public void onNoUpdateAvailable(UpdateManager mgr) {
				Toast.makeText(ctx, "No update available", Toast.LENGTH_SHORT).show();				
			}
			
			@Override
			public void onErrorDownloadingUpdate(UpdateManager mgr) {
				Toast.makeText(ctx, "Error updating", Toast.LENGTH_SHORT).show();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

}
