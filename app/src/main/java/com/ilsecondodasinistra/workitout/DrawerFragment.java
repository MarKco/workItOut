package com.ilsecondodasinistra.workitout;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class DrawerFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Retrieving the currently selected item number
        int position = getArguments().getInt("position");

        String[] elements = getResources().getStringArray(R.array.drawer_items);

        // Creating view correspoding to the fragment
        View v = inflater.inflate(R.layout.activity_drawer_fragment, container, false);

        // Getting reference to the TextView of the Fragment
        TextView tv = (TextView) v.findViewById(R.id.drawertv);

        // Setting currently selected river name in the TextView
        tv.setText(elements[position]);

        // Updating the action bar title
        getActivity().getActionBar().setTitle(elements[position]);

        return v;
    }

}
