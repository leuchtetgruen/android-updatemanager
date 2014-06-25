package de.leuchtetgruen;

public interface UpdateListener {
	public void onStartedDownloadingUpdate(UpdateManager mgr);
	public void onSuccessfullyDownloadedUpdate(UpdateManager mgr);
	public void onErrorDownloadingUpdate(UpdateManager mgr);
	public void onNoUpdateAvailable(UpdateManager mgr);
	public void onNotAllowedToLoadUpdate(UpdateManager mgr); // due to network mask
}
