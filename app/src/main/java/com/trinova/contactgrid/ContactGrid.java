package com.trinova.contactgrid;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.Toast;

import java.io.InputStream;

public class ContactGrid extends Activity {
	// Constants that are internal to this class
	private static final int POPUP_OPTIONS_CONTACT = 1;
	private static final int POPUP_OPTIONS_EMPTY = 2;

	private static final int ACTION_SELECT = 1;
	private static final int ACTION_ADD = 2;
	private static final int ACTION_REMOVE = 3;
	private static final int ACTION_ADD_GROUP = 4;

	private static final int PICK_CONTACT = 1;
	private static final int CHANGE_PREFS = 2;
	private static final int PICK_GROUP = 3;

	private static final long NO_CONTACT = -1;

	private static final int PERMISSION_READ_CONTACTS_REQUEST = 1;

	// Class variables
	private long[] savedKeys;
	private boolean showMessages;
	private boolean useActionShortcuts;
	private int numRows;
	private int numCols;
	private int numEntries;
	private boolean readContactsAllowed;

	/**
	 * Called when the activity is first created.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Initialize preference variables
		GetPreferences();

		// Initialize the grid with saved preferences
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		for (int x = 0; x < numEntries; x++) {
			savedKeys[x] = settings.getLong("SavedID" + x, NO_CONTACT);
		}

		// Check to see if the READ_CONTACTS permission has been granted
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Marshmallow or later
			if (checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
				readContactsAllowed = true;
			}
			else {
				readContactsAllowed = false;
				requestPermissions(new String[] { Manifest.permission.READ_CONTACTS }, PERMISSION_READ_CONTACTS_REQUEST);
			}
		}
		else { // Pre-marshmallow
			readContactsAllowed = true;
		}

		// Initialize the grid
		GridView grid = (GridView) findViewById(R.id.gridview);
		grid.setNumColumns(numCols);
		grid.setAdapter(new ContactAdapter(this));

		grid.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				HandleClickedItem(position, ACTION_SELECT);
			}
		});
		grid.setOnItemLongClickListener(new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
				return HandleLongClickedItem(position);
			}
		});

	}

	/**
	 * Called when the activity is stopped
	 */
	@Override
	protected void onStop() {
		super.onStop();

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = settings.edit();
		for (int x = 0; x < numEntries; x++) {
			editor.putLong("SavedID" + x, savedKeys[x]);
		}
		editor.commit();
	}

	/**
	 * Create menu items
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_grid_menu, menu);

		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * Handle menu items
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_preferences:
				LaunchPreferences();
				return true;
			case R.id.menu_contacts:
				LaunchContacts();
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * Handle return values from other activities
	 */
	@Override
	public void onActivityResult(int reqCode, int resultCode, Intent data) {
		super.onActivityResult(reqCode, resultCode, data);

		// The request code has the real code and the index embedded in it
		int realCode = reqCode / 100;
		int index = reqCode % 100;

		GridView grid = (GridView) findViewById(R.id.gridview);

		switch (realCode) {
			case PICK_CONTACT:
				if (resultCode == Activity.RESULT_OK) {
					Uri contactData = data.getData();
					Cursor c = managedQuery(contactData, null, null, null, null);
					if (c.moveToFirst()) {
						long key = c.getLong(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
						savedKeys[index] = key;
					}
				}

				// Refresh the grid
				grid.invalidateViews();
				break;
			case PICK_GROUP:
				if (resultCode == Activity.RESULT_OK) {
					Uri groupData = data.getData();
					Cursor c = managedQuery(groupData, null, null, null, null);
					if (c.moveToFirst()) {
						long key = c.getLong(c.getColumnIndexOrThrow(ContactsContract.Groups._ID));
						savedKeys[index] = -key - 1; // Group id's are stored as negative numbers
					}
				}
				else if (resultCode == Activity.RESULT_CANCELED) {
					showToast("You have no groups defined");
				}

				grid.invalidateViews();
				break;
			case CHANGE_PREFS:
				// Update the preferences
				GetPreferences();

				// Resize the grid
				grid.setNumColumns(numCols);
				grid.invalidateViews();
				break;
		}
	}

	/**
	 * Handle return value from Permissions request
	 */
	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantPermissions) {
		switch (requestCode) {
			case PERMISSION_READ_CONTACTS_REQUEST: {
				readContactsAllowed = (grantPermissions.length > 0 && grantPermissions[0] == PackageManager.PERMISSION_GRANTED);
				if (readContactsAllowed) // If the user granted the permission, restart the Activity so that it is refreshed correctly
					this.recreate();
			}
		}
	}

