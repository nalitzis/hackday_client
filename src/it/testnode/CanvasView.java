package it.testnode;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;

public class CanvasView extends View{
	private Paint vLinePaint;
	private Paint paint;
	private ArrayList<Point> points;

	public ArrayList<Point> getPoints() {
		return points;
	}

	public void setPoints(ArrayList<Point> points) {
		this.points = points;
		invalidate();
	}
	
	public void addPoint(Point point){
		points.add(point);
		if(points.size() > 10){
			points.remove(0);
		}
		invalidate();
	}

	public CanvasView(Context context, AttributeSet attrs) {
		super(context, attrs);
		paint = new Paint();
		vLinePaint = new Paint();
		paint.setColor(getResources().getColor(R.color.red));
		vLinePaint.setColor(getResources().getColor(R.color.blue));
		points = new ArrayList<Point>();
		// TODO Auto-generated constructor stub
	}

	public CanvasView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	
	public void onDraw(Canvas canvas){
		canvas.drawLine(200, 0, 200, 500, vLinePaint);
		for(int i=0; i<points.size() - 1; i++){
			Point p0 = points.get(i);
			Point p1 = points.get(i+1);
			canvas.drawLine(p0.x, p0.y, p1.x, p1.y, paint);
		}
		
	}
	
}
