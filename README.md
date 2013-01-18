android-updatemanager
=====================

Say you have an app that gets all its data bundled from a server in something like a ZIP file.
You might need to update that ZIP file from time to time. This is where android-updatemanager
helps you.

All you need to do is to update the file on your server and android-updatemanager will automatically download
the updated file.

This is how you do it:

* `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />` (As updating will not be done on cellular)
* `<uses-permission android:name="android.permission.INTERNET" />`

* Instantiate UpdateManager with the following data
	- your Context
	- The URL where the file that needs to be checked is available
	- The name the file should be saved as locally
	- A SharedPreferences-Prefix
	
* Call checkForUpdate and provide an UpdateListener

* That's it

Here are some other nice features:

- UpdateManager will only check for updates when on a WIFI-Network
- You can also provide an initial version of your file in the assets folder and copy it when the update could not be loaded from the server (Use `copyFileFromAssets`)
