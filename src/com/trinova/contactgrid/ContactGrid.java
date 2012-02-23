package com.trinova.contactgrid;

import java.io.InputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;

public class ContactGrid extends Activity
{
	// Constants that are internal to this class
	private static final int MENU_PREFERENCES = Menu.FIRST;
	private static final int MENU_CONTACTS = Menu.FIRST + 1;

	private static final int POPUP_OPTIONS_CONTACT = 1;
	private static final int POPUP_OPTIONS_EMPTY = 2;

	private static final int ACTION_SELECT = 1;
	private static final int ACTION_ADD = 2;
	private static final int ACTION_REMOVE = 3;

	private static final int PICK_CONTACT = 1;
	private static final int CHANGE_PREFS = 2;

	private static final long NO_CONTACT = -1;

	// Class variables
	private long[] _savedKeys;
	private boolean _showmessages;
	private boolean _actionshortcuts;
	private int _numrows;
	private int _numcols;
	private int _numentries;

	/** Called when the activity is first created. */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Initialize class variables
		GetPreferences();

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		for (int x = 0; x < _numentries; x++)
		{
			_savedKeys[x] = settings.getLong("SavedID" + x, NO_CONTACT);
		}

		// Initialize the grid
		GridView grid = (GridView) findViewById(R.id.gridview);
		grid.setAdapter(new ContactAdapter(this));
		grid.setNumColumns(_numcols);

