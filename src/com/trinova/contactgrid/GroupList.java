package com.trinova.contactgrid;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;


public class GroupList extends Activity
{
	/** Called when the activity is first created */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.grouplistlayout);
		
		// Get all of the Groups and put them into the listview
		String[] projection = { ContactsContract.Groups._ID, ContactsContract.Groups.TITLE, ContactsContract.Groups.SUMMARY_COUNT };
		String[] visiblecolumns = { ContactsContract.Groups._ID, ContactsContract.Groups.TITLE };
		int[] idstofill = { R.id.txtGroupName };
		
		Cursor groupcursor = getContentResolver().query(ContactsContract.Groups.CONTENT_SUMMARY_URI, projection, null, null, "ASC");
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(getApplicationContext(), R.layout.grouplistentry, groupcursor, visiblecolumns, idstofill);
		
		ListView list = (ListView)findViewById(R.id.lstGroups);
		list.setAdapter(adapter);
	}
	
}
