package ajbobo.contactgrid;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class ContactAdapter extends BaseAdapter
{
	private ContactGrid context;

	public ContactAdapter(Context c)
	{
		context = (ContactGrid)c;
	}

	public int getCount()
	{
		return ContactGrid.MAX_ENTRIES;
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
		// TODO: Somehow this also needs to show the Contacts' names
		//       Do I need to create my own View object that has an ImageView and a TextView together?
		ImageView imageView;
		if (convertView == null) // if it's not recycled, initialize some attributes
		{ 
			imageView = new ImageView(context);
			imageView.setLayoutParams(new GridView.LayoutParams(85, 85));
			imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
			imageView.setPadding(8, 8, 8, 8);
		} 
		else
		{
			imageView = (ImageView) convertView;
		}

		if (!context.hasContact(position)) // Nothing assigned
		{
			imageView.setImageResource(R.drawable.question);
		}
		else
		{
			Bitmap bitmap = context.getGridPhoto(position);
			if (bitmap != null)
				imageView.setImageBitmap(bitmap); // The contact has a photo - use it
			else
				imageView.setImageResource(R.drawable.android); // The contact does not have a photo - use a place-holder image
			
		}
		return imageView;
	}

}
