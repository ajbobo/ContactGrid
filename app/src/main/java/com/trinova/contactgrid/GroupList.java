package com.trinova.contactgrid;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract.Groups;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.*;
import java.lang.Comparable;

public class GroupList extends Activity {
	private List<GroupInfo> groupList;

	/**
	 * Called when the activity is first created
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.grouplistlayout);

		// Get all of the Groups and information about each
		groupList = new ArrayList<GroupInfo>();
		String[] projection = { Groups._ID, Groups.TITLE, Groups.SUMMARY_COUNT };
		String constraint = Groups.SUMMARY_COUNT + " > 0";
		Cursor groupCursor = managedQuery(Groups.CONTENT_SUMMARY_URI, projection, constraint, null, "");
		int groupCount = groupCursor.getCount();
		if (groupCount == 0) {
			Intent data = new Intent();
			data.setData(null);
			setResult(Activity.RESULT_CANCELED, data); // Return the Group's ID to the calling activity
			finish();
		}
		else {
			groupCursor.moveToFirst();
			do {
				String name = groupCursor.getString(groupCursor.getColumnIndex(Groups.TITLE));
				GroupInfo group = null;
				for (int x = 0; x < groupList.size() && group == null; x++) {
					if (name.equals(groupList.get(x).getName()))
						group = groupList.get(x);
				}
				if (group == null) // haven't seen this group yet
				{
					group = new GroupInfo();
					group.setName(name);
					group.setID(groupCursor.getInt(groupCursor.getColumnIndex(Groups._ID)));
					group.setMemberCnt(groupCursor.getInt(groupCursor.getColumnIndex(Groups.SUMMARY_COUNT)));
					groupList.add(group);
				}
				else {
					group.addMembers(groupCursor.getInt(groupCursor.getColumnIndex(Groups.SUMMARY_COUNT)));
				}
			} while (groupCursor.moveToNext());
			groupCursor.close();

			Collections.sort(groupList, Collections.reverseOrder());

			// Put the requested Group data into the ListView
			ListView list = (ListView) findViewById(R.id.lstGroups);
			GroupInfoAdapter adapter = new GroupInfoAdapter(this);
			list.setAdapter(adapter);
		}
	}

	private void HandleClick(GroupInfo group) {
		int id = group.getID();

		Intent data = new Intent();
		data.setData(ContentUris.withAppendedId(Groups.CONTENT_URI, id));
		setResult(Activity.RESULT_OK, data); // Return the Group's ID to the calling activity
		finish();
	}

	private class GroupInfo implements Comparable<GroupInfo> {
		private String name;
		private int memberCnt;
		private int id;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getMemberCnt() {
			return memberCnt;
		}

		public void setMemberCnt(int cnt) {
			memberCnt = cnt;
		}

		public void addMembers(int newMembers) {
			memberCnt += newMembers;
		}

		public int getID() {
			return id;
		}

		public void setID(int id) {
			this.id = id;
		}

		@Override
		public String toString() {
			return name;
		}

		public int compareTo(GroupInfo other) {
			return other.name.compareTo(this.name);
		}
	}

	/**
	 * An Adapter to show group members and assign functionality to the views used
	 */
	private class GroupInfoAdapter extends BaseAdapter {
		private GroupList context;

		public GroupInfoAdapter(Context c) {
			context = (GroupList) c;
		}

		public int getCount() {
			return context.groupList.size();
		}

		public Object getItem(int arg0) {
			return null;
		}

		public long getItemId(int arg0) {
			return 0;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			int entryToInflate = R.layout.grouplistentry;

			View v;
			if (convertView == null) // if it's not recycled, initialize some attributes
			{
				LayoutInflater li = context.getLayoutInflater();
				v = li.inflate(entryToInflate, null);
			}
			else {
				v = convertView;
			}

			final GroupInfo group = context.groupList.get(position);
			TextView tv = (TextView) v.findViewById(R.id.txtGroupName);
			tv.setText(group.getName());

			tv = (TextView) v.findViewById(R.id.txtGroupCount);
			tv.setText("(" + group.getMemberCnt() + ")");

			v.setOnClickListener(new View.OnClickListener() // Attach this to the view so that the user can click anywhere - not just on the name
			{
				public void onClick(View v) {
					context.HandleClick(group);
				}
			});

			return v;
		}
	}
}
