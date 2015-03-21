package com.ilsecondodasinistra.workitout;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;

public class DrawerLayoutHelper {

    private WorkItOutMain activity;

    private ActionBar actionBar;

    private DrawerLayout drawerLayout;

    private ListView drawerListView;

    private ActionBarDrawerToggle drawerToggle;

    private int defaultActionBarDisplay;

    private int defaultNavigationMode;

    private CharSequence defaultTitle;

    public DrawerLayoutHelper(final WorkItOutMain activity, final ActionBar actionBar) {

        this.activity = activity;
        this.actionBar = actionBar;

        /*
         * dati di default dell'action bar
         */
        defaultNavigationMode = actionBar.getNavigationMode();
        defaultTitle = actionBar.getTitle();
        defaultActionBarDisplay = actionBar.getDisplayOptions();

        drawerLayout = (DrawerLayout)activity.findViewById(R.id.main_layout);
//        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        drawerListView = (ListView)activity.findViewById(R.id.left_drawer);

        /*
         * qui imposti l'adapter per la lista
         */
        // Creating an ArrayAdapter to add items to the listview mDrawerList
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
            activity.getBaseContext(),
            R.layout.drawer_list_item,
            activity.getResources().getStringArray(R.array.drawer_items)
        );
        
        drawerListView.setAdapter(adapter);
        
        // Set the list's click listener
        drawerListView.setOnItemClickListener(new DrawerItemClickListener());
        
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        drawerToggle = new ActionBarDrawerToggle(activity, drawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {

            public void onDrawerClosed(View view) {
            	
                restoreActionBar();
            }
            
            public void onDrawerOpened(View drawerView) {
                actionBar.setTitle(activity.getString(R.string.title_activity_settings));
                actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            }
        };
        drawerLayout.setDrawerListener(drawerToggle);
    }

	public ActionBarDrawerToggle getDrawerToggle() {
        return drawerToggle;
    }

    public DrawerLayout getDrawerLayout() {
        return drawerLayout;
    }
    
    public void toggle() {
        if (drawerLayout.isDrawerOpen(drawerListView)) {
            drawerLayout.closeDrawer(drawerListView);
        }
        else {
            drawerLayout.openDrawer(drawerListView);
        }
    }

    private void restoreActionBar() {
        actionBar.setCustomView(null);
//        actionBar.setDisplayOptions(defaul<tActionBarDisplay); //Removes the three dots on the left. I like them!
        actionBar.setHomeButtonEnabled(true);
        actionBar.setNavigationMode(defaultNavigationMode);
        actionBar.setTitle(defaultTitle);
    }
    
    public class DrawerItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    /** Swaps fragments in the main content view */
    private void selectItem(int position) {
        // Create a new fragment and specify the planet to show based on position
//        Fragment fragment = new PlanetFragment();
        Bundle args = new Bundle();
//        args.putInt(PlanetFragment.ARG_PLANET_NUMBER, position);
//        fragment.setArguments(args);
        
        switch (position) {
		case 0:
			final CustomTimeDialog dialog = new CustomTimeDialog(this.activity);
			dialog.setContentView(R.layout.time_preference);
			dialog.show();
			break;
		case 1:
			activity.clearAllInput();
			break;
		case 2:
			this.activity.sendMail();
			break;
		case 3:
            this.activity.startActivity(new Intent(this.activity, PrefActivity.class));
            break;
        case 4:
            Intent aboutIntent = new Intent(this.activity, AboutActivity.class);
            this.activity.startActivity(aboutIntent);
            break;
        default:
			break;
		}

        // Highlight the selected item, update the title, and close the drawer
        drawerListView.setItemChecked(position, true);
        drawerLayout.closeDrawer(drawerListView);
        
    }
}
