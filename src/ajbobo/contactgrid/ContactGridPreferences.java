package ajbobo.contactgrid;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class ContactGridPreferences extends PreferenceActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);
	}
}
