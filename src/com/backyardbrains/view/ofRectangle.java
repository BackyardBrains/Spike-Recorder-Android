package com.backyardbrains.view;

public class ofRectangle {
	public float x = 0;
	public float y = 0;
	public float width = 0;
	public float height = 0;
	
	public static final int	ASPECT_RATIO_IGNORE				= 0;
	public static final int	ASPECT_RATIO_KEEP				= 1;
	public static final int	ASPECT_RATIO_KEEP_BY_EXPANDING	= 2;
	public static final int	ALIGN_VERT_IGNORE				= 3;
	public static final int	ALIGN_VERT_TOP					= 4;
	public static final int	ALIGN_VERT_BOTTOM				= 5;
	public static final int	ALIGN_VERT_CENTER				= 6;
	public static final int	ALIGN_HORZ_IGNORE				= 7;
	public static final int	ALIGN_HORZ_LEFT					= 8;
	public static final int	ALIGN_HORZ_RIGHT				= 9;
	public static final int	ALIGN_HORZ_CENTER				= 10;
	public static final int	RECTMODE_CORNER					= 11;
	public static final int	RECTMODE_CENTER					= 12;
	public static final int	SCALEMODE_FIT					= 13;
	public static final int	SCALEMODE_FILL					= 14;
	public static final int	SCALEMODE_CENTER				= 15;
	public static final int	SCALEMODE_STRETCH_TO_FILL		= 16;
	
	public class ofPoint{
		ofPoint(float x, float y){
			this.x = x;
			this.y = y;
		}
		float x;
		float y;
	}
	
	//----------------------------------------------------------
	public ofRectangle(){
	    set(0,0,0,0);
	}
	//----------------------------------------------------------
	public ofRectangle(float px, float py, float w, float h) {
		set(px,py,w,h);
	}

	//----------------------------------------------------------
	public ofRectangle(ofRectangle rect){
	    set(rect);
	}

	//----------------------------------------------------------
	public void set(float px, float py, float w, float h) {
		x		= px;
		y		= py;
		width	= w;
		height	= h;
	}

	//----------------------------------------------------------
	public void set( ofRectangle rect){
	    set(rect.x, rect.y, rect.width, rect.height);
	}

	//----------------------------------------------------------
	public void setX(float px) {
	    x = px;
	}

	//----------------------------------------------------------
	public void setY(float py) {
	    y = py;
	}

	//----------------------------------------------------------
	public void setWidth(float w) {
	    width = w;
	}

	//----------------------------------------------------------
	public void setHeight(float h) {
	    height = h;
	}
	//----------------------------------------------------------
	public void setPosition(float px, float py) {
	    x = px;
	    y = py;
	}
	//----------------------------------------------------------
	public void setSize(float w, float h) {
		width = w;
		height = h;
	}

	//----------------------------------------------------------
	public void setFromCenter(float px, float py, float w, float h) {
	    set(px - w*0.5f, py - h*0.5f, w, h);
	}

	//----------------------------------------------------------
	public void translate(float dx, float dy) {
	    translateX(dx);
	    translateY(dy);
	}

	//----------------------------------------------------------
	public void translateX(float dx) {
	    x += dx;
	}

	//----------------------------------------------------------
	public void translateY(float dy) {
	    y += dy;
	}

	//----------------------------------------------------------
	public void scale(float s) {
	    scaleWidth(s);
	    scaleHeight(s);
	}

	//----------------------------------------------------------
	public void scale(float sX, float sY) {
	    scaleWidth(sX);
	    scaleHeight(sY);
	}

	//----------------------------------------------------------
	public void scaleWidth(float  sX) {
	    width  *= sX;
	}
	//----------------------------------------------------------
	public void scaleHeight(float sY) {
	    height *= sY;
	}

	//----------------------------------------------------------
	public void scaleFromCenter(float s) {
	    scaleFromCenter(s,s);
	}

	//----------------------------------------------------------
	public void scaleFromCenter(float sX, float sY) {

	    if(sX == 1.0f && sX == 1.0f) return; // nothing to do

	    float newWidth  = width  * sX;
	    float newHeight = height * sY;

	    ofPoint center = getCenter();

	    x = center.x - newWidth  / 2.0f;
	    y = center.y - newHeight / 2.0f;

	    width  = newWidth;
	    height = newHeight;
	}

