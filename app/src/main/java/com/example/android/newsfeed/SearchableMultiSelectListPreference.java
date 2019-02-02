package com.example.android.newsfeed;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import androidx.annotation.Keep;
import androidx.annotation.RequiresApi;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import timber.log.Timber;

@SuppressWarnings("unused")
class SearchableMultiSelectListPreference extends MultiSelectListPreference {
    private EditText mEditText;
    private ArrayList<CharSequence> mOriginalObjects;
    private SparseBooleanArray mCheckedItems;
    private Object mAlertParams;
    private Field mAlertParamsCheckedItemsField;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public SearchableMultiSelectListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public SearchableMultiSelectListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SearchableMultiSelectListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SearchableMultiSelectListPreference(Context context) {
        super(context);
    }

    public EditText getEditText() {
        return mEditText;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        try {
            //noinspection JavaReflectionMemberAccess
            Field paramsField = AlertDialog.Builder.class.getDeclaredField("P");
            paramsField.setAccessible(true);
            mAlertParams = paramsField.get(builder);
            mAlertParamsCheckedItemsField = mAlertParams.getClass().getDeclaredField("mCheckedItems");
            mAlertParamsCheckedItemsField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            Timber.e(e);
        } catch (IllegalAccessException e) {
            Timber.e(e);
        }
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        final ListView listView = ((AlertDialog) getDialog()).getListView();
        final AdapterView.OnItemClickListener onItemClickListener = listView.getOnItemClickListener();
        //noinspection unchecked
        final ArrayAdapter<CharSequence> listViewAdapter = (ArrayAdapter<CharSequence>) listView.getAdapter();
        mCheckedItems = convertBooleanArrayToSparseBooleanArray(getAlertParamsCheckedItems());
        FrameLayout listViewParent = (FrameLayout) listView.getParent();
        FrameLayout.LayoutParams newViewLayoutParams = (FrameLayout.LayoutParams) listView.getLayoutParams();
        listViewParent.removeView(listView);
        LinearLayout newView = (LinearLayout) LayoutInflater.from(getContext())
                .inflate(R.layout.searchable_multi_select_list, listViewParent, false);
        newView.addView(listView, newViewLayoutParams);
        listViewParent.addView(newView, newViewLayoutParams);
        listView.isItemChecked(0);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            int newPosition = mOriginalObjects.indexOf(((TextView) view).getText());
            SparseBooleanArray tempCheckedItems = listView.getCheckedItemPositions().clone();
            mCheckedItems.put(newPosition, listView.isItemChecked(position));

            SparseBooleanArray listViewCheckedItems = listView.getCheckedItemPositions();
            listViewCheckedItems.put(mOriginalObjects.size(), false);
            setAlertParamsCheckedItems(
                    convertSparseBooleanArrayToBooleanArray(listViewCheckedItems));

            for (int i = 0; i < mOriginalObjects.size(); i++) {
                listView.setItemChecked(i, mCheckedItems.get(i));
            }

            if (null != onItemClickListener)
                onItemClickListener.onItemClick(parent, view, newPosition, id);

            for (int i = 0; i < mOriginalObjects.size(); i++) {
                listView.setItemChecked(i, tempCheckedItems.get(i));
            }
        });
        try {
            //noinspection JavaReflectionMemberAccess
            Field objectsField = ArrayAdapter.class.getDeclaredField("mObjects");
            objectsField.setAccessible(true);
            //noinspection unchecked
            mOriginalObjects = new ArrayList<>((List<CharSequence>) objectsField.get(listViewAdapter));
            objectsField.set(listViewAdapter, mOriginalObjects.clone());
        } catch (NoSuchFieldException e) {
            Timber.e(e);
        } catch (IllegalAccessException e) {
            Timber.e(e);
        }
        mEditText = newView.findViewById(R.id.search_edit_text);
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s == null)
                    s = "";
                if (mCheckedItems.size() == 0)
                    mCheckedItems = listView.getCheckedItemPositions().clone();
                listViewAdapter.clear();
                for (int i = 0; i < mOriginalObjects.size(); i++) {
                    String curItem = (String) mOriginalObjects.get(i);
                    assert curItem != null;
                    if (curItem.toLowerCase().contains(s.toString().toLowerCase().trim())) {
                        listViewAdapter.add(curItem);
                        int position = listViewAdapter.getCount() - 1;
                        listView.setItemChecked(position, mCheckedItems.get(i));
                    }
                }
                SparseBooleanArray listViewCheckedItems = listView.getCheckedItemPositions();
                listViewCheckedItems.put(mOriginalObjects.size(), false);
                setAlertParamsCheckedItems(
                        convertSparseBooleanArrayToBooleanArray(listView.getCheckedItemPositions()));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        mEditText.setOnClickListener(v -> mEditText.post(() -> {
            Objects.requireNonNull(getDialog().getWindow()).clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }));
    }

    private boolean[] convertSparseBooleanArrayToBooleanArray(SparseBooleanArray sparseBooleanArray) {
        boolean[] result = new boolean[sparseBooleanArray.keyAt(sparseBooleanArray.size() - 1) + 1];
        for (int i = 0; i < sparseBooleanArray.size(); i++) {
            result[sparseBooleanArray.keyAt(i)] = sparseBooleanArray.valueAt(i);
        }
        return result;
    }

    private boolean[] getAlertParamsCheckedItems() {
        try {
            return (boolean[]) mAlertParamsCheckedItemsField.get(mAlertParams);
        } catch (IllegalAccessException e) {
            Timber.e(e);
        }
        return null;
    }

    private void setAlertParamsCheckedItems(boolean[] checkedItems) {
        try {
            mAlertParamsCheckedItemsField.set(mAlertParams, checkedItems);
        } catch (IllegalAccessException e) {
            Timber.e(e);
        }
    }

    private SparseBooleanArray convertBooleanArrayToSparseBooleanArray(boolean[] booleanArray) {
        if (booleanArray == null)
            return null;
        SparseBooleanArray sparseBooleanArray = new SparseBooleanArray(booleanArray.length);
        for (int i = 0; i < booleanArray.length; i++) {
            sparseBooleanArray.append(i, booleanArray[i]);
        }
        return sparseBooleanArray;
    }

    @SuppressWarnings({"unused", "SameReturnValue"})
    @Keep
    protected boolean needInputMethod() {
        return true;
    }

    public void add(String[][] arrayToAdd) {
        String[] entryValues = arrayToAdd[0];
        String[] entries = arrayToAdd[1];
        int previousSize = getEntries() != null ? getEntries().length : 0;
        setEntryValues(ArrayUtils.addAll(getEntryValues(), entryValues));
        setEntries(ArrayUtils.addAll(getEntries(), entries));
        if (previousSize == 0)
            previousSize = getEntries().length;
        if (getDialog() != null && getDialog().isShowing()) {
            mOriginalObjects.addAll(Arrays.asList(entries));
            mCheckedItems.put(entryValues.length - 1, false);
            for (int i = 0; i < entryValues.length; i++)
                if (getValues().contains(entryValues[i])) {
                    mCheckedItems.put(previousSize + i, true);
                }
            mEditText.setText(mEditText.getText());
        }
    }

    public void clear() {
        setEntryValues(new CharSequence[0]);
        setEntries(new CharSequence[0]);
        if (getDialog() != null && getDialog().isShowing()) {
            mOriginalObjects.clear();
            mEditText.setText(mEditText.getText());
        }
    }
}
