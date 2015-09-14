package com.slim.slimfilemanager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.os.Environment;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.slim.slimfilemanager.fragments.SftpFragment;
import com.slim.slimfilemanager.settings.SettingsActivity;
import com.slim.slimfilemanager.settings.SettingsProvider;
import com.slim.slimfilemanager.sftp.AccountProvider;
import com.slim.slimfilemanager.sftp.SftpAccount;
import com.slim.slimfilemanager.sftp.SftpAccountActivity;
import com.slim.slimfilemanager.utils.FragmentLifecycle;
import com.slim.slimfilemanager.utils.IconCache;
import com.slim.slimfilemanager.widget.PageIndicator;
import com.slim.slimfilemanager.widget.TabPageIndicator;

public class FileManager extends ThemeActivity {

    public static final String ACTION_ADD_SFTP_SERVER = "add_sftp_server";

    public static final String SFTP = "TYPE_SFTP";
    public static final String LOCAL = "TYPE_LOCAL";

    SectionsPagerAdapter mSectionsPagerAdapter;

    BrowserFragment mFragment;

    ViewPager mViewPager;
    ListView mDrawer;
    DrawerLayout mDrawerLayout;
    ActionBarDrawerToggle mDrawerToggle;
    DrawerAdapter mDrawerAdapter;
    private FloatingActionsMenu mActionMenu;

    AccountProvider mProvider;

    int mCurrentPosition;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mProvider = new AccountProvider(this);

        SettingsProvider.getInstance(this).get()
                .registerOnSharedPreferenceChangeListener(mPreferenceListener);