	//----------------------------------------------------------
	public void scaleTo( ofRectangle targetRect, int scaleMode) {

	    if(scaleMode == SCALEMODE_FIT) {
	        scaleTo(targetRect,
	                ASPECT_RATIO_KEEP,
	                ALIGN_HORZ_CENTER,
	                ALIGN_VERT_CENTER);
	    } else if(scaleMode == SCALEMODE_FILL) {
	        scaleTo(targetRect,
	                ASPECT_RATIO_KEEP_BY_EXPANDING,
	                ALIGN_HORZ_CENTER,
	                ALIGN_VERT_CENTER);
	    } else if(scaleMode == SCALEMODE_CENTER) {
	        alignTo(targetRect,
	                ALIGN_HORZ_CENTER,
	                ALIGN_VERT_CENTER);
	    } else if(scaleMode == SCALEMODE_STRETCH_TO_FILL) {
	        scaleTo(targetRect,
	                ASPECT_RATIO_IGNORE,
	                ALIGN_HORZ_CENTER,
	                ALIGN_VERT_CENTER);
	    } else {
	        scaleTo(targetRect,
	                ASPECT_RATIO_KEEP);
	    }
	}

	//----------------------------------------------------------
	public void scaleTo( ofRectangle targetRect,
	                          int subjectAspectRatioMode,
	                          int sharedHorzAnchor,
	                          int sharedVertAnchor) {
	    scaleTo(targetRect,
	            subjectAspectRatioMode,
	            sharedHorzAnchor,
	            sharedVertAnchor,
	            sharedHorzAnchor,
	            sharedVertAnchor);
	}

	//----------------------------------------------------------
	public void scaleTo( ofRectangle targetRect,
	                                 int aspectRatioMode,
	                                 int modelHorzAnchor,
	                                 int modelVertAnchor,
	                                 int thisHorzAnchor,
	                                 int thisVertAnchor) {

	    float tw = targetRect.getWidth();    // target width
	    float th = targetRect.getHeight();   // target height
	    float sw = getWidth();   // subject width
	    float sh = getHeight();  // subject height

	    if(aspectRatioMode == ASPECT_RATIO_KEEP_BY_EXPANDING ||
	       aspectRatioMode == ASPECT_RATIO_KEEP) {
	        if(sw != 0.0f && sh != 0.0f) {
	            float wRatio = Math.abs(tw) / Math.abs(sw);
	            float hRatio = Math.abs(th) / Math.abs(sh);
	            if(aspectRatioMode == ASPECT_RATIO_KEEP_BY_EXPANDING) {
	                scale(Math.max(wRatio,hRatio));
	            } else if(aspectRatioMode == ASPECT_RATIO_KEEP) {
	                scale(Math.min(wRatio,hRatio));
	            }
	        } else {
	            //ofLogWarning("ofRectangle") << "scaleTo(): no scaling applied to avoid divide by zero, rectangle has 0 width and/or height: " << sw << "x" << sh;
	        }
	    } else if(aspectRatioMode == ASPECT_RATIO_IGNORE) {
	        width  = tw;
	        height = th;
	    } else {
	        //ofLogWarning("ofRectangle") << "scaleTo(): unknown ofAspectRatioMode = " << aspectRatioMode << ", using ASPECT_RATIO_IGNORE";
	        width  = tw;
	        height = th;
	    }

	    // now align if anchors are not ignored.
	    alignTo(targetRect,
	            modelHorzAnchor,
	            modelVertAnchor,
	            thisHorzAnchor,
	            thisVertAnchor);

	}

	//----------------------------------------------------------
	public void alignToHorz( float targetX,
	                              int thisHorzAnchor) {

	    if(thisHorzAnchor != ALIGN_HORZ_IGNORE) {
	        translateX(targetX - getHorzAnchor(thisHorzAnchor));
	    } else {
	        //ofLogVerbose("ofRectangle") << "alignToHorz(): thisHorzAnchor == ALIGN_HORZ_IGNORE, no alignment applied";
	    }
	}

	//----------------------------------------------------------
	public void alignToHorz( ofRectangle targetRect,
	                              int sharedAnchor) {

	    alignToHorz(targetRect, sharedAnchor, sharedAnchor);
	}

