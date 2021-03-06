/**
 * Copyright (c) 2014 Robert Nyholm. All rights reserved.
 */
package ax.ha.it.smsalarm.fragment.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import android.util.Log;
import ax.ha.it.smsalarm.R;
import ax.ha.it.smsalarm.ui.NoBlanksEditText;

/**
 * {@link DialogFragment} which let's the user add a <b><i>Phone Number</i></b> to the list of <b><i>Primary or Secondary Alarm Trigger Phone
 * Numbers</i></b>, this depends on which <b><i>RequestCode</i></b> this <code>AddSmsNumberDialog</code> is set to.<br>
 *
 * @author Robert Nyholm <robert.nyholm@aland.net>
 * @version 2.3.1
 * @since 2.3.1
 * @see EditSmsNumberDialog
 * @see RemoveSmsNumberDialog
 * @see #ADD_SMS_NUMBER
 * @see #ADD_SMS_NUMBER_DIALOG_TAG
 * @see #ADD_PRIMARY_SMS_NUMBER_DIALOG_REQUEST_CODE
 * @see #ADD_SECONDARY_SMS_NUMBER_DIALOG_REQUEST_CODE
 */
public class AddSmsNumberDialog extends DialogFragment {
	private static final String LOG_TAG = AddSmsNumberDialog.class.getSimpleName();

	// Used as a key when putting data into bundles and intents, dialog tag can come in handy for classes using this dialog
	public static final String ADD_SMS_NUMBER = "addSmsNumber";
	public static final String ADD_SMS_NUMBER_DIALOG_TAG = "addSmsNumberDialog";

	// Request codes used for this dialog
	public static final int ADD_PRIMARY_SMS_NUMBER_DIALOG_REQUEST_CODE = 1;
	public static final int ADD_SECONDARY_SMS_NUMBER_DIALOG_REQUEST_CODE = 2;

	// Must have application context
	private Context context;

	// Must be declared as class variable as it will be used when handling instance states
	private NoBlanksEditText inputEditText;

	/**
	 * To create a new instance of {@link AddSmsNumberDialog}.
	 */
	public AddSmsNumberDialog() {
		// Just empty...
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set context here, it's safe because this dialog fragment has been attached to it's container, hence we have access to context
		context = getActivity();
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Setup the EditText
		// @formatter:off
		inputEditText = new NoBlanksEditText(context);
		inputEditText.setHint(R.string.PHONE_NUMBER_DIALOG_HINT);	// Set hint to EditText
		inputEditText.setInputType(InputType.TYPE_CLASS_TEXT);		// Set input type to EditText
		// @formatter:on

		// If not null, the fragment is being re-created, get data from saved instance, if exist.
		// If saved instance doesn't contain certain key or it's associated value the EditText field will be empty
		if (savedInstanceState != null) {
			// Check if we got any data in saved instance associated with certain key
			if (savedInstanceState.getCharSequence(ADD_SMS_NUMBER) != null) {
				inputEditText.setText(savedInstanceState.getCharSequence(ADD_SMS_NUMBER).toString());
			}
		}

		// Need to resolve correct message in dialog depending on request code
		String message = "";
		switch (getTargetRequestCode()) {
			case (ADD_PRIMARY_SMS_NUMBER_DIALOG_REQUEST_CODE):
				message = getString(R.string.ADD_PRIMARY_PHONE_NUMBER_DIALOG_MESSAGE);
				break;
			case (ADD_SECONDARY_SMS_NUMBER_DIALOG_REQUEST_CODE):
				message = getString(R.string.ADD_SECONDARY_PHONE_NUMBER_DIALOG_MESSAGE);
				break;
			default:
				Log.e(LOG_TAG + ":onCreateDialog()", "Cannot resolve dialog message due to an unsupported request code: \"" + getTargetRequestCode() + "\"");
		}

		// Setup the dialog with correct resources, listeners and values
		// @formatter:off
		return new AlertDialog.Builder(context)
				.setIcon(android.R.drawable.ic_dialog_info)			// Set icon
				.setTitle(R.string.ADD_PHONE_NUMBER_DIALOG_TITLE)	// Set title
				.setMessage(message)								// Set resolved message
				.setView(inputEditText)								// Bind dialog to input
				// @formatter:on

				.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						// Create an intent and put data from this dialogs EditText and associate it with a certain key
						Intent intent = new Intent();
						intent.putExtra(ADD_SMS_NUMBER, inputEditText.getText().toString());

						// Make a call to this dialog fragments owning fragments onAcitivityResult with correct request code, result code and intent
						getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
					}
				})

				.setNegativeButton(R.string.CANCEL, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, null);
					}
				})

				.create();

	}

	@Override
	public void onSaveInstanceState(Bundle arg0) {
		super.onSaveInstanceState(arg0);
		arg0.putCharSequence(ADD_SMS_NUMBER, inputEditText.getText().toString());
	}
}
