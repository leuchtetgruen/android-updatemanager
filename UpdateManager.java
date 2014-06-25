package de.leuchtetgruen;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.ParcelFileDescriptor;

public class UpdateManager {

	private Context ctx;
	private String strUrl;
	private String strTargetFile;
	private String prefsName;
	private DownloadManager downloadManager;
	private long myDownloadId;
	private BroadcastReceiver broadcastReceiver;
	private UpdateListener updateListener = null;
	private Handler runner = new Handler();
	
	private final static String PREFS_ETAG			= ".ETAG";
	private final static String PREFS_LMOD			= ".LAST_MODIFIED";
	private final static String PREFS_FSIZE			= ".FILESIZE";
	private final static String DEFAULT_SP_VAL		= "$$foobar$$";
	
	public final static int DESIRED_NETWORK_TYPE_ANY = -1;

	private int desiredNetworkType = ConnectivityManager.TYPE_WIFI;
	

	public UpdateManager(Context ctx, String strUrl, String targetFile, String prefsName) {
		super();
		this.ctx = ctx;
		this.strUrl = strUrl;
		this.strTargetFile = targetFile;
		this.prefsName = prefsName;
	}
	
	public void setDesiredNetworkType(int desiredNetworkType) {
		this.desiredNetworkType = desiredNetworkType;
	}
	
	public void checkForUpdate(UpdateListener listener) {
		ConnectivityManager mgr = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = mgr.getActiveNetworkInfo();
		
		if (networkInfo==null) {
			listener.onErrorDownloadingUpdate(this);
		}
		else if ((networkInfo.getType() != this.desiredNetworkType) && (this.desiredNetworkType != DESIRED_NETWORK_TYPE_ANY)) {
			listener.onNotAllowedToLoadUpdate(this);
			return;
		}

		setUpdateListener(listener);
		final UpdateManager umgr = this;
		final UpdateListener lnr = listener;
		Thread t = new Thread() {
			public void run() {
				if (hasNewData() || (!ctx.getFileStreamPath(strTargetFile).exists())) update();
				else {
					runner.post(new Runnable() {
						public void run() {
							lnr.onNoUpdateAvailable(umgr);
						}
					});
				}
			};
		};
		t.start();
	}
	
	
	public void setUpdateListener(UpdateListener listener) {
		this.updateListener = listener;
	}
	
	private boolean hasNewData() {
		HttpClient client = new DefaultHttpClient();
		try {
			HttpResponse response = client.execute(new HttpHead(strUrl));
			
			Header[] headers = response.getAllHeaders();
			for (Header header : headers) {
				if (header.getName().equals("Etag")) {
					return (valuesAreTheSame(header.getValue(), prefsName + PREFS_ETAG)!=TriState.YES);
				}
				else if (header.getName().equals("Last-Modified")) {
					return (valuesAreTheSame(header.getValue(), prefsName + PREFS_LMOD)!=TriState.YES);
				}
				else if (header.getName().equals("Content-Length")) {
					return (valuesAreTheSame(header.getValue(), prefsName + PREFS_FSIZE)!=TriState.YES);
				}
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		return false;
	}
	
	public void update() {
		downloadManager 					= (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
		DownloadManager.Request request 	= new DownloadManager.Request(Uri.parse(strUrl));
		
		myDownloadId 						= downloadManager.enqueue(request);
		final UpdateManager umgr 			= this;
		
		if (updateListener!=null) runner.post(new Runnable() {
			public void run() {
				updateListener.onStartedDownloadingUpdate(umgr);
			}
		});
		
		
		broadcastReceiver  = new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                    //long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                    Query query = new Query();
                    query.setFilterById(myDownloadId);
                    Cursor c = downloadManager.query(query);
                    if (c.moveToFirst()) {
                        int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        
                        int status = c.getInt(columnIndex);
                        if (DownloadManager.STATUS_SUCCESSFUL == status) {
 
                            //String uriString = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                            ParcelFileDescriptor f;
							try {
								f = downloadManager.openDownloadedFile(myDownloadId);
								FileInputStream in = new ParcelFileDescriptor.AutoCloseInputStream(f);
	                            copyToAppDir(ctx, in, strTargetFile);
	                            if (updateListener!=null) updateListener.onSuccessfullyDownloadedUpdate(umgr);
							} catch (FileNotFoundException e) {
								e.printStackTrace();
								if (updateListener!=null) updateListener.onErrorDownloadingUpdate(umgr);
							}
                        }
                        else if (DownloadManager.STATUS_FAILED == status){
                        	if (updateListener!=null) updateListener.onErrorDownloadingUpdate(umgr);
                        }
                    }
                }
			}
		};
		ctx.registerReceiver(broadcastReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
		
	}
	
	
	private TriState valuesAreTheSame(String read, String prefsKey) {
		TriState ret = TriState.NO;
		SharedPreferences prefs = ctx.getSharedPreferences(prefsName, 0);
		String savedValue = prefs.getString(prefsKey, DEFAULT_SP_VAL);
		
		if (savedValue.equals(DEFAULT_SP_VAL)) ret =TriState.UNKNOWN;
		else if (savedValue.equals(read)) ret = TriState.YES;
		
		if (ret != TriState.YES) {
			prefs.edit().putString(prefsKey, read).apply();
		}
		return ret;
	}
	

	private void copyToAppDir(Context ctx, InputStream in, String fileDest) {
	   try{
                 
          
          OutputStream out = ctx.openFileOutput(fileDest, Context.MODE_PRIVATE);

          byte[] buf = new byte[4096];
          int len;
          while ((len = in.read(buf)) > 0){
            out.write(buf, 0, len);
          }
          in.close();
          out.close();
          
        }
        catch(FileNotFoundException ex){
            ex.printStackTrace();
        }
        catch(IOException e){
            e.printStackTrace();      
        }
	}
	
	public void copyFileFromAssets() {
		try {
			InputStream in = ctx.getAssets().open(strTargetFile);
			copyToAppDir(ctx, in, strTargetFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
