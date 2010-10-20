package com.pivotallabs.tracker;

import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import com.pivotallabs.R;
import com.pivotallabs.RobolectricTestRunner;
import com.pivotallabs.TestResponses;
import com.pivotallabs.api.ApiRequest;
import com.pivotallabs.api.TestApiGateway;
import com.xtremelabs.robolectric.fakes.TestMenu;
import com.xtremelabs.robolectric.fakes.TestMenuItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.pivotallabs.TestHelper.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class RecentActivityActivityTest {

    private RecentActivityActivity activity;
    private TrackerAuthenticator trackerAuthenticator;
    private TestApiGateway apiGateway;
    private ListView activityListView;

    @Before
    public void setUp() throws Exception {
        signIn();
        createActivity();

        trackerAuthenticator = new TrackerAuthenticator(apiGateway, activity);
        activityListView = (ListView) activity.findViewById(R.id.recent_activity_list);
    }

    @Test
    public void shouldShowTheSignInDialogIfNotCurrentlySignedIn() throws Exception {
        signOutAndReCreateActivity();

        assertThat(trackerAuthenticator.isAuthenticated(), equalTo(false));
        assertThat(activity.signInDialog.isShowing(), equalTo(true));
    }

    @Test
    public void shouldNotShowTheSignInDialogIfSignedIn() {
        assertThat(activity.signInDialog, nullValue());
    }

    @Test
    public void shouldRetrieveRecentActivityUponSuccessfulSignIn() {
        signOutAndReCreateActivity();
        signInThroughDialog();

        ApiRequest expectedRequest = new RecentActivityRequest("c93f12c");
        assertThat(apiGateway.getLatestRequest(), equalTo(expectedRequest));
    }

    @Test
    public void onCreate_shouldRetrieveRecentActivityWhenSignedIn() {
        assertThat(apiGateway.getLatestRequest(),
                equalTo((ApiRequest) new RecentActivityRequest("c93f12c")));
    }

    @Test
    public void shouldPopulateViewWithRetrievedRecentActivity() throws Exception {
        apiGateway.simulateResponse(200, TestResponses.RECENT_ACTIVITY);
        yieldToUiThread();
        String firstRowText = proxyFor((TextView) activityListView.getChildAt(0)).innerText();
        assertThat(firstRowText, equalTo("I changed the 'request' for squidward. \"Add 'Buyout'\""));
    }

    @Test
    public void shouldShowProgressBarWhileRequestIsOutstanding() throws Exception {
        View footerView = proxyFor(activityListView).footerViews.get(0);

        assertThat(footerView.getVisibility(), equalTo(View.VISIBLE));

        apiGateway.simulateResponse(200, TestResponses.RECENT_ACTIVITY);
        yieldToUiThread();

        assertThat(footerView.getVisibility(), equalTo(View.GONE));
    }

    @Test
    public void shouldFinishWhenSignInDialogIsDismissedWithoutSuccessfulSignIn() {
        signOutAndReCreateActivity();

        activity.signInDialog.cancel();

        assertThat(proxyFor(activity).finishWasCalled, equalTo(true));
    }

    @Test
    public void shouldSignOutWhenTheSignOutButtonIsClicked() throws Exception {
        TestMenu menu = new TestMenu();
        menu.add("garbage that should be cleared upon onPrepareOptionsMenu");

        activity.onPrepareOptionsMenu(menu);

        TestMenuItem signOutMenuItem = (TestMenuItem) menu.getItem(0);
        assertThat(signOutMenuItem.isEnabled(), equalTo(true));
        assertThat(signOutMenuItem.getTitle().toString(), equalTo("Sign Out"));

        signOutMenuItem.simulateClick();
        assertThat(trackerAuthenticator.isAuthenticated(), equalTo(false));
        assertThat(proxyFor(activity).finishWasCalled, equalTo(true));
    }

    @Test
    public void signOutButtonShouldBeDisabledWhenNotSignedIn() throws Exception {
        trackerAuthenticator.signOut();
        TestMenu menu = new TestMenu();

        activity.onPrepareOptionsMenu(menu);

        TestMenuItem signOutMenuItem = (TestMenuItem) menu.getItem(0);
        assertThat(signOutMenuItem.isEnabled(), equalTo(false));
        assertThat(signOutMenuItem.getTitle().toString(), equalTo("Sign Out"));
    }

    private void signOutAndReCreateActivity() {
        trackerAuthenticator.signOut();
        createActivity();
    }

    private void signInThroughDialog() {
        assertThat(activity.signInDialog.isShowing(), equalTo(true));
        ((EditText) activity.signInDialog.findViewById(R.id.username)).setText("user");
        ((EditText) activity.signInDialog.findViewById(R.id.password)).setText("pass");
        activity.signInDialog.findViewById(R.id.sign_in_button).performClick();

        apiGateway.simulateResponse(200, TestResponses.AUTH_SUCCESS);
        assertThat(activity.signInDialog.isShowing(), equalTo(false));
    }

    private void createActivity() {
        apiGateway = new TestApiGateway();
        activity = new RecentActivityActivity();
        activity.apiGateway = apiGateway;
        activity.onCreate(null);
    }
}
