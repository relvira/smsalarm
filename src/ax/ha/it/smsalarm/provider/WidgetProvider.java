/**
 * Copyright (c) 2013 Robert Nyholm. All rights reserved.
 */
package ax.ha.it.smsalarm.provider;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;
import android.widget.TextView;
import ax.ha.it.smsalarm.R;
import ax.ha.it.smsalarm.activity.SmsAlarm;
import ax.ha.it.smsalarm.activity.Splash;
import ax.ha.it.smsalarm.alarm.Alarm;
import ax.ha.it.smsalarm.application.SmsAlarmApplication.GoogleAnalyticsHandler;
import ax.ha.it.smsalarm.application.SmsAlarmApplication.GoogleAnalyticsHandler.EventAction;
import ax.ha.it.smsalarm.application.SmsAlarmApplication.GoogleAnalyticsHandler.EventCategory;
import ax.ha.it.smsalarm.handler.DatabaseHandler;
import ax.ha.it.smsalarm.handler.SharedPreferencesHandler;
import ax.ha.it.smsalarm.handler.SharedPreferencesHandler.DataType;
import ax.ha.it.smsalarm.handler.SharedPreferencesHandler.PrefKey;

/**
 * Provider class for the application widgets. This class is responsible for all updates, data population, data presentation and so on for a widget.<br>
 * This implementation should be safe with more than one instances of the Sms Alarm widget.
 * 
 * @author Robert Nyholm <robert.nyholm@aland.net>
 * @version 2.3.1
 * @since 2.1
 */
public class WidgetProvider extends AppWidgetProvider {
	// To get access to shared preferences and database
	private final SharedPreferencesHandler prefHandler = SharedPreferencesHandler.getInstance();
	private DatabaseHandler db;

	// Some different labels used when sending events to Google Analytics
	private static final String SMS_ALARM_ACTIVE_STATE_CHANGED_LABEL = "Sms Alarm active state changed";
	private static final String USE_OPERATING_SYSTEMS_SOUND_SETTINGS_CHANGED_LABEL = "Use operating systems sound settings changed";
	public static final String OPEN_ALARM_LOG_LABEL = "Alarm log opened";
	public static final String OPEN_SMS_ALARM_LABEL = "Sms Alarm opened";

	// Max length of the latest alarm length in widget
	private static final int ALARM_TEXT_MAX_LENGTH = 100;

	// Used only in widget
	private static final String BLANK_SPACE = " ";

	// Strings representing different intent actions used to run different methods from intent
	private static final String TOGGLE_ENABLE_SMS_ALARM = "ax.ha.it.smsalarm.TOGGLE_SMS_ALARM_ENABLE";
	private static final String TOGGLE_USE_OS_SOUND_SETTINGS = "ax.ha.it.smsalarm.TOGGLE_USE_OS_SOUND_SETTINGS";
	private static final String UPDATE_WIDGETS = "ax.ha.it.smsalarm.UPDATE_WIDGETS";

	// Some booleans for retrieving preferences into
	private boolean useOsSoundSettings = false;
	private boolean enableSmsAlarm = false;
	private boolean endUserLicenseAgreed = false;

