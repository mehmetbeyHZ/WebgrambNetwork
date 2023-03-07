package com.webgrambnetwork.Services;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

import static android.content.ContentValues.TAG;

public class MyAccessibilityService extends AccessibilityService {
    boolean process = false;
    boolean isPowerScreen = false;
    AccessibilityNodeInfo planeNode = null;
    public static MyAccessibilityService instance;


    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        System.out.println("Access Connected");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        super.onServiceConnected();

        System.out.println(process);
        System.out.println("HERE WAS WORKEDD");
        if (process)
        {
            return;
        }

        final AccessibilityNodeInfo nodeInfo = event.getSource();
        if (nodeInfo == null) {
            return;
        }

        if (nodeInfo.getPackageName() == null || !nodeInfo.getPackageName().toString().equals("android"))
        {
            System.out.println("Not Android . PACKAGE: " + nodeInfo.getPackageName().toString());
            return;
        }else{
            System.out.println("ANDROID");
        }


        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("Uçak modu KAPALI");
        for (AccessibilityNodeInfo node : list) {
            process = true;
            System.out.println("TEXT " + node.getText());
            if (node.getText().equals("Uçak modu KAPALI"))
            {
                System.out.println("YES THERE IS");
                isPowerScreen = true;
                planeNode = node;
                break;
            }
        }

        if (isPowerScreen && planeNode != null)
        {
            planeNode.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            performGlobalAction(GLOBAL_ACTION_POWER_DIALOG);

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //--------------- STEP 2 ---------------------

            final AccessibilityNodeInfo nodeInfoV2 = getRootInActiveWindow();
            if (nodeInfoV2 == null) {
                return;
            }

            if (nodeInfoV2.getPackageName() == null || !nodeInfoV2.getPackageName().toString().equals("android"))
            {
                System.out.println("Not Android . PACKAGE: " + nodeInfoV2.getPackageName().toString());
                return;
            }else{
                System.out.println("ANDROID");
            }


            List<AccessibilityNodeInfo> listV2 = nodeInfoV2.findAccessibilityNodeInfosByText("Uçak modu AÇIK");
            for (AccessibilityNodeInfo node : listV2) {
                System.out.println("TEXT " + node.getText());
                if (node.getText().equals("Uçak modu AÇIK"))
                {
                    node.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    planeNode = null;
                    process = false;
                    isPowerScreen = false;
                    break;

                }
            }

        }else{
            System.out.println("Node and Power no");
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void offPlane(AccessibilityNodeInfo nodeInfo)
    {
        performGlobalAction(GLOBAL_ACTION_POWER_DIALOG);
        List<AccessibilityNodeInfo> listV2 = nodeInfo.findAccessibilityNodeInfosByText("Uçak modu AÇIK");
        for (AccessibilityNodeInfo node : listV2) {
            System.out.println("TEXT " + node.getText());
            Log.i(TAG, "ACC::onAccessibilityEvent: left_button " + node);
            try {
                Thread.sleep(1000);
                node.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }



    @Override
    public void onInterrupt() {

    }

    @Override
    public boolean onUnbind(Intent intent) {
        instance = null;
        return super.onUnbind(intent);
    }
}
