package com.ilia.advanceclock;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public final class NoForgetEditorActivity extends Activity {
    private static final int REQ_FILES=520;
    private static final int REQ_APP=521;
    private static final int REQ_SOUND=522;

    private final Calendar due=Calendar.getInstance();

    private long noteId=-1L;
    private long createdAt;

    private EditText title;
    private EditText body;
    private Spinner priority;
    private Switch alarmEnabled;
    private LinearLayout alarmControls;
    private Button dateButton;
    private Button timeButton;
    private Button repeatButton;
    private Button remindersButton;
    private Button deleteButton;
    private SketchView sketch;
    private Spinner penSize;
    private ImageButton gridToggle;
    private Switch vibrate;
    private Switch fullscreenUnlocked;
    private Switch fullscreenLocked;
    private Button soundButton;
    private LinearLayout attachmentsView;

    private Button sketchAction;
    private Button filesAction;
    private Button appSiteAction;
    private AlertDialog filesDialog;
    private AlertDialog appSiteDialog;
    private LinearLayout fileModalList;
    private LinearLayout appSiteModalList;

    private final ArrayList<NoteAttachment> attachments=new ArrayList<>();
    private String soundUri="";

    private int dateCalendarType;
    private int recurrenceMode=RecurrenceUtils.NONE;
    private int intervalDays=1;
    private String customDatesJson="[]";
    private int reminderMode=AlarmReminderUtils.MODE_NONE;
    private String reminderMinutesJson="[]";

    private boolean createMode;
    private boolean reopenFilesDialog;
    private boolean reopenAppSiteDialog;

    @Override protected void onCreate(Bundle savedInstanceState){
        AppSettings.applyTheme(this);
        createMode=getIntent().getBooleanExtra("modalCreate",false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_noforget_editor);
        applySystemBarInsets(findViewById(R.id.note_editor_root));

        title=findViewById(R.id.note_title);
        body=findViewById(R.id.note_body);
        priority=findViewById(R.id.note_priority);
        alarmEnabled=findViewById(R.id.note_alarm_enabled);
        alarmControls=findViewById(R.id.note_alarm_controls);
        dateButton=findViewById(R.id.note_date);
        timeButton=findViewById(R.id.note_time);
        repeatButton=findViewById(R.id.note_repeat);
        deleteButton=findViewById(R.id.delete_note);
        sketch=findViewById(R.id.note_sketch);
        penSize=findViewById(R.id.editor_pen_size);
        gridToggle=findViewById(R.id.editor_grid_toggle);
        vibrate=findViewById(R.id.note_vibrate);
        fullscreenUnlocked=findViewById(R.id.note_fullscreen_unlocked);
        fullscreenLocked=findViewById(R.id.note_fullscreen_locked);
        soundButton=findViewById(R.id.note_sound);
        attachmentsView=findViewById(R.id.note_attachments);
        dateCalendarType=AppSettings.defaultCalendar(this);

        setupPriority();

        due.add(Calendar.HOUR_OF_DAY,1);
        due.set(Calendar.SECOND,0);
        due.set(Calendar.MILLISECOND,0);
        createdAt=System.currentTimeMillis();

        noteId=getIntent().getLongExtra("noteId",-1L);
        if(noteId>=0)loadExisting();
        else deleteButton.setVisibility(View.GONE);

        installReminderButton();
        installAttachmentActionButtons();
        setupAlarmControls();
        setupHiddenSketchControls();

        findViewById(R.id.note_add_files).setOnClickListener(v->pickFiles());
        findViewById(R.id.note_add_app).setOnClickListener(v->pickApp());
        soundButton.setOnClickListener(v->startActivityForResult(
                new Intent(this,SoundPickerActivity.class),REQ_SOUND));
        findViewById(R.id.save_note).setOnClickListener(v->save());
        deleteButton.setOnClickListener(v->delete());
        findViewById(R.id.cancel_note).setOnClickListener(v->finish());

        updateDueButtons();
        updateReminderButton();
        alarmControls.setVisibility(
                alarmEnabled.isChecked()?View.VISIBLE:View.GONE);

        if(noteId<0){
            vibrate.setChecked(true);
            fullscreenUnlocked.setChecked(true);
            fullscreenLocked.setChecked(true);
            soundButton.setText(
                    "صدای هشدار • "+SoundLibrary.name(this,soundUri));
        }

        renderAttachments();
        updateActionButtons();
    }

    private void setupPriority(){
        String[] labels=PriorityUtils.labels();
        String[] priorityValues=new String[labels.length];
        for(int i=0;i<labels.length;i++){
            priorityValues[i]="اهمیت "+labels[i];
        }
        ArrayAdapter<String> adapter=new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                priorityValues);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        priority.setAdapter(adapter);
        priority.setSelection(PriorityUtils.MEDIUM);
    }

    private void setupAlarmControls(){
        alarmEnabled.setOnCheckedChangeListener((button,checked)->
                alarmControls.setVisibility(
                        checked?View.VISIBLE:View.GONE));

        dateButton.setOnClickListener(v->CalendarPickerDialog.showDate(
                this,
                due.getTimeInMillis(),
                dateCalendarType,
                (picked,type)->{
                    dateCalendarType=type;
                    Calendar source=Calendar.getInstance();
                    source.setTimeInMillis(picked);
                    due.set(Calendar.YEAR,source.get(Calendar.YEAR));
                    due.set(Calendar.MONTH,source.get(Calendar.MONTH));
                    due.set(Calendar.DAY_OF_MONTH,source.get(Calendar.DAY_OF_MONTH));
                    updateDueButtons();
                }));

        timeButton.setOnClickListener(v->new TimePickerDialog(
                this,
                (view,hour,minute)->{
                    due.set(Calendar.HOUR_OF_DAY,hour);
                    due.set(Calendar.MINUTE,minute);
                    due.set(Calendar.SECOND,0);
                    due.set(Calendar.MILLISECOND,0);
                    updateDueButtons();
                },
                due.get(Calendar.HOUR_OF_DAY),
                due.get(Calendar.MINUTE),
                true).show());

        repeatButton.setOnClickListener(v->RecurrenceDialog.show(
                this,
                due.getTimeInMillis(),
                recurrenceMode,
                intervalDays,
                customDatesJson,
                (mode,interval,dates)->{
                    recurrenceMode=mode;
                    intervalDays=interval;
                    customDatesJson=dates;
                    repeatButton.setText(
                            RecurrenceUtils.summary(
                                    mode,
                                    interval,
                                    dates));
                }));

        remindersButton.setOnClickListener(v->AlarmReminderDialog.show(
                this,
                reminderMode,
                reminderMinutesJson,
                (mode,json)->{
                    reminderMode=mode;
                    reminderMinutesJson=json;
                    updateReminderButton();
                }));
    }

    private void installReminderButton(){
        remindersButton=new Button(this);
        remindersButton.setAllCaps(false);
        remindersButton.setTextColor(AppSettings.primaryColor(this));
        remindersButton.setBackgroundResource(R.drawable.bg_field);
        remindersButton.setTextSize(13);

        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52));
        params.topMargin=dp(8);

        int index=alarmControls.indexOfChild(repeatButton);
        alarmControls.addView(
                remindersButton,
                Math.max(0,index+1),
                params);
    }

    private void installAttachmentActionButtons(){
        LinearLayout parent=(LinearLayout)priority.getParent();

        LinearLayout row=new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        sketchAction=makeActionButton("ترسیم");
        filesAction=makeActionButton("افزودن فایل");
        appSiteAction=makeActionButton("افزودن اپ یا سایت");

        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(
                0,
                dp(54),
                1f);

        row.addView(sketchAction,new LinearLayout.LayoutParams(bp));
        Space s1=new Space(this);
        row.addView(s1,new LinearLayout.LayoutParams(dp(6),1));
        row.addView(filesAction,new LinearLayout.LayoutParams(bp));
        Space s2=new Space(this);
        row.addView(s2,new LinearLayout.LayoutParams(dp(6),1));
        row.addView(appSiteAction,new LinearLayout.LayoutParams(bp));

        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(54));
        rp.topMargin=dp(8);
        int priorityIndex=parent.indexOfChild(priority);
        parent.addView(row,priorityIndex+1,rp);

        sketchAction.setOnClickListener(v->showSketchModal());
        filesAction.setOnClickListener(v->showFilesModal());
        appSiteAction.setOnClickListener(v->showAppSiteModal());

        View addFiles=findViewById(R.id.note_add_files);
        if(addFiles!=null&&addFiles.getParent() instanceof View){
            View oldRow=(View)addFiles.getParent();
            if(oldRow.getParent() instanceof View){
                ((View)oldRow.getParent()).setVisibility(View.GONE);
            }
        }

        if(sketch.getParent() instanceof View){
            ((View)sketch.getParent()).setVisibility(View.GONE);
        }
    }

    private Button makeActionButton(String text){
        Button button=new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(10.5f);
        button.setTextColor(AppSettings.primaryColor(this));
        button.setBackgroundResource(R.drawable.bg_soft_button);
        button.setPadding(dp(3),0,dp(3),0);
        return button;
    }

    private void setupHiddenSketchControls(){
        findViewById(R.id.editor_undo).setOnClickListener(v->sketch.undo());
        findViewById(R.id.clear_sketch).setOnClickListener(v->sketch.clearSketch());
        findViewById(R.id.editor_redo).setOnClickListener(v->sketch.redo());
        findViewById(R.id.editor_palette).setOnClickListener(v->
                PaletteDialog.show(
                        this,
                        sketch.getPenColor(),
                        sketch::setPenColor));

        penSize.setAdapter(new PenSizeAdapter(this));
        penSize.setSelection(1);
        penSize.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener(){
                    @Override public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id){
                        float[] widths={2f,4f,7f,10f};
                        sketch.setPenWidthDp(
                                widths[Math.max(
                                        0,
                                        Math.min(
                                                widths.length-1,
                                                position))]);
                    }

                    @Override public void onNothingSelected(
                            AdapterView<?> parent){}
                });

        final boolean[] gridVisible={true};
        sketch.setGridVisible(true);
        gridToggle.setImageResource(R.drawable.ic_grid);
        gridToggle.setSelected(false);
        gridToggle.setOnClickListener(v->{
            gridVisible[0]=!gridVisible[0];
            sketch.setGridVisible(gridVisible[0]);
            gridToggle.setSelected(!gridVisible[0]);
            gridToggle.setImageResource(
                    gridVisible[0]
                            ?R.drawable.ic_grid
                            :R.drawable.ic_grid_off);
        });
    }

    private void showSketchModal(){
        LinearLayout root=NoteModalStyler.content(
                this,
                "ترسیم",
                "با ابزارهای زیر طراحی کنید؛ نتیجه فقط با زدن «ذخیره ترسیم» ثبت می‌شود.");

        LinearLayout toolbar=new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);

        Button undo=smallToolButton("↶");
        Button redo=smallToolButton("↷");
        Button clear=smallToolButton("پاک");
        Button palette=smallToolButton("رنگ");
        Button grid=smallToolButton("گرید");

        Spinner size=new Spinner(this);
        size.setAdapter(new PenSizeAdapter(this));
        size.setSelection(1);

        toolbar.addView(undo,toolParams());
        toolbar.addView(redo,toolParams());
        toolbar.addView(clear,toolParams());
        toolbar.addView(palette,toolParams());
        toolbar.addView(grid,toolParams());
        toolbar.addView(size,toolParams());

        SketchView canvas=new SketchView(this);
        canvas.load(sketch.serialize());
        canvas.setPenColor(sketch.getPenColor());
        canvas.setGridVisible(true);

        size.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener(){
                    @Override public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id){
                        float[] widths={2f,4f,7f,10f};
                        canvas.setPenWidthDp(
                                widths[Math.max(
                                        0,
                                        Math.min(
                                                widths.length-1,
                                                position))]);
                    }

                    @Override public void onNothingSelected(
                            AdapterView<?> parent){}
                });

        undo.setOnClickListener(v->canvas.undo());
        redo.setOnClickListener(v->canvas.redo());
        clear.setOnClickListener(v->canvas.clearSketch());
        palette.setOnClickListener(v->PaletteDialog.show(
                this,
                canvas.getPenColor(),
                canvas::setPenColor));
        grid.setOnClickListener(v->{
            boolean next=!canvas.isGridVisible();
            canvas.setGridVisible(next);
            grid.setText(next?"گرید":"بدون گرید");
        });

        root.addView(
                toolbar,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(52)));
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(420));
        cp.topMargin=dp(8);
        root.addView(canvas,cp);

        AlertDialog dialog=new AlertDialog.Builder(this)
                .setView(root)
                .setNegativeButton("انصراف",null)
                .setPositiveButton(
                        "ذخیره ترسیم",
                        (d,which)->{
                            sketch.load(canvas.serialize());
                            updateActionButtons();
                        })
                .create();
        NoteModalStyler.show(dialog);
    }

    private Button smallToolButton(String text){
        Button button=new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(11);
        button.setMinWidth(0);
        button.setPadding(0,0,0,0);
        button.setTextColor(AppSettings.primaryColor(this));
        button.setBackgroundResource(R.drawable.bg_soft_button);
        return button;
    }

    private LinearLayout.LayoutParams toolParams(){
        return new LinearLayout.LayoutParams(
                0,
                dp(48),
                1f);
    }

    private void showFilesModal(){
        if(filesDialog!=null&&filesDialog.isShowing()){
            filesDialog.dismiss();
        }

        LinearLayout root=NoteModalStyler.content(
                this,
                "فایل‌ها و تصاویر",
                "هر تعداد فایل یا تصویر اضافه کنید؛ برای باز کردن هر مورد روی آن بزنید.");

        Button add=makeModalPrimaryButton("+ افزودن فایل یا تصویر");
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(50));
        ap.topMargin=dp(8);
        root.addView(add,ap);

        ScrollView scroll=new ScrollView(this);
        fileModalList=new LinearLayout(this);
        fileModalList.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(fileModalList);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(310));
        sp.topMargin=dp(8);
        root.addView(scroll,sp);

        filesDialog=new AlertDialog.Builder(this)
                .setView(root)
                .setNegativeButton("بستن",null)
                .create();

        add.setOnClickListener(v->{
            reopenFilesDialog=true;
            filesDialog.dismiss();
            pickFiles();
        });

        renderFileModalList();
        NoteModalStyler.show(filesDialog);
    }

    private void renderFileModalList(){
        if(fileModalList==null)return;
        fileModalList.removeAllViews();

        int count=0;
        for(NoteAttachment item:new ArrayList<>(attachments)){
            if(!NoteAttachment.KIND_FILE.equals(item.kind))continue;
            count++;
            addAttachmentRow(fileModalList,item);
        }

        if(count==0){
            TextView empty=new TextView(this);
            empty.setText("هنوز فایلی اضافه نشده است.");
            empty.setGravity(Gravity.CENTER);
            empty.setTextColor(AppSettings.textPrimary(this));
            empty.setPadding(0,dp(24),0,dp(24));
            fileModalList.addView(empty);
        }
    }

    private void showAppSiteModal(){
        if(appSiteDialog!=null&&appSiteDialog.isShowing()){
            appSiteDialog.dismiss();
        }

        LinearLayout root=NoteModalStyler.content(
                this,
                "افزودن اپ یا سایت",
                "یک برنامه نصب‌شده انتخاب کنید یا نشانی کامل سایت را وارد کنید.");

        Button chooseApp=makeModalPrimaryButton("انتخاب برنامه نصب‌شده");
        root.addView(
                chooseApp,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(50)));

        EditText url=new EditText(this);
        url.setHint("لینک سایت، مثلاً https://example.com");
        url.setSingleLine(true);
        url.setInputType(
                InputType.TYPE_CLASS_TEXT
                        |InputType.TYPE_TEXT_VARIATION_URI);
        url.setBackgroundResource(R.drawable.bg_field);
        url.setTextColor(AppSettings.textPrimary(this));
        url.setHintTextColor(AppSettings.textSecondary(this));
        url.setPadding(dp(12),0,dp(12),0);
        LinearLayout.LayoutParams up=new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52));
        up.topMargin=dp(8);
        root.addView(url,up);

        Button addSite=makeModalPrimaryButton("+ افزودن سایت");
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48));
        bp.topMargin=dp(6);
        root.addView(addSite,bp);

        ScrollView scroll=new ScrollView(this);
        appSiteModalList=new LinearLayout(this);
        appSiteModalList.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(appSiteModalList);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(280));
        sp.topMargin=dp(8);
        root.addView(scroll,sp);

        appSiteDialog=new AlertDialog.Builder(this)
                .setView(root)
                .setNegativeButton("بستن",null)
                .create();

        chooseApp.setOnClickListener(v->{
            reopenAppSiteDialog=true;
            appSiteDialog.dismiss();
            pickApp();
        });

        addSite.setOnClickListener(v->{
            String value=url.getText().toString().trim();
            if(value.isEmpty()){
                LogoToast.makeText(
                        this,
                        "لینک سایت را وارد کنید",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            NoteAttachment attachment=NoteAttachment.url(value);
            if(attachment.value.length()<9){
                LogoToast.makeText(
                        this,
                        "لینک معتبر وارد کنید",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            attachments.add(attachment);
            url.setText("");
            renderAppSiteModalList();
            renderAttachments();
            updateActionButtons();
        });

        renderAppSiteModalList();
        NoteModalStyler.show(appSiteDialog);
    }

    private void renderAppSiteModalList(){
        if(appSiteModalList==null)return;
        appSiteModalList.removeAllViews();

        int count=0;
        for(NoteAttachment item:new ArrayList<>(attachments)){
            if(!NoteAttachment.KIND_APP.equals(item.kind)
                    &&!NoteAttachment.KIND_URL.equals(item.kind))continue;
            count++;
            addAttachmentRow(appSiteModalList,item);
        }

        if(count==0){
            TextView empty=new TextView(this);
            empty.setText("هنوز برنامه یا سایتی اضافه نشده است.");
            empty.setGravity(Gravity.CENTER);
            empty.setTextColor(AppSettings.textPrimary(this));
            empty.setPadding(0,dp(24),0,dp(24));
            appSiteModalList.addView(empty);
        }
    }

    private Button makeModalPrimaryButton(String text){
        Button button=new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(0xFFFFFFFF);
        button.setTextSize(13);
        button.setBackgroundResource(R.drawable.bg_orange_button);
        return button;
    }

    private void addAttachmentRow(
            LinearLayout parent,
            NoteAttachment item){
        LinearLayout row=new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        Button open=new Button(this);
        String prefix=NoteAttachment.KIND_URL.equals(item.kind)?"🌐 ":"";
        open.setText(prefix+item.name);
        open.setAllCaps(false);
        open.setGravity(Gravity.CENTER_VERTICAL);
        open.setBackgroundResource(R.drawable.bg_field);

        if(NoteAttachment.KIND_APP.equals(item.kind)){
            try{
                Drawable icon=getPackageManager()
                        .getApplicationIcon(item.value);
                icon.setBounds(0,0,dp(36),dp(36));
                open.setCompoundDrawables(icon,null,null,null);
                open.setCompoundDrawablePadding(dp(8));
            }catch(Exception ignored){}
        }

        open.setOnClickListener(v->item.open(this));

        Button remove=new Button(this);
        remove.setText("حذف");
        remove.setAllCaps(false);
        remove.setTextColor(0xFFC44C4C);
        remove.setBackgroundResource(R.drawable.bg_soft_button);
        remove.setOnClickListener(v->{
            attachments.remove(item);
            renderFileModalList();
            renderAppSiteModalList();
            renderAttachments();
            updateActionButtons();
        });

        row.addView(
                open,
                new LinearLayout.LayoutParams(
                        0,
                        dp(58),
                        1f));
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(
                dp(72),
                dp(58));
        rp.setMarginStart(dp(6));
        row.addView(remove,rp);

        LinearLayout.LayoutParams line=new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58));
        line.bottomMargin=dp(6);
        parent.addView(row,line);
    }

    private void updateActionButtons(){
        if(sketchAction==null)return;

        boolean hasSketch=!"[]".equals(sketch.serialize());
        int files=0;
        int targets=0;
        for(NoteAttachment item:attachments){
            if(NoteAttachment.KIND_FILE.equals(item.kind))files++;
            else if(NoteAttachment.KIND_APP.equals(item.kind)
                    ||NoteAttachment.KIND_URL.equals(item.kind))targets++;
        }

        sketchAction.setText(hasSketch?"ترسیم ✓":"ترسیم");
        filesAction.setText(
                files==0
                        ?"افزودن فایل"
                        :"افزودن فایل ("+CalendarUtils.fa(
                                Integer.toString(files))+")");
        appSiteAction.setText(
                targets==0
                        ?"افزودن اپ یا سایت"
                        :"اپ یا سایت ("+CalendarUtils.fa(
                                Integer.toString(targets))+")");
    }

    private void applySystemBarInsets(View root){
        int start=root.getPaddingStart();
        int top=root.getPaddingTop();
        int end=root.getPaddingEnd();
        int bottom=root.getPaddingBottom();
        root.setOnApplyWindowInsetsListener((view,insets)->{
            view.setPaddingRelative(
                    start,
                    top+insets.getSystemWindowInsetTop(),
                    end,
                    bottom+insets.getSystemWindowInsetBottom());
            return insets;
        });
        root.requestApplyInsets();
    }

    @Override public void finish(){
        super.finish();
        if(createMode){
            overridePendingTransition(
                    R.anim.editor_stay,
                    R.anim.editor_exit);
        }
    }

    private void loadExisting(){
        NoForgetItem item=new NoForgetStore(this).find(noteId);
        if(item==null){
            finish();
            return;
        }

        title.setText(item.title);
        body.setText(item.body);
        priority.setSelection(PriorityUtils.clamp(item.priority));
        alarmEnabled.setChecked(item.reminderEnabled&&item.hasDue);
        createdAt=item.createdAt;

        if(item.hasDue&&item.dueAtMillis>0){
            due.setTimeInMillis(item.dueAtMillis);
        }

        recurrenceMode=item.recurrenceMode;
        intervalDays=item.intervalDays;
        customDatesJson=item.customDatesJson;
        repeatButton.setText(
                RecurrenceUtils.summary(
                        recurrenceMode,
                        intervalDays,
                        customDatesJson));

        reminderMode=item.reminderMode;
        reminderMinutesJson=item.reminderMinutesJson;

        sketch.load(item.sketchJson);
        attachments.clear();
        attachments.addAll(
                NoteAttachment.parse(item.attachmentsJson));
        vibrate.setChecked(item.vibrate);
        fullscreenUnlocked.setChecked(item.fullscreenUnlocked);
        fullscreenLocked.setChecked(item.fullscreenLocked);
        soundUri=item.soundUri;
        soundButton.setText(
                "صدای هشدار • "+SoundLibrary.name(this,soundUri));
    }

    private void updateDueButtons(){
        dateButton.setText(
                CalendarUtils.formatDate(
                        due.getTimeInMillis(),
                        dateCalendarType));

        String time=String.format(
                Locale.US,
                "%02d:%02d",
                due.get(Calendar.HOUR_OF_DAY),
                due.get(Calendar.MINUTE));
        timeButton.setText(CalendarUtils.fa(time));
    }

    private void updateReminderButton(){
        if(remindersButton==null)return;
        remindersButton.setText(
                "یادآوری قبل از موعد\n"
                        +AlarmReminderUtils.summary(
                                reminderMode,
                                reminderMinutesJson));
    }

    private void save(){
        String titleText=title.getText().toString().trim();
        String bodyText=body.getText().toString().trim();
        String sketchJson=sketch.serialize();

        if(titleText.isEmpty()
                &&bodyText.isEmpty()
                &&"[]".equals(sketchJson)
                &&attachments.isEmpty()){
            LogoToast.makeText(
                    this,
                    "یک متن، ترسیم، فایل، برنامه یا سایت اضافه کنید",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        boolean enabled=alarmEnabled.isChecked();
        long dueAt=enabled?due.getTimeInMillis():0L;

        if(enabled&&dueAt<=System.currentTimeMillis()){
            LogoToast.makeText(
                    this,
                    "آلارم یادداشت را نمی‌توان برای گذشته تنظیم کرد",
                    Toast.LENGTH_LONG).show();
            return;
        }

        long id=noteId>=0?noteId:System.currentTimeMillis();
        NoForgetItem item=new NoForgetItem(
                id,
                titleText,
                bodyText,
                sketchJson,
                priority.getSelectedItemPosition(),
                enabled,
                dueAt,
                enabled,
                createdAt,
                recurrenceMode,
                intervalDays,
                customDatesJson);

        item.attachmentsJson=NoteAttachment.encode(attachments);
        item.vibrate=vibrate.isChecked();
        item.soundUri=soundUri;
        item.fullscreenUnlocked=fullscreenUnlocked.isChecked();
        item.fullscreenLocked=fullscreenLocked.isChecked();
        item.reminderMode=reminderMode;
        item.reminderMinutesJson=reminderMinutesJson;

        new NoForgetStore(this).save(item);

        boolean scheduled=true;
        if(enabled){
            scheduled=NoForgetScheduler.schedule(this,item);
        }else{
            NoForgetScheduler.cancel(this,id);
        }

        NoForgetWidgetProvider.updateAll(this);

        if(!scheduled){
            LogoToast.makeText(
                    this,
                    "یادداشت ذخیره شد؛ برای آلارم، دسترسی آلارم دقیق لازم است.",
                    Toast.LENGTH_LONG).show();
        }
        finish();
    }

    private void pickFiles(){
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("*/*")
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true)
                .addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                |Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent,REQ_FILES);
    }

    private void pickApp(){
        Intent base=new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER);
        startActivityForResult(
                new Intent(Intent.ACTION_PICK_ACTIVITY)
                        .putExtra(Intent.EXTRA_INTENT,base)
                        .putExtra(Intent.EXTRA_TITLE,"انتخاب برنامه"),
                REQ_APP);
    }

    @Override protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data){
        super.onActivityResult(requestCode,resultCode,data);

        if(resultCode!=RESULT_OK||data==null){
            if(requestCode==REQ_FILES)reopenFilesDialog=false;
            if(requestCode==REQ_APP)reopenAppSiteDialog=false;
            return;
        }

        if(requestCode==REQ_SOUND){
            soundUri=data.getStringExtra(SoundPickerActivity.EXTRA_URI);
            soundButton.setText(
                    "صدای هشدار • "
                            +data.getStringExtra(
                                    SoundPickerActivity.EXTRA_NAME));
            return;
        }

        if(requestCode==REQ_APP&&data.getComponent()!=null){
            attachments.add(
                    NoteAttachment.app(
                            this,
                            data.getComponent().getPackageName()));
            renderAttachments();
            updateActionButtons();
            if(reopenAppSiteDialog){
                reopenAppSiteDialog=false;
                showAppSiteModal();
            }
            return;
        }

        if(requestCode==REQ_FILES){
            ClipData clips=data.getClipData();
            if(clips!=null){
                for(int i=0;i<clips.getItemCount();i++){
                    addFile(clips.getItemAt(i).getUri());
                }
            }else if(data.getData()!=null){
                addFile(data.getData());
            }
            renderAttachments();
            updateActionButtons();
            if(reopenFilesDialog){
                reopenFilesDialog=false;
                showFilesModal();
            }
        }
    }

    private void addFile(Uri uri){
        if(uri==null)return;
        try{
            getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }catch(Exception ignored){}
        String mime=getContentResolver().getType(uri);
        attachments.add(
                new NoteAttachment(
                        NoteAttachment.KIND_FILE,
                        uri.toString(),
                        fileName(uri),
                        mime==null?"*/*":mime));
    }

    private String fileName(Uri uri){
        try(Cursor c=getContentResolver().query(
                uri,
                new String[]{OpenableColumns.DISPLAY_NAME},
                null,
                null,
                null)){
            if(c!=null&&c.moveToFirst())return c.getString(0);
        }catch(Exception ignored){}
        return "فایل";
    }

    private void renderAttachments(){
        attachmentsView.removeAllViews();
        for(NoteAttachment item:new ArrayList<>(attachments)){
            Button button=new Button(this);
            button.setText(item.name);
            button.setAllCaps(false);
            button.setOnClickListener(v->item.open(this));
            if(NoteAttachment.KIND_APP.equals(item.kind)){
                try{
                    button.setCompoundDrawablesWithIntrinsicBounds(
                            getPackageManager().getApplicationIcon(item.value),
                            null,
                            null,
                            null);
                }catch(Exception ignored){}
            }
            button.setOnLongClickListener(v->{
                attachments.remove(item);
                renderAttachments();
                updateActionButtons();
                return true;
            });
            LinearLayout.LayoutParams p=
                    new LinearLayout.LayoutParams(dp(140),dp(88));
            p.setMarginEnd(dp(7));
            attachmentsView.addView(button,p);
        }
    }

    private int dp(int value){
        return Math.round(
                value*getResources().getDisplayMetrics().density);
    }

    private void delete(){
        NoForgetScheduler.cancel(this,noteId);
        new NoForgetStore(this).delete(noteId);
        NoForgetWidgetProvider.updateAll(this);
        finish();
    }
}