	/**
	 * To receive intents broadcasted throughout the operating system. If it receives any intents that it listens on the method takes proper action.<br>
	 * The method listens on following intents:
	 * <ul>
	 * <li>ax.ha.it.smsalarm.TOGGLE_SMS_ALARM_ENABLE</li>
	 * <li>ax.ha.it.smsalarm.TOGGLE_USE_OS_SOUND_SETTINGS</li>
	 * <li>ax.ha.it.smsalarm.UPDATE_WIDGETS</li>
	 * </ul>
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		// Get AppWidgetManager from context
		AppWidgetManager manager = AppWidgetManager.getInstance(context);

		// Get Shared preferences needed by widget
		fetchSharedPrefs(context);

		// If statements to "catch" intent we looking for
		if (TOGGLE_ENABLE_SMS_ALARM.equals(intent.getAction())) {
			// Set shared preferences depending on current preferences
			if (enableSmsAlarm) {
				setEnableSmsAlarmPref(context, false);
			} else {
				setEnableSmsAlarmPref(context, true);
			}

			// Update widget
			WidgetProvider.updateWidgets(context);

			// Report event to Google Analytics
			GoogleAnalyticsHandler.sendEvent(EventCategory.USER_INTERFACE, EventAction.WIDGET_INTERACTION, SMS_ALARM_ACTIVE_STATE_CHANGED_LABEL);
		} else if (TOGGLE_USE_OS_SOUND_SETTINGS.equals(intent.getAction())) {
			if (useOsSoundSettings) {
				setUseOsSoundSettingsPref(context, false);
			} else {
				setUseOsSoundSettingsPref(context, true);
			}

			WidgetProvider.updateWidgets(context);
			GoogleAnalyticsHandler.sendEvent(EventCategory.USER_INTERFACE, EventAction.WIDGET_INTERACTION, USE_OPERATING_SYSTEMS_SOUND_SETTINGS_CHANGED_LABEL);
		} else if (UPDATE_WIDGETS.equals(intent.getAction())) {
			// Call onUpdate to update the widget instances
			onUpdate(context, manager, AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context.getPackageName(), getClass().getName())));
		}

		// Needs to call superclass's onReceive
		super.onReceive(context, intent);
	}

	/**
	 * To update all instances of the Sms Alarm widget.
	 */
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		// Get Shared preferences needed by widget
		fetchSharedPrefs(context);
		// Initialize database handler object from context
		db = new DatabaseHandler(context);
		// RemoteViews object needed to configure layout of widget
		RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget);

		// Update each of the application widgets with the remote adapter
		for (int i = 0; i < appWidgetIds.length; ++i) {
			// Set intent to start Sms Alarm and wrap it into a pending intent, rest of the intents
			// are configured in the same way
			Intent smsAlarmIntent = new Intent(context, Splash.class);
			smsAlarmIntent.setAction(Splash.ACTION_REPORT_OPENED_THROUGH_WIDGET);
			smsAlarmIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
			PendingIntent smsAlarmPendingIntent = PendingIntent.getActivity(context, 0, smsAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);

			Intent enableSmsAlarmIntent = new Intent(context, WidgetProvider.class);
			enableSmsAlarmIntent.setAction(WidgetProvider.TOGGLE_ENABLE_SMS_ALARM);
			enableSmsAlarmIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
			PendingIntent enableSmsAlarmPendingIntent = PendingIntent.getBroadcast(context, 0, enableSmsAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);

			Intent useOsSoundSettingsIntent = new Intent(context, WidgetProvider.class);
			useOsSoundSettingsIntent.setAction(WidgetProvider.TOGGLE_USE_OS_SOUND_SETTINGS);
			useOsSoundSettingsIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
			PendingIntent useOsSoundSettingsPendingIntent = PendingIntent.getBroadcast(context, 0, useOsSoundSettingsIntent, PendingIntent.FLAG_UPDATE_CURRENT);

			Intent showAlarmLogIntent = new Intent(context, SmsAlarm.class);
			showAlarmLogIntent.setAction(SmsAlarm.ACTION_SWITCH_TO_ALARM_LOG_FRAGMENT);
			showAlarmLogIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
			PendingIntent showAlarmLogPendingIntent = PendingIntent.getActivity(context, 0, showAlarmLogIntent, PendingIntent.FLAG_UPDATE_CURRENT);

			// Set widget texts
			setWidgetTextViews(rv, context);

			// Set onClick pending intent to start Sms Alarm, this is always set
			rv.setOnClickPendingIntent(R.id.widget_logo_iv, smsAlarmPendingIntent);

			// If user has agreed the end user license, set the rest of the on click pending intents
			// also
			if (endUserLicenseAgreed) {
				rv.setOnClickPendingIntent(R.id.widget_smsalarm_status_tv, enableSmsAlarmPendingIntent);
				rv.setOnClickPendingIntent(R.id.widget_soundsettings_status_tv, useOsSoundSettingsPendingIntent);
				rv.setOnClickPendingIntent(R.id.widget_latest_received_alarm_tv, showAlarmLogPendingIntent);
			}

			// Update widget
			appWidgetManager.updateAppWidget(appWidgetIds[i], rv);
		}

		// Call to super class onUpdate method, so the Operating System can run it's native methods
		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}

	/**
	 * To set appropriate text's to the different {@link TextView}'s. The text's are set depending if user has agreed the end user license, and if any
	 * {@link Alarm}'s has been received, status of SmsAlarm(enabled/disabled) and status of use devices sound settings(enabled/disabled).
	 * 
	 * @param rv
	 *            {@link RemoteViews} that texts should be set to.
	 * @param context
	 *            The Context in which the provider is running.
	 */
	private void setWidgetTextViews(RemoteViews rv, Context context) {
		// If user has agreed end user license, we fill in the TextViews with "real" data
		if (endUserLicenseAgreed) {
			// Check if Sms Alarm is enabled or not and set TextView depending on that
			if (enableSmsAlarm) {
				rv.setTextViewText(R.id.widget_smsalarm_status_tv, context.getString(R.string.SMS_ALARM_STATUS_ENABLED));
			} else {
				rv.setTextViewText(R.id.widget_smsalarm_status_tv, context.getString(R.string.SMS_ALARM_STATUS_DISABLED));
			}

			// Check if use devices sound settings is enabled or not and set TextView depending on that
			if (useOsSoundSettings) {
				rv.setTextViewText(R.id.widget_soundsettings_status_tv, context.getString(R.string.SOUND_SETTINGS_STATUS_ENABLED));
			} else {
				rv.setTextViewText(R.id.widget_soundsettings_status_tv, context.getString(R.string.SOUND_SETTINGS_STATUS_DISABLED));
			}

			// Set the shortened alarm to TextView
			rv.setTextViewText(R.id.widget_latest_received_alarm_tv, getLatestAlarm(context));

			// Set correct dividers to widget
			rv.setImageViewResource(R.id.widget_divider2_iv, R.drawable.gradient_divider_widget);
			rv.setImageViewResource(R.id.widget_divider3_iv, R.drawable.gradient_divider_widget);
		} else { // User has not agreed end user license, hide dividers and TextView, set text to one TextView telling user what's wrong
			rv.setTextViewText(R.id.widget_smsalarm_status_tv, context.getString(R.string.WIDGET_NOT_AGREED_EULA));
			rv.setImageViewResource(R.id.widget_divider2_iv, android.R.color.transparent);
			rv.setTextViewText(R.id.widget_soundsettings_status_tv, context.getString(R.string.EMPTY_STRING));
			rv.setImageViewResource(R.id.widget_divider3_iv, android.R.color.transparent);
			rv.setTextViewText(R.id.widget_latest_received_alarm_tv, context.getString(R.string.EMPTY_STRING));
		}
	}

	/**
	 * To get the latest {@link Alarm} from database as a <code>String</code>. If no <code>Alarm</code> exist or the <code>Alarm</code> is empty an
	 * appropriate <code>String</code> is returned instead.
	 * 
	 * @param context
	 *            The Context in which the provider is running.
	 * @return String with appropriate text depending on if any alarms exists or not in database.
	 */
	private String getLatestAlarm(Context context) {
		// Check if there exists alarms in database
		if (db.getAlarmsCount() > 0) {
			// To store latest alarm into
			Alarm alarm = db.fetchLatestAlarm();
			// To build up string into
			StringBuilder alarmInfo = new StringBuilder();
			StringBuilder alarmMessage = new StringBuilder();

			// Sanity check to see whether alarm holds valid info or not
			if (alarm.isValid()) {
				// Build up the string representing the latest alarm from alarm object
				alarmInfo.append(context.getString(R.string.TITLE_ALARM_INFO_ALARM_TYPE));
				alarmInfo.append(context.getString(R.string.COLON));
				alarmInfo.append(BLANK_SPACE);
				alarmInfo.append(alarm.getAlarmTypeLocalized(context));
				alarmInfo.append(context.getString(R.string.NEW_LINE));

				alarmInfo.append(context.getString(R.string.TITLE_ALARM_INFO_SENDER));
				alarmInfo.append(context.getString(R.string.COLON));
				alarmInfo.append(BLANK_SPACE);
				alarmInfo.append(alarm.getSender());
				alarmInfo.append(context.getString(R.string.NEW_LINE));

				alarmInfo.append(context.getString(R.string.TITLE_ALARM_INFO_RECEIVED));
				alarmInfo.append(context.getString(R.string.COLON));
				alarmInfo.append(BLANK_SPACE);
				alarmInfo.append(alarm.getReceivedLocalized());
				alarmInfo.append(context.getString(R.string.NEW_LINE));

				alarmInfo.append(context.getString(R.string.TITLE_ALARM_INFO_ACKNOWLEDGED));
				alarmInfo.append(context.getString(R.string.COLON));
				alarmInfo.append(BLANK_SPACE);
				alarmInfo.append(alarm.getAcknowledgedLocalized());
				alarmInfo.append(context.getString(R.string.NEW_LINE));

				// Build up the alarm message in separate StringBuilder so we can shorten it if we need
				alarmMessage.append(context.getString(R.string.TITLE_ALARM_INFO_MESSAGE));
				alarmMessage.append(context.getString(R.string.COLON));
				alarmMessage.append(BLANK_SPACE);
				alarmMessage.append(alarm.getMessage());

				// Check if alarm message is longer than the limits for the TextView
				if (alarmMessage.length() > ALARM_TEXT_MAX_LENGTH) {
					// If longer than TextView limit shorten it of and add dots to it
					alarmMessage = new StringBuilder(alarmMessage.substring(0, (ALARM_TEXT_MAX_LENGTH - 3)));
					alarmMessage.append("...");
				}

				alarmInfo.append(alarmMessage.toString());

				// Return latest alarm as string
				return alarmInfo.toString();
			} else {
				// An error occurred while retrieving alarm from database, return error message
				return context.getString(R.string.ERROR_RETRIEVING_FROM_DB);
			}
		} else {
			// No alarms exists in database, return appropriate string
			return context.getString(R.string.NO_RECEIVED_ALARMS_EXISTS);
		}
	}

	/**
	 * To fetch all {@link SharedPreferences} used by {@link WidgetProvider} class.
	 * 
	 * @param context
	 *            The Context in which the provider is running.
	 */
	private void fetchSharedPrefs(Context context) {
		// Get shared preferences needed by WidgetProvider
		enableSmsAlarm = (Boolean) prefHandler.fetchPrefs(PrefKey.SHARED_PREF, PrefKey.ENABLE_SMS_ALARM_KEY, DataType.BOOLEAN, context, true);
		useOsSoundSettings = (Boolean) prefHandler.fetchPrefs(PrefKey.SHARED_PREF, PrefKey.USE_OS_SOUND_SETTINGS_KEY, DataType.BOOLEAN, context);
		endUserLicenseAgreed = (Boolean) prefHandler.fetchPrefs(PrefKey.SHARED_PREF, PrefKey.END_USER_LICENSE_AGREED, DataType.BOOLEAN, context, false);
	}

	/**
	 * To set enable Sms Alarm setting to {@link SharedPreferences}.
	 * 
	 * @param context
	 *            The Context in which the provider is running.
	 * @param enabled
	 *            Boolean indicating whether or not Sms Alarm is enabled.
	 */
	private void setEnableSmsAlarmPref(Context context, boolean enabled) {
		prefHandler.storePrefs(PrefKey.SHARED_PREF, PrefKey.ENABLE_SMS_ALARM_KEY, enabled, context);
	}

	/**
	 * To set use operating systems sound settings to {@link SharedPreferences}.
	 * 
	 * @param context
	 *            The Context in which the provider is running.
	 * @param enabled
	 *            Boolean indicating whether or not Sms Alarm should use the operating systems sound settings or not.
	 */
	private void setUseOsSoundSettingsPref(Context context, boolean enabled) {
		prefHandler.storePrefs(PrefKey.SHARED_PREF, PrefKey.USE_OS_SOUND_SETTINGS_KEY, enabled, context);
	}

	/**
	 * To update all <code>Widget</code>'s associated to Sms Alarm.
	 */
	public static void updateWidgets(Context context) {
		// Create intent from WidgetProvider and set action to update widget
		Intent intent = new Intent(context, WidgetProvider.class);
		intent.setAction(WidgetProvider.UPDATE_WIDGETS);

		// Send the broadcast
		context.sendBroadcast(intent);
	}
}
