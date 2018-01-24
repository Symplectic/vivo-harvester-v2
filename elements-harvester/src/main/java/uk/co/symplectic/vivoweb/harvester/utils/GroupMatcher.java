/*
 * ******************************************************************************
 *   Copyright (c) 2017 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 *   Version :  ${git.branch}:${git.commit.id}
 * ******************************************************************************
 */

package uk.co.symplectic.vivoweb.harvester.utils;

import uk.co.symplectic.vivoweb.harvester.model.ElementsGroupInfo;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemInfo;

import java.util.*;
import java.util.regex.Pattern;

public class GroupMatcher {
    private final List<Pattern> matchingPatterns = new ArrayList<Pattern>();
    private final Set<ElementsItemId.GroupId> matchingIds = new HashSet<ElementsItemId.GroupId>();

    public GroupMatcher(Collection<ElementsItemId.GroupId> matchIds, Set<String> matchPatterns){
        if(matchIds != null && matchIds.size() > 0) this.matchingIds.addAll(matchIds);
        if(matchPatterns != null){
            for(String aPatternString : matchPatterns){
                this.matchingPatterns.add(Pattern.compile(aPatternString, Pattern.CASE_INSENSITIVE));
            }
        }
    }

    public boolean isActive(){
        if(matchingIds.size() > 0  || matchingPatterns.size() > 0) return true;
        return false;
    }

    public boolean isMatch(ElementsItemInfo itemInfo){
        if(itemInfo.isGroupInfo()) {
            ElementsGroupInfo groupInfo = itemInfo.asGroupInfo();
            if (matchingIds.contains(groupInfo.getItemId())) return true;
            for (Pattern pattern : matchingPatterns) {
                if (pattern.matcher(groupInfo.getName()).matches()) return true;
            }
        }
        return false;
    }
}
