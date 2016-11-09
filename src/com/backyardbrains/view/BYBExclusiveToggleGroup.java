package com.backyardbrains.view;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by roy on 08-11-16.
 */

public class BYBExclusiveToggleGroup {
    public List<BYBToggleButton> group = null;
    public BYBExclusiveToggleGroup(){}

    public void addToGroup(BYBToggleButton b){
        if(group == null){
            group = new ArrayList<>();
            b.setActive(true);
        }
        group.add(b);
        b.setGroup(this);
    }
    public void updateGroup(BYBToggleButton b){
        if(b.isActive()){
            for (BYBToggleButton t:group) {
                if(t != b){
                    t.setActive(false);
                }
            }
        }
    }
}