	//----------------------------------------------------------
	public void alignToHorz( ofRectangle targetRect,
	                              int targetHorzAnchor,
	                              int thisHorzAnchor) {

	    if(targetHorzAnchor != ALIGN_HORZ_IGNORE &&
	       thisHorzAnchor   != ALIGN_HORZ_IGNORE) {
	        alignToHorz(targetRect.getHorzAnchor(targetHorzAnchor),thisHorzAnchor);
	    } else {
	        if(targetHorzAnchor == ALIGN_HORZ_IGNORE) {
	            //ofLogVerbose("ofRectangle") << "alignToHorz(): targetHorzAnchor == ALIGN_HORZ_IGNORE, no alignment applied";
	        } else {
	            //ofLogVerbose("ofRectangle") << "alignToHorz(): thisHorzAnchor == ALIGN_HORZ_IGNORE, no alignment applied";
	        }
	    }

	}

	//----------------------------------------------------------
	public void alignToVert( float targetY,
	                              int thisVertAnchor) {

	    if(thisVertAnchor != ALIGN_VERT_IGNORE) {
	        translateY(targetY - getVertAnchor(thisVertAnchor));
	    } else {
	        //ofLogVerbose("ofRectangle") << "alignToVert(): thisVertAnchor == ALIGN_VERT_IGNORE, no alignment applied";
	    }
	}

	//----------------------------------------------------------
	public void alignToVert( ofRectangle targetRect,
	                              int sharedAnchor) {

	    alignToVert(targetRect,sharedAnchor,sharedAnchor);
	}

	//----------------------------------------------------------
	public void alignToVert( ofRectangle targetRect,
	                              int targetVertAnchor,
	                              int thisVertAnchor) {

	    if(targetVertAnchor != ALIGN_VERT_IGNORE &&
	       thisVertAnchor   != ALIGN_VERT_IGNORE) {
	        alignToVert(targetRect.getVertAnchor(targetVertAnchor),thisVertAnchor);
	    } else {
	        if(targetVertAnchor == ALIGN_VERT_IGNORE) {
	            //ofLogVerbose("ofRectangle") << "alignToVert(): targetVertAnchor == ALIGN_VERT_IGNORE, no alignment applied";
	        } else {
	            //ofLogVerbose("ofRectangle") << "alignToVert(): thisVertAnchor == ALIGN_VERT_IGNORE, no alignment applied";
	        }

	    }
	}

	//----------------------------------------------------------
	public void alignTo( ofPoint targetPoint,
	                          int thisHorzAnchor,
	                          int thisVertAnchor) {

	    alignToHorz(targetPoint.x, thisHorzAnchor);
	    alignToVert(targetPoint.y, thisVertAnchor);
	}


	//----------------------------------------------------------
	public void alignTo( ofRectangle targetRect,
	                          int sharedHorzAnchor,
	                          int sharedVertAnchor) {
	    alignTo(targetRect,
	            sharedHorzAnchor,
	            sharedVertAnchor,
	            sharedHorzAnchor,
	            sharedVertAnchor);
	}

	//----------------------------------------------------------
	public void alignTo( ofRectangle targetRect,
	                                 int targetHorzAnchor,
	                                 int targetVertAnchor,
	                                 int thisHorzAnchor,
	                                 int thisVertAnchor) {

	    alignToHorz(targetRect,targetHorzAnchor,thisHorzAnchor);
	    alignToVert(targetRect,targetVertAnchor,thisVertAnchor);
	}

	//----------------------------------------------------------
	public boolean inside(float px, float py)  {
		return px > getMinX() && py > getMinY() && px < getMaxX() && py < getMaxY();
		
	}

	//----------------------------------------------------------
	public boolean inside( ofPoint p)  {
		return inside(p.x,p.y);
	}

	//----------------------------------------------------------
	public boolean inside( ofRectangle rect)  {
	    return inside(rect.getMinX(),rect.getMinY()) &&
	           inside(rect.getMaxX(),rect.getMaxY());
	}

	//----------------------------------------------------------
	public boolean inside( ofPoint p0,  ofPoint p1)  {
	    // check to see if a line segment is inside the rectangle
	    return inside(p0) && inside(p1);
	}

