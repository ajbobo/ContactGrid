package ajbobo.contactgrid;

import ajbobo.contactgrid.ContactGrid;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;

public class ContactGrid extends Activity
{
	// Constants that are available to other classes
	public static final int MAX_ENTRIES = 12;

	public static final int MODE_SELECT = 1;
	public static final int MODE_ADD = 2;
	public static final int MODE_REMOVE = 3;
	
	public static final String NO_CONTACT = "<none>";

	// Constants that are internal to this class
	private static final int MENU_SELECT = Menu.FIRST;
	private static final int MENU_ADD = Menu.FIRST + 1;
	private static final int MENU_REMOVE = Menu.FIRST + 2;
	
	private static final int PICK_CONTACT = 1;
	
	// Class variables
	private int _currentmode;
	private String[] _savedKeys;
	private int _currentindex;
	
	/** Called when the activity is first created. */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		// Initialize class variables
		_currentmode = MODE_SELECT;
		_currentindex = -1;
		
		_savedKeys = new String[MAX_ENTRIES];
		SharedPreferences settings = getPreferences(0);
		for (int x = 0; x < MAX_ENTRIES; x++)
		{
			_savedKeys[x] = settings.getString("SavedID" + x, NO_CONTACT);
		}

		// Initialize the grid
		GridView grid = (GridView) findViewById(R.id.gridview);
		grid.setAdapter(new ContactAdapter(this));

		grid.setOnItemClickListener(new OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> parent, View v, int position, long id)
			{
				HandleClickedItem(position);
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
		
		SharedPreferences settings = getPreferences(0);
		SharedPreferences.Editor editor = settings.edit();
		for (int x = 0; x < MAX_ENTRIES; x++)
		{
			editor.putString("SavedID" + x, _savedKeys[x]);
		}
		editor.commit();
	}
	
	/** Create menu items */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		menu.add(0, MENU_SELECT, 0, "Select").setIcon(android.R.drawable.ic_menu_info_details);
		menu.add(0, MENU_ADD, 0, "Add").setIcon(android.R.drawable.ic_menu_add);
		menu.add(0, MENU_REMOVE, 0, "Remove").setIcon(android.R.drawable.ic_menu_delete);
		
		return true;
	}
	
	/** Handle menu items */
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
		case MENU_SELECT:	_currentmode = MODE_SELECT; setWindowTitle();	return true;
		case MENU_ADD:		_currentmode = MODE_ADD; setWindowTitle();		return true;
		case MENU_REMOVE:	_currentmode = MODE_REMOVE; setWindowTitle();	return true;
		}
		
		return false;
	}
	
	/** Handle return values from other activities */
	@Override
	public void onActivityResult(int reqCode, int resultCode, Intent data)
	{
		super.onActivityResult(reqCode, resultCode, data);
		
		switch(reqCode)
		{
		case PICK_CONTACT:
			if (resultCode == Activity.RESULT_OK)
			{
				int index = _currentindex;
				Uri contactdata = data.getData();
				Cursor c = managedQuery(contactdata, null, null, null, null);
				if (c.moveToFirst())
				{
					String key = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID));	
					_savedKeys[index] = key;
				}
			}
			_currentindex = -1;
			
			// Refresh the grid
			GridView grid = (GridView) findViewById(R.id.gridview);
			grid.invalidateViews();
			break;
		}
	}
	
	/** Deal with the selected item based on the current mode */
	private void HandleClickedItem(int index)
	{
		// Figure out what to do with the selected item
		if (_currentmode == MODE_SELECT)
		{
			if (hasContact(index))
			{
				Uri lookupuri = Uri.parse("content://contacts/people/" + _savedKeys[index]);
				Intent intent = new Intent(Intent.ACTION_VIEW,lookupuri);
				startActivity(intent);
			}
		}
		else if (_currentmode == MODE_ADD)
		{
			if (!hasContact(index))
			{
				// Opens a Contact list so that the user can select a Contant to add to the Grid
				_currentindex = index;
				Intent intent = new Intent(Intent.ACTION_PICK,ContactsContract.Contacts.CONTENT_URI);
				startActivityForResult(intent, PICK_CONTACT);
			}
			else
			{
				showToast("That space is already taken");
			}
		}
		else if (_currentmode == MODE_REMOVE)
		{
			_savedKeys[index] = NO_CONTACT;
		}
		
		// Refresh the grid
		GridView grid = (GridView) findViewById(R.id.gridview);
		grid.invalidateViews();
	}
	
	/** Sets the window's title based on the current mode */
	private void setWindowTitle()
	{
		String title = "Contact Grid - ";
		switch (_currentmode)
		{
		case MODE_SELECT:	title += "Select Contact"; break;
		case MODE_ADD:		title += "Add Contact"; break;
		case MODE_REMOVE:	title += "Remove Contact"; break;
		}
		
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
		String id = _savedKeys[index];
		
		if (id.compareTo(NO_CONTACT) == 0)
			return false;
		
		return true;
	}
}