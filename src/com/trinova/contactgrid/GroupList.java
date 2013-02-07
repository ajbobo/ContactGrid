package com.trinova.contactgrid;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.OnItemClickListener;


public class GroupList extends Activity
{
	private Cursor _groupcursor;
	
	/** Called when the activity is first created */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.grouplistlayout);
		
		// Get all of the Groups and information about each
		String[] projection = { ContactsContract.Groups._ID, ContactsContract.Groups.TITLE, ContactsContract.Groups.SUMMARY_COUNT };
		String constraint = ContactsContract.Groups.SUMMARY_COUNT + " > 0";
		_groupcursor = managedQuery(ContactsContract.Groups.CONTENT_SUMMARY_URI, projection, constraint, null, "");

		// Put the requested Group data into the ListView
		ListView list = (ListView)findViewById(R.id.lstGroups);
		String[] visiblecolumns = { ContactsContract.Groups.TITLE, ContactsContract.Groups.SUMMARY_COUNT };
		int[] idstofill = { R.id.txtGroupName, R.id.txtGroupCount };
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(getApplicationContext(), R.layout.grouplistentry, _groupcursor, visiblecolumns, idstofill);
		list.setAdapter(adapter);
		list.setOnItemClickListener(new OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> parent, View v, int position, long id)
			{
				HandleClick(position);
			}
		});
	}
	
	private void HandleClick(int position)
	{
		_groupcursor.moveToPosition(position);
		int id = _groupcursor.getInt(0);
		
		Intent data = new Intent();
		data.setData(ContentUris.withAppendedId(ContactsContract.Groups.CONTENT_URI, id));
		setResult(Activity.RESULT_OK, data); // Return the Group's ID to the calling activity
		finish();
	}
	
}