	//----------------------------------------------------------
	public boolean intersects( ofRectangle rect)  {
	    return (getMinX() < rect.getMaxX() && getMaxX() > rect.getMinX() &&
	            getMinY() < rect.getMaxY() && getMaxY() > rect.getMinY());
	}

//	//----------------------------------------------------------
//	boolean intersects( ofPoint p0,  ofPoint p1)  {
//	    // check for a line intersection
//	    ofPoint p;
//
//	    ofPoint topLeft     = getTopLeft();
//	    ofPoint topRight    = getTopRight();
//	    ofPoint bottomRight = getBottomRight();
//	    ofPoint bottomLeft  = getBottomLeft();
//
//	    return inside(p0) || // check end inside
//	           inside(p1) || // check end inside
//	           ofLineSegmentIntersection(p0, p1, topLeft,     topRight,    p) || // cross top
//	           ofLineSegmentIntersection(p0, p1, topRight,    bottomRight, p) || // cross right
//	           ofLineSegmentIntersection(p0, p1, bottomRight, bottomLeft,  p) || // cross bottom
//	           ofLineSegmentIntersection(p0, p1, bottomLeft,  topLeft,     p);   // cross left
//	}

	//----------------------------------------------------------
	public void growToInclude(ofPoint p) {
	    growToInclude(p.x,p.y);
	}

	//----------------------------------------------------------
	public void growToInclude( float px, float py) {
	    float x0 = Math.min(getMinX(),px);
	    float x1 = Math.max(getMaxX(),px);
	    float y0 = Math.min(getMinY(),py);
	    float y1 = Math.max(getMaxY(),py);
	    float w = x1 - x0;
	    float h = y1 - y0;
	    set(x0,y0,w,h);
	}

	//----------------------------------------------------------
	public void growToInclude( ofRectangle rect) {
	    float x0 = Math.min(getMinX(),rect.getMinX());
	    float x1 = Math.max(getMaxX(),rect.getMaxX());
	    float y0 = Math.min(getMinY(),rect.getMinY());
	    float y1 = Math.max(getMaxY(),rect.getMaxY());
	    float w = x1 - x0;
	    float h = y1 - y0;
	    set(x0,y0,w,h);
	}

	//----------------------------------------------------------
	public void growToInclude( ofPoint p0,  ofPoint p1) {
	    growToInclude(p0);
	    growToInclude(p1);
	}

	//----------------------------------------------------------
	public ofRectangle getIntersection( ofRectangle rect)  {

	    float x0 = Math.max(getMinX(),rect.getMinX());
	    float x1 = Math.min(getMaxX(),rect.getMaxX());

	    float w = x1 - x0;
	    if(w < 0.0f) return new ofRectangle(0,0,0,0); // short circuit if needed

	    float y0 = Math.max(getMinY(),rect.getMinY());
	    float y1 = Math.min(getMaxY(),rect.getMaxY());

	    float h = y1 - y0;
	    if(h < 0.0f) return new ofRectangle(0,0,0,0);  // short circuit if needed

	    return new ofRectangle(x0,y0,w,h);
	}

	//----------------------------------------------------------
	public ofRectangle getUnion( ofRectangle rect)  {
	    ofRectangle united = new ofRectangle(this);
	    united.growToInclude(rect);
	    return united;
	}

	//----------------------------------------------------------
	public void standardize() {
	    if(width < 0.0f) {
	        x += width;
	        width = -1.0f * width;
	    }

	    if(height < 0.0f) {
	        y += height;
	        height = -1.0f * height;
	    }
	}

	//----------------------------------------------------------
	public ofRectangle getStandardized()  {
	    if(isStandardized()) {
	        return this;
	    } else {
	        ofRectangle canRect = new ofRectangle(this); // copy it
	        canRect.standardize();
	        return canRect;
	    }
	}

	//----------------------------------------------------------
	public boolean isStandardized()  {
	    return width >= 0.0f && height >= 0.0f;
	}

	//----------------------------------------------------------
	public float getArea()  {
	    return Math.abs(width) * Math.abs(height);
	}

	//----------------------------------------------------------
	public float getPerimeter()  {
	    return 2.0f * Math.abs(width) + 2.0f * Math.abs(height);
	}

	//----------------------------------------------------------
	public float getAspectRatio()  {
	    return Math.abs(width) / Math.abs(height);
	}

	//----------------------------------------------------------
	public boolean isEmpty()  {
	    return width == 0.0f && height == 0.0f;
	}

	//----------------------------------------------------------
	public ofPoint getMin()  {
	    return new ofPoint(getMinX(),getMinY());
	}

	//----------------------------------------------------------
	public ofPoint getMax()  {
	    return new ofPoint(getMaxX(),getMaxY());
	}

