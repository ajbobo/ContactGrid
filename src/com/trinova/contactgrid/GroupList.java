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
		
		// Get all of the Groups and information about each
		String[] projection = { ContactsContract.Groups._ID, ContactsContract.Groups.TITLE, ContactsContract.Groups.SUMMARY_COUNT };
		String constraint = ContactsContract.Groups.SUMMARY_COUNT + " > 0";
		Cursor groupcursor = managedQuery(ContactsContract.Groups.CONTENT_SUMMARY_URI, projection, constraint, null, "");

		// Put the requested Group data into the ListView
		ListView list = (ListView)findViewById(R.id.lstGroups);
		String[] visiblecolumns = { ContactsContract.Groups.TITLE, ContactsContract.Groups.SUMMARY_COUNT };
		int[] idstofill = { R.id.txtGroupName, R.id.txtGroupCount };
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(getApplicationContext(), R.layout.grouplistentry, groupcursor, visiblecolumns, idstofill);
		list.setAdapter(adapter);
	}
	
}
