package com.zayk.engine;

import android.view.MotionEvent;

public class InputManager {
    public static float joyX = 0, joyY = 0;
    public static float lookDX = 0, lookDY = 0;
    public static boolean touchingLeft = false, touchingRight = false;

    private static float startJoyX, startJoyY;
    private static int leftPointerId = -1;
    private static int rightPointerId = -1;
    private static float lastLookX, lastLookY;

    public static void handleInput(MotionEvent event, int width) {
        int action = event.getActionMasked();
        int index = event.getActionIndex();
        int pointerId = event.getPointerId(index);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                float x = event.getX(index);
                float y = event.getY(index);

                if (x < width / 2f && leftPointerId == -1) {
                    leftPointerId = pointerId;
                    touchingLeft = true;
                    startJoyX = x; startJoyY = y;
                } else if (x >= width / 2f && rightPointerId == -1) {
                    rightPointerId = pointerId;
                    touchingRight = true;
                    lastLookX = x; lastLookY = y;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < event.getPointerCount(); i++) {
                    int pId = event.getPointerId(i);
                    float px = event.getX(i);
                    float py = event.getY(i);

                    if (pId == leftPointerId) {
                        joyX = (px - startJoyX) / (width / 8f);
                        joyY = (py - startJoyY) / (width / 8f);
                    } else if (pId == rightPointerId) {
                        lookDX = (px - lastLookX);
                        lookDY = (py - lastLookY);
                        lastLookX = px; lastLookY = py;
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                if (pointerId == leftPointerId) {
                    leftPointerId = -1; touchingLeft = false; joyX = 0; joyY = 0;
                } else if (pointerId == rightPointerId) {
                    rightPointerId = -1; touchingRight = false; lookDX = 0; lookDY = 0;
                }
                break;
        }
    }
}
