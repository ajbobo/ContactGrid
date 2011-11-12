package com.trinova.contactgrid;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ContactAdapter extends BaseAdapter
{
	private ContactGrid context;

	public ContactAdapter(Context c)
	{
		context = (ContactGrid) c;
	}

	public int getCount()
	{
		return context.getNumEntries();
	}

	public Object getItem(int arg0)
	{
		return null;
	}

	public long getItemId(int arg0)
	{
		return 0;
	}

	public View getView(int position, View convertView, ViewGroup parent)
	{
		View v;
		if (convertView == null) // if it's not recycled, initialize some attributes
		{
			LayoutInflater li = context.getLayoutInflater();
			v = li.inflate(R.layout.gridicon, null);
		}
		else
		{
			v = convertView;
		}

		TextView tv = (TextView) v.findViewById(R.id.icon_text);
		ImageView iv = (ImageView) v.findViewById(R.id.icon_image);
		if (!context.hasContact(position)) // Nothing assigned
		{
			tv.setText("");
			iv.setImageResource(R.drawable.question);
		}
		else
		{
			Bitmap bitmap = context.getGridPhoto(position);
			tv.setText(context.getGridName(position));
			if (bitmap != null)
				iv.setImageBitmap(bitmap);
			else
				iv.setImageResource(R.drawable.android);
		}
		return v;
	}
}
