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
import java.lang.Comparable;

public class GroupActions extends Activity {
	private Boolean smsAvailable;
	private SimpleContact[] groupMembers;

	/**
	 * Called when the activity is first created
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.groupactionlayout);

		checkForSMS();

		ImageButton ibtn = (ImageButton) findViewById(R.id.btnEmailGroup);
		ibtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				EmailGroup();
			}
		});

		ibtn = (ImageButton) findViewById(R.id.btnTextGroup);
		ibtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				TextGroup();
			}
		});
		ibtn.setEnabled(smsAvailable);

		Button btn = (Button) findViewById(R.id.btnSelectAll);
		btn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				SelectAll((CheckBox) v);
			}
		});

		Intent intent = this.getIntent();
		long groupId = intent.getLongExtra("GroupID", 0);
		String groupName = "";

		// Get the group name
		ContentResolver resolver = getContentResolver();
		String[] projection = { Groups._ID, Groups.TITLE };
		String constraint = Groups._ID + "=" + groupId;
		Cursor cursor = resolver.query(Groups.CONTENT_URI, projection, constraint, null, "");
		if (cursor.moveToFirst())
			groupName = cursor.getString(cursor.getColumnIndex(Groups.TITLE));
		cursor.close();

		// Get ids of all groups with this name
		// Android groups don't go across accounts, I'm trying to fake it here
		List<Long> groupIds = new ArrayList<Long>();
		constraint = Groups.TITLE + "= '" + groupName + "'";
		cursor = resolver.query(Groups.CONTENT_URI, projection, constraint, null, "");
		cursor.moveToFirst();
		do {
			groupIds.add(cursor.getLong(cursor.getColumnIndex(Groups._ID)));
		} while (cursor.moveToNext());
		cursor.close();

		// Get all of the MemberIDs of the groups with the right name
		projection = new String[] { GroupMembership.CONTACT_ID };
		constraint = GroupMembership.GROUP_ROW_ID + "=" + groupIds.get(0);
		for (int x = 1; x < groupIds.size(); x++)
			constraint += " OR " + GroupMembership.GROUP_ROW_ID + "=" + groupIds.get(x);
		Cursor memberIdCursor = resolver.query(Data.CONTENT_URI, projection, constraint, null, "");
		if (memberIdCursor.moveToFirst()) {
			int column = memberIdCursor.getColumnIndex(GroupMembership.CONTACT_ID);

			// For each ID find the actual contact
			groupMembers = new SimpleContact[memberIdCursor.getCount()];
			for (int x = 0; x < groupMembers.length; x++, memberIdCursor.moveToNext()) {
				long memberId = memberIdCursor.getLong(column);
				Uri uri = ContentUris.withAppendedId(Contacts.CONTENT_URI, memberId);
				Cursor memberCursor = resolver.query(uri, null, null, null, null);
				if (memberCursor.moveToFirst()) {
					groupMembers[x] = new SimpleContact();
					groupMembers[x].setDisplayName(memberCursor.getString(memberCursor.getColumnIndex(Contacts.DISPLAY_NAME)));
					groupMembers[x].setID(memberId);

					// Find the contact's first listed cell phone number
					projection = new String[] { Phone.CONTACT_ID, Phone.NUMBER, Phone.TYPE };
					constraint = Phone.CONTACT_ID + "=" + memberId + " AND " + Phone.TYPE + "=" + Phone.TYPE_MOBILE + " AND " + Phone.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'";
					cursor = resolver.query(Data.CONTENT_URI, projection, constraint, null, "");
					if (cursor.moveToFirst()) {
						groupMembers[x].setCellphone(cursor.getString(cursor.getColumnIndex(Phone.NUMBER)));
					}
					cursor.close();

					// Find the contact's first listed email address
					projection = new String[] { Email.CONTACT_ID, Email.DATA1 }; // Data1 = Address - for some reason Eclipse won't recognize Email.Address
					constraint = Email.CONTACT_ID + "=" + memberId + " AND " + Email.MIMETYPE + "='" + Email.CONTENT_ITEM_TYPE + "'";
					cursor = resolver.query(Data.CONTENT_URI, projection, constraint, null, "");
					if (cursor.moveToFirst()) {
						groupMembers[x].setEmail(cursor.getString(cursor.getColumnIndex(Email.DATA1)));
					}
					cursor.close();
				}
				memberCursor.close();
			}

			// Sort the list of members
			Arrays.sort(groupMembers, Collections.reverseOrder());

			// Put the names in the list
			ListView list = (ListView) findViewById(R.id.lstGroupMembers);
			list.setAdapter(new SimpleContactAdapter(this));
		}
		memberIdCursor.close();
	}

	/**
	 * Check to see if the device can handle SMS
	 */
	private void checkForSMS() {
		// Is SMS Available on this device?
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse("smsto:"));
		PackageManager packageManager = getPackageManager();
		List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
		smsAvailable = activities.size() > 0;
	}

	/**
	 * Mark all members as selected or not
	 */
	private void SelectAll(CheckBox box) {
		boolean checked = box.isChecked();
		ListView list = (ListView) findViewById(R.id.lstGroupMembers);
		int cnt = list.getChildCount();
		for (int x = 0; x < cnt; x++) {
			View v = list.getChildAt(x);
			CheckBox memberBox = (CheckBox) v.findViewById(R.id.btnCheckMember);
			memberBox.setChecked(checked);
			groupMembers[x].setChecked(checked);
		}
	}

	/**
	 * Compose an email to all selected members
	 */
	private void EmailGroup() {
		int checkedCount = 0;
		String[] addresses = new String[groupMembers.length];
		for (int x = 0; x < groupMembers.length; x++) {
			if (!groupMembers[x].getChecked())
				continue;
			addresses[x] = groupMembers[x].getEmail();
			checkedCount++;
		}
		if (checkedCount > 0) {
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("plain/text");
			intent.putExtra(Intent.EXTRA_EMAIL, addresses);
			startActivity(intent);
		}
		else {
			Toast.makeText(this, "You do not have any group members selected", Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Compose a text to all selected members
	 */
	private void TextGroup() {
		int checkedCount = 0;
		String numbers = "smsto:";
		for (int x = 0; x < groupMembers.length; x++) {
			if (!groupMembers[x].getChecked())
				continue;
			if (groupMembers[x].getCellphone().length() == 0)
				continue;

			if (x > 0)
				numbers += ","; // This may need to be a ; for some phones
			numbers += groupMembers[x].getCellphone();
			checkedCount++;
		}

		if (checkedCount > 0) {
			Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(numbers));
			startActivity(intent);
		}
		else {
			Toast.makeText(this, "You do not have any group members selected", Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Mark or unmark a member as selected
	 */
	private void CheckMember(SimpleContact contact, CheckBox box) {
		CheckBox btn = (CheckBox) findViewById(R.id.btnSelectAll);
		btn.setChecked(false);

		contact.setChecked(box.isChecked());
	}

	/**
	 * Compose an email to a single member
	 */
	private void EmailMember(SimpleContact contact) {
		String[] addresses = new String[1];
		addresses[0] = contact.getEmail();

		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("plain/text");
		intent.putExtra(Intent.EXTRA_EMAIL, addresses);
		startActivity(intent);
	}

	/**
	 * Compose a text to a single member
	 */
	private void TextMember(SimpleContact contact) {
		String number = "smsto:" + contact.getCellphone();

		Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(number));
		startActivity(intent);
	}

	/**
	 * Show the device's information for a single member
	 */
	private void DisplayMember(SimpleContact contact) {
		Uri uri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contact.getID());
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_NO_HISTORY);
		startActivity(intent);
	}

	/**
	 * A class to hold basic information about a single contact. Easier to use this than to look up this information constantly.
	 */
	private class SimpleContact implements Comparable<SimpleContact> {
		private String displayName = "";
		private String email = "";
		private String cellphone = "";
		private boolean checked = false;
		private long id = -1;

		public void setCellphone(String cellphone) {
			this.cellphone = cellphone;
		}

		public String getCellphone() {
			return cellphone;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public String getEmail() {
			return email;
		}

		public void setDisplayName(String displayname) {
			displayName = displayname;
		}

		public String getDisplayName() {
			return displayName;
		}

		public boolean getChecked() {
			return checked;
		}

		public void setChecked(boolean value) {
			checked = value;
		}

		public long getID() {
			return id;
		}

		public void setID(long id) {
			this.id = id;
		}

		@Override
		public String toString() {
			return displayName;
		}

		public int compareTo(SimpleContact other) {
			return other.displayName.compareTo(this.displayName);
		}
	}

	/**
	 * An Adapter to show group members and assign functionality to the views used
	 */
	private class SimpleContactAdapter extends BaseAdapter {
		private GroupActions context;

		public SimpleContactAdapter(Context c) {
			context = (GroupActions) c;
		}

		public int getCount() {
			return context.groupMembers.length;
		}

		public Object getItem(int arg0) {
			return null;
		}

		public long getItemId(int arg0) {
			return 0;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			int entrytoinflate = R.layout.groupactionentry;

			View v;
			if (convertView == null) // if it's not recycled, initialize some attributes
			{
				LayoutInflater li = context.getLayoutInflater();
				v = li.inflate(entrytoinflate, null);
			}
			else {
				v = convertView;
			}

			final SimpleContact contact = context.groupMembers[position];
			TextView tv = (TextView) v.findViewById(R.id.txtGroupMemberName);
			tv.setText(contact.getDisplayName());

			ImageButton ibtn = (ImageButton) v.findViewById(R.id.btnTextMember);
			ibtn.setEnabled(contact.getCellphone().length() > 0 & context.smsAvailable);
			ibtn.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					context.TextMember(contact);
				}
			});

			ibtn = (ImageButton) v.findViewById(R.id.btnEmailMember);
			ibtn.setEnabled(contact.getEmail().length() > 0);
			ibtn.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					context.EmailMember(contact);
				}
			});

			Button btn = (Button) v.findViewById(R.id.btnCheckMember);
			btn.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					context.CheckMember(contact, (CheckBox) v);
				}
			});

			v.setOnClickListener(new View.OnClickListener() // Attach this to the view so that the user can click anywhere, not just on the name
			{
				public void onClick(View v) {
					context.DisplayMember(contact);
				}
			});

			return v;
		}
	}
}
