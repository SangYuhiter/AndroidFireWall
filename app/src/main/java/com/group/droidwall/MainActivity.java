/*
 * Main application activity.
 * This is the screen displayed when you open the application
 */

package com.group.droidwall;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.group.droidwall.Api.DroidApp;
import com.group.droidwall.Service.LoginService;


/**
 * Main application activity.
 * This is the screen displayed when you open the application
 */
public class MainActivity extends Activity implements OnCheckedChangeListener, OnClickListener {
	
	// Menu options
	private static final int MENU_DISABLE	= 0;
	private static final int MENU_TOGGLELOG	= 1;
	private static final int MENU_APPLY		= 2;
	private static final int MENU_SHOWRULES	= 3;
	private static final int MENU_HELP		= 4;
	private static final int MENU_SHOWLOG	= 5;
	private static final int MENU_CLEARLOG	= 6;
	private static final int MENU_SETUSERINFO	= 7;
    private static final int MENU_CHNGTHEME=8;//设置主题

	private static String path="/sdcard/myHead/";//sd路径

    /** special application UID used to indicate "any application" */
    private static final int SPECIAL_UID_ANY	= -10;
	
	/** progress dialog instance */
	private ProgressDialog progress = null;
	private ListView listview;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
		ThemeUtile.changeTheme(this);
        super.onCreate(savedInstanceState);
        checkPreferences();
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);//去掉标题栏
		setContentView(R.layout.main);
		this.findViewById(R.id.label_mode).setOnClickListener(this);
		this.findViewById(R.id.img_setting).setOnClickListener(this);
		TextView tv_showusername = findViewById(R.id.tv_showusername);
		ImageView iv_showuserimage = findViewById(R.id.iv_showuserimage);
		Api.assertBinaries(this, true);
		Map<String,String> map = LoginService.getSaveInfo(this);
		if(map!=null){
			tv_showusername.setText(map.get("username"));
		}
		Bitmap bt = BitmapFactory.decodeFile(path + "head.jpg");//从Sd中找头像，转换成Bitmap
		if(bt!=null){
			@SuppressWarnings("deprecation") Drawable drawable = new BitmapDrawable(bt);//转换成drawable
			iv_showuserimage.setImageDrawable(drawable);
		}
    }
    @Override
    protected void onResume() {
    	super.onResume();
    	if (this.listview == null) {
    		this.listview = this.findViewById(R.id.listview);
    	}
    	refreshHeader();
    	// Force re-loading the application list
    	Api.applications = null;
		showOrLoadApplications();
    }
    @Override
    protected void onPause() {
    	super.onPause();
    	this.listview.setAdapter(null);
    }
    /**
     * Check if the stored preferences are OK
     */
    private void checkPreferences() {
    	final SharedPreferences prefs = getSharedPreferences(Api.PREFS_NAME, 0);
    	final Editor editor = prefs.edit();
    	boolean changed = false;
    	if (prefs.getString(Api.PREF_MODE, "").length() == 0) {
    		editor.putString(Api.PREF_MODE, Api.MODE_WHITELIST);
    		changed = true;
    	}
    	/* delete the old preference names */
    	if (prefs.contains("AllowedUids")) {
    		editor.remove("AllowedUids");
    		changed = true;
    	}
    	if (prefs.contains("Interfaces")) {
    		editor.remove("Interfaces");
    		changed = true;
    	}
    	if (changed) editor.apply();
    }
    /**
     * Refresh informative header
     */
    private void refreshHeader() {
    	final SharedPreferences prefs = getSharedPreferences(Api.PREFS_NAME, 0);
    	final String mode = prefs.getString(Api.PREF_MODE, Api.MODE_WHITELIST);
		final TextView labelmode = this.findViewById(R.id.label_mode);
		final ImageView img_setting = (ImageView) this.findViewById(R.id.img_setting);
		if (mode.equals(Api.MODE_WHITELIST)) {
			labelmode.setText(R.string.WhiteModeString);
		} else {
			labelmode.setText(R.string.BlackModeString);
		}
		setTitle(Api.isEnabled(this) ? R.string.title_enabled : R.string.title_disabled);
    }
    /**
     * Displays a dialog box to select the operation mode (black or white list)
     */
    private void selectMode() {
    	new AlertDialog.Builder(this).setItems(new String[]{"White list (allow selected)","Black list (block selected)"}, new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int which) {
				final String mode = (which==0 ? Api.MODE_WHITELIST : Api.MODE_BLACKLIST);
				final Editor editor = getSharedPreferences(Api.PREFS_NAME, 0).edit();
				editor.putString(Api.PREF_MODE, mode);
				editor.apply();
				refreshHeader();
			}
    	}).setTitle("Select mode:")
    	.show();
    }
	/**
	 * Toggle iptables log enabled/disabled
	 */
	private void toggleLogEnabled() {
		final SharedPreferences prefs = getSharedPreferences(Api.PREFS_NAME, 0);
		final boolean enabled = !prefs.getBoolean(Api.PREF_LOGENABLED, false);
		final Editor editor = prefs.edit();
		editor.putBoolean(Api.PREF_LOGENABLED, enabled);
		editor.apply();
		if (Api.isEnabled(this)) {
			Api.applySavedIptablesRules(this, true);
		}
		Toast.makeText(MainActivity.this, "Log has been "+(enabled?"enabled.":"disabled."), Toast.LENGTH_SHORT).show();
	}
	/**
	 * If the applications are cached, just show them, otherwise load and show
	 */
	private void showOrLoadApplications() {
    	if (Api.applications == null) {
    		// The applications are not cached.. so lets display the progress dialog
    		progress = ProgressDialog.show(this, "Working...", "Reading installed applications", true);
        	@SuppressLint("HandlerLeak") final Handler handler = new Handler() {
        		public void handleMessage(Message msg) {
        			if (progress != null) progress.dismiss();
        			showApplications();
        		}
        	};
        	new Thread() {
        		public void run() {
        			Api.getApps(MainActivity.this);
        			handler.sendEmptyMessage(0);
        		}
        	}.start();
    	} else {
    		// the applications are cached, just show the list
        	showApplications();
    	}
	}
    /**
     * Show the list of applications
     */
    private void showApplications() {
        final DroidApp[] apps = Api.getApps(this);
        // Sort applications - selected first, then alphabetically
        if (apps != null) {
            Arrays.sort(apps, new Comparator<DroidApp>() {
                @Override
                public int compare(DroidApp o1, DroidApp o2) {
                    if ((o1.selected_wifi|o1.selected_3g) == (o2.selected_wifi|o2.selected_3g)) {
                        return o1.names[0].compareTo(o2.names[0]);
                    }
                    if (o1.selected_wifi || o1.selected_3g) return -1;
                    return 1;
                }
            });
        }
        final LayoutInflater inflater = getLayoutInflater();
		final ListAdapter adapter = new ArrayAdapter<DroidApp>(this,R.layout.listitem,R.id.itemtext,apps) {
        	@NonNull
            @Override
        	public View getView(final int position, View convertView, ViewGroup parent) {
       			ListEntry entry;
        		if (convertView == null) {
        			// Inflate a new view
        			convertView = inflater.inflate(R.layout.listitem, parent, false);
       				entry = new ListEntry();
       				entry.box_wifi = convertView.findViewById(R.id.itemcheck_wifi);
       				entry.box_3g = convertView.findViewById(R.id.itemcheck_3g);
       				entry.icon = convertView.findViewById(R.id.itemicon);
       				entry.text = convertView.findViewById(R.id.itemtext);
       				convertView.setTag(entry);
       				entry.box_wifi.setOnCheckedChangeListener(MainActivity.this);
       				entry.box_3g.setOnCheckedChangeListener(MainActivity.this);
       				entry.text.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View view) {
							//新建AlertDialog并显示
							LayoutInflater inflaterdialog = LayoutInflater.from(MainActivity.this);
							View dialog = inflaterdialog.inflate(R.layout.app_dialog, null);

							TextView tv_version = dialog.findViewById(R.id.tv_version);
							TextView tv_permission = dialog.findViewById(R.id.tv_permission);

							final DroidApp app = apps[position];

							Set<Integer> specialapp = new HashSet<Integer>(){{
                                add(SPECIAL_UID_ANY);
                                add(android.os.Process.getUidForName("root"));
                                add(android.os.Process.getUidForName("media"));
                                add(android.os.Process.getUidForName("vpn"));
                            }};
							if(!specialapp.contains(app.uid)){
                                tv_version.setText(app.version);
                                String showpermission = null;
                                for(String permission:app.permission) showpermission += permission+"\n";
                                tv_permission.setText(showpermission);

                                AlertDialog.Builder builder =new AlertDialog.Builder(MainActivity.this) ;
                                builder.setView(dialog) ;
                                builder.setTitle(app.appname+"的信息为：") ;
                                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel() ;
                                    }

                                });
                                builder.create().show() ;
                            }
						}
					});
        		} else {
        			// Convert an existing view
        			entry = (ListEntry) convertView.getTag();
        		}
        		final DroidApp app = apps[position];
        		entry.text.setText(app.toString());
        		if(app.icon!=null){
					entry.icon.setImageDrawable(app.icon);
				}
        		final CheckBox box_wifi = entry.box_wifi;
        		box_wifi.setTag(app);
        		box_wifi.setChecked(app.selected_wifi);
        		final CheckBox box_3g = entry.box_3g;
        		box_3g.setTag(app);
        		box_3g.setChecked(app.selected_3g);
       			return convertView;
        	}
        };
        this.listview.setAdapter(adapter);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add(0, MENU_DISABLE, 0, R.string.fw_enabled).setIcon(android.R.drawable.button_onoff_indicator_on);
    	menu.add(0, MENU_TOGGLELOG, 0, R.string.log_enabled).setIcon(android.R.drawable.button_onoff_indicator_on);
    	menu.add(0, MENU_APPLY, 0, R.string.applyrules).setIcon(R.drawable.apply);
    	menu.add(0, MENU_SHOWRULES, 0, R.string.showrules).setIcon(R.drawable.show);
    	menu.add(0, MENU_HELP, 0, R.string.help).setIcon(android.R.drawable.ic_menu_help);
    	menu.add(0, MENU_SHOWLOG, 0, R.string.show_log).setIcon(R.drawable.show);
    	menu.add(0, MENU_CLEARLOG, 0, R.string.clear_log).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
    	menu.add(0, MENU_SETUSERINFO, 0, R.string.setuserinfo).setIcon(android.R.drawable.ic_lock_lock);
        menu.add(0, MENU_CHNGTHEME, 0, R.string.change_theme).setIcon(R.drawable.show);

    	return true;
    }
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	final MenuItem item_onoff = menu.getItem(MENU_DISABLE);
    	final MenuItem item_apply = menu.getItem(MENU_APPLY);
    	final boolean enabled = Api.isEnabled(this);
    	if (enabled) {
    		item_onoff.setIcon(android.R.drawable.button_onoff_indicator_on);
    		item_onoff.setTitle(R.string.fw_enabled);
    		item_apply.setTitle(R.string.applyrules);
    	} else {
    		item_onoff.setIcon(android.R.drawable.button_onoff_indicator_off);
    		item_onoff.setTitle(R.string.fw_disabled);
    		item_apply.setTitle(R.string.saverules);
    	}
    	final MenuItem item_log = menu.getItem(MENU_TOGGLELOG);
    	final boolean logenabled = getSharedPreferences(Api.PREFS_NAME, 0).getBoolean(Api.PREF_LOGENABLED, false);
    	if (logenabled) {
    		item_log.setIcon(android.R.drawable.button_onoff_indicator_on);
    		item_log.setTitle(R.string.log_enabled);
    	} else {
    		item_log.setIcon(android.R.drawable.button_onoff_indicator_off);
    		item_log.setTitle(R.string.log_disabled);
    	}
    	return super.onPrepareOptionsMenu(menu);
    }
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
    	switch (item.getItemId()) {
    	case MENU_DISABLE:
    		disableOrEnable();
    		return true;
    	case MENU_SHOWRULES:
    		showRules();
    		return true;
    	case MENU_APPLY:
    		applyOrSaveRules();
    		return true;
    	case MENU_SETUSERINFO:
            Toast.makeText(this, "设置用户信息", Toast.LENGTH_SHORT).show();
    		setUserInfo();
    		return true;
    	case MENU_HELP:
    		new HelpDialog(this).show();
    		return true;
    	case MENU_TOGGLELOG:
    		toggleLogEnabled();
    		return true;
    	case MENU_CLEARLOG:
    		clearLog();
    		return true;
    	case MENU_SHOWLOG:
    		showLog();
    		return true;
    	case MENU_CHNGTHEME:
    	    changeTheme();
    	    return true;
    	}
    	return false;
    }

	private void setUserInfo() {
		Intent intent = new Intent();
		intent.setClass(MainActivity.this, UserInfo.class);
		startActivity(intent);
		finish();
	}

	/**
     * Enables or disables the firewall
     */
	private void disableOrEnable() {
		final boolean enabled = !Api.isEnabled(this);
		Api.setEnabled(this, enabled);
		if (enabled) {
			applyOrSaveRules();
			setTitle(R.string.title_enabled);
		} else {
			purgeRules();
			setTitle(R.string.title_disabled);
		}
	}

	/**
	 * Show iptable rules on a dialog
	 */
	@SuppressLint("HandlerLeak")
    private void showRules() {
		final Handler handler;
		progress = ProgressDialog.show(this, "Working...", "Please wait", true);
		handler = new Handler() {
			public void handleMessage(Message msg) {
				if (progress != null) progress.dismiss();
				if (!Api.hasRootAccess(MainActivity.this, true)) return;
				Api.showIptablesRules(MainActivity.this);
			}
		};
		handler.sendEmptyMessageDelayed(0, 100);
	}
    /**
     *
     *change theme
     */
    private void changeTheme()
    {
        new AlertDialog.Builder(this).setItems(new String[]{"DayTheme", "NightTheme"}, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                ThemeUtile.night = (which == 0 ? false : true);
                recreate();
            }
        }).setTitle("Select Theme:").show();
    }
	/**
	 * Show logs on a dialog
	 */
	@SuppressLint("HandlerLeak")
    private void showLog() {
		final Handler handler;
		progress = ProgressDialog.show(this, "Working...", "Please wait", true);
		handler = new Handler() {
			public void handleMessage(Message msg) {
				if (progress != null) progress.dismiss();
				Api.showLog(MainActivity.this);
			}
		};
		handler.sendEmptyMessageDelayed(0, 100);
	}
	/**
	 * Clear logs
	 */
	@SuppressLint("HandlerLeak")
    private void clearLog() {
		final Handler handler;
		progress = ProgressDialog.show(this, "Working...", "Please wait", true);
		handler = new Handler() {
			public void handleMessage(Message msg) {
				if (progress != null) progress.dismiss();
				if (!Api.hasRootAccess(MainActivity.this, true)) return;
				if (Api.clearLog(MainActivity.this)) {
					Toast.makeText(MainActivity.this, "Logs cleared", Toast.LENGTH_SHORT).show();
				}
			}
		};
		handler.sendEmptyMessageDelayed(0, 100);
	}
	/**
	 * Apply or save iptable rules, showing a visual indication
	 */
	@SuppressLint("HandlerLeak")
    private void applyOrSaveRules() {
		final Handler handler;
		final boolean enabled = Api.isEnabled(this);
		progress = ProgressDialog.show(this, "Working...", (enabled?"Applying":"Saving") + " iptables rules.", true);
		handler = new Handler() {
			public void handleMessage(Message msg) {
				if (progress != null) progress.dismiss();
				if (!Api.hasRootAccess(MainActivity.this, true)) return;
				if (enabled) {
					if (Api.applyIptablesRules(MainActivity.this, true)) {
						Toast.makeText(MainActivity.this, "Rules applied with success", Toast.LENGTH_SHORT).show();
					}
				} else {
					Api.saveRules(MainActivity.this);
					Toast.makeText(MainActivity.this, "Rules saved with success", Toast.LENGTH_SHORT).show();
				}
			}
		};
		handler.sendEmptyMessageDelayed(0, 100);
	}
	/**
	 * Purge iptable rules, showing a visual indication
	 */
	@SuppressLint("HandlerLeak")
    private void purgeRules() {
		final Handler handler;
		progress = ProgressDialog.show(this, "Working...", "Deleting iptables rules.", true);
		handler = new Handler() {
			public void handleMessage(Message msg) {
				if (progress != null) progress.dismiss();
				if (!Api.hasRootAccess(MainActivity.this, true)) return;
				if (Api.purgeIptables(MainActivity.this, true)) {
					Toast.makeText(MainActivity.this, "Rules purged with success", Toast.LENGTH_SHORT).show();
				}
			}
		};
		handler.sendEmptyMessageDelayed(0, 100);
	}
	/**
	 * Called an application is check/unchecked
	 */
	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		final DroidApp app = (DroidApp) buttonView.getTag();
		if (app != null) {
			switch (buttonView.getId()) {
				case R.id.itemcheck_wifi: app.selected_wifi = isChecked; break;
				case R.id.itemcheck_3g: app.selected_3g = isChecked; break;
			}
		}
	}

	private static class ListEntry {
		private CheckBox box_wifi;
		private CheckBox box_3g;
		private ImageView icon;
		private TextView text;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.label_mode:
			selectMode();
			break;
        case R.id.img_setting:
            String _tmp[]={getString(R.string.fw_enabled),getString(R.string.log_enabled),
                    getString(R.string.applyrules),getString(R.string.showrules),
                    getString(R.string.help),getString(R.string.show_log),
                    getString(R.string.clear_log),getString(R.string.pass_titleset),
                    getString(R.string.change_theme)};
            final boolean enabled = Api.isEnabled(this);
            if (enabled) {
                _tmp[0]=getString(R.string.fw_enabled);
            } else {
                _tmp[0]=getString(R.string.fw_disabled);
            }
            final boolean logenabled = getSharedPreferences(Api.PREFS_NAME, 0).getBoolean(Api.PREF_LOGENABLED, false);
            if (logenabled) {
                _tmp[1]=getString(R.string.log_enabled);
            } else {
                _tmp[1]=getString(R.string.log_disabled);
            }
            new AlertDialog.Builder(this).setItems(_tmp, new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case MENU_DISABLE:
                            disableOrEnable();
                            return ;
                        case MENU_SHOWRULES:
                            showRules();
                            return ;
                        case MENU_APPLY:
                            applyOrSaveRules();
                            return ;
                        case MENU_SETUSERINFO:
                            Toast.makeText(MainActivity.this, "设置用户信息", Toast.LENGTH_SHORT).show();
                            setUserInfo();
                            return ;
                        case MENU_HELP:
                            new HelpDialog(MainActivity.this).show();
                            return ;
                        case MENU_TOGGLELOG:
                            toggleLogEnabled();
                            return ;
                        case MENU_CLEARLOG:
                            clearLog();
                            return ;
                        case MENU_SHOWLOG:
                            showLog();
                            return ;
                        case MENU_CHNGTHEME:
                            changeTheme();
                            return ;
                    }
                }
            }).setTitle("menu").show();
            break;
		}
	}
}