		grid.setOnItemClickListener(new OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> parent, View v, int position, long id)
			{
				HandleClickedItem(position, ACTION_SELECT);
			}
		});
		grid.setOnItemLongClickListener(new OnItemLongClickListener()
		{
			public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id)
			{
				return HandleLongClickedItem(position);
			}
		});

		// Initialize the title bar
		setWindowTitle();
	}

	/** Called when the activity is stopped */
	@Override
	protected void onStop()
	{
		super.onStop();

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = settings.edit();
		for (int x = 0; x < _numentries; x++)
		{
			editor.putLong("SavedID" + x, _savedKeys[x]);
		}
		editor.commit();
	}

	/** Create menu items */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		menu.add(0, MENU_CONTACTS, 0, "Contacts").setIcon(R.drawable.ic_menu_cc);
		menu.add(0, MENU_PREFERENCES, 0, "Preferences").setIcon(R.drawable.ic_menu_preferences);

		return true;
	}

	/** Handle menu items */
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case MENU_PREFERENCES:
			LaunchPreferences();
			return true;
		case MENU_CONTACTS:
			LaunchContacts();
			return true;
		}

		return false;
	}

	/** Handle return values from other activities */
	@Override
	public void onActivityResult(int reqCode, int resultCode, Intent data)
	{
		super.onActivityResult(reqCode, resultCode, data);

		// The request code has the real code and the index embedded in it
		int realcode = reqCode / 100;
		int index = reqCode % 100;

		GridView grid = (GridView) findViewById(R.id.gridview);

		switch (realcode)
		{
		case PICK_CONTACT:
			if (resultCode == Activity.RESULT_OK)
			{
				Uri contactdata = data.getData();
				Cursor c = managedQuery(contactdata, null, null, null, null);
				if (c.moveToFirst())
				{
					long key = c.getLong(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
					_savedKeys[index] = key;
				}
			}

			// Refresh the grid
			grid.invalidateViews();
			break;
		case CHANGE_PREFS:
			// Update the preferences
			GetPreferences();

			// Resize the grid
			grid.setNumColumns(_numcols);
			grid.invalidateViews();
			break;
		}
	}
	
	/** Update the title of a Dialog because onCreateDialog() is only called once */
	@Override
	protected void onPrepareDialog(int id, Dialog dialog)
	{
		int realid = id / 100;
		int index = id % 100;
		switch (realid)
		{
		case POPUP_OPTIONS_CONTACT:
			dialog.setTitle(getGridName(index)); // The title needs to be updated because the person in the grid space may have been changed
			break;
		}
	}

	/** Create a popup dialog */
	@Override
	public Dialog onCreateDialog(int id)
	{
		int realid = id / 100;
		final int index = id % 100; // This has to be final so that the internal objects I'm about to declare can see it
		switch (realid)
		{
		case POPUP_OPTIONS_CONTACT:
			return new AlertDialog.Builder(this).setTitle(getGridName(index)).setItems(R.array.list_popup_options_contact, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int which)
				{
					switch (which)
					{
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
			return new AlertDialog.Builder(this).setTitle("Empty Space").setItems(R.array.list_popup_options_empty, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int which)
				{
					switch (which)
					{
					case 0:
						HandleClickedItem(index, ACTION_ADD);
						break;
					}
				}
			}).create();
		}
		return null;
	}

	/** Read the Preferences */
	private void GetPreferences()
	{
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		_showmessages = settings.getBoolean("preference_messages", true);
		_actionshortcuts = settings.getBoolean("preference_actionshortcuts", true);

		// I'm sure there's a better way to do this - http://stackoverflow.com/questions/3206765/number-preferences-in-preference-activity-in-android
		String val = settings.getString("preference_numrows", "Four");
		if (val.equalsIgnoreCase("one"))
			_numrows = 1;
		else if (val.equalsIgnoreCase("two"))
			_numrows = 2;
		else if (val.equalsIgnoreCase("three"))
			_numrows = 3;
		else if (val.equalsIgnoreCase("four"))
			_numrows = 4;
		else if (val.equalsIgnoreCase("five"))
			_numrows = 5;
		else if (val.equalsIgnoreCase("six"))
			_numrows = 6;
		else if (val.equalsIgnoreCase("seven"))
			_numrows = 7;
		else if (val.equalsIgnoreCase("eight"))
			_numrows = 8;
		else if (val.equalsIgnoreCase("nine"))
			_numrows = 9;
		else if (val.equalsIgnoreCase("ten"))
			_numrows = 10;

		val = settings.getString("preference_numcols", "Three");
		if (val.equalsIgnoreCase("one"))
			_numcols = 1;
		else if (val.equalsIgnoreCase("two"))
			_numcols = 2;
		else if (val.equalsIgnoreCase("three"))
			_numcols = 3;
		else if (val.equalsIgnoreCase("four"))
			_numcols = 4;
		else if (val.equalsIgnoreCase("five"))
			_numcols = 5;
		else if (val.equalsIgnoreCase("six"))
			_numcols = 6;
		else if (val.equalsIgnoreCase("seven"))
			_numcols = 7;
		else if (val.equalsIgnoreCase("eight"))
			_numcols = 8;
		else if (val.equalsIgnoreCase("nine"))
			_numcols = 9;
		else if (val.equalsIgnoreCase("ten"))
			_numcols = 10;

		_numentries = _numrows * _numcols;

		// Recreate the saved keys array
		long temp[] = new long[_numentries];
		int keycnt = 0;
		if (_savedKeys != null)
		{
			keycnt = _savedKeys.length;
			for (int x = 0; x < Math.min(keycnt, temp.length); x++)
				temp[x] = _savedKeys[x];
		}
		for (int x = keycnt; x < temp.length; x++)
			// Fill the new spaces in the array with NO_CONTACT
			temp[x] = NO_CONTACT;
		_savedKeys = temp;
	}

	/** Launch the Preferences activity */
	private void LaunchPreferences()
	{
		Intent intent = new Intent();
		intent.setClass(this, ContactGridPreferences.class);
		startActivityForResult(intent, CHANGE_PREFS * 100); // RequestCodes have to be multiplied by 100
	}

	/** Launch the Android Contact Manager */
	private void LaunchContacts()
	{
		Intent intent = new Intent(Intent.ACTION_DEFAULT, ContactsContract.Contacts.CONTENT_URI);
		startActivity(intent);
	}

	/** Deal with the selected item */
	private void HandleClickedItem(int index, int action)
	{
		// Figure out what to do with the selected item
		if (action == ACTION_SELECT)
		{
			if (hasContact(index))
			{
				Uri lookupuri = getGridURI(index);
				Intent intent = new Intent(Intent.ACTION_VIEW, lookupuri);
				startActivity(intent);
			}
			else if (_showmessages)
			{
				showToast("You must add a contact to that space first");
			}
		}
		else if (action == ACTION_ADD)
		{
			if (!hasContact(index))
			{
				// Opens a Contact list so that the user can select a Contant to add to the Grid
				Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
				startActivityForResult(intent, PICK_CONTACT * 100 + index); // Merge the request code and the index into a single value
			}
			else if (_showmessages)
			{
				showToast("That space is already taken");
			}
		}
		else if (action == ACTION_REMOVE)
		{
			_savedKeys[index] = NO_CONTACT;
		}

		// Refresh the grid
		GridView grid = (GridView) findViewById(R.id.gridview);
		grid.invalidateViews();
	}

	/** Bring up a menu to valid options for the selected space */
	private boolean HandleLongClickedItem(int index)
	{
		if (hasContact(index))
		{
			showDialog(POPUP_OPTIONS_CONTACT * 100 + index); // Merge the code and the index into a single value
		}
		else
		{
			showDialog(POPUP_OPTIONS_EMPTY * 100 + index);
		}

		return true;
	}
	
	/** Sets the window's title based on the current mode */
	private void setWindowTitle()
	{
		String title = "Contact Grid - Select Contact";

		setTitle(title);
	}

	/** Display a short Toast with the specified text */
	private void showToast(String msg)
	{
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}

	/** Return whether or not there's an entry in a grid space */
	public boolean hasContact(int index)
	{
		long id = _savedKeys[index];

		if (id == NO_CONTACT)
			return false;

		return true;
	}

	/** Returns the URI of the person in the specified grid space */
	public Uri getGridURI(int index)
	{
		if (!hasContact(index))
			return null;

		Uri griduri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, _savedKeys[index]);

		return griduri;
	}

	/** Returns the photo assigned to the specified grid space */
	public Bitmap getGridPhoto(int index)
	{
		Uri contacturi = getGridURI(index);
		if (contacturi == null)
			return null;

		InputStream stream = ContactsContract.Contacts.openContactPhotoInputStream(getContentResolver(), contacturi);
		if (stream == null)
			return null;

		return BitmapFactory.decodeStream(stream);
	}

	/** Returns the name assigned to teh specified grid space */
	public String getGridName(int index)
	{
		Uri contacturi = getGridURI(index);
		if (contacturi == null)
			return "<unknown>";

		Cursor c = managedQuery(contacturi, null, null, null, null);
		if (c.moveToFirst())
		{
			String name = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
			return name;
		}

		return "<null>";
	}

	/** Returns the number of entries in the grid */
	public int getNumEntries()
	{
		return _numentries;
	}
	
	/** Returns whether or not Action Shortcuts should be used */
	public boolean getUseActionShortcuts()
	{
		return _actionshortcuts;
	}
}