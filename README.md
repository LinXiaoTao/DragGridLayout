### 效果

![效果图](http://oy017242u.bkt.clouddn.com/DragGridLayout.gif)

### 背景

在之前的项目中为了实现类似微信朋友圈的图片九宫格效果，手写了一个简单的网格控件，继承于 ViewGroup，后面因为准备做小组的技术分享，关于自定义控件的，所以就想把这个控件优化下，来作为这次分享的例子。自定义控件一般涉及测量、布局、绘制三大流程，再加上触摸事件的处理。之前的已完成的部分，已经包括了测量和布局，绘制暂时没有好想法，所以这次就想再加上触摸事件的处理。在使用微信发布朋友圈的时候，看到可以通过拖拽来重新排序图片，所以这次就加上可拖拽的功能。

### 思路

因为网格控件是位置是比较固定的，所以可以通过实时计算每个 Item 的位置，这里我们为了方便，所以缓存了每个 Item 的位置。当我们拖拽某个 Item 靠近新的位置时，如果新位置大于原来的位置，我们将原来位置到新位置的 Item 向后移动一位，反之，则向前移动一位。不知道这样解释是否清晰。

### 实现

**[源码](https://github.com/LinXiaoTao/DragGridLayout)**

首先我们要保存初始布局时的位置信息，这里我们使用 `SparseArray<Point>` 去缓存，相关代码如下：

``` java
int childTop = getPaddingTop();                                                                
int childLeft = getPaddingLeft();                                                              
                                                                                               
for (int i = 0; i < getChildCount(); i++) {                                                    
    final View childView = getChildAt(i);                                                      
    Point point = mLayoutPositionArray.get(i);                                                 
                                                                                               
    if(point == null){                                                                         
        point = acquireTempPoint();                                                            
    }                                                                                          
                                                                                               
    point.x = childLeft;                                                                       
    point.y = childTop;                                                                        
    mLayoutPositionArray.put(i, point);                                                        
                                                                                               
    childView.layout(childLeft, childTop, childLeft + itemWidth, childTop + itemHeight);       
                                                                                               
    if (mGridAdapter != null) {                                                                
        mGridAdapter.handleItemView(childView, i);                                             
    }                                                                                          
    childLeft += itemWidth + mHGap;                                                            
    if ((i + 1) % mColumnCount == 0) {                                                         
        childTop += itemHeight + mVGap;                                                        
        childLeft = getPaddingLeft();                                                          
    }                                                                                          
}                                                                                                                               
```

接下来是 Item 的拖拽移动处理，同样的我们可以通过手动处理 `onTouchEvent()` 和 `onInterceptTouchEvent()` 去实现，为了节约时间，我们使用 `ViewDragHelper` 去实现，关于 `ViewDragHelper` 的使用，这里我们就不去细讲，可以参考这篇[博客](https://blog.csdn.net/lmj623565791/article/details/46858663)，关于触发拖动操作，这里我们使用长按开始拖动，同样的，长按等手势判定，我们交给  `GestureDetector` 去处理，下面是两个类的集成代码：

``` java
@Override                                                                   
public boolean onInterceptTouchEvent(MotionEvent ev) {                      
    boolean intercept = mDragHelper.shouldInterceptTouchEvent(ev);          
    if (mDragHelper.getViewDragState() == ViewDragHelper.STATE_DRAGGING) {  
        if (getParent() != null) {
            // 这里需要禁止父视图拦截事件，防止父视图为滚动控件，发生滑动冲突
            getParent().requestDisallowInterceptTouchEvent(true);           
        }                                                                   
    }                                                                       
    return intercept;                                                       
}                                                                           
                                                                            
@SuppressLint("ClickableViewAccessibility")                                 
@Override                                                                   
public boolean onTouchEvent(MotionEvent event) {                            
    mDragHelper.processTouchEvent(event);                                   
    if (mDragHelper.getViewDragState() == ViewDragHelper.STATE_DRAGGING) {  
        if (getParent() != null) {
            // 这里需要禁止父视图拦截事件，防止父视图为滚动控件，发生滑动冲突
            getParent().requestDisallowInterceptTouchEvent(true);           
        }                                                                   
    }                                                                       
    // 手势判断                                                                 
    mGestureDetector.onTouchEvent(event);                                   
    return true;                                                            
}                                                                           
```

这时候，我们的长按开始拖拽已经实现了，接下来，当我们拖拽时，如何让目标位置的子视图移动出位置呢？在上面，我们已经提到了思路，即将部分视图进行整体移动去实现。我们通过代码来看下实现思路：

``` java
@Override                                                                                                                                          
public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
    // 当子视图拖拽移动时，ViewDragHelper 会回调这个方法返回新的位置
    super.onViewPositionChanged(changedView, left, top, dx, dy);                                                                                   
    Log.d(TAG,"dx: " + dx + "，dy: " + dy);
    // findFirstContactView 拖拽到新的目标位置
    int targetPosition = findFirstContactView(left, top, left + changedView.getWidth(), top + changedView.getHeight(), changedView);               
    int sourcePosition = indexOfChild(changedView);                                                                                                
    if (targetPosition == -1 || sourcePosition == targetPosition) {                                                                                
        if (targetPosition == -1) {                                                                                                                
            Log.d(TAG, "targetPosition == -1");                                                                                                    
        }                                                                                                                                          
        if (sourcePosition == targetPosition) {                                                                                                    
            Log.w(TAG, "sourcePosition == targetPosition");                                                                                        
        }                                                                                                                                          
        return;                                                                                                                                    
    }                                                                                                                                              
    if (mAnimatorCountDown.get() > 0) {
        // 上一次整体移动的动画还没结束
        return;                                                                                                                                    
    }                                                                                                                                              
    Log.d(TAG, "sourcePositon: " + sourcePosition + " ,targetPosition: " + targetPosition);                                                        
    // 移动位置                                                                                                                                        
    if (targetPosition < sourcePosition && (dx < 0 || dy < 0)) {
        // 当前目标位置小于拖拽视图的原位置，同时手势为向左或者向上
        mTargetPosition = targetPosition;                                                                                                          
        for (int i = targetPosition; i < sourcePosition; i++) {                                                                                    
            moveAnimation(getChildAt(i), mLayoutPositionArray.get(i + 1), i + 1, null);                                                            
        }                                                                                                                                          
    } else if (targetPosition > sourcePosition && (dx > 0 || dy > 0)) {
        // 当前目标位置大于拖拽视图的原位置，同时手势为向右或者向下
        mTargetPosition = targetPosition;                                                                                                          
        for (int i = sourcePosition + 1; i < (targetPosition + 1); i++) {                                                                          
            moveAnimation(getChildAt(i), mLayoutPositionArray.get(i - 1), i - 1, null);                                                            
        }                                                                                                                                          
    }                                                                                                                                              
}                                                                                                                                                  
```

通过上面的代码和注释，实现的思路应该很清晰了。接下来，我们再看下 `findFirstContactView` 这个方法，这是获取拖拽视图移动目标位置，代码如下：

``` java
private int findFirstContactView(int left, int top, int right, int bottom, View excludeView) {                          
    final int childCount = getChildCount();                                                                             
    for (int i = childCount - 1; i >= 0; i--) {
        // 这里我们遍历子视图
        final View child = getChildAt(i);                                                                               
        if (child == excludeView) {                                                                                     
            continue;                                                                                                   
        }                                                                                                               
        if (left < child.getRight() && child.getLeft() < right && top < child.getBottom() && child.getTop() < bottom) { 
            // 是否存在相交的子视图                                                                                                       
            int calculateLeft = left;                                                                                   
            int calculateRight = right;                                                                                 
            int caluculateTop = top;                                                                                    
            int caluculateBottom = bottom;                                                                              
            if (child.getLeft() > calculateLeft){                                                                       
                calculateLeft = child.getLeft();                                                                        
            }                                                                                                           
            if (child.getRight() < calculateRight){                                                                     
                calculateRight = child.getRight();                                                                      
            }                                                                                                           
            if (child.getTop() > caluculateTop){                                                                        
                caluculateTop = child.getTop();                                                                         
            }                                                                                                           
            if (child.getBottom() < caluculateBottom){                                                                  
                caluculateBottom = child.getBottom();                                                                   
            }                                                                                                           
            int unionArea = (calculateRight - calculateLeft) * (caluculateBottom - caluculateTop);                      
            if (unionArea > (child.getWidth() * child.getHeight() * 0.5f)){
                // 这里我们再通过 相交面积是否大于视图面积的一半 去判定
                // 这是为了不那么敏感
                return i;                                                                                               
            }                                                                                                           
        }                                                                                                               
    }                                                                                                                   
    return -1;                                                                                                          
}                                                                                                                       
```

经过上面的步骤，我们已经实现了百分之八十的代码，这时，我们还需要考虑几个问题。

* 位置交换后，我们通过 `indexOfChild` 或 `getChildAt` 得到的数据还是原来的，即我们通过调用 `addView` 添加的顺序。这里我们自己去缓存一个子视图列表，重写上面两个方法：

  ``` java
  @Override                             
  public int indexOfChild(View child) { 
      return mChildViews.indexOf(child);
  }                                     
                                        
  @Override                             
  public View getChildAt(int index) {   
      return mChildViews.get(index);    
  }                                     
  ```

  同时在视图移动时，去修改这个列表：

  ``` java
  mChildViews.set(targetPosition, targetView); 
  ```

* 绘制顺序问题，ViewGroup 的默认绘制顺序是根据 `addView`  的调用顺序，这样会带来一个问题，当你拖拽到比当前拖拽视图更晚添加的子视图时，会显示在这个视图下面，同样我们可以通过 `setChildrenDrawingOrderEnabled` 和 

  `getChildDrawingOrder` 去自定义绘制顺序，这里为了方便，我们通过设置拖拽视图的 Z 值来实现同样的效果：

  ``` java
  ViewCompat.setZ(capturedChild, 100f);     
  postInvalidate();                         
  ```

最后我们顺便把 Item 的点击事件实现下。。。

``` java
 @Override                                                                                 
 public boolean onSingleTapConfirmed(MotionEvent e) {                                      
     final View targetView = mDragHelper.findTopChildUnder((int) e.getX(), (int) e.getY());
     if (targetView == null) {                                                             
         return performClick();                                                            
     } else if (mGridItemClickListener != null) {                                          
         mGridItemClickListener.onClickItem(indexOfChild(targetView), targetView);         
         return true;                                                                      
     }                                                                                     
     return false;                                                                         
 }                                                                                         
```



