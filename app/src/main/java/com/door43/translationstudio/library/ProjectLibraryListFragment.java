package com.door43.translationstudio.library;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.dialogs.ModelItemAdapter;
import com.door43.translationstudio.library.temp.LibraryTempData;
import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.tasks.GetAvailableProjectsTask;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.threads.ManagedTask;
import com.door43.util.threads.ThreadManager;

/**
 * A list fragment representing a list of Projects. This fragment
 * also supports tablet devices by allowing list items to be given an
 * 'activated' state upon selection. This helps indicate which item is
 * currently being viewed in a {@link ProjectLibraryDetailFragment}.
 * <p/>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class ProjectLibraryListFragment extends ListFragment {

    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";
    private State mState = State.NOTHING;
    private int mTaskId;
    private ModelItemAdapter mAdapter;

    private enum State {
        NOTHING,
        DOWNLOADING_PROJECTS_CATALOG,
        BROWSING_PROJECTS_CATALOG,
        DOWNLOADING_LANGUAGES_CATALOG,
        BROWSING_LANGUAGES_CATALOG,
        DOWNLOADING_PROJECT_SOURCE,
        RELOADING_SELECTED_PROJECT
    }
    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks = sDummyCallbacks;

    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = ListView.INVALID_POSITION;

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        public void onItemSelected(int index);
    }

    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(int index) {
        }
    };

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ProjectLibraryListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LibraryTempData.setAvailableProjects(new Project[]{});
        mAdapter = new ModelItemAdapter(AppContext.context(), LibraryTempData.getProjects(), false);
        setListAdapter(mAdapter);

        preparProjectList();
    }

    private void preparProjectList() {
        if(mTaskId != -1) {
            // check progress
            GetAvailableProjectsTask task = (GetAvailableProjectsTask) ThreadManager.getTask(mTaskId);

            if (task.isFinished()) {
                LibraryTempData.setAvailableProjects(task.getProjects().toArray(new Project[task.getProjects().size()]));
                notifyDataChanged();

                ThreadManager.clearTask(mTaskId);
                mTaskId = -1;
            } else {
                // TODO: display loading status
            }
        } else {
            // start process
            GetAvailableProjectsTask task = new GetAvailableProjectsTask();
            task.setOnFinishedListener(new ManagedTask.OnFinishedListener() {
                @Override
                public void onFinished(ManagedTask task) {
                    // TODO: hide loading status
                    GetAvailableProjectsTask myTask = ((GetAvailableProjectsTask)task);
                    LibraryTempData.setAvailableProjects(myTask.getProjects().toArray(new Project[myTask.getProjects().size()]));
                    notifyDataChanged();

                    ThreadManager.clearTask(mTaskId);
                    mTaskId = -1;
                }
            });
            mTaskId = ThreadManager.addTask(task);

            // TODO: display loading status
        }
    }

    /**
     * Let the adapter know the data has changed
     */
    private void notifyDataChanged() {
        Handler handle = new Handler(Looper.getMainLooper());
        handle.post(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        mCallbacks.onItemSelected(position);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be
     * given the 'activated' state when touched.
     */
    public void setActivateOnItemClick(boolean activateOnItemClick) {
        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
        getListView().setChoiceMode(activateOnItemClick
                ? ListView.CHOICE_MODE_SINGLE
                : ListView.CHOICE_MODE_NONE);
    }

    private void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            getListView().setItemChecked(mActivatedPosition, false);
        } else {
            getListView().setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }
}