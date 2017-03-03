package com.peterlaurence.trekadvisor.menu.tracksmanage;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.core.track.TrackImporter;

import java.io.File;

/**
 * A {@link Fragment} subclass that shows the tracks currently available for a given map, and
 * provides the ability to import new tracks.
 *
 * @author peterLaurence on 01/03/17.
 */
public class TracksManageFragment extends Fragment {
    private FrameLayout rootView;
    private RecyclerView mRecyclerView;

    private static final int TRACK_REQUEST_CODE = 1337;
    public static final String TAG = "TracksManageFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = (FrameLayout) inflater.inflate(R.layout.fragment_tracks_manage, container, false);
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_fragment_tracks_manage, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.import_tracks_id:
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                /* Search for all documents available via installed storage providers */
                intent.setType("*/*");
                startActivityForResult(intent, TRACK_REQUEST_CODE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        Log.i(TAG, "Received an \"Activity Result\"");

        /* Check if the request code is the one we are interested in */
        if (requestCode == TRACK_REQUEST_CODE && resultCode == Activity.RESULT_OK) {

            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                File file = new File(uri.getPath());
                if (!TrackImporter.isFileSupported(file)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
                    LayoutInflater inflater = getActivity().getLayoutInflater();
                    builder.setView(inflater.inflate(R.layout.track_warning, null));
                    builder.setCancelable(false)
                            .setPositiveButton(getString(R.string.ok_dialog), null);
                    AlertDialog alert = builder.create();
                    alert.show();
                }
                Log.i(TAG, "Uri: " + uri.toString());
                // TODO : process file
            }
        }
    }

    public void generateTracks(Map map) {
        mRecyclerView = new RecyclerView(this.getContext());
        mRecyclerView.setHasFixedSize(false);

        LinearLayoutManager llm = new LinearLayoutManager(this.getContext());
        mRecyclerView.setLayoutManager(llm);

        TrackAdapter trackAdapter = new TrackAdapter(map);
        mRecyclerView.setAdapter(trackAdapter);

        rootView.addView(mRecyclerView, 0);
    }
}