        setContentView(R.layout.file_manager);

        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }

        Intent intent = new Intent();
        intent.setAction("com.slim.slimfilemanager.plugins.SERVICE");

        List<ResolveInfo> services = getPackageManager().queryIntentServices(intent, 0);
        for (ResolveInfo info : services) {
            ComponentName cmp = new ComponentName(info.serviceInfo.packageName, info.serviceInfo.name);
            Log.d("TEST", cmp.flattenToString());
        }

        // setup drawer
        mDrawer = (ListView) findViewById(R.id.drawer);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.drawer_open, R.string.drawer_close);

        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();
        mDrawerAdapter = new DrawerAdapter(this);
        mDrawer.setAdapter(mDrawerAdapter);
        mDrawerAdapter.addItem(getString(R.string.root_title), "/", LOCAL);
        mDrawerAdapter.addItem(getString(R.string.sdcard_title),
                Environment.getExternalStorageDirectory().getPath(), LOCAL);
        mDrawerAdapter.addItem(getString(R.string.downloads_title),
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS).getPath(), LOCAL);
        mDrawerAdapter.addItem(getString(R.string.dcim_title),
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DCIM).getPath(), LOCAL);
        addSftpServers();
        getExternalSDCard();
        mDrawer.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //mFragment.filesChanged(mDrawerAdapter.getPath(position));
                mSectionsPagerAdapter.addTab(mDrawerAdapter.getPath(position), mDrawerAdapter.getType(position), position);
                mDrawerLayout.closeDrawers();
            }
        });

        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position,
                                       float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                FragmentLifecycle fragmentToShow = (FragmentLifecycle)
                        mSectionsPagerAdapter.getItem(position);
                fragmentToShow.onResumeFragment();

                if (mActionMenu != null) {
                    mActionMenu.collapse();
                }

                //setCurrentlyDisplayedFragment(
                  //      (BrowserFragment) mSectionsPagerAdapter.getItem(position));

                FragmentLifecycle fragmentToHide = (FragmentLifecycle)
                        mSectionsPagerAdapter.getItem(mCurrentPosition);
                if (fragmentToHide != null) fragmentToHide.onPauseFragment();

                mCurrentPosition = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        mViewPager.setCurrentItem(SettingsProvider.getInstance(null)
                .getInt("current_tab", 0));

        mActionMenu = (FloatingActionsMenu) findViewById(R.id.float_button);
        buildActionButtons();

        PageIndicator indicator = (PageIndicator) findViewById(R.id.indicator);
        indicator.setViewPager(mViewPager);

        TabPageIndicator tabPageIndicator = (TabPageIndicator) findViewById(R.id.tab_indicator);
        tabPageIndicator.setViewPager(mViewPager);
    }

    private void buildActionButtons() {
        FloatingActionButton button = new FloatingActionButton(this);
        button.setColorNormalResId(R.color.accent);
        button.setColorPressedResId(R.color.primary_dark);
        button.setIcon(R.drawable.add_folder);
        button.setTitle(getString(R.string.create_folder));
        //button.setTag(ACTION_ADD_FOLDER);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //showDialog(ACTION_ADD_FOLDER);
                mActionMenu.collapseImmediately();
            }
        });
        mActionMenu.addButton(button);
        button = new FloatingActionButton(this);
        button.setColorNormalResId(R.color.accent);
        button.setColorPressedResId(R.color.primary_dark);
        button.setIcon(R.drawable.add_file);
        button.setTitle(getString(R.string.create_file));
        //button.setTag(ACTION_ADD_FILE);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //BrowserFragment.showDialog(ACTION_ADD_FILE);
                mActionMenu.collapseImmediately();
            }
        });
        mActionMenu.addButton(button);
        button = new FloatingActionButton(this);
        button.setColorNormalResId(R.color.accent);
        button.setColorPressedResId(R.color.primary_dark);
        button.setIcon(R.drawable.add_file);
        button.setTitle("Add SFTP Server");
        button.setTag(ACTION_ADD_SFTP_SERVER);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FileManager.this, SftpAccountActivity.class));
                mActionMenu.collapseImmediately();
            }
        });
        mActionMenu.addButton(button);
    }

    public void getExternalSDCard() {
        final String rawSecondaryStoragesStr = System.getenv("SECONDARY_STORAGE");
        Log.d("TEST", "secondary= " + rawSecondaryStoragesStr);
    }

    public void addSftpServers() {
        Log.d("TEST", "addSftpServers");
        List<SftpAccount> accounts = mProvider.getAllAccounts();
        for (SftpAccount account : accounts) {
            Log.d("TEST", "name=" + account.host);
            mDrawerAdapter.addItem(account.username + "@" + account.host, account.initialPath, SFTP);
            mDrawerAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_file_manager, menu);
        return true;
    }

    public void setCurrentlyDisplayedFragment(final BrowserFragment fragment) {
        mFragment = fragment;
    }

    public void closeDrawesrs() {
        mDrawerLayout.closeDrawers();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (id) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.add_tab:
                mSectionsPagerAdapter.addTab(Environment.getExternalStorageDirectory().getPath(), LOCAL, mSectionsPagerAdapter.getCount());
                return true;
            case R.id.close_tab:
                mSectionsPagerAdapter.removeCurrentTab();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class TabItem {
        Fragment fragment;
        String path;

        private TabItem(Fragment f, String p) {
            fragment = f;
            path = p;
        }
    }

    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        ArrayList<TabItem> mItems = new ArrayList<>();
        ArrayList<String> mTabs = new ArrayList<>();

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            addDefault();
            mTabs = SettingsProvider.getInstance(null).getListString("tabs", mTabs);
            for (String tab : mTabs) {
                mItems.add(new TabItem(
                        BrowserFragment.newInstance(tab, "default"), tab));
            }
        }

        public void addTab(String path, String type, int position) {
            if (type.equals(LOCAL)) {
                mItems.add(new TabItem(
                        BrowserFragment.newInstance(path, "default"), path));
            } else {
                mItems.add(new TabItem(
                        SftpFragment.newInstance(path, 1), path));
            }
            notifyDataSetChanged();
            mViewPager.setCurrentItem(getCount());
        }

        public void removeCurrentTab() {
            int id = mCurrentPosition;
            //mViewPager.removeView(mViewPager.getChildAt(mCurrentPosition));
            mViewPager.setCurrentItem(id - 1, true);
            mItems.get(id).fragment.onDestroy();
            mItems.remove(mItems.get(id));
            notifyDataSetChanged();
        }

        public void addDefault() {
            mTabs.add(Environment.getExternalStorageDirectory().getAbsolutePath());
            mTabs.add("/");
        }

        @Override
        public Fragment getItem(int position) {
            //setCurrentlyDisplayedFragment(mItems.get(position).fragment);
            return mItems.get(position).fragment;
        }

        public ArrayList<TabItem> getItems() {
            return mItems;
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            /*if (!TextUtils.isEmpty(mItems.get(position).fragment.getCurrentPath())) {
                File file = new File(mItems.get(position).fragment.getCurrentPath());
                if (file.exists())
                    return file.getName();
            }*/
            return "";
        }

        @Override
        public int getItemPosition(Object object) {
            return PagerAdapter.POSITION_NONE;
        }
    }

    public class DrawerAdapter extends BaseAdapter {

        public class DrawerItem {
            String title;
            String path;
            String type;
        }

        public class ViewHolder {

            TextView title;

            public ViewHolder(View view) {
                title = (TextView) view.findViewById(R.id.title);
                view.setTag(this);
            }
        }

        ArrayList<DrawerItem> mItems = new ArrayList<>();
        Context mContext;

        public DrawerAdapter(Context context) {
            mContext = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) mContext
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.item_drawer, parent, false);
                holder = new ViewHolder(convertView);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.title.setText(mItems.get(position).title);
            return convertView;
        }

        public void addItem(String title, String path, String type) {
            DrawerItem item = new DrawerItem();
            item.title = title;
            item.path = path;
            item.type = type;
            mItems.add(item);
            notifyDataSetChanged();
        }

        @Override
        public String getItem(int i) {
            return mItems.get(i).title;
        }

        public String getPath(int i) {
            return mItems.get(i).path;
        }

        public String getType(int i) {
            return mItems.get(i).type;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public int getCount() {
            return mItems.size();
        }
    }

    @Override
    public void onTrimMemory(int level) {
        if (level >= Activity.TRIM_MEMORY_MODERATE) {
            IconCache.clearCache();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SettingsProvider.getInstance(this).get()
                .unregisterOnSharedPreferenceChangeListener(mPreferenceListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        /*ArrayList<String> arrayList = new ArrayList<>();
        for (TabItem item : mSectionsPagerAdapter.getItems()) {
            String path = item.fragment.getCurrentPath();
            if (!TextUtils.isEmpty(path)) {
                arrayList.add(item.fragment.getCurrentPath());
            }
        }
        if (!arrayList.isEmpty()) {
            SettingsProvider.getInstance(null).putListString("tabs", arrayList);
        }*/
        SettingsProvider.getInstance(null).putInt("current_tab", mViewPager.getCurrentItem());
    }

    SharedPreferences.OnSharedPreferenceChangeListener mPreferenceListener
            = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            for (TabItem tabItem : mSectionsPagerAdapter.getItems()) {
                //tabItem.fragment.onPreferencesChanged();
            }
        }
    };
}
