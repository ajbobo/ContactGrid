package ajbobo.contactgrid;

import ajbobo.contactgrid.ContactGrid;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
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
	
	// Constants that are internal to this class
	private static final int MENU_SELECT = Menu.FIRST;
	private static final int MENU_ADD = Menu.FIRST + 1;
	private static final int MENU_REMOVE = Menu.FIRST + 2;
	
	// Class variables
	private int _currentmode = 0;
	private int[] _savedIDs;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		// Initialize class variables
		_currentmode = MODE_SELECT;
		
		_savedIDs = new int[MAX_ENTRIES];
		SharedPreferences settings = getPreferences(0);
		for (int x = 0; x < MAX_ENTRIES; x++)
		{
			_savedIDs[x] = settings.getInt("SavedID" + x, -1);
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
			editor.putInt("SavedID" + x, _savedIDs[x]);
		}
		editor.commit();
	}
	
	/** Create menu items */
	public boolean onCreateOptionsMenu(Menu menu)
	{
		menu.add(0, MENU_SELECT, 0, "Select").setIcon(android.R.drawable.ic_menu_info_details);
		menu.add(0, MENU_ADD, 0, "Add").setIcon(android.R.drawable.ic_menu_add);
		menu.add(0, MENU_REMOVE, 0, "Remove").setIcon(android.R.drawable.ic_menu_delete);
		
		return true;
	}
	
	/** Handle menu items */
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
	
	/** Return the current mode */
	public int currentMode()
	{
		return _currentmode;
	}
	
	/** Return the specified saved id */
	public int getID(int id)
	{
		return _savedIDs[id];
	}
	
	/** Deal with the selected item based on the current mode */
	private void HandleClickedItem(int index)
	{
		// Figure out what to do with the selected item
		if (_currentmode == MODE_SELECT)
		{
			if (_savedIDs[index] != -1)
				Toast.makeText(ContactGrid.this, "Selected: " + index, Toast.LENGTH_SHORT).show(); // TODO: Should open the Contact's Details
		}
		else if (_currentmode == MODE_ADD)
		{
			if (_savedIDs[index] == -1)
				_savedIDs[index] = index; // TODO: Should open a Contact list so that the user can select a Contant to add to the Grid
			else
				Toast.makeText(ContactGrid.this, "That space is already taken", Toast.LENGTH_SHORT).show();
		}
		else if (_currentmode == MODE_REMOVE)
		{
			_savedIDs[index] = -1;
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
}