	//----------------------------------------------------------
	public float getMinX()  {
	    return Math.min(x, x + width);  // - width
	}

	//----------------------------------------------------------
	public float getMaxX()  {
	    return Math.max(x, x + width);  // - width
	}

	//----------------------------------------------------------
	public float getMinY() {
	    return Math.min(y, y + height);  // - height
	}

	//----------------------------------------------------------
	public float getMaxY()  {
	    return Math.max(y, y + height);  // - height
	}

	//----------------------------------------------------------
	public float getLeft()  {
	    return getMinX();
	}

	//----------------------------------------------------------
	public float getRight()  {
	    return getMaxX();
	}

	//----------------------------------------------------------
	public float getTop()  {
	    return getMinY();
	}

	//----------------------------------------------------------
	public float getBottom()  {
	    return getMaxY();
	}

	//----------------------------------------------------------
	public ofPoint getTopLeft()  {
	    return getMin();
	}

	//----------------------------------------------------------
	public ofPoint getTopRight()  {
	    return new ofPoint(getRight(),getTop());
	}

	//----------------------------------------------------------
	public ofPoint getBottomLeft()  {
	    return new ofPoint(getLeft(),getBottom());
	}

	//----------------------------------------------------------
	public ofPoint getBottomRight()  {
	    return getMax();
	}

	//----------------------------------------------------------
	public float getHorzAnchor(int anchor)  {
	    switch (anchor) {
	        case ALIGN_HORZ_IGNORE:
	            //ofLogError("ofRectangle") << "getHorzAnchor(): unable to get anchor for ALIGN_HORZ_IGNORE, returning 0";
	            return 0.0f;
	        case ALIGN_HORZ_LEFT:
	            return getLeft();
	        case ALIGN_HORZ_RIGHT:
	            return getRight();
	        case ALIGN_HORZ_CENTER:
	            return getCenter().x;
	        default:
	            //ofLogError("ofRectangle") << "getHorzAnchor(): unknown int = " << anchor << ", returning 0.0";
	            return 0.0f;
	    }
	}

	//----------------------------------------------------------
	public float getVertAnchor(int anchor)  {
	    switch (anchor) {
	        case ALIGN_VERT_IGNORE:
	            //ofLogError("ofRectangle") << "getVertAnchor(): unable to get anchor for ALIGN_VERT_IGNORE, returning 0.0";
	            return 0.0f;
	        case ALIGN_VERT_TOP:
	            return getTop();
	        case ALIGN_VERT_BOTTOM:
	            return getBottom();
	        case ALIGN_VERT_CENTER:
	            return getCenter().y;
	        default:
	            //ofLogError("ofRectangle") << "getVertAnchor(): unknown int = " << anchor << ", returning 0.0";
	            return 0.0f;
	    }
	}

	

	//----------------------------------------------------------
	public  ofPoint getPosition()  {
	    return new ofPoint(x, y);
	}


	//----------------------------------------------------------
	public ofPoint getCenter()  {
		return new ofPoint(x + width * 0.5f, y + height * 0.5f);
	}

	//----------------------------------------------------------
	public float getX()  {
	    return x;
	}

	//----------------------------------------------------------
	public float getY()  {
	    return y;
	}

	//----------------------------------------------------------
	public float getWidth()  {
	    return width;
	}

	//----------------------------------------------------------
	public float getHeight()  {
	    return height;
	}

//	//----------------------------------------------------------
//	ofRectangle operator = ( ofRectangle rect) {
//	    set(rect);
//		return *this;
//	}
//
//	//----------------------------------------------------------
//	ofRectangle operator + ( ofPoint & point){
//		ofRectangle rect=*this;
//		rect.x += point.x;
//		rect.y += point.y;
//		return rect;
//	}
//
//	//----------------------------------------------------------
//	ofRectangle operator - ( ofPoint & point){
//		ofRectangle rect=*this;
//		rect.x -= point.x;
//		rect.y -= point.y;
//		return rect;
//	}

	//----------------------------------------------------------
	public boolean equals( ofRectangle rect)  {
		return (x == rect.x) && (y == rect.y) && (width == rect.width) && (height == rect.height);
	}

	//----------------------------------------------------------
	public boolean isZero() {
		return (x == 0) && (y == 0) && (width == 0) && (height == 0);
	}


}
