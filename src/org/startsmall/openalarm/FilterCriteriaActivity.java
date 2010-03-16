package org.startsmall.openalarm;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.util.SparseBooleanArray;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import java.util.*;
import java.text.*;

public class FilterCriteriaActivity extends ListActivity
                                   implements AdapterView.OnItemSelectedListener,
                                              View.OnClickListener {
    private static final String TAG = "FilterCriteriaActivity";

    public static final int FILTER_BY_ACTION = 0;
    public static final int FILTER_BY_REPEAT_DAYS = 1;

    public static final String EXTRA_KEY_FILTER_BY_REPEAT_DAYS_OPERATOR = "operator";

    private Spinner mSpinner;
    private Button mOkButton;
    private LinearLayout mOperatorLayout;
    private RadioGroup mOperators;

    private HashMap<String, HandlerInfo> mHandlerInfoMap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.filter_criteria_activity);

        mHandlerInfoMap = HandlerInfo.getMap(this);

        initLayout();
    }

    private void initLayout() {
        mSpinner = (Spinner)findViewById(R.id.filters);
        mOkButton = (Button)findViewById(R.id.ok);
        mOperatorLayout = (LinearLayout)findViewById(R.id.operator_layout);
        mOperators = (RadioGroup)mOperatorLayout.findViewById(R.id.operators);

        ArrayAdapter<CharSequence> adapter =
            ArrayAdapter.createFromResource(
                this, R.array.filter_by, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);
        mSpinner.setOnItemSelectedListener(this);

        mOkButton.setOnClickListener(this);
        Button cancelButton = (Button)findViewById(R.id.cancel);
        cancelButton.setOnClickListener(
            new View.OnClickListener() {
                public void onClick(View v) {
                    finish();
                }
            });
        mOperators.check(R.id.and);
    }

    public void onClick(View view) {
        ListView listView = getListView();
        int searchBy = mSpinner.getSelectedItemPosition();
        switch (searchBy) {
        case FILTER_BY_ACTION: {
            int position = listView.getCheckedItemPosition();
            if (position > -1) {
                HandlerInfo handlerInfo = (HandlerInfo)listView.getItemAtPosition(position);
                setResult(RESULT_FIRST_USER + FILTER_BY_ACTION,
                          handlerInfo.getIntent());
                finish();
            }
            break;
        }

        case FILTER_BY_REPEAT_DAYS:
            int filterCode = 0;
            for (int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
                if (listView.isItemChecked(i - 1)) {
                    filterCode = Alarms.RepeatWeekdays.set(filterCode, i, true);
                }
            }

            if (filterCode > 0) {
                Intent data = new Intent();
                data.putExtra(AlarmColumns.REPEAT_DAYS, filterCode);
                data.putExtra(EXTRA_KEY_FILTER_BY_REPEAT_DAYS_OPERATOR,
                              mOperators.getCheckedRadioButtonId());
                setResult(RESULT_FIRST_USER + FILTER_BY_REPEAT_DAYS, data);
                finish();
            }
            break;
        }
    }

    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        ListView listView = getListView();

        if (position == FILTER_BY_ACTION) {
            Collection<HandlerInfo> handlerInfos = mHandlerInfoMap.values();
            if (handlerInfos.size() > 0) {
                Object[] handlerInfoArray = handlerInfos.toArray();
                Arrays.sort(handlerInfoArray);
                ArrayAdapter<Object> actionAdapter =
                    new ArrayAdapter<Object>(this,
                                             android.R.layout.simple_list_item_single_choice,
                                             handlerInfoArray);
                listView.setAdapter(actionAdapter);
                listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            }
            showOperatorView(false);
        } else if (position == FILTER_BY_REPEAT_DAYS) {
            DateFormatSymbols dateFormatSymbols =
                ((SimpleDateFormat)DateFormat.getDateInstance(DateFormat.MEDIUM)).getDateFormatSymbols();
            CharSequence[] weekdays = new CharSequence[7];
            System.arraycopy(dateFormatSymbols.getWeekdays(),
                             Calendar.SUNDAY,
                             weekdays,
                             0,
                             7);
            ArrayAdapter<Object> repeatDaysAdapter =
                new ArrayAdapter<Object>(this,
                                         android.R.layout.simple_list_item_multiple_choice,
                                         weekdays);
            listView.setAdapter(repeatDaysAdapter);
            listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            showOperatorView(true);
        }
    }
    public void onNothingSelected(AdapterView<?> parent) {}

    private void showOperatorView(boolean show) {
        if (show) {
            mOperatorLayout.setVisibility(View.VISIBLE);
        } else {
            mOperatorLayout.setVisibility(View.GONE);
        }
    }

}
