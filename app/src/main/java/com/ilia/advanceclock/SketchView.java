package com.ilia.advanceclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public final class SketchView extends View {
    private static final int MAX_STROKES=250;
    private static final int MAX_POINTS_PER_STROKE=3500;

    private static final class Stroke{
        final ArrayList<float[]> points=new ArrayList<>();
        int color;float widthDp;
        Stroke(int color,float widthDp){this.color=color;this.widthDp=widthDp;}
    }

    private final Paint strokePaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ArrayList<Stroke> strokes=new ArrayList<>();
    private final ArrayList<Stroke> redo=new ArrayList<>();
    private Stroke current;
    private int activePointerId=MotionEvent.INVALID_POINTER_ID;
    private int penColor=0xFF087C77;
    private float penWidthDp=4f;
    private boolean gridVisible=true;
    private final float density;

    public SketchView(Context context){this(context,null);}
    public SketchView(Context context,AttributeSet attrs){
        super(context,attrs);
        density=getResources().getDisplayMetrics().density;
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(Math.max(1f,density*0.6f));
        gridPaint.setColor(AppSettings.themeMode(context)==AppSettings.THEME_DARK?0xFF28302E:0xFFEAF1F0);

        GradientDrawable bg=new GradientDrawable();
        bg.setColor(AppSettings.surface(context));
        bg.setCornerRadius(dp(14));
        bg.setStroke(dpInt(1),AppSettings.themeMode(context)==AppSettings.THEME_DARK?0xFF343D3A:0xFFDCE8E6);
        setBackground(bg);
        setClickable(true);
        setFocusable(true);
    }

    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        drawGrid(canvas);
        for(Stroke s:strokes)drawStroke(canvas,s);
        if(current!=null)drawStroke(canvas,current);
    }

    private void drawGrid(Canvas canvas){
        if(!gridVisible)return;
        float step=dp(24);
        for(float x=step;x<getWidth();x+=step)canvas.drawLine(x,0,x,getHeight(),gridPaint);
        for(float y=step;y<getHeight();y+=step)canvas.drawLine(0,y,getWidth(),y,gridPaint);
    }

    private void drawStroke(Canvas canvas,Stroke stroke){
        if(stroke==null||stroke.points.isEmpty())return;
        strokePaint.setColor(stroke.color);
        strokePaint.setStrokeWidth(Math.max(dp(1f),dp(stroke.widthDp)));
        if(stroke.points.size()==1){
            float[] p=stroke.points.get(0);
            canvas.drawCircle(p[0]*getWidth(),p[1]*getHeight(),strokePaint.getStrokeWidth()/2f,strokePaint);
            return;
        }
        Path path=new Path();
        float[] first=stroke.points.get(0);
        float px=first[0]*getWidth(),py=first[1]*getHeight();
        path.moveTo(px,py);
        for(int i=1;i<stroke.points.size();i++){
            float[] p=stroke.points.get(i);
            float x=p[0]*getWidth(),y=p[1]*getHeight();
            float mx=(px+x)*0.5f,my=(py+y)*0.5f;
            path.quadTo(px,py,mx,my);
            px=x;py=y;
        }
        path.lineTo(px,py);
        canvas.drawPath(path,strokePaint);
    }

    @Override public boolean onTouchEvent(MotionEvent event){
        if(getWidth()<=0||getHeight()<=0)return false;
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                activePointerId=event.getPointerId(0);
                requestParentInterception(false);
                beginStroke(event.getX(0),event.getY(0));
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                requestParentInterception(false);
                return true;
            case MotionEvent.ACTION_MOVE:{
                int index=event.findPointerIndex(activePointerId);
                if(index<0||current==null)return true;
                for(int h=0;h<event.getHistorySize();h++)addPoint(event.getHistoricalX(index,h),event.getHistoricalY(index,h));
                addPoint(event.getX(index),event.getY(index));
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_POINTER_UP:{
                int index=event.getActionIndex();
                if(event.getPointerId(index)==activePointerId){
                    addPoint(event.getX(index),event.getY(index));
                    commitStroke();
                    activePointerId=MotionEvent.INVALID_POINTER_ID;
                    requestParentInterception(true);
                }
                return true;
            }
            case MotionEvent.ACTION_UP:{
                int index=event.findPointerIndex(activePointerId);
                if(index>=0)addPoint(event.getX(index),event.getY(index));
                commitStroke();
                activePointerId=MotionEvent.INVALID_POINTER_ID;
                requestParentInterception(true);
                performClick();
                return true;
            }
            case MotionEvent.ACTION_CANCEL:
                current=null;activePointerId=MotionEvent.INVALID_POINTER_ID;
                requestParentInterception(true);invalidate();return true;
            default:return true;
        }
    }

    @Override public boolean performClick(){super.performClick();return true;}

    private void beginStroke(float x,float y){
        if(strokes.size()>=MAX_STROKES)strokes.remove(0);
        redo.clear();
        current=new Stroke(penColor,penWidthDp);
        addPointRaw(x,y);
        invalidate();
    }

    private void addPoint(float x,float y){
        if(current==null||current.points.size()>=MAX_POINTS_PER_STROKE)return;
        if(x<0||y<0||x>getWidth()||y>getHeight())return;
        if(!current.points.isEmpty()){
            float[] last=current.points.get(current.points.size()-1);
            float lx=last[0]*getWidth(),ly=last[1]*getHeight();
            float dist=(float)Math.hypot(x-lx,y-ly);
            if(dist<dp(0.7f))return;
            float maxJump=Math.max(dp(88f),Math.max(getWidth(),getHeight())*0.28f);
            if(dist>maxJump)return;
        }
        addPointRaw(x,y);
    }

    private void addPointRaw(float x,float y){
        if(current==null||current.points.size()>=MAX_POINTS_PER_STROKE)return;
        current.points.add(new float[]{clamp(x/Math.max(1f,getWidth())),clamp(y/Math.max(1f,getHeight()))});
    }

    private void commitStroke(){
        if(current!=null&&!current.points.isEmpty())strokes.add(current);
        current=null;invalidate();
    }

    private void requestParentInterception(boolean allow){
        ViewParent parent=getParent();
        while(parent!=null){parent.requestDisallowInterceptTouchEvent(!allow);parent=parent.getParent();}
    }

    public void setPenColor(int color){penColor=color;}
    public int getPenColor(){return penColor;}
    public void setPenWidthDp(float width){penWidthDp=Math.max(1f,Math.min(18f,width));}
    public void setGridVisible(boolean visible){gridVisible=visible;invalidate();}
    public boolean isGridVisible(){return gridVisible;}
    public void undo(){if(current!=null){current=null;invalidate();return;}if(strokes.isEmpty())return;redo.add(strokes.remove(strokes.size()-1));invalidate();}
    public void redo(){if(redo.isEmpty())return;strokes.add(redo.remove(redo.size()-1));invalidate();}
    public void clearSketch(){strokes.clear();redo.clear();current=null;invalidate();}

    public String serialize(){
        JSONArray all=new JSONArray();
        try{
            for(Stroke s:strokes){
                JSONObject o=new JSONObject();o.put("c",s.color);o.put("w",s.widthDp);
                JSONArray pts=new JSONArray();
                for(float[] p:s.points){JSONArray point=new JSONArray();point.put(p[0]);point.put(p[1]);pts.put(point);}
                o.put("p",pts);all.put(o);
            }
        }catch(Exception ignored){}
        return all.toString();
    }

    public void load(String raw){
        strokes.clear();redo.clear();current=null;
        try{
            JSONArray all=new JSONArray(raw==null?"[]":raw);
            for(int i=0;i<Math.min(all.length(),MAX_STROKES);i++){
                Object entry=all.opt(i);
                if(entry instanceof JSONArray){
                    Stroke s=new Stroke(AppSettings.primaryColor(getContext()),4f);
                    readPoints((JSONArray)entry,s);if(!s.points.isEmpty())strokes.add(s);continue;
                }
                if(entry instanceof JSONObject){
                    JSONObject o=(JSONObject)entry;
                    Stroke s=new Stroke(o.optInt("c",AppSettings.primaryColor(getContext())),(float)o.optDouble("w",4f));
                    JSONArray pts=o.optJSONArray("p");if(pts!=null)readPoints(pts,s);
                    if(!s.points.isEmpty())strokes.add(s);
                }
            }
        }catch(Exception ignored){}
        invalidate();
    }

    private void readPoints(JSONArray pts,Stroke s){
        for(int j=0;j<Math.min(pts.length(),MAX_POINTS_PER_STROKE);j++){
            JSONArray p=pts.optJSONArray(j);
            if(p!=null&&p.length()>=2)s.points.add(new float[]{clamp((float)p.optDouble(0,0)),clamp((float)p.optDouble(1,0))});
        }
    }

    private float dp(float v){return v*density;}
    private int dpInt(int v){return Math.round(v*density);}
    private static float clamp(float v){return Math.max(0f,Math.min(1f,v));}
}
