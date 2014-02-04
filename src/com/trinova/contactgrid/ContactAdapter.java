package com.trinova.contactgrid;

//import android.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.QuickContactBadge;
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
		boolean useactionshortcuts = context.getUseActionShortcuts();
		int imagesize = (int) (parent.getMeasuredWidth() / context.getNumColumns() * .9);

		int gridtoinflate = R.layout.gridicon;
		if (context.hasContact(position) && context.isIndexAContact(position) && useactionshortcuts)
			gridtoinflate = R.layout.gridicon_quickcontact;

		// Was the last view the right type?
		if (convertView != null)
		{
			if (!convertView.getTag().equals(gridtoinflate))
				convertView = null;
		}

		View v;
		if (convertView == null) // if it's not recycled, initialize some attributes
		{
			LayoutInflater li = context.getLayoutInflater();
			v = li.inflate(gridtoinflate, null);
			v.setTag(gridtoinflate); // Record which type of grid it is
		}
		else
		{
			v = convertView;
		}

		TextView tv = (TextView) v.findViewById(R.id.icon_text);
		ImageView iv = (ImageView) v.findViewById(R.id.icon_image);
		QuickContactBadge qcb = (QuickContactBadge) v.findViewById(R.id.icon_badge);
		if (!context.hasContact(position)) // Nothing assigned
		{
			tv.setText("");
			iv.setImageResource(R.drawable.question);
		}
		else
		{
			tv.setText(context.getGridName(position));
			if (useactionshortcuts && context.isIndexAContact(position))
			{
				iv = qcb; // Bitmaps will be assigned to the QuickContactBadge
				qcb.assignContactUri(context.getGridURI(position));
				qcb.setOnLongClickListener(new View.OnLongClickListener() {
					public boolean onLongClick(View v)
					{
						return false; // Does nothing - passes the LongClick up the hierarchy
					}
				});
			}
			Bitmap bitmap = context.getGridPhoto(position);
			if (bitmap != null)
				iv.setImageBitmap(bitmap);
			else if (context.isIndexAContact(position))
				iv.setImageResource(R.drawable.android);
			else
				iv.setImageResource(R.drawable.group);
		}
		iv.getLayoutParams().width = imagesize;
		iv.getLayoutParams().height = imagesize;
		return v;
	}
}