	/**
	 * Update the title of a Dialog because onCreateDialog() is only called once
	 */
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		int realId = id / 100;
		int index = id % 100;
		switch (realId) {
			case POPUP_OPTIONS_CONTACT:
				dialog.setTitle(getGridName(index)); // The title needs to be updated because the person in the grid space may have been changed
				break;
		}
	}

	/**
	 * Create a popup dialog
	 */
	@Override
	public Dialog onCreateDialog(int id) {
		int realId = id / 100;
		final int index = id % 100; // This has to be final so that the internal objects I'm about to declare can see it
		switch (realId) {
			case POPUP_OPTIONS_CONTACT:
				return new AlertDialog.Builder(this).setTitle(getGridName(index)).setItems(R.array.list_popup_options_contact, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
							case 0:
								HandleClickedItem(index, ACTION_SELECT);
								break;
							case 1:
								HandleClickedItem(index, ACTION_REMOVE);
								break;
						}
					}
				}).create();
			case POPUP_OPTIONS_EMPTY:
				return new AlertDialog.Builder(this).setTitle("Empty Space").setItems(R.array.list_popup_options_empty, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
							case 0:
								HandleClickedItem(index, ACTION_ADD);
								break;
							case 1:
								HandleClickedItem(index, ACTION_ADD_GROUP);
								break;
						}
					}
				}).create();
		}
		return null;
	}

	/**
	 * Read the Preferences
	 */
	private void GetPreferences() {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		showMessages = settings.getBoolean("preference_messages", true);
		useActionShortcuts = settings.getBoolean("preference_actionshortcuts", true);

		// I'm sure there's a better way to do this - http://stackoverflow.com/questions/3206765/number-preferences-in-preference-activity-in-android
		String val = settings.getString("preference_numrows", "Four");
		if (val.equalsIgnoreCase("one"))
			numRows = 1;
		else if (val.equalsIgnoreCase("two"))
			numRows = 2;
		else if (val.equalsIgnoreCase("three"))
			numRows = 3;
		else if (val.equalsIgnoreCase("four"))
			numRows = 4;
		else if (val.equalsIgnoreCase("five"))
			numRows = 5;
		else if (val.equalsIgnoreCase("six"))
			numRows = 6;
		else if (val.equalsIgnoreCase("seven"))
			numRows = 7;
		else if (val.equalsIgnoreCase("eight"))
			numRows = 8;
		else if (val.equalsIgnoreCase("nine"))
			numRows = 9;
		else if (val.equalsIgnoreCase("ten"))
			numRows = 10;

		val = settings.getString("preference_numcols", "Three");
		if (val.equalsIgnoreCase("one"))
			numCols = 1;
		else if (val.equalsIgnoreCase("two"))
			numCols = 2;
		else if (val.equalsIgnoreCase("three"))
			numCols = 3;
		else if (val.equalsIgnoreCase("four"))
			numCols = 4;
		else if (val.equalsIgnoreCase("five"))
			numCols = 5;
		else if (val.equalsIgnoreCase("six"))
			numCols = 6;
		else if (val.equalsIgnoreCase("seven"))
			numCols = 7;
		else if (val.equalsIgnoreCase("eight"))
			numCols = 8;
		else if (val.equalsIgnoreCase("nine"))
			numCols = 9;
		else if (val.equalsIgnoreCase("ten"))
			numCols = 10;

		numEntries = numRows * numCols;

		// Recreate the saved keys array
		long temp[] = new long[numEntries];
		int keycnt = 0;
		if (savedKeys != null) {
			keycnt = savedKeys.length;
			System.arraycopy(savedKeys, 0, temp, 0, Math.min(keycnt, temp.length));
		}
		for (int x = keycnt; x < temp.length; x++)
			// Fill the new spaces in the array with NO_CONTACT
			temp[x] = NO_CONTACT;
		savedKeys = temp;
	}

	/**
	 * Launch the Preferences activity
	 */
	private void LaunchPreferences() {
		Intent intent = new Intent();
		intent.setClass(this, ContactGridPreferences.class);
		startActivityForResult(intent, CHANGE_PREFS * 100); // RequestCodes have to be multiplied by 100
	}

	/**
	 * Launch the Android Contact Manager
	 */
	private void LaunchContacts() {
		Intent intent = new Intent(Intent.ACTION_DEFAULT, ContactsContract.Contacts.CONTENT_URI);
		startActivity(intent);
	}

	/**
	 * Deal with the selected item
	 */
	private void HandleClickedItem(int index, int action) {
		// Figure out what to do with the selected item
		if (action == ACTION_SELECT) {
			if (hasContact(index)) {
				if (savedKeys[index] >= 0) {
					Uri lookupuri = getGridURI(index);
					Intent intent = new Intent(Intent.ACTION_VIEW, lookupuri);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_NO_HISTORY);
					startActivity(intent);
				}
				else {
					Intent intent = new Intent();
					intent.setClass(this, GroupActions.class);
					intent.putExtra("GroupID", getGroupID(index));
					startActivity(intent);
				}
			}
			else if (showMessages) {
				showToast("You must add a contact to that space first");
			}
		}
		else if (action == ACTION_ADD) {
			if (!hasContact(index)) {
				// Opens a Contact list so that the user can select a Contact to add to the Grid
				if (readContactsAllowed) {
					Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
					startActivityForResult(intent, PICK_CONTACT * 100 + index); // Merge the request code and the index into a single value
				}
				else {
					showToast("You have not granted permission to access your Contacts");
				}
			}
			else if (showMessages) {
				showToast("That space is already taken");
			}
		}
		else if (action == ACTION_ADD_GROUP) {
			if (!hasContact(index)) {
				if (readContactsAllowed) {
					// Opens a list of Groups (there isn't a standard one, so I had to write a custom one)
					Intent intent = new Intent();
					intent.setClass(this, GroupList.class);
					startActivityForResult(intent, PICK_GROUP * 100 + index); // Merge the request code and the index into a single value
				}
				else {
					showToast("You have not granted permission to access your Contacts");
				}
			}
			else if (showMessages) {
				showToast("That space is already taken");
			}
		}
		else if (action == ACTION_REMOVE) {
			savedKeys[index] = NO_CONTACT;
		}

		// Refresh the grid
		GridView grid = (GridView) findViewById(R.id.gridview);
		grid.invalidateViews();
	}

	/**
	 * Bring up a menu to valid options for the selected space
	 */
	private boolean HandleLongClickedItem(int index) {
		if (hasContact(index)) {
			showDialog(POPUP_OPTIONS_CONTACT * 100 + index); // Merge the code and the index into a single value
		}
		else {
			showDialog(POPUP_OPTIONS_EMPTY * 100 + index);
		}

		return true;
	}

	/**
	 * Display a short Toast with the specified text
	 */
	private void showToast(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}

	/**
	 * Return whether or not there's an entry in a grid space
	 */
	public boolean hasContact(int index) {
		long id = savedKeys[index];

		return id != NO_CONTACT;
	}

	/**
	 * Returns the URI of the person in the specified grid space
	 */
	public Uri getGridURI(int index) {
		if (!hasContact(index))
			return null;

		if (!readContactsAllowed)
			return null;

		Uri griduri;
		if (savedKeys[index] >= 0)
			griduri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, savedKeys[index]);
		else
			griduri = ContentUris.withAppendedId(ContactsContract.Groups.CONTENT_URI, getGroupID(index));
		return griduri;
	}

	/**
	 * Returns the photo assigned to the specified grid space
	 */
	public Bitmap getGridPhoto(int index) {
		Uri contactUri = getGridURI(index);
		if (contactUri == null || savedKeys[index] < 0)
			return null;

		InputStream stream = ContactsContract.Contacts.openContactPhotoInputStream(getContentResolver(), contactUri);
		if (stream == null)
			return null;

		return BitmapFactory.decodeStream(stream);
	}

	/**
	 * Returns the name assigned to the specified grid space
	 */
	public String getGridName(int index) {
		Uri contactUri = getGridURI(index);
		if (contactUri == null)
			return "<unknown>";

		Cursor c = managedQuery(contactUri, null, null, null, null);
		if (c.moveToFirst()) {
			String column;
			if (savedKeys[index] >= 0)
				column = ContactsContract.Contacts.DISPLAY_NAME;
			else
				column = ContactsContract.Groups.TITLE;
			return c.getString(c.getColumnIndexOrThrow(column));
		}

		return "<null>";
	}

	/**
	 * Converts the stored index for a group into a usable id
	 */
	private long getGroupID(int index) {
		// ID 1 is saved as -2, 2 is -3, etc. (so NO_CONTACT still works)
		return -1 * (savedKeys[index] + 1);
	}

	/**
	 * Returns the number of entries in the grid
	 */
	public int getNumEntries() {
		return numEntries;
	}

	/**
	 * Returns the number of columns in the grid
	 */
	public int getNumColumns() {
		return numCols;
	}

	/**
	 * Returns whether or not Action Shortcuts should be used
	 */
	public boolean getUseActionShortcuts() {
		return useActionShortcuts;
	}

	/**
	 * Return whether or not the index is a contact (not a group)
	 */
	public boolean isIndexAContact(int index) {
		return savedKeys[index] >= 0;
	}
}
