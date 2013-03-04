package com.trinova.contactgrid;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.database.*;
import android.net.*;
import android.os.*;
import android.provider.ContactsContract.*;
import android.provider.ContactsContract.CommonDataKinds.*;
import android.widget.*;
import android.view.*;
import java.util.*;

public class GroupActions extends Activity
{
	private Boolean _smsavailable;
	private SimpleContact[] _groupmembers;

	/** Called when the activity is first created */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.groupactionlayout);
		
		checkForSMS();
		
		Button btn =(Button)findViewById(R.id.btnEmailGroup);
		btn.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v) { EmailGroup(); }
		});
		
		btn = (Button)findViewById(R.id.btnTextGroup);
		btn.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v) { TextGroup(); }
		});
		btn.setEnabled(_smsavailable);

		Intent intent = this.getIntent();
		long groupid = intent.getLongExtra("GroupID", 0);

		// Get all of the MemberIDs of the group
		String[] projection = { GroupMembership.CONTACT_ID };
		String constraint = GroupMembership.GROUP_ROW_ID + "=" + groupid;
		Cursor memberidcursor = managedQuery(Data.CONTENT_URI, projection, constraint, null, "");

		if (memberidcursor.moveToFirst())
		{
			ContentResolver resolver = getContentResolver();
			int column = memberidcursor.getColumnIndex(GroupMembership.CONTACT_ID);

			// For each ID find the actual contact
			_groupmembers = new SimpleContact[memberidcursor.getCount()];
			for (int x = 0; x < _groupmembers.length; x++, memberidcursor.moveToNext())
			{
				long memberid = memberidcursor.getLong(column);
				Uri uri = ContentUris.withAppendedId(Contacts.CONTENT_URI, memberid);
				Cursor membercursor = resolver.query(uri, null, null, null, null);
				if (membercursor.moveToFirst())
				{
					_groupmembers[x] = new SimpleContact();
					_groupmembers[x].setDisplayName(membercursor.getString(membercursor.getColumnIndex(Contacts.DISPLAY_NAME)));
				
					projection = new String [] {Phone.CONTACT_ID, Phone.NUMBER, Phone.TYPE };
					constraint = Phone.CONTACT_ID + "=" + memberid + " AND " +
					             Phone.TYPE + "=" + Phone.TYPE_MOBILE + " AND " +
					             Phone.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE +"'";
					Cursor cursor = resolver.query(Data.CONTENT_URI, projection, constraint, null, "");
					if (cursor.moveToFirst())
					{
						_groupmembers[x].setCellphone(cursor.getString(cursor.getColumnIndex(Phone.NUMBER)));
					}
					cursor.close();
					
					projection = new String [] {Email.CONTACT_ID, Email.DATA1 }; // Data1 = Address - for some reason Eclipse won't recognize Email.Address
					constraint = Email.CONTACT_ID + "=" + memberid + " AND " +
					             Email.MIMETYPE + "='" + Email.CONTENT_ITEM_TYPE +"'";
					cursor = resolver.query(Data.CONTENT_URI, projection, constraint, null, "");
					if (cursor.moveToFirst())
					{
						_groupmembers[x].setEmail(cursor.getString(cursor.getColumnIndex(Email.DATA1)));
					}
					cursor.close();
				}
				membercursor.close();
			}

			// Put the names in the list
			ListView list = (ListView)findViewById(R.id.lstGroupMembers);
			ArrayAdapter<SimpleContact> adapter = new ArrayAdapter<SimpleContact>(this, R.layout.groupactionentry, R.id.txtGroupMemberName, _groupmembers);
			list.setAdapter(adapter);
		}
	}

	private void checkForSMS()
	{
		// Is SMS Available on this device?
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setType("vnd.android-dir/mms-sms");
		PackageManager packageManager = getPackageManager();
		List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
		_smsavailable = activities.size() > 0;
	}
	
	private void EmailGroup()
	{
		Toast.makeText(this, "Emailing Group", Toast.LENGTH_SHORT).show();
	}
	
	private void TextGroup()
	{
		Toast.makeText(this, "Texting Group", Toast.LENGTH_SHORT).show();
	}
	
	private class SimpleContact
	{
		private String _displayname = "";
		private String _email = "";
		private String _cellphone = "";

		public void setCellphone(String cellphone)
		{
			_cellphone = cellphone;
		}

		public String getCellphone()
		{
			return _cellphone;
		}

		public void setEmail(String email)
		{
			_email = email;
		}

		public String getEmail()
		{
			return _email;
		}

		public void setDisplayName(String displayname)
		{
			_displayname = displayname;
		}

		public String getDisplayName()
		{
			return _displayname;
		}
		
		@Override
		public String toString()
		{
			return _displayname + " | " + _cellphone + " | " + _email;
		}
	}
}
